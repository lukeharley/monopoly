package com.luca.monopoly.repository;

import com.luca.monopoly.domain.Segnalini;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaGiocatoreRepository extends JpaRepository<JpaGiocatore, Integer> {

    JpaGiocatore findBySegnalino(Segnalini segnalini);

    /** In ordine di registrazione: è l'ordine dei turni della partita. */
    List<JpaGiocatore> findAllByOrderByIdAsc();

}
