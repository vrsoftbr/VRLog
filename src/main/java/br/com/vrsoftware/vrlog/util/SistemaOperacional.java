package br.com.vrsoftware.vrlog.util;

import br.com.vrsoftware.vrlog.domain.enums.TipoSistemaOperacional;

import java.io.File;

public class SistemaOperacional {

    private static final String OSName = System.getProperty("os.name").toLowerCase();
    private static final int TAM_MAX_OS_NAME = 7;

    static {
        System.out.println("os.name: " + OSName);
    }

    public static TipoSistemaOperacional get() {
        if (OSName.contains(TipoSistemaOperacional.WINDOWS.getDesc())) return TipoSistemaOperacional.WINDOWS;
        if (OSName.contains(TipoSistemaOperacional.MAC.getDesc())) return TipoSistemaOperacional.MAC;
        return TipoSistemaOperacional.LINUX;
    }

    public static boolean isMacOs() {
        return (Texto.substring(OSName, 0, TAM_MAX_OS_NAME).equalsIgnoreCase("MAC OS"));
    }

    public static boolean isWindows() {
        return (Texto.substring(OSName, 0, TAM_MAX_OS_NAME).equalsIgnoreCase("WINDOWS"));
    }

    public static boolean isVRUbuntu() {
        return new File("/etc/vrubuntu-os-release").exists();
    }

    public static String getOSName() {
        return OSName;
    }
}