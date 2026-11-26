package com.team.tetris.network;

import java.util.Locale;
import java.util.Objects;

/**
 * 서버 화면에 표시할 이 PC의 IP 주소와 그 주소가 붙은 네트워크 어댑터 이름.
 * 한 PC에 어댑터가 여러 개(Wi-Fi + VirtualBox 등)면 주소도 여러 개가 나오므로,
 * 어댑터 이름을 함께 보여줘서 접속하는 사람이 어떤 IP를 입력해야 하는지 알 수 있게 한다.
 *
 * @param ip          IPv4 주소 (예: 10.50.90.62)
 * @param adapterName 어댑터 표시 이름 (예: Wi-Fi, VirtualBox Host-Only Ethernet Adapter)
 */
public record HostAddress(String ip, String adapterName) {

    /** 가상 어댑터로 보는 이름 (VM·컨테이너·WSL 등 다른 PC에서 접속할 수 없는 주소) */
    private static final String[] VIRTUAL_ADAPTER_KEYWORDS = {
            "virtualbox", "vmware", "hyper-v", "vethernet", "wsl", "docker",
            "host-only", "virtual", "loopback", "pseudo", "vmnet", "vbox"
    };

    public HostAddress {
        Objects.requireNonNull(ip, "ip must not be null");
        adapterName = adapterName == null ? "" : adapterName.trim();
    }

    /** VirtualBox·VMware·Hyper-V·WSL 등 가상 어댑터 주소인지 (이름으로 판단) */
    public boolean isVirtualAdapter() {
        return isVirtualAdapterName(adapterName);
    }

    /** 화면 표시용 "IP (어댑터 이름)". 이름이 없으면 IP만 */
    public String displayText() {
        return adapterName.isEmpty() ? ip : ip + " (" + adapterName + ")";
    }

    static boolean isVirtualAdapterName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String keyword : VIRTUAL_ADAPTER_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
