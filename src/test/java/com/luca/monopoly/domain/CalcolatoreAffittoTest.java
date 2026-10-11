package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import static com.luca.monopoly.domain.ScenarioPartita.*;
import static org.junit.jupiter.api.Assertions.*;

class CalcolatoreAffittoTest {

	private static final int DADI = 7;

	private final Tabellone tabellone = new Tabellone();
	private final CalcolatoreAffitto calcolatore = new CalcolatoreAffitto(tabellone);
	private final Giocatore mario = new Giocatore("Mario", Segnalino.DINOSAURO);

	private int affitto(int posizione) {
		return calcolatore.calcola(tabellone.getCasella(posizione), DADI);
	}

	@Test
	void proprieta_libera_o_ipotecata_non_rende() {
		assertEquals(0, affitto(1));
		tabellone.getCasella(1).assegnaA(mario);
		tabellone.getCasella(1).ipoteca();
		assertEquals(0, affitto(1));
	}

	@Test
	void terreno_nudo_doppio_con_il_monopolio_anche_se_un_altro_terreno_e_ipotecato() {
		tabellone.getCasella(1).assegnaA(mario);
		assertEquals(2, affitto(1));
		tabellone.getCasella(3).assegnaA(mario);
		tabellone.getCasella(3).ipoteca();
		assertEquals(4, affitto(1));
	}

	@Test
	void terreno_con_case_e_albergo() {
		tabellone.getCasella(39).assegnaA(mario);
		tabellone.getCasella(37).assegnaA(mario);
		conEdifici(tabellone.getCasella(39), 3);
		assertEquals(1400, affitto(39));
		conEdifici(tabellone.getCasella(39), 2);
		assertEquals(2000, affitto(39));
	}

	@Test
	void stazioni_25_50_100_200() {
		int[] posizioni = { 5, 15, 25, 35 };
		int[] attesi = { 25, 50, 100, 200 };
		for (int possedute = 0; possedute < posizioni.length; possedute++) {
			tabellone.getCasella(posizioni[possedute]).assegnaA(mario);
			assertEquals(attesi[possedute], affitto(5));
		}
	}

	@Test
	void societa_quattro_o_dieci_volte_i_dadi() {
		tabellone.getCasella(12).assegnaA(mario);
		assertEquals(4 * DADI, affitto(12));
		tabellone.getCasella(28).assegnaA(mario);
		assertEquals(10 * DADI, affitto(12));
	}
}
