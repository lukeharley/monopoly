package com.luca.monopoly;

import com.luca.monopoly.domain.Segnalini;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Environment environment;

	@BeforeEach
	void resetta() throws Exception {
		mockMvc.perform(post("/resettaPartita")).andExpect(status().is3xxRedirection());
	}

	private void aggiungiGiocatore(String nome, Segnalini segnalino) throws Exception {
		mockMvc.perform(post("/").param("nome", nome).param("segnalino", segnalino.name()))
				.andExpect(status().is3xxRedirection());
	}

	private JsonNode stato() throws Exception {
		String json = mockMvc.perform(get("/stato")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json);
	}

	private void avviaPartitaConDueGiocatori() throws Exception {
		aggiungiGiocatore("Luca", Segnalini.CANE);
		aggiungiGiocatore("Mario", Segnalini.DINOSAURO);
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
		aggiungiGiocatore("Luca", Segnalini.CANE);
		mockMvc.perform(post("/").param("nome", "Mario").param("segnalino", "CANE"))
				.andExpect(flash().attribute("messaggio", "Il segnalino selezionato è già stato scelto."));
		mockMvc.perform(post("/").param("nome", "  ").param("segnalino", "DINOSAURO"))
				.andExpect(flash().attribute("messaggio", "Inserire il nome del giocatore."));
		mockMvc.perform(post("/").param("nome", "X".repeat(31)).param("segnalino", "DINOSAURO"))
				.andExpect(flash().attribute("messaggio", "Il nome può avere al massimo 30 caratteri."));
		mockMvc.perform(post("/").param("nome", "Mario").param("segnalino", "DRAGO"))
				.andExpect(flash().attribute("messaggio", "Segnalino non valido."));
		mockMvc.perform(post("/").param("nome", "Mario"))
				.andExpect(flash().attribute("messaggio", "Scegliere un segnalino."));
		// nessuno dei tentativi falliti è stato registrato
		assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM giocatore", Integer.class));
	}

	@Test
	void partita_con_un_solo_giocatore_non_parte() throws Exception {
		aggiungiGiocatore("Luca", Segnalini.CANE);
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

	// --- Sicurezza e integrità (PR 1 del piano di review) ---

	@Test
	void console_h2_disattivata_in_modo_esplicito() throws Exception {
		// devtools la abilita solo se la proprietà non è impostata: deve restare esplicita
		assertEquals("false", environment.getProperty("spring.h2.console.enabled"));
		mockMvc.perform(get("/h2-console/")).andExpect(status().isNotFound());
	}

	@Test
	void segnalino_salvato_per_nome() throws Exception {
		aggiungiGiocatore("Luca", Segnalini.CANE);
		assertEquals("CANE", jdbcTemplate.queryForObject("SELECT segnalino FROM giocatore", String.class));
	}

	@Test
	void ordine_dei_turni_uguale_all_ordine_di_registrazione() throws Exception {
		aggiungiGiocatore("Sara", Segnalini.SCARPONE);
		aggiungiGiocatore("Luca", Segnalini.CANE);
		aggiungiGiocatore("Mario", Segnalini.MACCHINA);
		mockMvc.perform(post("/avviaPartita")).andExpect(status().is3xxRedirection());

		JsonNode giocatori = stato().get("giocatori");
		assertEquals("Sara", giocatori.get(0).get("nome").asText());
		assertEquals("Luca", giocatori.get(1).get("nome").asText());
		assertEquals("Mario", giocatori.get(2).get("nome").asText());
	}

	@Test
	void giocatori_non_aggiungibili_a_partita_iniziata() throws Exception {
		avviaPartitaConDueGiocatori();
		mockMvc.perform(post("/").param("nome", "Anna").param("segnalino", "GATTO"))
				.andExpect(flash().attribute("messaggio",
						"La partita è già iniziata: resettala per aggiungere nuovi giocatori."));
		assertEquals(2, stato().get("giocatori").size());
	}

	@Test
	void post_da_un_altro_sito_rifiutato_con_403() throws Exception {
		avviaPartitaConDueGiocatori();

		mockMvc.perform(post("/resettaPartita").header("Sec-Fetch-Site", "cross-site"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errore").exists());
		mockMvc.perform(post("/lanciaDadi").header("Sec-Fetch-Site", "same-site"))
				.andExpect(status().isForbidden());
		// browser senza Sec-Fetch-Site: decide l'header Origin
		mockMvc.perform(post("/resettaPartita")
				.header("Origin", "https://sito-malevolo.example").header("Host", "localhost:8080"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/resettaPartita").header("Origin", "null").header("Host", "localhost:8080"))
				.andExpect(status().isForbidden());

		// la partita è ancora lì
		mockMvc.perform(get("/stato")).andExpect(status().isOk());
	}

	@Test
	void post_dalla_stessa_origine_accettato() throws Exception {
		avviaPartitaConDueGiocatori();

		mockMvc.perform(post("/lanciaDadi").header("Sec-Fetch-Site", "same-origin"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/fineTurno")
				.header("Origin", "http://localhost:8080").header("Host", "localhost:8080"))
				// 200 o 422 a seconda dei dadi: conta solo che il filtro l'abbia lasciata passare
				.andExpect(risultato -> assertNotEquals(403, risultato.getResponse().getStatus()));
		// dietro il proxy dei Codespaces l'host pubblico arriva in X-Forwarded-Host
		mockMvc.perform(post("/terminaPartita")
				.header("Origin", "https://gioco-8080.app.github.dev")
				.header("Host", "localhost:8080")
				.header("X-Forwarded-Host", "gioco-8080.app.github.dev"))
				.andExpect(status().isOk());
	}

	@Test
	void get_da_un_altro_sito_ammessa() throws Exception {
		avviaPartitaConDueGiocatori();
		mockMvc.perform(get("/stato").header("Sec-Fetch-Site", "cross-site")).andExpect(status().isOk());
	}

	@Test
	void stato_mai_in_cache() throws Exception {
		avviaPartitaConDueGiocatori();
		long versione = stato().get("versione").asLong();

		mockMvc.perform(get("/stato"))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
		mockMvc.perform(get("/stato").param("versione", String.valueOf(versione)))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
	}
}
