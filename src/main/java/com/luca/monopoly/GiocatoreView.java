package com.luca.monopoly;

public class GiocatoreView {

    private String nome;

    private String segnalino;

    /** Nome della costante (es. "CANE"), usato dal form per togliere il giocatore. */
    private String valoreSegnalino;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSegnalino() {
        return segnalino;
    }

    public void setSegnalino(String segnalino) {
        this.segnalino = segnalino;
    }

    public String getValoreSegnalino() {
        return valoreSegnalino;
    }

    public void setValoreSegnalino(String valoreSegnalino) {
        this.valoreSegnalino = valoreSegnalino;
    }
}
