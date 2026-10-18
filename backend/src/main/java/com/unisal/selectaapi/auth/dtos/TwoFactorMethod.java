package com.unisal.selectaapi.auth.dtos;

public enum TwoFactorMethod {
    /** Código enviado ao e-mail da conta. */
    EMAIL,
    /** Código gerado em outro dispositivo já conectado à conta. */
    DEVICE
}
