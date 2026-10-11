package com.luca.monopoly.domain;

import java.util.Random;

public class Dado {
    private final int NUMERO_FACCE = 6;

    public int getNumeroFacce() {
        return NUMERO_FACCE;
    }

    public int lancia(Random random) {
        return random.nextInt(NUMERO_FACCE) + 1;
    }

}
