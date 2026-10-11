package com.luca.monopoly.domain;

public class Imprevisto extends Carta {

    public Imprevisto(String testo, EffettoCarta effetto, int valore, int valoreAlbergo) {
        super(testo, effetto, valore, valoreAlbergo);
    }

    public Imprevisto(String testo, EffettoCarta effetto, int valore) {
        super(testo, effetto, valore);
    }

    public Imprevisto(String testo, EffettoCarta effetto) {
        super(testo, effetto);
    }

}
