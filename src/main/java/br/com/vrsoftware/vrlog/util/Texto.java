package br.com.vrsoftware.vrlog.util;

public class Texto {

    public static String substring(String pTexto, int pInicio, int pFinal) {
        final int iTamanhoTexto = pTexto.length();
        if (pInicio >= iTamanhoTexto) return "";
        if (pFinal > iTamanhoTexto) pFinal = iTamanhoTexto;
        return pTexto.substring(pInicio, pFinal);
    }

}