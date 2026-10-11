package com.luca.monopoly.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Regole su case, alberghi e ipoteche. Non verifica di chi sia il turno: lo fa
 * {@link Partita} prima di delegare.
 */
public class GestoreProprieta {

    private final Tabellone tabellone;
    private final Banca banca;
    private final Consumer<String> registro;

    public GestoreProprieta(Tabellone tabellone, Banca banca, Consumer<String> registro) {
        this.tabellone = tabellone;
        this.banca = banca;
        this.registro = registro;
    }

    void costruisci(Giocatore giocatore, Casella casella) {
        verificaCostruibile(giocatore, casella);

        if (casella.getNumeroEdifici() == Casella.MASSIMO_CASE) {
            // l'albergo prende il posto delle 4 case, che tornano alla banca
            banca.prelevaAlbergo();
            banca.restituisciCasette(Casella.MASSIMO_CASE);
        } else {
            banca.prelevaCasette(1);
        }

        giocatore.paga(casella.getCostoCasetta());
        casella.aggiungiEdificio();
        registro.accept(giocatore.getNome()
                + (casella.haAlbergo() ? " costruisce un albergo su " : " costruisce una casa su ")
                + casella.getTesto() + " per " + casella.getCostoCasetta() + "€.");
    }

    private void verificaCostruibile(Giocatore giocatore, Casella casella) {
        if (!casella.isTerreno()) {
            throw new MossaNonValidaException("Si può costruire solo sui terreni.");
        }
        List<Casella> gruppo = tabellone.getCaselleDelGruppo(casella.getGruppo());
        if (!tabellone.possiedeTuttoIlGruppo(giocatore, casella.getGruppo())) {
            throw new MossaNonValidaException(
                    "Per costruire devi possedere tutti i terreni del gruppo " + casella.getGruppo().getNome() + ".");
        }
        if (gruppo.stream().anyMatch(Casella::isIpotecata)) {
            throw new MossaNonValidaException("Non puoi costruire: un terreno del gruppo è ipotecato.");
        }
        if (casella.haAlbergo()) {
            throw new MossaNonValidaException("Su " + casella.getTesto() + " c'è già un albergo.");
        }
        if (casella.getNumeroEdifici() > minimoEdifici(gruppo)) {
            throw new MossaNonValidaException(
                    "Devi costruire in modo uniforme: prima aggiungi una casa sugli altri terreni del gruppo.");
        }
        if (giocatore.getPortafoglio() < casella.getCostoCasetta()) {
            throw new MossaNonValidaException("Non hai abbastanza denaro per costruire.");
        }
    }

    /**
     * Vende alla banca un edificio a metà del prezzo. La vendita forzata (per
     * bancarotta) ignora la regola dell'uniformità e, se la banca non ha 4 case
     * per sostituire un albergo, lo vende per intero.
     */
    void vendiEdificio(Giocatore giocatore, Casella casella, boolean venditaForzata) {
        if (!casella.isTerreno() || casella.getNumeroEdifici() == 0) {
            throw new MossaNonValidaException("Su " + casella.getTesto() + " non ci sono edifici da vendere.");
        }
        if (!venditaForzata
                && casella.getNumeroEdifici() < massimoEdifici(tabellone.getCaselleDelGruppo(casella.getGruppo()))) {
            throw new MossaNonValidaException(
                    "Devi vendere in modo uniforme: prima vendi gli edifici sui terreni del gruppo che ne hanno di più.");
        }

        boolean albergo = casella.haAlbergo();
        int ricavo = albergo ? vendiAlbergo(casella, venditaForzata) : vendiCasa(casella);
        giocatore.incassa(ricavo);
        registro.accept(giocatore.getNome() + (albergo ? " vende l'albergo su " : " vende una casa su ")
                + casella.getTesto() + " e incassa " + ricavo + "€.");
    }

    private int vendiCasa(Casella casella) {
        banca.restituisciCasette(1);
        casella.rimuoviEdificio();
        return casella.getRicavoVenditaEdificio();
    }

    /** @return il ricavo: un edificio se l'albergo torna 4 case, tutti e 5 se sparisce */
    private int vendiAlbergo(Casella casella, boolean venditaForzata) {
        if (banca.getCasetteDisponibili() >= Casella.MASSIMO_CASE) {
            banca.prelevaCasette(Casella.MASSIMO_CASE);
            banca.restituisciAlbergo();
            casella.rimuoviEdificio();
            return casella.getRicavoVenditaEdificio();
        }
        if (!venditaForzata) {
            throw new MossaNonValidaException(
                    "La banca non ha 4 casette per sostituire l'albergo: non puoi venderlo ora.");
        }
        banca.restituisciAlbergo();
        casella.rimuoviTuttiGliEdifici();
        return casella.getRicavoVenditaEdificio() * Casella.ALBERGO;
    }

    void ipoteca(Giocatore giocatore, Casella casella) {
        if (casella.isIpotecata()) {
            throw new MossaNonValidaException(casella.getTesto() + " è già ipotecata.");
        }
        if (casella.isTerreno() && massimoEdifici(tabellone.getCaselleDelGruppo(casella.getGruppo())) > 0) {
            throw new MossaNonValidaException("Prima di ipotecare devi vendere gli edifici del gruppo.");
        }
        casella.ipoteca();
        giocatore.incassa(casella.getValoreIpotecario());
        registro.accept(giocatore.getNome() + " ipoteca " + casella.getTesto() + " e incassa "
                + casella.getValoreIpotecario() + "€.");
    }

    void riscatta(Giocatore giocatore, Casella casella) {
        if (!casella.isIpotecata()) {
            throw new MossaNonValidaException(casella.getTesto() + " non è ipotecata.");
        }
        if (giocatore.getPortafoglio() < casella.getCostoRiscatto()) {
            throw new MossaNonValidaException("Non hai abbastanza denaro per riscattare l'ipoteca.");
        }
        giocatore.paga(casella.getCostoRiscatto());
        casella.riscatta();
        registro.accept(giocatore.getNome() + " riscatta l'ipoteca su " + casella.getTesto() + " pagando "
                + casella.getCostoRiscatto() + "€.");
    }

    /** Vende alla banca tutti gli edifici del giocatore (bancarotta). */
    void liquidaEdifici(Giocatore giocatore) {
        Optional<Casella> conEdifici;
        while ((conEdifici = terrenoConPiuEdifici(giocatore)).isPresent()) {
            vendiEdificio(giocatore, conEdifici.get(), true);
        }
    }

    private Optional<Casella> terrenoConPiuEdifici(Giocatore giocatore) {
        return tabellone.getProprietaDi(giocatore).stream()
                .filter(proprieta -> proprieta.getNumeroEdifici() > 0)
                .max(Comparator.comparingInt(Casella::getNumeroEdifici));
    }

    /**
     * Denaro massimo che il giocatore può raccogliere: contanti + metà del costo
     * degli edifici + valore ipotecario delle proprietà non ancora ipotecate.
     */
    public int valoreLiquidabile(Giocatore giocatore) {
        int valore = giocatore.getPortafoglio();
        for (Casella casella : tabellone.getProprietaDi(giocatore)) {
            valore += casella.getNumeroEdifici() * casella.getRicavoVenditaEdificio();
            if (!casella.isIpotecata()) {
                valore += casella.getValoreIpotecario();
            }
        }
        return valore;
    }

    private static int minimoEdifici(List<Casella> gruppo) {
        return gruppo.stream().mapToInt(Casella::getNumeroEdifici).min().orElse(0);
    }

    private static int massimoEdifici(List<Casella> gruppo) {
        return gruppo.stream().mapToInt(Casella::getNumeroEdifici).max().orElse(0);
    }

}
