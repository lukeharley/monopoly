package com.luca.monopoly.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Gli ultimi eventi della partita, dal più vecchio al più recente. Superata la
 * capienza esce il più vecchio: inserimento e rimozione costano O(1).
 */
public class RegistroEventi {

    private final int capienza;
    private final Deque<String> eventi = new ArrayDeque<>();

    RegistroEventi(int capienza) {
        this.capienza = capienza;
    }

    void registra(String evento) {
        eventi.addLast(evento);
        if (eventi.size() > capienza) {
            eventi.removeFirst();
        }
    }

    /** Copia immutabile: chi la riceve non vede le modifiche successive. */
    public List<String> elenco() {
        return List.copyOf(eventi);
    }
}
