package com.luca.monopoly.domain;

public class PartitaAssenteException extends RuntimeException {

    public PartitaAssenteException() {
        super("Nessuna partita in corso.");
    }

}
