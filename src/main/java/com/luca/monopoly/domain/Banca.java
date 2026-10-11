package com.luca.monopoly.domain;

/**
 * La banca custodisce le scorte limitate di case e alberghi.
 */
public class Banca {

    /** Pezzi contenuti nella scatola del gioco. */
    public static final int CASETTE_TOTALI = 32;
    public static final int ALBERGHI_TOTALI = 12;

    private int casetteDisponibili = CASETTE_TOTALI;
    private int alberghiDisponibili = ALBERGHI_TOTALI;

    public int getCasetteDisponibili() {
        return casetteDisponibili;
    }

    public int getAlberghiDisponibili() {
        return alberghiDisponibili;
    }

    void prelevaCasette(int numero) {
        if (casetteDisponibili < numero) {
            throw new MossaNonValidaException("La banca ha esaurito le casette.");
        }
        casetteDisponibili -= numero;
    }

    void restituisciCasette(int numero) {
        casetteDisponibili += numero;
    }

    void prelevaAlbergo() {
        if (alberghiDisponibili == 0) {
            throw new MossaNonValidaException("La banca ha esaurito gli alberghi.");
        }
        alberghiDisponibili--;
    }

    void restituisciAlbergo() {
        alberghiDisponibili++;
    }

}
