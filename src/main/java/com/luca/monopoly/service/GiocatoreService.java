package com.luca.monopoly.service;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.domain.giocatore.Giocatore;
import com.luca.monopoly.repository.JpaGiocatore;
import com.luca.monopoly.repository.JpaGiocatoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GiocatoreService {

    public static final int LUNGHEZZA_MASSIMA_NOME = 30;

    private final JpaGiocatoreRepository jpaGiocatoreRepository;

    public GiocatoreService(JpaGiocatoreRepository jpaGiocatoreRepository) {
        this.jpaGiocatoreRepository = jpaGiocatoreRepository;
    }

    public List<JpaGiocatore> getGiocatori() {
        return jpaGiocatoreRepository.findAll();
    }

    @Transactional
    public void aggiungiGiocatore(String nome, Segnalini segnalino) {
        if (nome == null || nome.isBlank()) {
            throw new MossaNonValidaException("Inserire il nome del giocatore.");
        }
        if (nome.trim().length() > LUNGHEZZA_MASSIMA_NOME) {
            throw new MossaNonValidaException(
                    "Il nome può avere al massimo " + LUNGHEZZA_MASSIMA_NOME + " caratteri.");
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
    @Transactional
    public void salvaStato(List<Giocatore> giocatori) {
        Map<Segnalini, JpaGiocatore> perSegnalino = jpaGiocatoreRepository.findAll().stream()
                .collect(Collectors.toMap(JpaGiocatore::getSegnalino, Function.identity()));

        for (Giocatore giocatore : giocatori) {
            JpaGiocatore jpaGiocatore = perSegnalino.get(giocatore.getSegnalino());
            if (jpaGiocatore != null) {
                jpaGiocatore.setPortafoglio(giocatore.getPortafoglio());
                jpaGiocatore.setPosizione(giocatore.getPosizione());
            }
        }
        jpaGiocatoreRepository.saveAll(perSegnalino.values());
    }

    @Transactional
    public void eliminaGiocatori() {

        jpaGiocatoreRepository.deleteAll();
    }

}
