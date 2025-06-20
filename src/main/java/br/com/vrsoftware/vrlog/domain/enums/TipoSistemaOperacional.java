package br.com.vrsoftware.vrlog.domain.enums;

public enum TipoSistemaOperacional {
    WINDOWS("windows"), LINUX("linux"), MAC("mac");

    private final String desc;

    TipoSistemaOperacional(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}