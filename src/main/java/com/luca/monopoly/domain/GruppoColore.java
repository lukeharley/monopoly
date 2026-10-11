package com.luca.monopoly.domain;

/** Gruppi di terreni dello stesso colore: chi li possiede tutti ha il monopolio. */
public enum GruppoColore {
    FUCSIA,
    AZZURRO,
    ARANCIONE,
    MARRONE,
    ROSSO,
    GIALLO,
    VERDE,
    VIOLA;

    /** Nome usato dall'interfaccia (es. "fucsia"). */
    public String getNome() {
        return name().toLowerCase();
    }
}
