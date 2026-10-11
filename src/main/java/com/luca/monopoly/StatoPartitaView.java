package com.luca.monopoly;

import com.luca.monopoly.domain.Casella;
import com.luca.monopoly.domain.Contratto;
import com.luca.monopoly.domain.Imprevisto;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.giocatore.Giocatore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotografia della partita inviata in JSON all'interfaccia dopo ogni mossa.
 */
public record StatoPartitaView(
        String fase,
        int dado1,
        int dado2,
        int giocatoreCorrente,
        Integer vincitore,
        CartaView ultimaCarta,
        int cartePescate,
        int portafoglioIniziale,
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

    public static StatoPartitaView da(Partita partita) {
        List<Giocatore> giocatori = partita.getGiocatori();

        List<GiocatoreStatoView> giocatoriView = new ArrayList<>();
        for (Giocatore giocatore : giocatori) {
            giocatoriView.add(new GiocatoreStatoView(giocatore.getNome(), giocatore.getSegnalino().getNome(),
                    giocatore.getPortafoglio(), partita.calcolaPatrimonio(giocatore), giocatore.getPosizione(),
                    giocatore.isInPrigione(), giocatore.getTurniInPrigione(),
                    giocatore.getCarteUscitaDiPrigione().size(), giocatore.isInBancarotta()));
        }

        List<CasellaView> caselleView = new ArrayList<>();
        List<Casella> caselle = partita.getTabellone().getCaselle();
        for (int posizione = 0; posizione < caselle.size(); posizione++) {
            Casella casella = caselle.get(posizione);
            Integer proprietario = casella.getProprietario() == null ? null
                    : giocatori.indexOf(casella.getProprietario());
            caselleView.add(new CasellaView(posizione, casella.getTesto(), casella.getTipo().name(),
                    casella.getColore(), casella.getCostoProprieta(), casella.getCostoCasetta(),
                    casella.getNumeroDiCasetteSullaCasella(), proprietario, casella.isIpotecata(),
                    partita.calcolaAffitto(casella), casella.getValoreIpotecario(), casella.getCostoRiscatto(),
                    rendite(casella, partita.getTabellone().getContratto(posizione))));
        }

        CartaView ultimaCarta = partita.getUltimaCartaPescata() == null ? null
                : new CartaView(partita.getUltimaCartaPescata() instanceof Imprevisto ? "Imprevisti" : "Probabilità",
                        partita.getUltimaCartaPescata().getTesto());

        Integer vincitore = partita.getVincitore() == null ? null : giocatori.indexOf(partita.getVincitore());

        return new StatoPartitaView(partita.getFase().name(), partita.getDado1(), partita.getDado2(),
                giocatori.indexOf(partita.getGiocatoreCorrente()), vincitore, ultimaCarta,
                partita.getCartePescate(), Partita.PORTAFOGLIO_INIZIALE,
                partita.getCasetteDisponibili(), partita.getAlberghiDisponibili(), Partita.MULTA_PRIGIONE,
                giocatoriView, caselleView, new ArrayList<>(partita.getEventi()));
    }

    /**
     * Tabella degli affitti del contratto. Terreno: nudo, gruppo completo, 1-4 case, albergo.
     * Stazione: con 1, 2, 3 e 4 stazioni possedute.
     */
    private static List<Integer> rendite(Casella casella, Contratto contratto) {
        switch (casella.getTipo()) {
            case TERRENO:
                return List.of(contratto.getRenditaTerreno(), contratto.getRenditaTerreno() * 2,
                        contratto.getRenditaUnaCasetta(), contratto.getRenditaDueCasette(),
                        contratto.getRenditaTreCasette(), contratto.getRenditaQuattroCasette(),
                        contratto.getRenditaAlbergo());
            case STAZIONE:
                return List.of(contratto.getTassa(), contratto.getTassa() << 1, contratto.getTassa() << 2,
                        contratto.getTassa() << 3);
            default:
                return List.of();
        }
    }
}
