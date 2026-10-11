package com.luca.monopoly.domain;

public enum TipoCasella {
    VIA,
    TERRENO,
    STAZIONE,
    SOCIETA,
    PROBABILITA,
    IMPREVISTI,
    TASSA,
    PRIGIONE,
    POSTEGGIO,
    VAI_IN_PRIGIONE;

    public boolean isAcquistabile() {
        return this == TERRENO || this == STAZIONE || this == SOCIETA;
    }
}
