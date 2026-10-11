package com.luca.monopoly.persistence;

import com.luca.monopoly.domain.Segnalino;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Giocatore registrato nella lobby. Lo stato di gioco (denaro, posizione) vive
 * solo nella partita in memoria: qui si conservano nome e segnalino.
 */
@Entity
@Table(name = "giocatore")
public class GiocatoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 30)
    private String nome;

    /*
     * Salvato per nome ("CANE") e non per posizione nell'enum: riordinare o aggiungere
     * segnalini non cambia il significato dei dati. VARCHAR esplicito perché Hibernate 6
     * userebbe il tipo ENUM nativo di MySQL, diverso dalla colonna creata da Flyway.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "segnalino", nullable = false, unique = true, length = 20)
    private Segnalino segnalino;

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Segnalino getSegnalino() {
        return segnalino;
    }

    public void setSegnalino(Segnalino segnalino) {
        this.segnalino = segnalino;
    }
}
