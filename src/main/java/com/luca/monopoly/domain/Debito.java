package com.luca.monopoly.domain;

import com.luca.monopoly.domain.giocatore.Giocatore;

/**
 * Pagamento che il giocatore di turno non riesce a coprire con i contanti.
 *
 * @param creditore             giocatore da pagare, oppure null se è la banca
 * @param importo               somma dovuta
 * @param passiDopoIlPagamento  caselle da percorrere una volta saldato il debito
 *                              (multa al terzo turno in prigione), altrimenti 0
 */
public record Debito(Giocatore creditore, int importo, int passiDopoIlPagamento) {
}
