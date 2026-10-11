package com.luca.monopoly.domain;

import java.util.List;

/**
 * Tabella degli affitti stampata sul contratto di una proprietà.
 * <ul>
 * <li>terreno: indice = numero di edifici (0 = terreno nudo, 5 = albergo);</li>
 * <li>stazione: indice = stazioni possedute dal proprietario - 1;</li>
 * <li>società: moltiplicatore del risultato dei dadi, indice = società possedute - 1.</li>
 * </ul>
 */
public final class Contratto {

    private final List<Integer> rendite;

    private Contratto(List<Integer> rendite) {
        this.rendite = List.copyOf(rendite);
    }

    static Contratto terreno(int nudo, int unaCasa, int dueCase, int treCase, int quattroCase, int albergo) {
        return new Contratto(List.of(nudo, unaCasa, dueCase, treCase, quattroCase, albergo));
    }

    static Contratto conRendite(List<Integer> rendite) {
        return new Contratto(rendite);
    }

    public int getRendita(int indice) {
        return rendite.get(indice);
    }

    public List<Integer> getRendite() {
        return rendite;
    }

}
