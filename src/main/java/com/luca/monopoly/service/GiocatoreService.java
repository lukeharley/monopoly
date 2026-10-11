package com.luca.monopoly.service;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.repository.JpaGiocatore;
import com.luca.monopoly.repository.JpaGiocatoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Giocatori registrati nella lobby. Va usato tramite {@link MonopolyService},
 * che serializza le modifiche della lobby con l'avvio della partita.
 */
@Service
public class GiocatoreService {

    public static final int LUNGHEZZA_MASSIMA_NOME = 30;

    private static final String SEGNALINO_GIA_SCELTO = "Il segnalino selezionato è già stato scelto.";

    private final JpaGiocatoreRepository jpaGiocatoreRepository;

    public GiocatoreService(JpaGiocatoreRepository jpaGiocatoreRepository) {
        this.jpaGiocatoreRepository = jpaGiocatoreRepository;
    }

    /** In ordine di registrazione, che è anche l'ordine dei turni. */
    public List<JpaGiocatore> getGiocatori() {
        return jpaGiocatoreRepository.findAllByOrderByIdAsc();
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
            throw new MossaNonValidaException(SEGNALINO_GIA_SCELTO);
        }

        JpaGiocatore jpaGiocatore = new JpaGiocatore();
        jpaGiocatore.setNome(nome.trim());
        jpaGiocatore.setSegnalino(segnalino);
        try {
            jpaGiocatoreRepository.saveAndFlush(jpaGiocatore);
        } catch (DataIntegrityViolationException eccezione) {
            // un'altra istanza o un altro client ha preso lo stesso segnalino dopo il controllo
            throw new MossaNonValidaException(SEGNALINO_GIA_SCELTO);
        }
    }

    @Transactional
    public void rimuoviGiocatore(Segnalini segnalino) {
        JpaGiocatore jpaGiocatore = jpaGiocatoreRepository.findBySegnalino(segnalino);
        if (jpaGiocatore != null) {
            jpaGiocatoreRepository.delete(jpaGiocatore);
        }
    }

    @Transactional
    public void eliminaGiocatori() {
        jpaGiocatoreRepository.deleteAllInBatch();
    }

}
