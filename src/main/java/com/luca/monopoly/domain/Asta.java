package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.luca.monopoly.domain.giocatore.Giocatore;

/**
 * Asta di una proprietà che il giocatore di turno ha rinunciato a comprare.
 * I partecipanti offrono a turno; chi non vuole rilanciare si ritira. L'asta
 * finisce quando resta solo il miglior offerente, oppure quando si ritirano
 * tutti senza offerte (la proprietà resta alla banca).
 */
public class Asta {

    private final Casella casella;
    private final List<Giocatore> partecipanti;
    private int indiceOfferente;
    private int offertaMassima;
    private Giocatore migliorOfferente;

    public Asta(Casella casella, List<Giocatore> partecipantiInOrdine) {
        this.casella = casella;
        this.partecipanti = new ArrayList<>(partecipantiInOrdine);
    }

    void offri(Giocatore giocatore, int importo) {
        verificaTurno(giocatore);
        if (importo <= offertaMassima) {
            throw new MossaNonValidaException("L'offerta deve superare " + offertaMassima + "€.");
        }
        if (importo > giocatore.getPortafoglio()) {
            throw new MossaNonValidaException("Non puoi offrire più dei tuoi contanti (" + giocatore.getPortafoglio()
                    + "€).");
        }
        offertaMassima = importo;
        migliorOfferente = giocatore;
        indiceOfferente = (indiceOfferente + 1) % partecipanti.size();
    }

    void ritirati(Giocatore giocatore) {
        verificaTurno(giocatore);
        if (giocatore == migliorOfferente) {
            throw new MossaNonValidaException("Non puoi ritirarti: la tua è l'offerta più alta.");
        }
        partecipanti.remove(indiceOfferente);
        if (indiceOfferente >= partecipanti.size()) {
            indiceOfferente = 0;
        }
    }

    public boolean isConclusa() {
        return partecipanti.isEmpty()
                || (partecipanti.size() == 1 && partecipanti.get(0) == migliorOfferente);
    }

    private void verificaTurno(Giocatore giocatore) {
        if (isConclusa() || giocatore != getOfferenteDiTurno()) {
            throw new MossaNonValidaException("Non è il turno di " + giocatore.getNome() + " nell'asta.");
        }
    }

    public Giocatore getOfferenteDiTurno() {
        return partecipanti.isEmpty() ? null : partecipanti.get(indiceOfferente);
    }

    public Casella getCasella() {
        return casella;
    }

    public int getOffertaMassima() {
        return offertaMassima;
    }

    public Giocatore getMigliorOfferente() {
        return migliorOfferente;
    }

    public List<Giocatore> getPartecipanti() {
        return Collections.unmodifiableList(partecipanti);
    }

}
