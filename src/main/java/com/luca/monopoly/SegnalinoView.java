package com.luca.monopoly;

/** Un segnalino nella griglia di scelta: preso se un giocatore al tavolo lo usa già. */
public record SegnalinoView(int valore, String nome, boolean preso) {
}
