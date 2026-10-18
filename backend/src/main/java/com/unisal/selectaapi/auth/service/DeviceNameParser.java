package com.unisal.selectaapi.auth.service;

public final class DeviceNameParser {

    private DeviceNameParser() {
    }

    public static String describe(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo desconhecido";
        }
        String browser = browser(userAgent);
        String os = operatingSystem(userAgent);
        return os == null ? browser : browser + " no " + os;
    }

    public static boolean isMobile(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        return userAgent.contains("Mobile") || userAgent.contains("iPhone") || userAgent.contains("Android")
                || userAgent.contains("iPad");
    }

    private static String browser(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgiOS") || ua.contains("EdgA")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("SamsungBrowser")) return "Samsung Internet";
        if (ua.contains("Firefox/") || ua.contains("FxiOS")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return "Navegador";
    }

    private static String operatingSystem(String ua) {
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }
}
