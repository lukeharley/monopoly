package com.luca.monopoly.domain;

import com.luca.monopoly.domain.giocatore.Giocatore;

/**
 * Regole per il calcolo degli affitti, separate dal flusso della partita.
 */
public class CalcolatoreAffitto {

    private final Tabellone tabellone;

    public CalcolatoreAffitto(Tabellone tabellone) {
        this.tabellone = tabellone;
    }

    /**
     * @param risultatoDadi somma dei dadi dell'ultimo lancio, usata per le società
     * @return l'affitto dovuto per la sosta sulla casella (0 se libera o ipotecata)
     */
    public int calcola(Casella casella, int risultatoDadi) {
        Giocatore proprietario = casella.getProprietario();
        if (proprietario == null || casella.isIpotecata()) {
            return 0;
        }
        Contratto contratto = tabellone.getContratto(tabellone.getPosizione(casella));

        switch (casella.getTipo()) {
            case TERRENO:
                int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
                if (numeroCasette == 0 && tabellone.possiedeTuttoIlGruppo(proprietario, casella.getColore())) {
                    // Monopolio: la rendita del terreno nudo raddoppia
                    return contratto.getRenditaTerreno() * 2;
                }
                return contratto.calcolaAffitto(numeroCasette);
            case STAZIONE:
                // 25€ con una stazione, raddoppia per ogni altra stazione posseduta
                int stazioni = tabellone.contaProprietaDelTipo(proprietario, TipoCasella.STAZIONE);
                return contratto.getTassa() << (stazioni - 1);
            case SOCIETA:
                int societa = tabellone.contaProprietaDelTipo(proprietario, TipoCasella.SOCIETA);
                return risultatoDadi * (societa == 2 ? 10 : 4);
            default:
                return 0;
        }
    }

}
