package com.luca.monopoly.web.dto;

/**
 * Un segnalino nella griglia di scelta: preso se un giocatore al tavolo lo usa già.
 *
 * @param valore nome della costante inviato dal form (es. "CANE")
 * @param nome   nome da mostrare (es. "Cane")
 */
public record SegnalinoView(String valore, String nome, boolean preso) {
}
