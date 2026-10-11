package com.luca.monopoly.service;

import com.luca.monopoly.domain.FasePartita;
import com.luca.monopoly.domain.Monopoly;
import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.PartitaAssenteException;
import com.luca.monopoly.domain.StatoCambiatoException;
import com.luca.monopoly.domain.giocatore.Giocatore;
import com.luca.monopoly.repository.JpaGiocatore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Custodisce la partita in corso, unica e condivisa da tutte le richieste HTTP.
 *
 * Ogni accesso alla partita (lettura o mossa) avviene sotto lo stesso lock, così
 * nessuno legge uno stato a metà di una mossa. Ogni mossa riuscita incrementa la
 * versione: il client la rimanda con le mosse successive e, se nel frattempo
 * qualcun altro ha giocato, la mossa viene rifiutata invece di essere applicata
 * a uno stato che il giocatore non ha visto (concorrenza ottimistica).
 */
@Service
public class MonopolyService {

    private final Monopoly monopoly = new Monopoly();
    private final GiocatoreService giocatoreService;

    private Partita partita;
    private long versione;

    public MonopolyService(GiocatoreService giocatoreService) {
        this.giocatoreService = giocatoreService;
    }

    public Monopoly getMonopoly() {
        return monopoly;
    }

    public synchronized boolean isPartitaPresente() {
        return partita != null;
    }

    public synchronized void avviaPartita() {
        if (partita != null && partita.getFase() != FasePartita.TERMINATA) {
            throw new MossaNonValidaException("C'è già una partita in corso.");
        }

        List<Giocatore> giocatori = new ArrayList<>();
        for (JpaGiocatore jpaGiocatore : giocatoreService.getGiocatori()) {
            giocatori.add(new Giocatore(jpaGiocatore.getNome(), jpaGiocatore.getSegnalino(),
                    Partita.PORTAFOGLIO_INIZIALE, 0));
        }

        partita = new Partita(giocatori);
        versione++;
        giocatoreService.salvaStato(partita.getGiocatori());
    }

    /** Legge la partita in corso producendo, sotto lock, una vista immutabile. */
    public synchronized <T> T leggi(BiFunction<Partita, Long, T> vista) {
        verificaPartitaPresente();
        return vista.apply(partita, versione);
    }

    /**
     * Esegue una mossa e restituisce la vista dello stato risultante.
     *
     * @param versioneAttesa versione vista dal client, oppure null per non controllarla
     */
    public synchronized <T> T esegui(Long versioneAttesa, Consumer<Partita> mossa, BiFunction<Partita, Long, T> vista) {
        verificaPartitaPresente();
        if (versioneAttesa != null && versioneAttesa != versione) {
            throw new StatoCambiatoException();
        }
        mossa.accept(partita);
        versione++;
        giocatoreService.salvaStato(partita.getGiocatori());
        return vista.apply(partita, versione);
    }

    public synchronized void resettaPartita() {
        partita = null;
        versione++;
        giocatoreService.eliminaGiocatori();
    }

    private void verificaPartitaPresente() {
        if (partita == null) {
            throw new PartitaAssenteException();
        }
    }
}
