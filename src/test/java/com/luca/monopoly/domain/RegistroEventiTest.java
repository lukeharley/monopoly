package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistroEventiTest {

	@Test
	void oltre_la_capienza_esce_l_evento_piu_vecchio() {
		RegistroEventi registro = new RegistroEventi(3);
		for (int evento = 1; evento <= 5; evento++) {
			registro.registra("evento " + evento);
		}
		assertEquals(List.of("evento 3", "evento 4", "evento 5"), registro.elenco());
	}

	@Test
	void l_elenco_e_una_copia_immutabile() {
		RegistroEventi registro = new RegistroEventi(3);
		registro.registra("primo");
		List<String> elenco = registro.elenco();
		registro.registra("secondo");

		assertEquals(List.of("primo"), elenco);
		assertThrows(UnsupportedOperationException.class, () -> elenco.add("altro"));
	}
}
