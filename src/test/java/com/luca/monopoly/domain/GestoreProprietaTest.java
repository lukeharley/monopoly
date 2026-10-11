package com.luca.monopoly.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.luca.monopoly.domain.ScenarioPartita.*;
import static org.junit.jupiter.api.Assertions.*;

class GestoreProprietaTest {

	private final Tabellone tabellone = new Tabellone();
	private final Banca banca = new Banca();
	private final List<String> eventi = new ArrayList<>();
	private final GestoreProprieta gestore = new GestoreProprieta(tabellone, banca, eventi::add);
	private final Giocatore luca = new Giocatore("Luca", Segnalino.CANE);
	private Casella vicoloCorto;
	private Casella vicoloStretto;

	@BeforeEach
	void setup() {
		vicoloCorto = tabellone.getCasella(1);
		vicoloStretto = tabellone.getCasella(3);
		vicoloCorto.assegnaA(luca);
		vicoloStretto.assegnaA(luca);
	}

	@Test
	void niente_alberghi_in_banca_niente_albergo() {
		conEdifici(vicoloCorto, Casella.MASSIMO_CASE);
		conEdifici(vicoloStretto, Casella.MASSIMO_CASE);
		for (int albergo = 0; albergo < Banca.ALBERGHI_TOTALI; albergo++) {
			banca.prelevaAlbergo();
		}

		assertThrows(MossaNonValidaException.class, () -> gestore.costruisci(luca, vicoloCorto));
		assertEquals(Casella.MASSIMO_CASE, vicoloCorto.getNumeroEdifici());
		assertEquals(Partita.PORTAFOGLIO_INIZIALE, luca.getPortafoglio());
	}

	@Test
	void niente_case_in_banca_niente_casa() {
		banca.prelevaCasette(Banca.CASETTE_TOTALI);
		assertThrows(MossaNonValidaException.class, () -> gestore.costruisci(luca, vicoloCorto));
		assertEquals(0, vicoloCorto.getNumeroEdifici());
	}

	@Test
	void albergo_senza_quattro_case_in_banca_si_vende_solo_per_bancarotta() {
		conEdifici(vicoloCorto, Casella.ALBERGO);
		banca.prelevaAlbergo();
		banca.prelevaCasette(Banca.CASETTE_TOTALI);

		assertThrows(MossaNonValidaException.class, () -> gestore.vendiEdificio(luca, vicoloCorto, false));

		gestore.liquidaEdifici(luca);
		assertEquals(0, vicoloCorto.getNumeroEdifici());
		assertEquals(Banca.ALBERGHI_TOTALI, banca.getAlberghiDisponibili());
		// tutti e 5 gli edifici rivenduti a metà prezzo: 5 x 25€
		assertEquals(Partita.PORTAFOGLIO_INIZIALE + 125, luca.getPortafoglio());
		assertTrue(eventi.contains("Luca vende l'albergo su Vicolo Corto e incassa 125€."));
	}

	@Test
	void albergo_venduto_torna_quattro_case() {
		conEdifici(vicoloCorto, Casella.ALBERGO);
		conEdifici(vicoloStretto, Casella.ALBERGO);
		banca.prelevaAlbergo();
		banca.prelevaAlbergo();

		gestore.vendiEdificio(luca, vicoloCorto, false);

		assertEquals(Casella.MASSIMO_CASE, vicoloCorto.getNumeroEdifici());
		assertEquals(Banca.CASETTE_TOTALI - Casella.MASSIMO_CASE, banca.getCasetteDisponibili());
		assertEquals(Banca.ALBERGHI_TOTALI - 1, banca.getAlberghiDisponibili());
		// la vendita uniforme ora impone di vendere l'albergo rimasto
		assertThrows(MossaNonValidaException.class, () -> gestore.vendiEdificio(luca, vicoloCorto, false));
	}

	@Test
	void ipoteca_vietata_con_edifici_nel_gruppo() {
		conEdifici(vicoloStretto, 1);
		assertThrows(MossaNonValidaException.class, () -> gestore.ipoteca(luca, vicoloCorto));
	}

	@Test
	void valore_liquidabile_conta_contanti_edifici_e_ipoteche() {
		conEdifici(vicoloCorto, 2);
		vicoloStretto.ipoteca();
		// 1500 + 2 case x 25€ + ipoteca di Vicolo Corto (30€); Vicolo Stretto è già ipotecato
		assertEquals(1500 + 50 + 30, gestore.valoreLiquidabile(luca));
	}
}
