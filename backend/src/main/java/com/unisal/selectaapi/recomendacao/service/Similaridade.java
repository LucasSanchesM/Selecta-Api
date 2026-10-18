package com.unisal.selectaapi.recomendacao.service;

public final class Similaridade {

    private Similaridade() {
    }

    /** Similaridade de cosseno entre dois vetores; 0 quando forem incompatíveis ou nulos. */
    public static double cosseno(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0;
        }
        double produto = 0;
        double normaA = 0;
        double normaB = 0;
        for (int i = 0; i < a.length; i++) {
            produto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        if (normaA == 0 || normaB == 0) {
            return 0;
        }
        return produto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }
}
