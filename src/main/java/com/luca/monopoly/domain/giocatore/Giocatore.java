package com.luca.monopoly.domain.giocatore;

import java.util.ArrayList;
import java.util.List;

import com.luca.monopoly.domain.Carta;
import com.luca.monopoly.domain.Segnalini;

public class Giocatore {
    private String nome;
    private Segnalini segnalino;
    private int portafoglio;
    private int posizione;
    private boolean inBancarotta;
    private boolean inPrigione;
    private int turniInPrigione;
    private final List<Carta> carteUscitaDiPrigione = new ArrayList<>();

    public Giocatore(String nome, Segnalini segnalino, int portafoglio, int posizione) {
        this.nome = nome;
        this.segnalino = segnalino;
        this.portafoglio = portafoglio;
        this.posizione = posizione;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getPosizione() {
        return posizione;
    }

    public void setPosizione(int posizione) {
        this.posizione = posizione;
    }

    public Segnalini getSegnalino() {
        return segnalino;
    }

    public void setSegnalino(Segnalini segnalino) {
        this.segnalino = segnalino;
    }

    public int getPortafoglio() {
        return portafoglio;
    }

    public void setPortafoglio(int portafoglio) {
        this.portafoglio = portafoglio;
    }

    public void incassa(int importo) {
        this.portafoglio += importo;
    }

    public void paga(int importo) {
        this.portafoglio -= importo;
    }

    public boolean isInBancarotta() {
        return inBancarotta;
    }

    public void setInBancarotta(boolean inBancarotta) {
        this.inBancarotta = inBancarotta;
    }

    public boolean isInPrigione() {
        return inPrigione;
    }

    public void setInPrigione(boolean inPrigione) {
        this.inPrigione = inPrigione;
    }

    public int getTurniInPrigione() {
        return turniInPrigione;
    }

    public void setTurniInPrigione(int turniInPrigione) {
        this.turniInPrigione = turniInPrigione;
    }

    public List<Carta> getCarteUscitaDiPrigione() {
        return carteUscitaDiPrigione;
    }

    @Override
    public String toString() {
        return "Nome: " + nome + " Segnalino: " + segnalino + " Portafoglio: " + portafoglio + " Posizione: "
                + posizione;
    }
}
