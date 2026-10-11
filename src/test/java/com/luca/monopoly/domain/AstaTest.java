package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstaTest {

	private final Casella vicoloStretto = new Tabellone().getCasella(3);
	private final Giocatore anna = new Giocatore("Anna", Segnalino.GATTO);
	private final Giocatore luca = new Giocatore("Luca", Segnalino.CANE);
	private final Giocatore mario = new Giocatore("Mario", Segnalino.DINOSAURO);

	@Test
	void con_tre_partecipanti_i_ritiri_non_saltano_nessun_turno() {
		Asta asta = new Asta(vicoloStretto, List.of(anna, luca, mario));

		asta.offri(anna, 10);
		assertEquals(luca, asta.getOfferenteDiTurno());
		asta.ritirati(luca);
		assertEquals(mario, asta.getOfferenteDiTurno());
		asta.offri(mario, 20);
		assertEquals(anna, asta.getOfferenteDiTurno());
		assertFalse(asta.isConclusa());
		asta.ritirati(anna);

		assertTrue(asta.isConclusa());
		assertEquals(mario, asta.getMigliorOfferente());
		assertEquals(20, asta.getOffertaMassima());
		assertEquals(List.of(mario), asta.getPartecipanti());
	}

	@Test
	void ritiro_dell_ultimo_della_lista_riparte_dal_primo() {
		Asta asta = new Asta(vicoloStretto, List.of(anna, luca, mario));
		asta.ritirati(anna);
		asta.ritirati(luca);
		assertEquals(mario, asta.getOfferenteDiTurno());
		asta.offri(mario, 1);
		assertTrue(asta.isConclusa());
	}

	@Test
	void senza_offerte_nessun_vincitore() {
		Asta asta = new Asta(vicoloStretto, List.of(anna, luca));
		asta.ritirati(anna);
		assertFalse(asta.isConclusa());
		asta.ritirati(luca);

		assertTrue(asta.isConclusa());
		assertNull(asta.getMigliorOfferente());
		assertNull(asta.getOfferenteDiTurno());
	}

	@Test
	void mosse_non_ammesse() {
		Asta asta = new Asta(vicoloStretto, List.of(anna, luca));

		assertThrows(MossaNonValidaException.class, () -> asta.offri(luca, 10), "non è il suo turno");
		assertThrows(MossaNonValidaException.class, () -> asta.offri(anna, 0), "offerta non positiva");
		assertThrows(MossaNonValidaException.class, () -> asta.offri(anna, Partita.PORTAFOGLIO_INIZIALE + 1),
				"oltre i contanti");
		asta.offri(anna, 10);
		assertThrows(MossaNonValidaException.class, () -> asta.offri(luca, 10), "deve superare l'offerta");
		asta.ritirati(luca);
		assertThrows(MossaNonValidaException.class, () -> asta.offri(anna, 20), "asta conclusa");
	}
}
