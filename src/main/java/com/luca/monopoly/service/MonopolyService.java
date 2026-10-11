package com.luca.monopoly.service;

import com.luca.monopoly.domain.FasePartita;
import com.luca.monopoly.domain.Giocatore;
import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.PartitaAssenteException;
import com.luca.monopoly.domain.Segnalino;
import com.luca.monopoly.domain.StatoCambiatoException;
import com.luca.monopoly.persistence.GiocatoreEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 *
 * Anche le modifiche della lobby passano da qui, sotto lo stesso lock: nessun
 * giocatore può essere registrato mentre la partita viene avviata. Le mosse non
 * toccano il database, quindi il lock non attende mai l'I/O durante il gioco.
 */
@Service
public class MonopolyService {

    private final GiocatoreService giocatoreService;

    private Partita partita;
    private long versione;

    public MonopolyService(GiocatoreService giocatoreService) {
        this.giocatoreService = giocatoreService;
    }

    public synchronized boolean isPartitaPresente() {
        return partita != null;
    }

    public synchronized void aggiungiGiocatore(String nome, Segnalino segnalino) {
        if (partita != null) {
            throw new MossaNonValidaException(
                    "La partita è già iniziata: resettala per aggiungere nuovi giocatori.");
        }
        giocatoreService.aggiungiGiocatore(nome, segnalino);
    }

    public synchronized void rimuoviGiocatore(Segnalino segnalino) {
        if (partita != null) {
            throw new MossaNonValidaException(
                    "La partita è già iniziata: resettala per togliere giocatori dal tavolo.");
        }
        giocatoreService.rimuoviGiocatore(segnalino);
    }

    public synchronized void avviaPartita() {
        if (partita != null && partita.getFase() != FasePartita.TERMINATA) {
            throw new MossaNonValidaException("C'è già una partita in corso.");
        }

        List<Giocatore> giocatori = new ArrayList<>();
        for (GiocatoreEntity registrato : giocatoreService.getGiocatori()) {
            giocatori.add(new Giocatore(registrato.getNome(), registrato.getSegnalino()));
        }

        partita = new Partita(giocatori);
        versione++;
    }

    /** Legge la partita in corso producendo, sotto lock, una vista immutabile. */
    public synchronized <T> T leggi(BiFunction<Partita, Long, T> vista) {
        verificaPartitaPresente();
        return vista.apply(partita, versione);
    }

    /**
     * Come {@link #leggi}, ma se il client ha già la versione attuale non costruisce
     * la vista: è il caso più frequente del polling.
     *
     * @param versioneNota versione già vista dal client, oppure null
     * @return la vista, oppure vuoto se la partita non è cambiata
     */
    public synchronized <T> Optional<T> leggiSeCambiata(Long versioneNota, BiFunction<Partita, Long, T> vista) {
        verificaPartitaPresente();
        if (versioneNota != null && versioneNota == versione) {
            return Optional.empty();
        }
        return Optional.of(vista.apply(partita, versione));
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
        return vista.apply(partita, versione);
    }

    /** Chiude la partita ma tiene al tavolo gli stessi giocatori, pronti per una nuova. */
    public synchronized void nuovaPartita() {
        partita = null;
        versione++;
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
