package com.luca.monopoly.service;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.domain.giocatore.Giocatore;
import com.luca.monopoly.repository.JpaGiocatore;
import com.luca.monopoly.repository.JpaGiocatoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GiocatoreService {

    @Autowired
    private JpaGiocatoreRepository jpaGiocatoreRepository;

    public List<JpaGiocatore> getGiocatori() {
        return jpaGiocatoreRepository.findAll();
    }

    public void aggiungiGiocatore(String nome, Segnalini segnalino) {
        if (nome == null || nome.isBlank()) {
            throw new MossaNonValidaException("Inserire il nome del giocatore.");
        }
        if (jpaGiocatoreRepository.findBySegnalino(segnalino) != null) {
            throw new MossaNonValidaException("Il segnalino selezionato è già stato scelto.");
        }

        JpaGiocatore jpaGiocatore = new JpaGiocatore();
        jpaGiocatore.setNome(nome.trim());
        jpaGiocatore.setSegnalino(segnalino);
        jpaGiocatore.setPortafoglio(Partita.PORTAFOGLIO_INIZIALE);
        jpaGiocatore.setPosizione(0);

        jpaGiocatoreRepository.save(jpaGiocatore);
    }

    /** Salva sul database portafoglio e posizione aggiornati dei giocatori. */
    public void salvaStato(List<Giocatore> giocatori) {
        for (Giocatore giocatore : giocatori) {
            JpaGiocatore jpaGiocatore = jpaGiocatoreRepository.findBySegnalino(giocatore.getSegnalino());
            if (jpaGiocatore != null) {
                jpaGiocatore.setPortafoglio(giocatore.getPortafoglio());
                jpaGiocatore.setPosizione(giocatore.getPosizione());
                jpaGiocatoreRepository.save(jpaGiocatore);
            }
        }
    }

    public void rimuoviGiocatore(Segnalini segnalino) {
        JpaGiocatore jpaGiocatore = jpaGiocatoreRepository.findBySegnalino(segnalino);
        if (jpaGiocatore != null) {
            jpaGiocatoreRepository.delete(jpaGiocatore);
        }
    }

    /** Riporta i giocatori registrati al portafoglio e alla posizione di partenza. */
    public void azzeraGiocatori() {
        for (JpaGiocatore jpaGiocatore : jpaGiocatoreRepository.findAll()) {
            jpaGiocatore.setPortafoglio(Partita.PORTAFOGLIO_INIZIALE);
            jpaGiocatore.setPosizione(0);
            jpaGiocatoreRepository.save(jpaGiocatore);
        }
    }

    public void eliminaGiocatori() {

        jpaGiocatoreRepository.deleteAll();
    }

}
