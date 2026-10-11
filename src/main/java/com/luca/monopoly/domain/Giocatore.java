package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Un giocatore della partita. Lo stato si legge liberamente ma si modifica solo
 * dal dominio, attraverso operazioni che rispettano le regole (per esempio chi
 * entra in prigione si trova sempre sulla casella della prigione).
 *
 * L'identità è per riferimento: due giocatori sono uguali solo se sono lo
 * stesso oggetto, come le pedine sul tavolo.
 */
public class Giocatore {

    private final String nome;
    private final Segnalini segnalino;
    private final List<Carta> carteUscitaDiPrigione = new ArrayList<>();

    private int portafoglio = Partita.PORTAFOGLIO_INIZIALE;
    private int posizione;
    private boolean inPrigione;
    private int turniInPrigione;
    private boolean inBancarotta;

    public Giocatore(String nome, Segnalini segnalino) {
        this.nome = nome;
        this.segnalino = segnalino;
    }

    // ---------------------------------------------------------------------
    // Lettura
    // ---------------------------------------------------------------------

    public String getNome() {
        return nome;
    }

    public Segnalini getSegnalino() {
        return segnalino;
    }

    public int getPortafoglio() {
        return portafoglio;
    }

    public int getPosizione() {
        return posizione;
    }

    public boolean isInPrigione() {
        return inPrigione;
    }

    public int getTurniInPrigione() {
        return turniInPrigione;
    }

    public boolean isInBancarotta() {
        return inBancarotta;
    }

    public int getNumeroCarteUscitaDiPrigione() {
        return carteUscitaDiPrigione.size();
    }

    // ---------------------------------------------------------------------
    // Modifica (solo dal dominio)
    // ---------------------------------------------------------------------

    void incassa(int importo) {
        portafoglio += importo;
    }

    void paga(int importo) {
        portafoglio -= importo;
    }

    /** Svuota il portafoglio e restituisce quanto conteneva. */
    int cediTuttoIlDenaro() {
        int denaro = portafoglio;
        portafoglio = 0;
        return denaro;
    }

    void spostaA(int nuovaPosizione) {
        posizione = nuovaPosizione;
    }

    void entraInPrigione() {
        posizione = Tabellone.POSIZIONE_PRIGIONE;
        inPrigione = true;
        turniInPrigione = 0;
    }

    void esciDiPrigione() {
        inPrigione = false;
        turniInPrigione = 0;
    }

    /** @return i turni passati in prigione, compreso questo */
    int registraTurnoInPrigione() {
        return ++turniInPrigione;
    }

    void riceviCartaUscitaDiPrigione(Carta carta) {
        carteUscitaDiPrigione.add(carta);
    }

    Carta usaCartaUscitaDiPrigione() {
        if (carteUscitaDiPrigione.isEmpty()) {
            throw new MossaNonValidaException("Non possiedi carte \"Uscite gratis di prigione\".");
        }
        return carteUscitaDiPrigione.remove(0);
    }

    /** Consegna tutte le carte "Uscite gratis di prigione" (bancarotta). */
    List<Carta> cediCarteUscitaDiPrigione() {
        List<Carta> carte = List.copyOf(carteUscitaDiPrigione);
        carteUscitaDiPrigione.clear();
        return carte;
    }

    void esceDalGioco() {
        inBancarotta = true;
        esciDiPrigione();
    }

    @Override
    public String toString() {
        return nome + " (" + segnalino.getNome() + ")";
    }
}
