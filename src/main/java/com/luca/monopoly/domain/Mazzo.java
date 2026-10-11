package com.luca.monopoly.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Mazzo di carte Imprevisti o Probabilità: si pesca dalla cima e la carta
 * pescata torna in fondo al mazzo. Le carte "Uscite gratis di prigione"
 * restano invece al giocatore finché non vengono usate.
 */
public class Mazzo<T extends Carta> {

    private final Deque<T> carte;

    public Mazzo(List<T> carte, Random random, boolean mescola) {
        List<T> copia = new ArrayList<>(carte);
        if (mescola) {
            Collections.shuffle(copia, random);
        }
        this.carte = new ArrayDeque<>(copia);
    }

    public T pesca() {
        T carta = carte.pollFirst();
        if (carta.getEffetto() != EffettoCarta.ESCI_GRATIS_DI_PRIGIONE) {
            carte.addLast(carta);
        }
        return carta;
    }

    public void rimettiInFondo(Carta carta) {
        @SuppressWarnings("unchecked")
        T cartaDelMazzo = (T) carta;
        carte.addLast(cartaDelMazzo);
    }

    public int size() {
        return carte.size();
    }

}
