package com.luca.monopoly;

import com.luca.monopoly.domain.Asta;
import com.luca.monopoly.domain.CalcolatoreAffitto;
import com.luca.monopoly.domain.Casella;
import com.luca.monopoly.domain.Debito;
import com.luca.monopoly.domain.Giocatore;
import com.luca.monopoly.domain.Imprevisto;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.TipoCasella;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotografia della partita inviata in JSON all'interfaccia dopo ogni mossa.
 */
public record StatoPartitaView(
        long versione,
        String fase,
        int dado1,
        int dado2,
        int giocatoreCorrente,
        Integer vincitore,
        CartaView ultimaCarta,
        int cartePescate,
        int portafoglioIniziale,
        AstaView asta,
        DebitoView debito,
        int casetteDisponibili,
        int alberghiDisponibili,
        int multaPrigione,
        List<GiocatoreStatoView> giocatori,
        List<CasellaView> caselle,
        List<String> eventi) {

    public record GiocatoreStatoView(String nome, String segnalino, int portafoglio, int patrimonio,
            int posizione, boolean inPrigione, int turniInPrigione, int carteUscitaDiPrigione,
            boolean inBancarotta) {
    }

    public record CasellaView(int posizione, String testo, String tipo, String colore, int costo,
            int costoCasetta, int casette, Integer proprietario, boolean ipotecata, int affitto,
            int valoreIpotecario, int costoRiscatto, List<Integer> rendite) {
    }

    public record CartaView(String mazzo, String testo) {
    }

    /** Indici dei giocatori; migliorOfferente è null finché nessuno ha offerto. */
    public record AstaView(int posizione, int offertaMassima, Integer migliorOfferente, int offerenteDiTurno,
            List<Integer> partecipanti) {
    }

    /** creditore è l'indice del giocatore da pagare, null se è la banca. */
    public record DebitoView(int importo, Integer creditore) {
    }

    public static StatoPartitaView da(Partita partita, long versione) {
        List<Giocatore> giocatori = partita.getGiocatori();

        List<GiocatoreStatoView> giocatoriView = new ArrayList<>();
        for (Giocatore giocatore : giocatori) {
            giocatoriView.add(new GiocatoreStatoView(giocatore.getNome(), giocatore.getSegnalino().getNome(),
                    giocatore.getPortafoglio(), partita.calcolaPatrimonio(giocatore), giocatore.getPosizione(),
                    giocatore.isInPrigione(), giocatore.getTurniInPrigione(),
                    giocatore.getNumeroCarteUscitaDiPrigione(), giocatore.isInBancarotta()));
        }

        List<CasellaView> caselleView = new ArrayList<>();
        for (Casella casella : partita.getTabellone().getCaselle()) {
            Integer proprietario = casella.getProprietario() == null ? null
                    : giocatori.indexOf(casella.getProprietario());
            caselleView.add(new CasellaView(casella.getPosizione(), casella.getTesto(), casella.getTipo().name(),
                    casella.getGruppo() == null ? null : casella.getGruppo().getNome(), costo(casella),
                    casella.getCostoCasetta(), casella.getNumeroEdifici(), proprietario, casella.isIpotecata(),
                    partita.calcolaAffitto(casella), casella.getValoreIpotecario(), casella.getCostoRiscatto(),
                    CalcolatoreAffitto.tabellaRendite(casella)));
        }

        CartaView ultimaCarta = partita.getUltimaCartaPescata() == null ? null
                : new CartaView(partita.getUltimaCartaPescata() instanceof Imprevisto ? "Imprevisti" : "Probabilità",
                        partita.getUltimaCartaPescata().getTesto());

        Integer vincitore = partita.getVincitore() == null ? null : giocatori.indexOf(partita.getVincitore());

        AstaView astaView = null;
        Asta asta = partita.getAsta();
        if (asta != null) {
            astaView = new AstaView(asta.getCasella().getPosizione(), asta.getOffertaMassima(),
                    asta.getMigliorOfferente() == null ? null : giocatori.indexOf(asta.getMigliorOfferente()),
                    giocatori.indexOf(asta.getOfferenteDiTurno()),
                    asta.getPartecipanti().stream().map(giocatori::indexOf).toList());
        }

        DebitoView debitoView = null;
        Debito debito = partita.getDebito();
        if (debito != null) {
            debitoView = new DebitoView(debito.importo(),
                    debito.creditore() == null ? null : giocatori.indexOf(debito.creditore()));
        }

        return new StatoPartitaView(versione, partita.getFase().name(), partita.getDado1(), partita.getDado2(),
                giocatori.indexOf(partita.getGiocatoreCorrente()), vincitore, ultimaCarta,
                partita.getCartePescate(), Partita.PORTAFOGLIO_INIZIALE, astaView, debitoView,
                partita.getCasetteDisponibili(), partita.getAlberghiDisponibili(), Partita.MULTA_PRIGIONE,
                giocatoriView, caselleView, partita.getEventi());
    }

    /** Prezzo per le proprietà, importo da pagare per le tasse. */
    private static int costo(Casella casella) {
        return casella.getTipo() == TipoCasella.TASSA ? casella.getImportoTassa() : casella.getPrezzo();
    }
}
