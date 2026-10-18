package com.unisal.selectaapi.auth.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Dados do dispositivo que iniciou a requisição. O frontend (Next.js) chama a API
 * pelo servidor, então repassa o navegador, o IP e a localização reais do usuário
 * nos cabeçalhos X-Client-*.
 */
public record ClientInfo(String deviceId, String userAgent, String ipAddress, String location) {

    public static final String DEVICE_HEADER = "X-Device-Id";

    public static ClientInfo unknown() {
        return new ClientInfo(null, null, null, null);
    }

    public static ClientInfo from(HttpServletRequest request) {
        String deviceId = request.getHeader(DEVICE_HEADER);
        if (deviceId != null && (deviceId.length() < 16 || deviceId.length() > 128)) {
            deviceId = null;
        }

        String userAgent = firstNonBlank(request.getHeader("X-Client-User-Agent"), request.getHeader("User-Agent"));

        String forwardedFor = request.getHeader("X-Forwarded-For");
        String forwardedIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : null;
        String ipAddress = firstNonBlank(request.getHeader("X-Client-IP"), forwardedIp, request.getRemoteAddr());

        return new ClientInfo(
                deviceId,
                truncate(userAgent, 512),
                truncate(ipAddress, 64),
                truncate(request.getHeader("X-Client-Location"), 120));
    }

    public String deviceIdHash() {
        return deviceId == null ? null : SecureTokens.sha256(deviceId);
    }

    public String deviceName() {
        return DeviceNameParser.describe(userAgent);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
