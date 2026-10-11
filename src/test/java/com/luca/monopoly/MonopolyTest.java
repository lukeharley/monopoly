package com.luca.monopoly;

import com.luca.monopoly.domain.*;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonopolyTest {

	private Monopoly monopoly;

	@BeforeEach
	void setup() {
		monopoly = new Monopoly();
	}

	@Order(1)
	@Test
	void test_inizializzazione_monopoly() {

		assertNotNull(monopoly);

	}

	@Order(2)
	@Test
	void test_caselle() {

		Tabellone tabellone = monopoly.getTabellone();

		List<Casella> caselle = tabellone.getCaselle();
		assertEquals(40, caselle.size());

		Casella primaCasella = caselle.get(0);
		assertEquals("Via", primaCasella.getTesto());
		assertFalse(primaCasella.isTerreno());
		assertNull(primaCasella.getColore());

		Casella secondaCasella = caselle.get(1);
		assertEquals("Vicolo Corto", secondaCasella.getTesto());
		assertTrue(secondaCasella.isTerreno());
		assertEquals("fucsia", secondaCasella.getColore());
		assertEquals(50, secondaCasella.getCostoCasetta());

		assertEquals(TipoCasella.STAZIONE, caselle.get(5).getTipo());
		assertEquals(TipoCasella.SOCIETA, caselle.get(12).getTipo());
		assertEquals(TipoCasella.VAI_IN_PRIGIONE, caselle.get(30).getTipo());
		assertEquals(200, caselle.get(39).getCostoCasetta());

		assertEquals(28, caselle.stream().filter(Casella::isAcquistabile).count());

	}

	@Order(3)
	@Test
	void test_segnalini() {

		List<Segnalini> segnalini = monopoly.getSegnalini();
		assertEquals(8, segnalini.size());

	}

	@Order(4)
	@Test
	void test_contratti() {

		Tabellone tabellone = monopoly.getTabellone();
		List<Contratto> contratti = tabellone.getContratti();

		assertEquals(40, contratti.size());

		assertEquals("Vicolo Corto", contratti.get(1).getTesto());
		assertEquals(4, contratti.get(3).getRenditaTerreno());
		assertEquals(175, contratti.get(37).getRenditaUnaCasetta());
		assertEquals(1000, contratti.get(19).getRenditaAlbergo());
		assertEquals(250, contratti.get(1).getRenditaAlbergo());
		assertEquals(1400, contratti.get(39).getRenditaTreCasette());
		assertEquals(1700, contratti.get(39).getRenditaQuattroCasette());
		assertEquals(65, contratti.get(35).getRenditaUnaStazione());
		assertEquals(130, contratti.get(35).getRenditaDueStazioni());

		// ogni casella acquistabile ha il proprio contratto nella stessa posizione
		for (int posizione = 0; posizione < 40; posizione++) {
			Casella casella = tabellone.getCasella(posizione);
			if (casella.isAcquistabile()) {
				assertEquals(casella.getTesto(), tabellone.getContratto(posizione).getTesto());
			}
		}

	}

	@Order(5)
	@Test
	void test_imprevisti() {

		List<Imprevisto> imprevisti = monopoly.getTabellone().getImprevisti();
		assertEquals(16, imprevisti.size());
		assertEquals("Andate sino al Largo Colombo: se passate dal Via ritirate 200 euro.",
				imprevisti.get(0).getTesto());

	}

	@Order(6)
	@Test
	void test_probabilita() {

		List<Probabilita> probabilita = monopoly.getTabellone().getProbabilita();
		assertEquals(16, probabilita.size());
		assertEquals("Ritornate al Vicolo Corto.", probabilita.get(0).getTesto());

	}

	@Order(7)
	@Test
	void test_casette() {

		Tabellone tabellone = monopoly.getTabellone();

		List<Casetta> casette = tabellone.getCasette();
		assertEquals(32, casette.size());
	}

	@Order(8)
	@Test
	void test_alberghi() {

		Tabellone tabellone = monopoly.getTabellone();

		List<Albergo> alberghi = tabellone.getAlberghi();
		assertEquals(12, alberghi.size());
	}

	@Order(9)
	@Test
	void test_dadi() {

		List<Dado> dadi = monopoly.getDadi();
		assertEquals(2, dadi.size());

		Dado primoDado = dadi.get(0);
		assertEquals(6, primoDado.getNumeroFacce());
	}

}
