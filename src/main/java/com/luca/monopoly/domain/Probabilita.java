package com.luca.monopoly.domain;

public class Probabilita extends Carta {

    public Probabilita(String testo, EffettoCarta effetto, int valore, int valoreAlbergo) {
        super(testo, effetto, valore, valoreAlbergo);
    }

    public Probabilita(String testo, EffettoCarta effetto, int valore) {
        super(testo, effetto, valore);
    }

    public Probabilita(String testo, EffettoCarta effetto) {
        super(testo, effetto);
    }

}
