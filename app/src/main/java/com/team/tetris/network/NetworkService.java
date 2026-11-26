package com.team.tetris.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 1:1 P2P TCP 연결 담당.
 * 서버(호스트)는 openHost로 한 명의 접속을 기다리고, 클라이언트는 connect로 접속한다.
 *
 * 스레드 구성 (연결 1개당)
 *  - 읽기 스레드: 받은 메시지를 순서대로 리스너에 전달
 *  - 쓰기 스레드: 송신 큐를 순서대로 전송. send()는 큐에 넣기만 하므로 엔진·UI 스레드를 막지 않음
 *  - 하트비트(공용): 주기적으로 PING을 보내 왕복 지연을 재고, 응답이 없으면 끊김 처리
 *
 * 송신 순서 규칙
 *  - 공격·게임오버·나가기 등 이벤트: 빠짐없이 보낸 순서대로 전송
 *  - SNAPSHOT: 화면 표시용이라 아직 못 보낸 이전 상태는 버리고 최신 상태만 전송
 */
public class NetworkService {

    /** 기본 포트 */
    public static final int DEFAULT_PORT = 7777;

    /**
     * 지연·끊김 기준.
     * @param connectTimeoutMillis    접속 시도 제한 시간
     * @param heartbeatIntervalMillis PING 주기
     * @param lagThresholdMillis      왕복 지연이 이 값을 넘으면 랙 상태
     * @param disconnectTimeoutMillis 이 시간 동안 아무것도 받지 못하면 연결 끊김
     * @param connectRetryIntervalMillis 서버를 못 찾으면 이 간격으로 다시 접속 시도
     * @param connectGiveUpMillis     이 시간 동안 서버를 못 찾으면 포기 (0이면 한 번만 시도)
     */
    public record Timing(
            long connectTimeoutMillis,
            long heartbeatIntervalMillis,
            long lagThresholdMillis,
            long disconnectTimeoutMillis,
            long connectRetryIntervalMillis,
            long connectGiveUpMillis
    ) {
        /** 서버를 못 찾을 때 다시 시도하는 간격 (회의 7차 문제 7: 3초마다) */
        public static final long DEFAULT_CONNECT_RETRY_MILLIS = 3000;
        /** 서버를 못 찾을 때 포기하기까지의 시간 (회의 7차 문제 7: 30초) */
        public static final long DEFAULT_CONNECT_GIVE_UP_MILLIS = 30_000;

        /**
         * 기본값.
         * 요구사항의 "키 입력~화면 표시 200ms 이하"에 맞춰 왕복 200ms 초과를 랙으로 본다.
         * 일시적인 지연으로 대전이 끊기지 않도록 5초간 응답이 없을 때만 끊김으로 본다.
         * 클라이언트가 서버보다 먼저 접속해도 3초마다 다시 시도하고, 30초 동안 못 찾으면 포기한다.
         */
        public static final Timing DEFAULT = new Timing(5000, 250, 200, 5000,
                DEFAULT_CONNECT_RETRY_MILLIS, DEFAULT_CONNECT_GIVE_UP_MILLIS);

        public Timing {
            if (connectTimeoutMillis <= 0 || heartbeatIntervalMillis <= 0
                    || lagThresholdMillis <= 0 || disconnectTimeoutMillis <= heartbeatIntervalMillis
                    || connectRetryIntervalMillis <= 0 || connectGiveUpMillis < 0) {
                throw new IllegalArgumentException("invalid timing");
            }
        }

        /** 재시도 없이 한 번만 접속 시도 */
        public Timing(long connectTimeoutMillis, long heartbeatIntervalMillis,
                      long lagThresholdMillis, long disconnectTimeoutMillis) {
            this(connectTimeoutMillis, heartbeatIntervalMillis, lagThresholdMillis, disconnectTimeoutMillis,
                    DEFAULT_CONNECT_RETRY_MILLIS, 0);
        }
    }

    // disconnect() 후 상대가 연결을 닫기를 기다리는 최대 시간
    private static final long CLOSE_GRACE_MILLIS = 1000;
    // 쓰기 스레드에 보내는 신호
    private static final Object SNAPSHOT_READY = new Object();
    private static final Object CLOSE = new Object();

    private static final NetworkListener NO_LISTENER = new NetworkListener() {
        @Override
        public void onConnected() {
        }

        @Override
        public void onMessage(NetworkMessage message) {
        }

        @Override
        public void onDisconnected() {
        }

        @Override
        public void onError(String message) {
        }
    };

    private final Timing timing;
    private final MessageCodec codec = new MessageCodec();
    private final ScheduledExecutorService scheduler;

    private final Object lock = new Object();
    // openHost·connect·disconnect마다 증가. 취소된 접속 시도가 나중에 연결을 덮어쓰지 않게 함
    private final AtomicLong generation = new AtomicLong();
    private volatile NetworkListener listener = NO_LISTENER;
    private ServerSocket serverSocket;      // lock으로 보호
    private boolean opening;                // 접속 대기·시도 중. lock으로 보호
    private volatile Connection connection;

    public NetworkService() {
        this(Timing.DEFAULT);
    }

    public NetworkService(Timing timing) {
        this.timing = Objects.requireNonNull(timing, "timing must not be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "p2p-heartbeat"));
    }

    /**
     * 상대 접속 대기. UI 스레드를 막지 않음.
     * 포트를 바로 열고(실패하면 onError), 접속은 별도 스레드에서 기다린다. 한 명이 접속하면 대기 종료.
     * port가 0이면 비어 있는 포트를 사용 (getLocalPort()로 확인).
     */
    public void openHost(int port) {
        checkPort(port);
        long id;
        ServerSocket server;
        String error = null;
        synchronized (lock) {
            ensureIdle();
            id = generation.incrementAndGet();
            try {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(port), 1);
                serverSocket = server;
                opening = true;
            } catch (IOException e) {
                server = null;
                error = "포트 " + port + "를 열 수 없습니다: " + e.getMessage();
            }
        }
        if (server == null) {
            // 리스너 호출은 lock 밖에서 (리스너가 다른 lock을 잡아도 교착되지 않게)
            listener.onError(error);
            return;
        }
        ServerSocket opened = server;
        daemon(() -> acceptLoop(id, opened), "p2p-accept").start();
    }

    /** 상대 호스트 접속. UI 스레드를 막지 않음. 실패하면 onError */
    public void connect(String host, int port) {
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        checkPort(port);
        if (port == 0) {
            throw new IllegalArgumentException("port must not be 0");
        }
        long id;
        synchronized (lock) {
            ensureIdle();
            id = generation.incrementAndGet();
            opening = true;
        }
        daemon(() -> connectTo(id, host.trim(), port), "p2p-connect").start();
    }

    /**
     * 메시지를 송신 큐에 추가. 실제 송신은 통신 스레드에서 처리.
     * 연결되어 있지 않으면 버린다. (끊김은 onDisconnected로 따로 알림)
     */
    public void send(NetworkMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        if (message.getType().isControl()) {
            throw new IllegalArgumentException(message.getType() + " is reserved for NetworkService");
        }
        Connection current = connection;
        if (current != null) {
            current.enqueue(message);
        }
    }

    /** 연결·수신·끊김·오류 콜백 등록. null이면 해제 */
    public void setListener(NetworkListener listener) {
        this.listener = listener == null ? NO_LISTENER : listener;
    }

    /** 연결 여부 반환 */
    public boolean isConnected() {
        Connection current = connection;
        return current != null && !current.closed.get();
    }

    /** 접속 대기·접속 시도 중인지 */
    public boolean isOpening() {
        synchronized (lock) {
            return opening;
        }
    }

    /**
     * 소켓·통신 스레드·송수신 자원 정리.
     * 이미 큐에 넣은 메시지(예: LEAVE)는 마저 보낸 뒤 닫는다.
     * 직접 호출한 종료이므로 onDisconnected는 호출하지 않음. 여러 번 호출해도 안전.
     */
    public void disconnect() {
        Connection current;
        synchronized (lock) {
            generation.incrementAndGet();
            opening = false;
            closeQuietly(serverSocket);
            serverSocket = null;
            current = connection;
            connection = null;
        }
        if (current != null) {
            current.closeGracefully();
        }
    }

    /** 앱 종료 시 호출. 연결을 끊고 하트비트 스레드도 정리 */
    public void shutdown() {
        disconnect();
        scheduler.shutdownNow();
    }

    /** 대기 중인 서버 포트. 대기 중이 아니면 -1 */
    public int getLocalPort() {
        synchronized (lock) {
            return serverSocket == null ? -1 : serverSocket.getLocalPort();
        }
    }

    /** 연결된 상대 주소. 연결되어 있지 않으면 null */
    public String getRemoteAddress() {
        Connection current = connection;
        return current == null ? null : current.socket.getInetAddress().getHostAddress();
    }

    /** 마지막으로 측정한 왕복 지연(ms). 측정 전이거나 연결이 없으면 -1 */
    public long getRoundTripMillis() {
        Connection current = connection;
        return current == null ? -1 : current.roundTripMillis;
    }

    /** 랙 상태 여부 */
    public boolean isLagging() {
        Connection current = connection;
        return current != null && current.lagging;
    }

    /**
     * 서버 화면에 표시할 이 PC의 IPv4 주소 목록 (IP만).
     * {@link #getLocalAddressDetails()}와 같은 순서이며, 첫 번째가 접속에 쓸 가능성이 가장 높은 주소.
     */
    public static List<String> getLocalAddresses() {
        List<String> ips = new ArrayList<>();
        for (HostAddress address : getLocalAddressDetails()) {
            ips.add(address.ip());
        }
        return ips;
    }

    /**
     * 서버 화면에 표시할 이 PC의 IPv4 주소와 어댑터 이름 목록.
     * VirtualBox 등 가상 어댑터가 있으면 주소가 여러 개 나오므로 어댑터 이름을 함께 돌려준다.
     * 순서: 실제 어댑터의 사설 주소 → 실제 어댑터의 기타 주소 → 가상 어댑터 주소.
     * 찾지 못하면 루프백 주소 하나.
     */
    public static List<HostAddress> getLocalAddressDetails() {
        List<HostAddress> found = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                String adapterName = nic.getDisplayName() == null || nic.getDisplayName().isBlank()
                        ? nic.getName() : nic.getDisplayName();
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLinkLocalAddress()) {
                        found.add(new HostAddress(address.getHostAddress(), adapterName));
                    }
                }
            }
        } catch (SocketException e) {
            // 목록을 못 읽으면 루프백만 반환
        }
        if (found.isEmpty()) {
            found.add(new HostAddress(InetAddress.getLoopbackAddress().getHostAddress(), "Loopback"));
            return found;
        }
        return orderForDisplay(found);
    }

    /** 접속에 쓸 가능성이 높은 주소가 앞에 오도록 정렬 (같은 등급끼리는 원래 순서 유지) */
    static List<HostAddress> orderForDisplay(List<HostAddress> addresses) {
        List<HostAddress> ordered = new ArrayList<>(addresses);
        ordered.sort(Comparator.comparingInt(NetworkService::displayRank));
        return ordered;
    }

    private static int displayRank(HostAddress address) {
        if (address.isVirtualAdapter()) {
            return 2;
        }
        return isSiteLocal(address.ip()) ? 0 : 1;
    }

    private static boolean isSiteLocal(String ip) {
        try {
            return InetAddress.getByName(ip).isSiteLocalAddress();   // IP 문자열이므로 DNS 조회 없음
        } catch (IOException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- 연결 수립

    private void acceptLoop(long id, ServerSocket server) {
        try {
            Socket socket = server.accept();
            synchronized (lock) {
                closeQuietly(server);
                if (serverSocket == server) {
                    serverSocket = null;
                }
            }
            install(id, socket);
        } catch (IOException e) {
            // disconnect()로 대기를 취소하면 accept가 예외로 끝남 → 알리지 않음
            if (generation.get() == id) {
                finishOpening(id);
                listener.onError("접속 대기 중 오류: " + e.getMessage());
            }
        }
    }

    /**
     * 접속 시도. 서버가 아직 열리지 않았으면(클라이언트가 먼저 들어온 경우 등)
     * connectRetryIntervalMillis마다 다시 시도하고, connectGiveUpMillis가 지나면 포기한다.
     * 그 사이 disconnect()하면 조용히 멈춘다.
     */
    private void connectTo(long id, String host, int port) {
        long retryNanos = TimeUnit.MILLISECONDS.toNanos(timing.connectRetryIntervalMillis());
        long giveUpAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timing.connectGiveUpMillis());
        int attempt = 0;
        String lastError = null;
        while (generation.get() == id) {
            attempt++;
            long attemptStart = System.nanoTime();
            // 한 번의 시도가 재시도 간격을 넘지 않게 (응답 없는 주소에서 3초마다 시도하도록)
            long limit = timing.connectGiveUpMillis() == 0
                    ? timing.connectTimeoutMillis()
                    : Math.min(timing.connectTimeoutMillis(), timing.connectRetryIntervalMillis());
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), (int) Math.max(1, limit));
                install(id, socket);
                return;
            } catch (IOException e) {
                closeQuietly(socket);
                lastError = e.getMessage();
            }
            long nextAttemptAt = attemptStart + retryNanos;
            if (generation.get() != id || nextAttemptAt - giveUpAt >= 0) {
                break;
            }
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(giveUpAt - System.nanoTime());
            listener.onConnectRetry(attempt, Math.max(0, remainingMillis));
            if (!sleepUntil(id, nextAttemptAt)) {
                return;
            }
        }
        if (generation.get() == id) {
            finishOpening(id);
            String target = "서버(" + host + ":" + port + ")";
            listener.onConnectFailed(timing.connectGiveUpMillis() == 0
                    ? target + "에 연결할 수 없습니다: " + lastError
                    : target + "를 찾을 수 없습니다. "
                            + TimeUnit.MILLISECONDS.toSeconds(timing.connectGiveUpMillis())
                            + "초 동안 응답이 없습니다. IP 주소를 다시 확인해주세요.");
        }
    }

    /** 다음 시도 시각까지 대기. 그 사이 disconnect()되면 false */
    private boolean sleepUntil(long id, long deadlineNanos) {
        while (generation.get() == id) {
            long left = deadlineNanos - System.nanoTime();
            if (left <= 0) {
                return true;
            }
            try {
                Thread.sleep(Math.max(1, Math.min(50, TimeUnit.NANOSECONDS.toMillis(left))));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void install(long id, Socket socket) throws IOException {
        socket.setTcpNoDelay(true);   // 작은 메시지를 모으지 않고 바로 보내 지연 감소
        socket.setKeepAlive(true);
        Connection created = new Connection(socket);
        synchronized (lock) {
            if (generation.get() != id) {
                // 그 사이에 disconnect()됨
                closeQuietly(socket);
                return;
            }
            opening = false;
            connection = created;
        }
        // 연결 통지가 첫 메시지보다 먼저 가도록 통지 후 읽기 시작 (통지 중 send()한 메시지는 큐에 쌓임)
        listener.onConnected();
        created.start();
    }

    private void finishOpening(long id) {
        synchronized (lock) {
            if (generation.get() == id) {
                opening = false;
                closeQuietly(serverSocket);
                serverSocket = null;
            }
        }
    }

    private void ensureIdle() {
        if (opening || connection != null) {
            throw new IllegalStateException("already connected or connecting");
        }
    }

    // ---------------------------------------------------------------- 연결 1개

    private final class Connection {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final BlockingQueue<Object> outbox = new LinkedBlockingQueue<>();
        final AtomicReference<NetworkMessage> pendingSnapshot = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        volatile long lastReceivedNanos = System.nanoTime();
        volatile long roundTripMillis = -1;
        volatile boolean lagging;
        long nextSequence = 1;     // 쓰기 스레드 전용
        ScheduledFuture<?> heartbeat;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }

        void start() {
            daemon(this::readLoop, "p2p-reader").start();
            daemon(this::writeLoop, "p2p-writer").start();
            long interval = timing.heartbeatIntervalMillis();
            heartbeat = scheduler.scheduleAtFixedRate(this::beat, 0, interval, TimeUnit.MILLISECONDS);
            if (closed.get()) {
                heartbeat.cancel(false);   // onConnected 안에서 바로 disconnect()한 경우
            }
        }

        void enqueue(NetworkMessage message) {
            if (closed.get()) {
                return;
            }
            if (message.getType() == MessageType.SNAPSHOT) {
                // 이미 대기 중인 스냅샷이 있으면 내용만 최신으로 바꾸고 신호는 하나만 유지
                if (pendingSnapshot.getAndSet(message) == null) {
                    outbox.offer(SNAPSHOT_READY);
                }
            } else {
                outbox.offer(message);
            }
        }

        private void readLoop() {
            try {
                while (true) {
                    byte[] frame = MessageCodec.readFrame(in);
                    lastReceivedNanos = System.nanoTime();
                    if (closed.get()) {
                        continue;   // 닫는 중: 상대가 닫을 때까지 버리기만 함
                    }
                    NetworkMessage message;
                    try {
                        message = codec.decode(frame);
                    } catch (IllegalArgumentException e) {
                        listener.onError("잘못된 데이터를 받아 연결을 끊습니다: " + e.getMessage());
                        fail();
                        return;
                    }
                    handle(message);
                }
            } catch (IOException e) {
                if (closed.get()) {
                    closeQuietly(socket);   // 정상 종료 마무리
                } else {
                    fail();
                }
            }
        }

        private void handle(NetworkMessage message) {
            switch (message.getType()) {
                case PING -> outbox.offer(NetworkMessage.of(MessageType.PONG, 0, message.getPayload()));
                case PONG -> {
                    long sent = message.getPayload(MessagePayload.Ping.class).timestampNanos();
                    long rtt = Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sent));
                    roundTripMillis = rtt;
                    lagging = rtt > timing.lagThresholdMillis();
                    listener.onLatencyChanged(rtt, lagging);
                }
                default -> {
                    try {
                        listener.onMessage(message);
                    } catch (RuntimeException e) {
                        // 처리 중 오류가 나도 이후 메시지는 계속 받는다
                        listener.onError("메시지 처리 중 오류(" + message.getType() + "): " + e.getMessage());
                    }
                }
            }
        }

        private void writeLoop() {
            try {
                while (true) {
                    Object item = outbox.take();
                    if (item == CLOSE) {
                        socket.shutdownOutput();   // 남은 데이터를 보낸 뒤 상대에게 종료(EOF) 전달
                        return;
                    }
                    NetworkMessage message = item == SNAPSHOT_READY
                            ? pendingSnapshot.getAndSet(null)
                            : (NetworkMessage) item;
                    if (message != null) {
                        write(message);
                    }
                }
            } catch (IOException e) {
                if (!closed.get()) {
                    fail();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void write(NetworkMessage message) throws IOException {
            NetworkMessage numbered = message.withSequence(nextSequence++);
            MessageCodec.writeFrame(out, codec.encode(numbered));
        }

        // 하트비트: 응답 없음 → 끊김, 잠시 응답 없음 → 랙, 그 외 PING 전송
        private void beat() {
            if (closed.get()) {
                return;
            }
            long silentMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastReceivedNanos);
            if (silentMillis > timing.disconnectTimeoutMillis()) {
                listener.onError("상대 응답이 " + silentMillis / 1000.0 + "초 동안 없어 연결을 끊습니다.");
                fail();
                return;
            }
            if (!lagging && silentMillis > timing.heartbeatIntervalMillis() + timing.lagThresholdMillis()) {
                lagging = true;
                listener.onLatencyChanged(silentMillis, true);
            }
            outbox.offer(NetworkMessage.of(MessageType.PING, 0, new MessagePayload.Ping(System.nanoTime())));
        }

        // 예상하지 못한 종료: 한 번만 정리하고 onDisconnected 통지
        void fail() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            release();
            boolean wasCurrent;
            synchronized (lock) {
                wasCurrent = connection == this;
                if (wasCurrent) {
                    connection = null;
                }
            }
            if (wasCurrent) {
                listener.onDisconnected();
            }
        }

        // disconnect(): 큐에 남은 메시지를 보내고, 상대가 닫거나 유예 시간이 지나면 소켓 닫기
        void closeGracefully() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            outbox.offer(CLOSE);
            try {
                scheduler.schedule(this::release, CLOSE_GRACE_MILLIS, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                release();   // shutdown된 경우 바로 닫기
            }
        }

        private void release() {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            outbox.clear();
            outbox.offer(CLOSE);
            closeQuietly(socket);
        }
    }

    // ---------------------------------------------------------------- 공통

    private static void checkPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            // 정리 중 오류는 무시
        }
    }
}
