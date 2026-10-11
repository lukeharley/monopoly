package com.luca.monopoly.web;

import com.luca.monopoly.domain.Segnalino;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HomeFormTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private Set<String> errori(String nome, Segnalino segnalino) {
		HomeForm form = new HomeForm();
		form.setNome(nome);
		form.setSegnalino(segnalino);
		return validator.validate(form).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
	}

	@Test
	void form_valido() {
		assertTrue(errori("Luca", Segnalino.CANE).isEmpty());
		assertTrue(errori("X".repeat(HomeForm.LUNGHEZZA_MASSIMA_NOME), Segnalino.CANE).isEmpty());
	}

	@Test
	void nome_obbligatorio_e_non_troppo_lungo() {
		assertEquals(Set.of("Inserire il nome del giocatore."), errori("   ", Segnalino.CANE));
		assertEquals(Set.of("Inserire il nome del giocatore."), errori(null, Segnalino.CANE));
		assertEquals(Set.of("Il nome può avere al massimo 30 caratteri."), errori("X".repeat(31), Segnalino.CANE));
	}

	@Test
	void segnalino_obbligatorio() {
		assertEquals(Set.of("Scegliere un segnalino."), errori("Luca", null));
	}
}
