package com.luca.monopoly.domain;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TabelloneTest {

	private Tabellone tabellone;

	@BeforeEach
	void setup() {
		tabellone = new Tabellone();
	}

	@Test
	void test_inizializzazione_monopoly() {

		assertNotNull(tabellone);

	}

	@Test
	void test_caselle() {

		List<Casella> caselle = tabellone.getCaselle();
		assertEquals(40, caselle.size());

		Casella primaCasella = caselle.get(0);
		assertEquals("Via", primaCasella.getTesto());
		assertFalse(primaCasella.isTerreno());
		assertNull(primaCasella.getGruppo());
		assertNull(primaCasella.getContratto());

		Casella secondaCasella = caselle.get(1);
		assertEquals("Vicolo Corto", secondaCasella.getTesto());
		assertTrue(secondaCasella.isTerreno());
		assertEquals(GruppoColore.FUCSIA, secondaCasella.getGruppo());
		assertEquals(50, secondaCasella.getCostoCasetta());

		assertEquals(TipoCasella.STAZIONE, caselle.get(5).getTipo());
		assertEquals(TipoCasella.SOCIETA, caselle.get(12).getTipo());
		assertEquals(TipoCasella.VAI_IN_PRIGIONE, caselle.get(30).getTipo());
		assertEquals(200, caselle.get(39).getCostoCasetta());

		assertEquals(28, caselle.stream().filter(Casella::isAcquistabile).count());

	}

	@Test
	void test_segnalini() {

		assertEquals(8, Segnalini.values().length);

	}

	@Test
	void test_contratti() {

		// indice delle rendite di un terreno = numero di edifici (0 = nudo, 5 = albergo)
		assertEquals(4, tabellone.getCasella(3).getContratto().getRendita(0));
		assertEquals(175, tabellone.getCasella(37).getContratto().getRendita(1));
		assertEquals(1000, tabellone.getCasella(19).getContratto().getRendita(Casella.ALBERGO));
		assertEquals(250, tabellone.getCasella(1).getContratto().getRendita(Casella.ALBERGO));
		assertEquals(1400, tabellone.getCasella(39).getContratto().getRendita(3));
		assertEquals(1700, tabellone.getCasella(39).getContratto().getRendita(4));

		// stazioni: rendita per 1, 2, 3, 4 stazioni possedute; società: moltiplicatori dei dadi
		assertEquals(List.of(25, 50, 100, 200), tabellone.getCasella(35).getContratto().getRendite());
		assertEquals(List.of(4, 10), tabellone.getCasella(12).getContratto().getRendite());

		// ogni proprietà ha un contratto, le altre caselle no
		for (Casella casella : tabellone.getCaselle()) {
			assertEquals(casella.isAcquistabile(), casella.getContratto() != null, casella.getTesto());
		}

	}

	@Test
	void test_gruppi_e_posizioni() {

		assertEquals(8, GruppoColore.values().length);
		assertEquals(22, tabellone.getCaselle().stream().filter(Casella::isTerreno).count());
		assertEquals(List.of(37, 39), tabellone.getCaselleDelGruppo(GruppoColore.VIOLA).stream()
				.map(Casella::getPosizione).toList());
		for (int posizione = 0; posizione < Tabellone.NUMERO_CASELLE; posizione++) {
			assertEquals(posizione, tabellone.getCasella(posizione).getPosizione());
		}
		assertEquals(200, tabellone.getCasella(4).getImportoTassa());
		assertEquals(100, tabellone.getCasella(38).getImportoTassa());

	}

	@Test
	void test_tabella_rendite() {

		// terreno: nudo, gruppo completo (doppio), 1-4 case, albergo
		assertEquals(List.of(2, 4, 10, 30, 90, 160, 250), CalcolatoreAffitto.tabellaRendite(tabellone.getCasella(1)));
		assertEquals(List.of(25, 50, 100, 200), CalcolatoreAffitto.tabellaRendite(tabellone.getCasella(5)));
		assertEquals(List.of(), CalcolatoreAffitto.tabellaRendite(tabellone.getCasella(12)));

	}

	@Test
	void test_imprevisti() {

		List<Imprevisto> imprevisti = tabellone.getImprevisti();
		assertEquals(16, imprevisti.size());
		assertEquals("Andate sino al Largo Colombo: se passate dal Via ritirate 200 euro.",
				imprevisti.get(0).getTesto());

	}

	@Test
	void test_probabilita() {

		List<Probabilita> probabilita = tabellone.getProbabilita();
		assertEquals(16, probabilita.size());
		assertEquals("Ritornate al Vicolo Corto.", probabilita.get(0).getTesto());

	}

	@Test
	void test_casette() {

		assertEquals(32, Banca.CASETTE_TOTALI);
		assertEquals(32, new Banca().getCasetteDisponibili());
	}

	@Test
	void test_alberghi() {

		assertEquals(12, Banca.ALBERGHI_TOTALI);
		assertEquals(12, new Banca().getAlberghiDisponibili());
	}

	@Test
	void test_dadi() {

		assertEquals(2, Partita.NUMERO_DADI);
		assertEquals(6, new Dado().getNumeroFacce());
	}

}
