package com.luca.monopoly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Giocatore {
    private final static int NUMERO_MAX_GIOCATORI = 2;
    private String nome;
    private Segnalini segnalino;
    private int posizione;
    private Dado dado;
    private int turno;
    private int portafoglio;
    private List<Casella> casellePossedute;
    private static List<Casella> casellePosseduteDaTuttiIGiocatori;

    public Giocatore(String nome, Segnalini segnalino) {
        this.nome = nome;
        this.segnalino = segnalino;
        this.posizione = 0;
        this.dado = new Dado();
        this.turno = 0;
        this.portafoglio = 1500;
        this.casellePossedute = new ArrayList<>();
    }

    public Giocatore(Segnalini segnalino) {
        this.segnalino = segnalino;
    }

    public static int getNumeroMaxGiocatori() {
        return NUMERO_MAX_GIOCATORI;
    }

    public String getNome() {
        return nome;
    }

    public Segnalini getSegnalino() {
        return segnalino;
    }

    public int getPosizione() {
        return posizione;
    }

    public Dado getDado() {
        return dado;
    }

    public int getTurno() {
        return turno;
    }

    public int getPortafoglio() {
        return portafoglio;
    }

    public List<Casella> getCasellePossedute() {
        return casellePossedute;
    }

    public List<Casella> getCasellePosseduteDaTuttiIGiocatori() {
        return casellePosseduteDaTuttiIGiocatori;
    }

    public <T> void pescaCarta(List<T> mazzoCorrente, List<T> mazzoCartePescate) {
        if (!mazzoCorrente.isEmpty()) {
            T cartaPescata = mazzoCorrente.remove(0);
            mazzoCartePescate.add(cartaPescata);
            System.out.println("Testo della carta pescata: " + cartaPescata);
        } else {
            Collections.shuffle(mazzoCartePescate);
            mazzoCorrente.addAll(mazzoCartePescate);
            mazzoCartePescate.clear();
        }

    }

    public int setNuovaPosizione(int posizione, int risultatoDado) {

        int nuovaPosizione = (posizione + risultatoDado) % 40;

        if ((posizione + risultatoDado) > 40) {
            portafoglio = portafoglio + 200;
        }

        return nuovaPosizione;
    }

    public int lanciaDadi() {
        return generaNumeroCasuale(1, 6);
    }

    private int generaNumeroCasuale(int valoreMinimo, int valoreMassimo) {

        if (valoreMinimo >= valoreMassimo) {
            throw new IllegalArgumentException("il valore massimo deve essere maggiore del valore minimo");
        }

        final Random r = new Random();
        return r.nextInt(valoreMassimo - valoreMinimo + 1) + valoreMinimo;
    }

    public void acquistaCasella(Casella casella) {
        casellePossedute.add(casella);
    }

    public boolean casellaAcquistabile(Casella casella, Contratto contratto) {
        return !(casellePosseduteDaTuttiIGiocatori.contains(casella)) && casella.isTerreno()
                && (portafoglio > contratto.getRenditaTerreno());
    }

}
