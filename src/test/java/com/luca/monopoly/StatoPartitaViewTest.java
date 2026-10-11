package com.luca.monopoly;

import com.luca.monopoly.domain.Giocatore;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalini;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatoPartitaViewTest {

	private final Partita partita = new Partita(
			List.of(new Giocatore("Luca", Segnalini.CANE), new Giocatore("Mario", Segnalini.DINOSAURO)));

	@Test
	void stato_iniziale() {
		StatoPartitaView stato = StatoPartitaView.da(partita, 7);

		assertEquals(7, stato.versione());
		assertEquals("LANCIO_DADI", stato.fase());
		assertEquals(0, stato.giocatoreCorrente());
		assertNull(stato.vincitore());
		assertNull(stato.asta());
		assertNull(stato.debito());
		assertNull(stato.ultimaCarta());
		assertEquals(List.of("Luca", "Mario"), stato.giocatori().stream().map(g -> g.nome()).toList());
		assertEquals("Cane", stato.giocatori().get(0).segnalino());
		assertEquals(Partita.PORTAFOGLIO_INIZIALE, stato.giocatori().get(1).portafoglio());
	}

	@Test
	void caselle_con_posizione_colore_costo_e_rendite() {
		List<StatoPartitaView.CasellaView> caselle = StatoPartitaView.da(partita, 0).caselle();

		assertEquals(40, caselle.size());
		for (int posizione = 0; posizione < caselle.size(); posizione++) {
			assertEquals(posizione, caselle.get(posizione).posizione());
		}
		StatoPartitaView.CasellaView vicoloCorto = caselle.get(1);
		assertEquals("fucsia", vicoloCorto.colore());
		assertEquals(60, vicoloCorto.costo());
		assertNull(vicoloCorto.proprietario());
		assertEquals(List.of(2, 4, 10, 30, 90, 160, 250), vicoloCorto.rendite());
		assertEquals(200, caselle.get(4).costo(), "per le tasse il costo è l'importo da pagare");
		assertNull(caselle.get(0).colore());
	}

	@Test
	void vincitore_come_indice_del_giocatore() {
		partita.terminaPartita();
		StatoPartitaView stato = StatoPartitaView.da(partita, 0);
		assertEquals("TERMINATA", stato.fase());
		assertNotNull(stato.vincitore());
		assertTrue(stato.vincitore() == 0 || stato.vincitore() == 1);
	}
}
