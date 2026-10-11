package com.luca.monopoly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test end-to-end dell'API HTTP sul database H2 del profilo "dev".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiPartitaIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void resetta() throws Exception {
		mockMvc.perform(post("/resettaPartita")).andExpect(status().is3xxRedirection());
	}

	private void aggiungiGiocatore(String nome, int segnalino) throws Exception {
		mockMvc.perform(post("/").param("nome", nome).param("segnalino", String.valueOf(segnalino)))
				.andExpect(status().is3xxRedirection());
	}

	private JsonNode stato() throws Exception {
		String json = mockMvc.perform(get("/stato")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json);
	}

	private void avviaPartitaConDueGiocatori() throws Exception {
		aggiungiGiocatore("Luca", 0);
		aggiungiGiocatore("Mario", 1);
		mockMvc.perform(post("/avviaPartita")).andExpect(status().is3xxRedirection());
	}

	@Test
	void senza_partita_lo_stato_risponde_404() throws Exception {
		mockMvc.perform(get("/stato")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errore").exists());
		mockMvc.perform(post("/lanciaDadi")).andExpect(status().isNotFound());
	}

	@Test
	void segnalino_duplicato_e_nome_vuoto_rifiutati() throws Exception {
		aggiungiGiocatore("Luca", 0);
		mockMvc.perform(post("/").param("nome", "Mario").param("segnalino", "0"))
				.andExpect(flash().attribute("messaggio", "Il segnalino selezionato è già stato scelto."));
		mockMvc.perform(post("/").param("nome", "  ").param("segnalino", "1"))
				.andExpect(flash().attribute("messaggio", "Inserire il nome del giocatore."));
		mockMvc.perform(post("/").param("nome", "X".repeat(31)).param("segnalino", "1"))
				.andExpect(flash().attributeExists("messaggio"));
	}

	@Test
	void partita_con_un_solo_giocatore_non_parte() throws Exception {
		aggiungiGiocatore("Luca", 0);
		mockMvc.perform(post("/avviaPartita"))
				.andExpect(flash().attribute("messaggio", "Servono almeno 2 giocatori per iniziare la partita."));
		mockMvc.perform(get("/stato")).andExpect(status().isNotFound());
	}

	@Test
	void lancio_dei_dadi_aggiorna_lo_stato_e_la_versione() throws Exception {
		avviaPartitaConDueGiocatori();
		JsonNode iniziale = stato();
		assertEquals("LANCIO_DADI", iniziale.get("fase").asText());
		assertEquals(40, iniziale.get("caselle").size());
		long versione = iniziale.get("versione").asLong();

		mockMvc.perform(get("/stato").param("versione", String.valueOf(versione)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/lanciaDadi").param("versione", String.valueOf(versione)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versione").value(versione + 1))
				.andExpect(jsonPath("$.dado1").isNumber());
	}

	@Test
	void mossa_su_versione_superata_rifiutata_con_409() throws Exception {
		avviaPartitaConDueGiocatori();
		long versione = stato().get("versione").asLong();

		mockMvc.perform(post("/lanciaDadi").param("versione", String.valueOf(versione)))
				.andExpect(status().isOk());
		// un secondo client che non ha visto il lancio prova a muovere
		mockMvc.perform(post("/terminaPartita").param("versione", String.valueOf(versione)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errore").exists());
		assertNotEquals("TERMINATA", stato().get("fase").asText());
	}

	@Test
	void mossa_non_ammessa_rifiutata_con_422() throws Exception {
		avviaPartitaConDueGiocatori();
		mockMvc.perform(post("/fineTurno"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errore").value("Non puoi ancora passare il turno."));
		mockMvc.perform(post("/offerta").param("importo", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void reset_solo_con_post() throws Exception {
		avviaPartitaConDueGiocatori();
		mockMvc.perform(get("/resettaPartita")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(post("/resettaPartita")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/stato")).andExpect(status().isNotFound());
	}

	@Test
	void terminare_la_partita_proclama_un_vincitore() throws Exception {
		avviaPartitaConDueGiocatori();
		mockMvc.perform(post("/terminaPartita"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fase").value("TERMINATA"))
				.andExpect(jsonPath("$.vincitore").isNumber());
	}
}
