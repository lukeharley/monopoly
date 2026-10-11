package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MazzoTest {

	private final Imprevisto multa = new Imprevisto("Multa", EffettoCarta.PAGA, 10);
	private final Imprevisto esciGratis = new Imprevisto("Uscite gratis", EffettoCarta.ESCI_GRATIS_DI_PRIGIONE);
	private final Imprevisto premio = new Imprevisto("Premio", EffettoCarta.INCASSA, 10);

	@Test
	void le_carte_pescate_tornano_in_fondo() {
		Mazzo<Imprevisto> mazzo = new Mazzo<>(List.of(multa, premio), new Random(0), false);

		assertSame(multa, mazzo.pesca());
		assertSame(premio, mazzo.pesca());
		assertSame(multa, mazzo.pesca());
		assertEquals(2, mazzo.size());
	}

	@Test
	void la_carta_uscite_gratis_resta_al_giocatore_finche_non_la_usa() {
		Mazzo<Imprevisto> mazzo = new Mazzo<>(List.of(multa, esciGratis, premio), new Random(0), false);

		mazzo.pesca();
		assertSame(esciGratis, mazzo.pesca());
		assertEquals(2, mazzo.size());
		for (int pescata = 0; pescata < 10; pescata++) {
			assertNotSame(esciGratis, mazzo.pesca());
		}

		mazzo.rimettiInFondo(esciGratis);
		assertEquals(3, mazzo.size());
	}

	@Test
	void mescolato_contiene_le_stesse_carte() {
		Mazzo<Imprevisto> mazzo = new Mazzo<>(List.of(multa, premio), new Random(42), true);
		List<Imprevisto> pescate = List.of(mazzo.pesca(), mazzo.pesca());
		assertTrue(pescate.containsAll(List.of(multa, premio)));
	}
}
