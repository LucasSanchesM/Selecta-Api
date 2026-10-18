package com.unisal.selectaapi.auth.entity;

public enum SecurityCodePurpose {
    PASSWORD_CHANGE,
    EMAIL_CHANGE,
    TWO_FACTOR_DISABLE,
    ACCOUNT_DELETION,
    /** Código gerado num dispositivo já conectado para aprovar o login em outro. */
    LOGIN_DEVICE_APPROVAL 
}
