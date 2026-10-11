package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Regole per il calcolo degli affitti, separate dal flusso della partita.
 */
public class CalcolatoreAffitto {

    /** Chi possiede tutto il gruppo incassa il doppio sul terreno senza edifici. */
    private static final int MOLTIPLICATORE_MONOPOLIO = 2;

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
        Contratto contratto = casella.getContratto();

        switch (casella.getTipo()) {
            case TERRENO:
                int edifici = casella.getNumeroEdifici();
                if (edifici == 0 && tabellone.possiedeTuttoIlGruppo(proprietario, casella.getGruppo())) {
                    return contratto.getRendita(0) * MOLTIPLICATORE_MONOPOLIO;
                }
                return contratto.getRendita(edifici);
            case STAZIONE:
                return contratto.getRendita(tabellone.contaProprietaDelTipo(proprietario, TipoCasella.STAZIONE) - 1);
            case SOCIETA:
                return risultatoDadi
                        * contratto.getRendita(tabellone.contaProprietaDelTipo(proprietario, TipoCasella.SOCIETA) - 1);
            default:
                return 0;
        }
    }

    /**
     * Tabella degli affitti da mostrare sul contratto. Terreno: nudo, con il gruppo
     * completo, con 1-4 case, con l'albergo. Stazione: con 1, 2, 3, 4 stazioni.
     * Società: vuota, perché l'affitto dipende dai dadi.
     */
    public static List<Integer> tabellaRendite(Casella casella) {
        switch (casella.getTipo()) {
            case TERRENO:
                List<Integer> rendite = new ArrayList<>(casella.getContratto().getRendite());
                rendite.add(1, rendite.get(0) * MOLTIPLICATORE_MONOPOLIO);
                return List.copyOf(rendite);
            case STAZIONE:
                return casella.getContratto().getRendite();
            default:
                return List.of();
        }
    }

}
