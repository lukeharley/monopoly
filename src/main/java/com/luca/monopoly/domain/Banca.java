package com.luca.monopoly.domain;

/**
 * La banca custodisce le scorte limitate di case e alberghi.
 */
public class Banca {

    private int casetteDisponibili;
    private int alberghiDisponibili;

    public Banca(int casetteDisponibili, int alberghiDisponibili) {
        this.casetteDisponibili = casetteDisponibili;
        this.alberghiDisponibili = alberghiDisponibili;
    }

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
