package com.luca.monopoly.domain;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Random;

import static com.luca.monopoly.domain.ScenarioPartita.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del motore di gioco. I mazzi non vengono mescolati e i dadi sono
 * impostati a mano, così ogni partita è deterministica.
 */
public class PartitaTest {

	private Partita partita;
	private Tabellone tabellone;
	private Giocatore luca;
	private Giocatore mario;

	@BeforeEach
	void setup() {
		luca = new Giocatore("Luca", Segnalino.CANE);
		mario = new Giocatore("Mario", Segnalino.DINOSAURO);
		partita = new Partita(List.of(luca, mario), new Random(0), false);
		tabellone = partita.getTabellone();
	}

	/** Rinuncia all'acquisto e chiude l'asta senza offerte: la proprietà resta alla banca. */
	private void rinunciaSenzaOfferte() {
		partita.rifiutaAcquisto();
		while (partita.getFase() == FasePartita.ASTA) {
			partita.ritiratiDallAsta();
		}
	}

	@Test
	void servono_almeno_due_giocatori() {
		assertThrows(MossaNonValidaException.class,
				() -> new Partita(List.of(luca), new Random(0), false));
	}

	@Test
	void lancio_dei_dadi_casuale_tra_2_e_12() {
		partita.lanciaDadi();
		int risultato = partita.getDado1() + partita.getDado2();
		assertTrue(risultato >= 2 && risultato <= 12);
	}

	@Test
	void acquisto_proprieta_e_cambio_turno() {
		partita.lanciaDadi(1, 2); // Vicolo Stretto
		assertEquals(FasePartita.DECISIONE_ACQUISTO, partita.getFase());

		partita.compraProprieta();
		assertEquals(luca, tabellone.getCasella(3).getProprietario());
		assertEquals(1440, luca.getPortafoglio());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());

		assertThrows(MossaNonValidaException.class, () -> partita.lanciaDadi(1, 2));

		partita.fineTurno();
		assertEquals(mario, partita.getGiocatoreCorrente());
	}

	@Test
	void affitto_versato_al_proprietario() {
		partita.lanciaDadi(1, 2);
		partita.compraProprieta();
		partita.fineTurno();

		partita.lanciaDadi(1, 2);
		assertEquals(1496, mario.getPortafoglio());
		assertEquals(1444, luca.getPortafoglio());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void affitto_doppio_con_monopolio_e_con_case() {
		Casella vicoloCorto = tabellone.getCasella(1);
		Casella vicoloStretto = tabellone.getCasella(3);
		vicoloCorto.assegnaA(luca);
		vicoloStretto.assegnaA(luca);

		assertEquals(8, partita.calcolaAffitto(vicoloStretto));

		partita.costruisciCasetta(3);
		assertEquals(1450, luca.getPortafoglio());
		assertEquals(20, partita.calcolaAffitto(vicoloStretto));
	}

	@Test
	void rifiuto_acquisto_senza_offerte_la_proprieta_resta_alla_banca() {
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		assertEquals(FasePartita.ASTA, partita.getFase());
		// offre per primo il giocatore successivo
		assertEquals(mario, partita.getAsta().getOfferenteDiTurno());

		partita.ritiratiDallAsta();
		partita.ritiratiDallAsta();
		assertNull(tabellone.getCasella(3).getProprietario());
		assertNull(partita.getAsta());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void asta_vinta_dall_offerta_piu_alta() {
		partita.lanciaDadi(1, 2); // Vicolo Stretto
		partita.rifiutaAcquisto();

		partita.offri(10); // Mario
		assertThrows(MossaNonValidaException.class, () -> partita.offri(10)); // Luca: non supera 10
		assertThrows(MossaNonValidaException.class, () -> partita.offri(5000)); // oltre i contanti
		partita.offri(20); // Luca
		partita.ritiratiDallAsta(); // Mario

		assertEquals(luca, tabellone.getCasella(3).getProprietario());
		assertEquals(1480, luca.getPortafoglio());
		assertEquals(1500, mario.getPortafoglio());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void asta_vinta_da_un_altro_giocatore() {
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		partita.offri(70); // Mario
		partita.ritiratiDallAsta(); // Luca

		assertEquals(mario, tabellone.getCasella(3).getProprietario());
		assertEquals(1430, mario.getPortafoglio());
		// il turno resta di Luca, che ora può passarlo
		assertEquals(luca, partita.getGiocatoreCorrente());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void doppio_permette_di_rilanciare() {
		partita.lanciaDadi(2, 2); // Tassa Patrimoniale
		assertEquals(1300, luca.getPortafoglio());
		assertEquals(FasePartita.LANCIO_DADI, partita.getFase());
		assertEquals(luca, partita.getGiocatoreCorrente());
	}

	@Test
	void tre_doppi_consecutivi_mandano_in_prigione() {
		partita.lanciaDadi(5, 5); // Prigione/Transito
		partita.lanciaDadi(5, 5); // Posteggio Gratuito
		partita.lanciaDadi(1, 1);
		assertTrue(luca.isInPrigione());
		assertEquals(Tabellone.POSIZIONE_PRIGIONE, luca.getPosizione());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void passaggio_dal_via() {
		luca.spostaA(38);
		partita.lanciaDadi(1, 3); // Probabilità (posizione 2): "Ritornate al Vicolo Corto"
		assertEquals(1, luca.getPosizione());
		assertEquals(1700, luca.getPortafoglio());
	}

	@Test
	void casella_vai_in_prigione() {
		luca.spostaA(24);
		partita.lanciaDadi(2, 4);
		assertTrue(luca.isInPrigione());
		assertEquals(10, luca.getPosizione());
		assertEquals(1500, luca.getPortafoglio());
	}

	@Test
	void uscita_di_prigione_pagando_la_multa() {
		inPrigione(luca, 0);
		partita.pagaUscitaDiPrigione();
		assertFalse(luca.isInPrigione());
		assertEquals(1500 - Partita.MULTA_PRIGIONE, luca.getPortafoglio());

		partita.lanciaDadi(5, 6); // Via Marco Polo
		assertEquals(21, luca.getPosizione());
	}

	@Test
	void uscita_di_prigione_con_un_doppio() {
		inPrigione(luca, 0);
		partita.lanciaDadi(3, 3);
		assertFalse(luca.isInPrigione());
		assertEquals(16, luca.getPosizione());
		rinunciaSenzaOfferte();
		// uscendo con un doppio non si rilancia
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void dopo_tre_turni_in_prigione_si_paga_e_si_esce() {
		inPrigione(luca, 0);
		mario.spostaA(20);

		for (int tentativo = 1; tentativo <= 2; tentativo++) {
			partita.lanciaDadi(1, 2);
			assertTrue(luca.isInPrigione());
			partita.fineTurno();
			partita.lanciaDadi(1, 1); // Mario: Posteggio -> Corso Magellano (doppio)
			if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
				rinunciaSenzaOfferte();
			}
			while (partita.getFase() == FasePartita.LANCIO_DADI) {
				partita.lanciaDadi(1, 2);
				if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
					rinunciaSenzaOfferte();
				}
			}
			partita.fineTurno();
		}

		partita.lanciaDadi(1, 2);
		assertFalse(luca.isInPrigione());
		assertEquals(13, luca.getPosizione());
		assertEquals(1500 - Partita.MULTA_PRIGIONE, luca.getPortafoglio());
	}

	@Test
	void carta_uscite_gratis_di_prigione() {
		// la sesta carta Probabilità (indice 5) è "Uscite gratis di prigione":
		// i giocatori pescano a turno, quindi tocca a Mario
		for (int carta = 0; carta < 6; carta++) {
			partita.getGiocatoreCorrente().spostaA(39);
			partita.lanciaDadi(1, 2); // Probabilità (posizione 2)
			if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
				rinunciaSenzaOfferte();
			}
			partita.fineTurno();
		}
		assertEquals(0, luca.getNumeroCarteUscitaDiPrigione());
		assertEquals(1, mario.getNumeroCarteUscitaDiPrigione());

		// Mario finisce in prigione e usa la carta
		partita.lanciaDadi(1, 2);
		rinunciaSenzaOfferte();
		partita.fineTurno();
		inPrigione(mario, 0);
		partita.usaCartaUscitaDiPrigione();
		assertFalse(mario.isInPrigione());
		assertEquals(0, mario.getNumeroCarteUscitaDiPrigione());
	}

	@Test
	void stazioni_affitto_cresce_con_il_numero_di_stazioni() {
		Casella stazioneSud = tabellone.getCasella(5);
		stazioneSud.assegnaA(mario);
		assertEquals(25, partita.calcolaAffitto(stazioneSud));
		tabellone.getCasella(15).assegnaA(mario);
		tabellone.getCasella(25).assegnaA(mario);
		assertEquals(100, partita.calcolaAffitto(stazioneSud));

		partita.lanciaDadi(2, 3);
		assertEquals(1400, luca.getPortafoglio());
		assertEquals(1600, mario.getPortafoglio());
	}

	@Test
	void societa_affitto_proporzionale_ai_dadi() {
		tabellone.getCasella(12).assegnaA(mario);
		luca.spostaA(7);
		partita.lanciaDadi(2, 3);
		assertEquals(1500 - 20, luca.getPortafoglio());
	}

	@Test
	void costruzione_uniforme_e_albergo() {
		tabellone.getCasella(6).assegnaA(luca);
		tabellone.getCasella(8).assegnaA(luca);
		tabellone.getCasella(9).assegnaA(luca);

		partita.costruisciCasetta(6);
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(6));
		partita.costruisciCasetta(8);
		partita.costruisciCasetta(9);

		for (int giro = 0; giro < 4; giro++) {
			partita.costruisciCasetta(6);
			partita.costruisciCasetta(8);
			partita.costruisciCasetta(9);
		}
		assertTrue(tabellone.getCasella(6).haAlbergo());
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(6));
		assertEquals(1500 - 15 * 50, luca.getPortafoglio());
		assertEquals(32, partita.getCasetteDisponibili());
		assertEquals(9, partita.getAlberghiDisponibili());

		partita.vendiCasetta(9);
		assertEquals(4, tabellone.getCasella(9).getNumeroEdifici());
		assertEquals(1500 - 15 * 50 + 25, luca.getPortafoglio());
	}

	@Test
	void non_si_costruisce_senza_monopolio() {
		tabellone.getCasella(6).assegnaA(luca);
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(6));
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(8));
	}

	@Test
	void ipoteca_e_riscatto() {
		Casella vicoloStretto = tabellone.getCasella(3);
		vicoloStretto.assegnaA(luca);

		partita.ipoteca(3);
		assertTrue(vicoloStretto.isIpotecata());
		assertEquals(1530, luca.getPortafoglio());
		assertEquals(0, partita.calcolaAffitto(vicoloStretto));

		partita.riscattaIpoteca(3);
		assertFalse(vicoloStretto.isIpotecata());
		assertEquals(1497, luca.getPortafoglio());
	}

	@Test
	void bancarotta_e_vittoria() {
		Casella parco = tabellone.getCasella(39);
		parco.assegnaA(mario);
		conEdifici(parco, Casella.ALBERGO);
		Casella vicoloCorto = tabellone.getCasella(1);
		vicoloCorto.assegnaA(luca);

		conContanti(luca, 100);
		luca.spostaA(35);
		partita.lanciaDadi(1, 3);

		assertTrue(luca.isInBancarotta());
		assertEquals(FasePartita.TERMINATA, partita.getFase());
		assertEquals(mario, partita.getVincitore());
		assertEquals(mario, vicoloCorto.getProprietario());
		assertEquals(1500 + 100, mario.getPortafoglio());
	}

	@Test
	void bancarotta_con_tre_giocatori_passa_il_turno() {
		Giocatore anna = new Giocatore("Anna", Segnalino.GATTO);
		partita = new Partita(List.of(luca, mario, anna), new Random(0), false);

		conContanti(luca, 50);
		partita.lanciaDadi(1, 3); // Tassa Patrimoniale: 200 euro

		assertTrue(luca.isInBancarotta());
		assertEquals(FasePartita.LANCIO_DADI, partita.getFase());
		assertEquals(mario, partita.getGiocatoreCorrente());

		partita.lanciaDadi(1, 2);
		rinunciaSenzaOfferte();
		partita.fineTurno();
		assertEquals(anna, partita.getGiocatoreCorrente());
		partita.lanciaDadi(1, 2);
		rinunciaSenzaOfferte();
		partita.fineTurno();
		// Luca è fuori dal gioco: si torna a Mario
		assertEquals(mario, partita.getGiocatoreCorrente());
	}

	@Test
	void debito_saldato_vendendo_edifici_e_ipotecando() {
		tabellone.getCasella(1).assegnaA(luca);
		tabellone.getCasella(3).assegnaA(luca);
		partita.costruisciCasetta(1);
		partita.costruisciCasetta(3);
		conContanti(luca, 100);

		partita.lanciaDadi(1, 3); // Tassa Patrimoniale: 200 euro

		assertEquals(FasePartita.SALDO_DEBITO, partita.getFase());
		assertEquals(200, partita.getDebito().importo());
		assertThrows(MossaNonValidaException.class, () -> partita.fineTurno());
		assertThrows(MossaNonValidaException.class, () -> partita.pagaDebito());
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(1));

		partita.vendiCasetta(1);
		partita.vendiCasetta(3);
		partita.ipoteca(1);
		partita.ipoteca(3);
		assertEquals(210, luca.getPortafoglio());

		partita.pagaDebito();
		assertEquals(10, luca.getPortafoglio());
		assertNull(partita.getDebito());
		assertEquals(32, partita.getCasetteDisponibili());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void debito_per_affitto_pagato_al_proprietario() {
		tabellone.getCasella(3).assegnaA(mario);
		tabellone.getCasella(1).assegnaA(mario);
		tabellone.getCasella(39).assegnaA(luca);
		conContanti(luca, 2);

		partita.lanciaDadi(1, 2); // Vicolo Stretto, affitto doppio: 8 euro
		assertEquals(FasePartita.SALDO_DEBITO, partita.getFase());
		assertEquals(mario, partita.getDebito().creditore());

		partita.ipoteca(39);
		partita.pagaDebito();
		assertEquals(1508, mario.getPortafoglio());
		assertEquals(2 + 200 - 8, luca.getPortafoglio());
	}

	@Test
	void bancarotta_dichiarata_durante_il_saldo_del_debito() {
		tabellone.getCasella(1).assegnaA(luca);
		tabellone.getCasella(3).assegnaA(luca);
		partita.costruisciCasetta(1);
		// 150 contanti + 25 (casa) + 60 (ipoteche) bastano per i 200 della tassa
		conContanti(luca, 150);

		partita.lanciaDadi(1, 3);
		assertEquals(FasePartita.SALDO_DEBITO, partita.getFase());
		partita.dichiaraBancarotta();

		assertTrue(luca.isInBancarotta());
		// gli edifici tornano alla banca e le proprietà sono di nuovo libere
		assertEquals(32, partita.getCasetteDisponibili());
		assertNull(tabellone.getCasella(1).getProprietario());
		assertEquals(0, tabellone.getCasella(1).getNumeroEdifici());
		assertEquals(mario, partita.getVincitore());
	}

	@Test
	void multa_di_prigione_pagata_dopo_aver_ipotecato() {
		tabellone.getCasella(39).assegnaA(luca);
		inPrigione(luca, 2);
		conContanti(luca, 50);

		partita.lanciaDadi(1, 2); // terzo tentativo fallito: multa di 125 euro
		assertEquals(FasePartita.SALDO_DEBITO, partita.getFase());
		assertTrue(luca.isInPrigione());

		partita.ipoteca(39);
		partita.pagaDebito();
		// saldata la multa, il segnalino si muove con il lancio fatto
		assertFalse(luca.isInPrigione());
		assertEquals(13, luca.getPosizione());
		assertEquals(FasePartita.DECISIONE_ACQUISTO, partita.getFase());
	}

	@Test
	void durante_l_asta_gestisce_le_proprieta_solo_chi_deve_offrire() {
		tabellone.getCasella(39).assegnaA(luca);
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();

		assertEquals(mario, partita.getGiocatoreCheGestisce());
		assertThrows(MossaNonValidaException.class, () -> partita.ipoteca(39), "Luca non deve offrire ora");
	}

	@Test
	void durante_l_asta_chi_offre_puo_ipotecare_per_raccogliere_denaro() {
		tabellone.getCasella(37).assegnaA(mario);
		tabellone.getCasella(39).assegnaA(mario);
		conEdifici(tabellone.getCasella(39), 1);
		conContanti(mario, 10);
		partita.lanciaDadi(1, 2); // Luca su Vicolo Stretto
		partita.rifiutaAcquisto();

		assertThrows(MossaNonValidaException.class, () -> partita.offri(50), "solo 10€ in contanti");
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(39), "niente spese");
		assertThrows(MossaNonValidaException.class, () -> partita.ipoteca(39), "prima vendere la casa");
		partita.vendiCasetta(39);
		partita.ipoteca(37);
		assertEquals(10 + 100 + 175, mario.getPortafoglio());

		partita.offri(250);
		partita.ritiratiDallAsta(); // Luca
		assertEquals(mario, tabellone.getCasella(3).getProprietario());
		assertEquals(35, mario.getPortafoglio());
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void dadi_fuori_intervallo_rifiutati() {
		assertThrows(IllegalArgumentException.class, () -> partita.lanciaDadi(0, 7));
	}

	@Test
	void termina_partita_vince_il_patrimonio_piu_alto() {
		tabellone.getCasella(39).assegnaA(mario);
		partita.terminaPartita();
		assertEquals(mario, partita.getVincitore());
		assertEquals(FasePartita.TERMINATA, partita.getFase());
		assertThrows(MossaNonValidaException.class, () -> partita.lanciaDadi(1, 2));
	}

	@Test
	void terminare_durante_asta_o_debito_lo_annulla_e_lo_registra() {
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		partita.offri(30);

		partita.terminaPartita();

		assertNull(partita.getAsta());
		assertNull(tabellone.getCasella(3).getProprietario());
		assertTrue(partita.getEventi().contains("L'asta per Vicolo Stretto è annullata."));
	}

	@Test
	void giocatore_in_prigione_sempre_sulla_casella_della_prigione() {
		luca.spostaA(25);
		luca.entraInPrigione();
		assertEquals(Tabellone.POSIZIONE_PRIGIONE, luca.getPosizione());
		assertEquals(0, luca.getTurniInPrigione());
		assertThrows(MossaNonValidaException.class, () -> luca.usaCartaUscitaDiPrigione());
	}
	@Test
	void riparazioni_contano_case_e_alberghi() {
		// mazzi non mescolati: la terza carta Imprevisti chiede 60€ per casa e 250€ per albergo
		tabellone.getCasella(1).assegnaA(luca);
		tabellone.getCasella(3).assegnaA(luca);
		conEdifici(tabellone.getCasella(1), Casella.ALBERGO);
		conEdifici(tabellone.getCasella(3), 2);

		luca.spostaA(4);
		partita.lanciaDadi(1, 2); // Imprevisti: "Andate sino al Largo Colombo"
		rinunciaSenzaOfferte();
		partita.fineTurno();
		mario.spostaA(4);
		partita.lanciaDadi(1, 2); // Imprevisti: "Andate in prigione"
		partita.fineTurno();
		luca.spostaA(4);
		partita.lanciaDadi(1, 2); // Imprevisti: riparazioni

		assertEquals(1500 - (2 * 60 + 250), luca.getPortafoglio());
	}

	@Test
	void bancarotta_verso_un_giocatore_cede_anche_le_carte_uscita_di_prigione() {
		Casella parco = tabellone.getCasella(39);
		parco.assegnaA(mario);
		conEdifici(parco, Casella.ALBERGO);
		luca.riceviCartaUscitaDiPrigione(tabellone.getProbabilita().get(5));
		conContanti(luca, 100);
		luca.spostaA(35);

		partita.lanciaDadi(1, 3); // Parco della Vittoria con albergo: 2000€

		assertTrue(luca.isInBancarotta());
		assertEquals(0, luca.getNumeroCarteUscitaDiPrigione());
		assertEquals(1, mario.getNumeroCarteUscitaDiPrigione());
		assertEquals(1500 + 100, mario.getPortafoglio());
		assertEquals(0, luca.getPortafoglio());
	}

	@Test
	void terminare_durante_un_debito_lo_annulla_e_lo_registra() {
		tabellone.getCasella(39).assegnaA(luca);
		conContanti(luca, 100);
		partita.lanciaDadi(1, 3); // Tassa Patrimoniale: 200€
		assertEquals(FasePartita.SALDO_DEBITO, partita.getFase());

		partita.terminaPartita();

		assertNull(partita.getDebito());
		assertTrue(partita.getEventi().contains("Il debito di 200€ di Luca è annullato."));
	}
}
