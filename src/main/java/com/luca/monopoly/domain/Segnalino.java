package com.luca.monopoly.domain;

public enum Segnalino {
    CANE("Cane"),
    DINOSAURO("Dinosauro"),
    GATTO("Gatto"),
    MACCHINA("Macchina"),
    NAVE("Nave"),
    PAPERA("Papera"),
    PINGUINO("Pinguino"),
    SCARPONE("Scarpone");

    private String nome;

    private Segnalino(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
