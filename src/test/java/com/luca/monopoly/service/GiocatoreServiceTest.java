package com.luca.monopoly.service;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.repository.JpaGiocatore;
import com.luca.monopoly.repository.JpaGiocatoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GiocatoreServiceTest {

	private final JpaGiocatoreRepository repository = mock(JpaGiocatoreRepository.class);
	private final GiocatoreService giocatoreService = new GiocatoreService(repository);

	@Test
	void segnalino_preso_da_un_altro_client_dopo_il_controllo() {
		// il controllo passa, ma nel frattempo qualcun altro ha salvato lo stesso segnalino
		when(repository.findBySegnalino(Segnalini.CANE)).thenReturn(null);
		when(repository.saveAndFlush(any(JpaGiocatore.class)))
				.thenThrow(new DataIntegrityViolationException("uk_giocatore_segnalino"));

		MossaNonValidaException eccezione = assertThrows(MossaNonValidaException.class,
				() -> giocatoreService.aggiungiGiocatore("Luca", Segnalini.CANE));
		assertEquals("Il segnalino selezionato è già stato scelto.", eccezione.getMessage());
	}
}
