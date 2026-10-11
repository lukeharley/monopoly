package com.luca.monopoly.domain;

import java.util.OptionalInt;

/**
 * Pagamento che il giocatore di turno non riesce a coprire con i contanti.
 *
 * @param creditore               giocatore da pagare, oppure null se è la banca
 * @param importo                 somma dovuta
 * @param movimentoDopoIlPagamento caselle da percorrere una volta pagato: solo per la
 *                                multa al terzo turno in prigione, che precede il movimento
 */
public record Debito(Giocatore creditore, int importo, OptionalInt movimentoDopoIlPagamento) {

    static Debito versoLaBanca(int importo) {
        return new Debito(null, importo, OptionalInt.empty());
    }

    static Debito verso(Giocatore creditore, int importo) {
        return new Debito(creditore, importo, OptionalInt.empty());
    }

    static Debito multaDiPrigione(int importo, int passiDopoIlPagamento) {
        return new Debito(null, importo, OptionalInt.of(passiDopoIlPagamento));
    }
}
