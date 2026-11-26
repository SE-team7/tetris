package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * [회의 7차 문제 5-1] 서버 IP가 두 개 뜨는 문제.
 * VirtualBox 등 가상 어댑터 주소까지 표시되어, 접속하는 사람이 어떤 IP를 넣어야 하는지 알 수 없었다.
 *  - 주소마다 어댑터 이름을 함께 제공 ("IP (어댑터 이름)")
 *  - 실제 어댑터의 사설 주소를 맨 앞에, 가상 어댑터 주소를 맨 뒤에 둔다
 */
class HostAddressTest {

    private static final HostAddress WIFI = new HostAddress("10.50.90.62", "Wi-Fi");
    private static final HostAddress VBOX =
            new HostAddress("192.168.56.1", "VirtualBox Host-Only Ethernet Adapter");

    // ================================================================ 표시 형식

    @Test
    void displayText_showsIpWithAdapterName() {
        assertEquals("10.50.90.62 (Wi-Fi)", WIFI.displayText());
        assertEquals("192.168.56.1 (VirtualBox Host-Only Ethernet Adapter)", VBOX.displayText());
    }

    @Test
    void displayText_withoutAdapterName_showsIpOnly() {
        assertEquals("10.0.0.5", new HostAddress("10.0.0.5", null).displayText());
        assertEquals("10.0.0.5", new HostAddress("10.0.0.5", "   ").displayText());
        assertEquals("", new HostAddress("10.0.0.5", null).adapterName());
    }

    @Test
    void ipIsRequired() {
        assertThrows(NullPointerException.class, () -> new HostAddress(null, "Wi-Fi"));
    }

    // ================================================================ 가상 어댑터 판별

    @Test
    void virtualAdapters_areDetectedByName() {
        for (String name : List.of(
                "VirtualBox Host-Only Ethernet Adapter",
                "VMware Virtual Ethernet Adapter for VMnet8",
                "vEthernet (WSL)",
                "Hyper-V Virtual Ethernet Adapter",
                "docker0",
                "vboxnet0")) {
            assertTrue(new HostAddress("192.168.56.1", name).isVirtualAdapter(), name);
        }
    }

    @Test
    void realAdapters_areNotVirtual() {
        for (String name : List.of(
                "Wi-Fi",
                "Intel(R) Wi-Fi 6 AX201 160MHz",
                "Realtek PCIe GbE Family Controller",
                "이더넷",
                "en0",
                "")) {
            assertFalse(new HostAddress("10.0.0.2", name).isVirtualAdapter(), name);
        }
        assertFalse(HostAddress.isVirtualAdapterName(null));
    }

    // ================================================================ 정렬 (접속에 쓸 주소가 맨 앞)

    @Test
    void order_putsVirtualAdapterLast_evenIfFoundFirst() {
        // 다른 팀원 PC에서 발생한 상황: VirtualBox 주소가 먼저 나와서 뒤의 IP를 입력해야 접속됨
        List<HostAddress> ordered = NetworkService.orderForDisplay(List.of(VBOX, WIFI));

        assertEquals(List.of(WIFI, VBOX), ordered);
    }

    @Test
    void order_privateAddressBeforePublic_andKeepsOriginalOrderWithinGroup() {
        HostAddress lan1 = new HostAddress("192.168.0.10", "Ethernet");
        HostAddress lan2 = new HostAddress("10.0.0.7", "Wi-Fi");
        HostAddress publicIp = new HostAddress("203.0.113.5", "USB Modem");
        HostAddress vm = new HostAddress("172.20.0.1", "vEthernet (WSL)");

        List<HostAddress> ordered = NetworkService.orderForDisplay(List.of(vm, publicIp, lan1, lan2));

        assertEquals(List.of(lan1, lan2, publicIp, vm), ordered);
    }

    @Test
    void order_doesNotModifyInput() {
        List<HostAddress> input = List.of(VBOX, WIFI);

        NetworkService.orderForDisplay(input);

        assertEquals(List.of(VBOX, WIFI), input);
    }

    // ================================================================ 실제 PC 주소 탐색

    @Test
    void localAddressDetails_areIpv4WithAdapterNames_andMatchIpOnlyList() throws Exception {
        List<HostAddress> details = NetworkService.getLocalAddressDetails();

        assertFalse(details.isEmpty());
        for (HostAddress address : details) {
            assertTrue(address.ip().matches("\\d{1,3}(\\.\\d{1,3}){3}"), address.ip());
            assertNotNull(InetAddress.getByName(address.ip()));
            assertFalse(address.adapterName().isEmpty(), "어댑터 이름이 있어야 함: " + address);
        }
        assertEquals(details.stream().map(HostAddress::ip).toList(), NetworkService.getLocalAddresses());
        assertEquals(details, NetworkService.orderForDisplay(details), "이미 표시 순서로 정렬되어 있음");
    }
}
