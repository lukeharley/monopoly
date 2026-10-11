package com.luca.monopoly.domain;

public enum FasePartita {
    /** Il giocatore di turno deve lanciare i dadi. */
    LANCIO_DADI,
    /** Il giocatore è su una proprietà libera e deve decidere se comprarla. */
    DECISIONE_ACQUISTO,
    /** Il giocatore può gestire le proprietà e poi passare il turno. */
    FINE_TURNO,
    TERMINATA
}
