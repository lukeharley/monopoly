package com.luca.monopoly.service;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Segnalino;
import com.luca.monopoly.persistence.GiocatoreEntity;
import com.luca.monopoly.persistence.GiocatoreRepository;
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

    private static final String SEGNALINO_GIA_SCELTO = "Il segnalino selezionato è già stato scelto.";

    private final GiocatoreRepository giocatoreRepository;

    public GiocatoreService(GiocatoreRepository giocatoreRepository) {
        this.giocatoreRepository = giocatoreRepository;
    }

    /** In ordine di registrazione, che è anche l'ordine dei turni. */
    public List<GiocatoreEntity> getGiocatori() {
        return giocatoreRepository.findAllByOrderByIdAsc();
    }

    /** Nome e segnalino arrivano già validati dal form ({@code HomeForm}). */
    @Transactional
    public void aggiungiGiocatore(String nome, Segnalino segnalino) {
        if (giocatoreRepository.findBySegnalino(segnalino) != null) {
            throw new MossaNonValidaException(SEGNALINO_GIA_SCELTO);
        }

        GiocatoreEntity registrato = new GiocatoreEntity();
        registrato.setNome(nome.trim());
        registrato.setSegnalino(segnalino);
        try {
            giocatoreRepository.saveAndFlush(registrato);
        } catch (DataIntegrityViolationException eccezione) {
            // un'altra istanza o un altro client ha preso lo stesso segnalino dopo il controllo
            throw new MossaNonValidaException(SEGNALINO_GIA_SCELTO);
        }
    }

    @Transactional
    public void rimuoviGiocatore(Segnalino segnalino) {
        GiocatoreEntity registrato = giocatoreRepository.findBySegnalino(segnalino);
        if (registrato != null) {
            giocatoreRepository.delete(registrato);
        }
    }

    @Transactional
    public void eliminaGiocatori() {
        giocatoreRepository.deleteAllInBatch();
    }

}
