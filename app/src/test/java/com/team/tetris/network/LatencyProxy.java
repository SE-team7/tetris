package com.team.tetris.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 테스트용 네트워크 상태 흉내 프록시.
 * 클라이언트 → 프록시 → 서버로 연결하고, 오가는 데이터에 지연·멈춤·끊김을 넣는다.
 *  - setDelayMillis: 한 방향 지연 (데이터 순서는 유지, 처리량은 제한하지 않음)
 *  - setStalled(true): 데이터를 붙잡아 두었다가 false가 되면 한꺼번에 전달 (일시적인 랙)
 *  - setBlackhole(true): 소켓은 열어 둔 채 데이터를 버림 (선이 뽑힌 것과 같은 상태)
 */
public final class LatencyProxy implements AutoCloseable {

    private final ServerSocket listener;
    private final int targetPort;
    private volatile long delayMillis;
    private volatile boolean stalled;
    private volatile boolean blackhole;
    private volatile boolean closed;
    private Socket clientSide;
    private Socket serverSide;

    public LatencyProxy(int targetPort, long delayMillis) throws IOException {
        this.targetPort = targetPort;
        this.delayMillis = delayMillis;
        this.listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::acceptOnce, "proxy-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** 클라이언트가 접속할 포트 */
    public int getPort() {
        return listener.getLocalPort();
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void setStalled(boolean stalled) {
        this.stalled = stalled;
    }

    public void setBlackhole(boolean blackhole) {
        this.blackhole = blackhole;
    }

    private void acceptOnce() {
        try {
            clientSide = listener.accept();
            serverSide = new Socket(InetAddress.getLoopbackAddress(), targetPort);
            clientSide.setTcpNoDelay(true);
            serverSide.setTcpNoDelay(true);
            pump(clientSide, serverSide, "proxy-up");
            pump(serverSide, clientSide, "proxy-down");
        } catch (IOException e) {
            // 테스트 종료
        }
    }

    // 한 방향: 읽은 시각 + 지연 후에 순서대로 쓴다
    private void pump(Socket from, Socket to, String name) throws IOException {
        InputStream in = from.getInputStream();
        OutputStream out = to.getOutputStream();
        BlockingQueue<Chunk> queue = new LinkedBlockingQueue<>();

        Thread reader = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (!blackhole) {
                        queue.add(new Chunk(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMillis),
                                Arrays.copyOf(buffer, read)));
                    }
                }
            } catch (IOException e) {
                // 연결 종료
            }
            queue.add(Chunk.END);
        }, name + "-read");

        Thread writer = new Thread(() -> {
            try {
                while (!closed) {
                    Chunk chunk = queue.take();
                    if (chunk == Chunk.END) {
                        to.shutdownOutput();
                        return;
                    }
                    long wait = chunk.dueNanos - System.nanoTime();
                    if (wait > 0) {
                        TimeUnit.NANOSECONDS.sleep(wait);
                    }
                    while (stalled && !closed) {
                        Thread.sleep(5);
                    }
                    if (!blackhole) {
                        out.write(chunk.data);
                        out.flush();
                    }
                }
            } catch (IOException | InterruptedException e) {
                // 연결 종료
            }
        }, name + "-write");

        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        listener.close();
        if (clientSide != null) {
            clientSide.close();
        }
        if (serverSide != null) {
            serverSide.close();
        }
    }

    private record Chunk(long dueNanos, byte[] data) {
        static final Chunk END = new Chunk(0, new byte[0]);
    }
}
