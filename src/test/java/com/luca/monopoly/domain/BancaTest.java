package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BancaTest {

	private final Banca banca = new Banca();

	@Test
	void parte_con_tutti_i_pezzi_della_scatola() {
		assertEquals(Banca.CASETTE_TOTALI, banca.getCasetteDisponibili());
		assertEquals(Banca.ALBERGHI_TOTALI, banca.getAlberghiDisponibili());
	}

	@Test
	void non_si_preleva_oltre_le_scorte() {
		banca.prelevaCasette(Banca.CASETTE_TOTALI);
		assertThrows(MossaNonValidaException.class, () -> banca.prelevaCasette(1));
		banca.restituisciCasette(4);
		assertEquals(4, banca.getCasetteDisponibili());

		for (int albergo = 0; albergo < Banca.ALBERGHI_TOTALI; albergo++) {
			banca.prelevaAlbergo();
		}
		assertThrows(MossaNonValidaException.class, banca::prelevaAlbergo);
		banca.restituisciAlbergo();
		assertEquals(1, banca.getAlberghiDisponibili());
	}
}
