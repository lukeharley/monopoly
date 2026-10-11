package com.luca.monopoly.service;

import com.luca.monopoly.domain.FasePartita;
import com.luca.monopoly.domain.Monopoly;
import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.domain.giocatore.Giocatore;
import com.luca.monopoly.repository.JpaGiocatore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custodisce la partita in corso. I metodi sono synchronized perché la partita
 * è unica e condivisa da tutte le richieste HTTP.
 */
@Service
public class MonopolyService {

    private final Monopoly monopoly = new Monopoly();

    private Partita partita;

    @Autowired
    private GiocatoreService giocatoreService;

    public Monopoly getMonopoly() {
        return monopoly;
    }

    public synchronized Partita getPartita() {
        return partita;
    }

    public synchronized boolean isPartitaInCorso() {
        return partita != null && partita.getFase() != FasePartita.TERMINATA;
    }

    public synchronized void avviaPartita() {
        if (isPartitaInCorso()) {
            throw new MossaNonValidaException("C'è già una partita in corso.");
        }

        List<Giocatore> giocatori = new ArrayList<>();
        for (JpaGiocatore jpaGiocatore : giocatoreService.getGiocatori()) {
            giocatori.add(new Giocatore(jpaGiocatore.getNome(), jpaGiocatore.getSegnalino(),
                    Partita.PORTAFOGLIO_INIZIALE, 0));
        }

        partita = new Partita(giocatori);
        giocatoreService.salvaStato(partita.getGiocatori());
    }

    /** Esegue una mossa sulla partita in corso e salva lo stato dei giocatori. */
    public synchronized Partita esegui(Consumer<Partita> mossa) {
        if (partita == null) {
            throw new MossaNonValidaException("Nessuna partita in corso.");
        }
        mossa.accept(partita);
        giocatoreService.salvaStato(partita.getGiocatori());
        return partita;
    }

    /** Chiude la partita ma tiene al tavolo gli stessi giocatori, pronti per una nuova. */
    public synchronized void nuovaPartita() {
        partita = null;
        giocatoreService.azzeraGiocatori();
    }

    public synchronized void rimuoviGiocatore(Segnalini segnalino) {
        if (partita != null) {
            throw new MossaNonValidaException(
                    "La partita è già iniziata: resettala per togliere giocatori dal tavolo.");
        }
        giocatoreService.rimuoviGiocatore(segnalino);
    }

    public synchronized void resettaPartita() {
        partita = null;
        giocatoreService.eliminaGiocatori();
    }
}
