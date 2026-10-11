package com.luca.monopoly.persistence;

import com.luca.monopoly.domain.Segnalino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiocatoreRepository extends JpaRepository<GiocatoreEntity, Integer> {

    GiocatoreEntity findBySegnalino(Segnalino segnalino);

    /** In ordine di registrazione: è l'ordine dei turni della partita. */
    List<GiocatoreEntity> findAllByOrderByIdAsc();

}
