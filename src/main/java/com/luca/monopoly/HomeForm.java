package com.luca.monopoly;

import com.luca.monopoly.domain.Segnalini;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dati del form di registrazione di un giocatore nella lobby. */
public class HomeForm {

    public static final int LUNGHEZZA_MASSIMA_NOME = 30;

    @NotBlank(message = "Inserire il nome del giocatore.")
    @Size(max = LUNGHEZZA_MASSIMA_NOME, message = "Il nome può avere al massimo " + LUNGHEZZA_MASSIMA_NOME
            + " caratteri.")
    private String nome;

    /** Arriva dal form come nome della costante (es. "CANE"), non come numero. */
    @NotNull(message = "Scegliere un segnalino.")
    private Segnalini segnalino;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Segnalini getSegnalino() {
        return segnalino;
    }

    public void setSegnalino(Segnalini segnalino) {
        this.segnalino = segnalino;
    }

}
