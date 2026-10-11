package com.luca.monopoly.domain;

/**
 * La mossa è stata pensata su uno stato della partita ormai superato (un altro
 * giocatore ha agito nel frattempo): il client deve aggiornarsi e riprovare.
 */
public class StatoCambiatoException extends RuntimeException {

    public StatoCambiatoException() {
        super("La partita è cambiata nel frattempo: la schermata è stata aggiornata, riprova.");
    }

}
