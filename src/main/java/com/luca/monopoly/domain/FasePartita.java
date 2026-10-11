package com.luca.monopoly.domain;

public enum FasePartita {
    /** Il giocatore di turno deve lanciare i dadi. */
    LANCIO_DADI,
    /**
     * Transitoria: il segnalino si sta muovendo e la casella è in risoluzione.
     * Nessuna mossa pubblica termina in questa fase.
     */
    MOVIMENTO,
    /** Il giocatore è su una proprietà libera e deve decidere se comprarla. */
    DECISIONE_ACQUISTO,
    /** La proprietà rifiutata è all'asta tra i giocatori. */
    ASTA,
    /** Il giocatore deve vendere edifici o ipotecare per saldare un debito. */
    SALDO_DEBITO,
    /** Il giocatore può gestire le proprietà e poi passare il turno. */
    FINE_TURNO,
    TERMINATA
}
