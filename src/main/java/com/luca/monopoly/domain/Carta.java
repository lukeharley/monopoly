package com.luca.monopoly.domain;

public class Carta {

    private final String testo;
    private final EffettoCarta effetto;
    /*
     * Significato in base all'effetto: importo (INCASSA, PAGA), posizione di
     * destinazione (VAI_A, TORNA_A), numero di caselle (SPOSTA, negativo = indietro),
     * costo per ogni casa (RIPARAZIONI).
     */
    private final int valore;
    /* Solo per RIPARAZIONI: costo per ogni albergo. */
    private final int valoreAlbergo;

    public Carta(String testo, EffettoCarta effetto, int valore, int valoreAlbergo) {
        this.testo = testo;
        this.effetto = effetto;
        this.valore = valore;
        this.valoreAlbergo = valoreAlbergo;
    }

    public Carta(String testo, EffettoCarta effetto, int valore) {
        this(testo, effetto, valore, 0);
    }

    public Carta(String testo, EffettoCarta effetto) {
        this(testo, effetto, 0, 0);
    }

    public String getTesto() {
        return testo;
    }

    public EffettoCarta getEffetto() {
        return effetto;
    }

    public int getValore() {
        return valore;
    }

    public int getValoreAlbergo() {
        return valoreAlbergo;
    }

}
