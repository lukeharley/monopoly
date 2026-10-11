package com.luca.monopoly.domain;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.luca.monopoly.domain.giocatore.Giocatore;

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
        if (!casella.isTerreno()) {
            throw new MossaNonValidaException("Si può costruire solo sui terreni.");
        }
        List<Casella> gruppo = tabellone.getCaselleDelColore(casella.getColore());
        if (!tabellone.possiedeTuttoIlGruppo(giocatore, casella.getColore())) {
            throw new MossaNonValidaException(
                    "Per costruire devi possedere tutti i terreni del gruppo " + casella.getColore() + ".");
        }
        if (gruppo.stream().anyMatch(Casella::isIpotecata)) {
            throw new MossaNonValidaException("Non puoi costruire: un terreno del gruppo è ipotecato.");
        }
        if (casella.haAlbergo()) {
            throw new MossaNonValidaException("Su " + casella.getTesto() + " c'è già un albergo.");
        }
        int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
        int minimoDelGruppo = gruppo.stream().mapToInt(Casella::getNumeroDiCasetteSullaCasella).min().orElse(0);
        if (numeroCasette > minimoDelGruppo) {
            throw new MossaNonValidaException(
                    "Devi costruire in modo uniforme: prima aggiungi una casa sugli altri terreni del gruppo.");
        }
        if (giocatore.getPortafoglio() < casella.getCostoCasetta()) {
            throw new MossaNonValidaException("Non hai abbastanza denaro per costruire.");
        }

        if (numeroCasette == 4) {
            banca.prelevaAlbergo();
            banca.restituisciCasette(4);
        } else {
            banca.prelevaCasette(1);
        }

        giocatore.paga(casella.getCostoCasetta());
        casella.setNumeroDiCasetteSullaCasella(numeroCasette + 1);
        registro.accept(giocatore.getNome()
                + (casella.haAlbergo() ? " costruisce un albergo su " : " costruisce una casa su ")
                + casella.getTesto() + " per " + casella.getCostoCasetta() + "€.");
    }

    /**
     * Vende alla banca un edificio a metà del prezzo. La vendita forzata (per
     * bancarotta) ignora la regola dell'uniformità e, se la banca non ha 4 case
     * per sostituire un albergo, lo vende per intero.
     */
    void vendiEdificio(Giocatore giocatore, Casella casella, boolean venditaForzata) {
        int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
        if (!casella.isTerreno() || numeroCasette == 0) {
            throw new MossaNonValidaException("Su " + casella.getTesto() + " non ci sono edifici da vendere.");
        }
        int massimoDelGruppo = tabellone.getCaselleDelColore(casella.getColore()).stream()
                .mapToInt(Casella::getNumeroDiCasetteSullaCasella).max().orElse(0);
        if (!venditaForzata && numeroCasette < massimoDelGruppo) {
            throw new MossaNonValidaException(
                    "Devi vendere in modo uniforme: prima vendi gli edifici sui terreni del gruppo che ne hanno di più.");
        }

        int ricavo = casella.getCostoCasetta() / 2;
        if (casella.haAlbergo()) {
            if (banca.getCasetteDisponibili() >= 4) {
                // l'albergo viene sostituito da 4 casette
                banca.prelevaCasette(4);
                casella.setNumeroDiCasetteSullaCasella(4);
            } else if (venditaForzata) {
                ricavo *= 5;
                casella.setNumeroDiCasetteSullaCasella(0);
            } else {
                throw new MossaNonValidaException(
                        "La banca non ha 4 casette per sostituire l'albergo: non puoi venderlo ora.");
            }
            banca.restituisciAlbergo();
            registro.accept(giocatore.getNome() + " vende l'albergo su " + casella.getTesto() + " e incassa "
                    + ricavo + "€.");
        } else {
            banca.restituisciCasette(1);
            casella.setNumeroDiCasetteSullaCasella(numeroCasette - 1);
            registro.accept(giocatore.getNome() + " vende una casa su " + casella.getTesto() + " e incassa "
                    + ricavo + "€.");
        }
        giocatore.incassa(ricavo);
    }

    void ipoteca(Giocatore giocatore, Casella casella) {
        if (casella.isIpotecata()) {
            throw new MossaNonValidaException(casella.getTesto() + " è già ipotecata.");
        }
        if (casella.isTerreno() && tabellone.getCaselleDelColore(casella.getColore()).stream()
                .anyMatch(terreno -> terreno.getNumeroDiCasetteSullaCasella() > 0)) {
            throw new MossaNonValidaException("Prima di ipotecare devi vendere gli edifici del gruppo.");
        }
        casella.setIpotecata(true);
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
        casella.setIpotecata(false);
        registro.accept(giocatore.getNome() + " riscatta l'ipoteca su " + casella.getTesto() + " pagando "
                + casella.getCostoRiscatto() + "€.");
    }

    /** Vende alla banca tutti gli edifici del giocatore (bancarotta). */
    void liquidaEdifici(Giocatore giocatore) {
        Casella casella;
        while ((casella = tabellone.getProprietaDi(giocatore).stream()
                .filter(proprieta -> proprieta.getNumeroDiCasetteSullaCasella() > 0)
                .max(Comparator.comparingInt(Casella::getNumeroDiCasetteSullaCasella))
                .orElse(null)) != null) {
            vendiEdificio(giocatore, casella, true);
        }
    }

    /**
     * Denaro massimo che il giocatore può raccogliere: contanti + metà del costo
     * degli edifici + valore ipotecario delle proprietà non ancora ipotecate.
     */
    public int valoreLiquidabile(Giocatore giocatore) {
        int valore = giocatore.getPortafoglio();
        for (Casella casella : tabellone.getProprietaDi(giocatore)) {
            valore += casella.getNumeroDiCasetteSullaCasella() * (casella.getCostoCasetta() / 2);
            if (!casella.isIpotecata()) {
                valore += casella.getValoreIpotecario();
            }
        }
        return valore;
    }

}
