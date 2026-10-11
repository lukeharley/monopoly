package com.luca.monopoly;

import com.luca.monopoly.domain.*;
import com.luca.monopoly.domain.giocatore.Giocatore;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Random;

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
		luca = new Giocatore("Luca", Segnalini.CANE, 1500, 0);
		mario = new Giocatore("Mario", Segnalini.DINOSAURO, 1500, 0);
		partita = new Partita(List.of(luca, mario), new Random(0), false);
		tabellone = partita.getTabellone();
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
		vicoloCorto.setProprietario(luca);
		vicoloStretto.setProprietario(luca);

		assertEquals(8, partita.calcolaAffitto(vicoloStretto));

		partita.costruisciCasetta(3);
		assertEquals(1450, luca.getPortafoglio());
		assertEquals(20, partita.calcolaAffitto(vicoloStretto));
	}

	@Test
	void rifiuto_acquisto() {
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		assertNull(tabellone.getCasella(3).getProprietario());
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
		luca.setPosizione(38);
		partita.lanciaDadi(1, 3); // Probabilità (posizione 2): "Ritornate al Vicolo Corto"
		assertEquals(1, luca.getPosizione());
		assertEquals(1700, luca.getPortafoglio());
	}

	@Test
	void casella_vai_in_prigione() {
		luca.setPosizione(24);
		partita.lanciaDadi(2, 4);
		assertTrue(luca.isInPrigione());
		assertEquals(10, luca.getPosizione());
		assertEquals(1500, luca.getPortafoglio());
	}

	@Test
	void uscita_di_prigione_pagando_la_multa() {
		luca.setInPrigione(true);
		luca.setPosizione(10);
		partita.pagaUscitaDiPrigione();
		assertFalse(luca.isInPrigione());
		assertEquals(1500 - Partita.MULTA_PRIGIONE, luca.getPortafoglio());

		partita.lanciaDadi(5, 6); // Via Marco Polo
		assertEquals(21, luca.getPosizione());
	}

	@Test
	void uscita_di_prigione_con_un_doppio() {
		luca.setInPrigione(true);
		luca.setPosizione(10);
		partita.lanciaDadi(3, 3);
		assertFalse(luca.isInPrigione());
		assertEquals(16, luca.getPosizione());
		partita.rifiutaAcquisto();
		// uscendo con un doppio non si rilancia
		assertEquals(FasePartita.FINE_TURNO, partita.getFase());
	}

	@Test
	void dopo_tre_turni_in_prigione_si_paga_e_si_esce() {
		luca.setInPrigione(true);
		luca.setPosizione(10);
		mario.setPosizione(20);

		for (int tentativo = 1; tentativo <= 2; tentativo++) {
			partita.lanciaDadi(1, 2);
			assertTrue(luca.isInPrigione());
			partita.fineTurno();
			partita.lanciaDadi(1, 1); // Mario: Posteggio -> Corso Magellano (doppio)
			if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
				partita.rifiutaAcquisto();
			}
			while (partita.getFase() == FasePartita.LANCIO_DADI) {
				partita.lanciaDadi(1, 2);
				if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
					partita.rifiutaAcquisto();
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
			partita.getGiocatoreCorrente().setPosizione(39);
			partita.lanciaDadi(1, 2); // Probabilità (posizione 2)
			if (partita.getFase() == FasePartita.DECISIONE_ACQUISTO) {
				partita.rifiutaAcquisto();
			}
			partita.fineTurno();
		}
		assertEquals(0, luca.getCarteUscitaDiPrigione().size());
		assertEquals(1, mario.getCarteUscitaDiPrigione().size());

		// Mario finisce in prigione e usa la carta
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		partita.fineTurno();
		mario.setInPrigione(true);
		mario.setPosizione(10);
		partita.usaCartaUscitaDiPrigione();
		assertFalse(mario.isInPrigione());
		assertEquals(0, mario.getCarteUscitaDiPrigione().size());
	}

	@Test
	void stazioni_affitto_cresce_con_il_numero_di_stazioni() {
		Casella stazioneSud = tabellone.getCasella(5);
		stazioneSud.setProprietario(mario);
		assertEquals(25, partita.calcolaAffitto(stazioneSud));
		tabellone.getCasella(15).setProprietario(mario);
		tabellone.getCasella(25).setProprietario(mario);
		assertEquals(100, partita.calcolaAffitto(stazioneSud));

		partita.lanciaDadi(2, 3);
		assertEquals(1400, luca.getPortafoglio());
		assertEquals(1600, mario.getPortafoglio());
	}

	@Test
	void societa_affitto_proporzionale_ai_dadi() {
		tabellone.getCasella(12).setProprietario(mario);
		luca.setPosizione(7);
		partita.lanciaDadi(2, 3);
		assertEquals(1500 - 20, luca.getPortafoglio());
	}

	@Test
	void costruzione_uniforme_e_albergo() {
		tabellone.getCasella(6).setProprietario(luca);
		tabellone.getCasella(8).setProprietario(luca);
		tabellone.getCasella(9).setProprietario(luca);

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
		assertEquals(4, tabellone.getCasella(9).getNumeroDiCasetteSullaCasella());
		assertEquals(1500 - 15 * 50 + 25, luca.getPortafoglio());
	}

	@Test
	void non_si_costruisce_senza_monopolio() {
		tabellone.getCasella(6).setProprietario(luca);
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(6));
		assertThrows(MossaNonValidaException.class, () -> partita.costruisciCasetta(8));
	}

	@Test
	void ipoteca_e_riscatto() {
		Casella vicoloStretto = tabellone.getCasella(3);
		vicoloStretto.setProprietario(luca);

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
		parco.setProprietario(mario);
		parco.setNumeroDiCasetteSullaCasella(5);
		Casella vicoloCorto = tabellone.getCasella(1);
		vicoloCorto.setProprietario(luca);

		luca.setPortafoglio(100);
		luca.setPosizione(35);
		partita.lanciaDadi(1, 3);

		assertTrue(luca.isInBancarotta());
		assertEquals(FasePartita.TERMINATA, partita.getFase());
		assertEquals(mario, partita.getVincitore());
		assertEquals(mario, vicoloCorto.getProprietario());
		assertTrue(vicoloCorto.isIpotecata());
		assertEquals(1500 + 100 + 30, mario.getPortafoglio());
	}

	@Test
	void bancarotta_con_tre_giocatori_passa_il_turno() {
		Giocatore anna = new Giocatore("Anna", Segnalini.GATTO, 1500, 0);
		partita = new Partita(List.of(luca, mario, anna), new Random(0), false);

		luca.setPortafoglio(50);
		partita.lanciaDadi(1, 3); // Tassa Patrimoniale: 200 euro

		assertTrue(luca.isInBancarotta());
		assertEquals(FasePartita.LANCIO_DADI, partita.getFase());
		assertEquals(mario, partita.getGiocatoreCorrente());

		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		partita.fineTurno();
		assertEquals(anna, partita.getGiocatoreCorrente());
		partita.lanciaDadi(1, 2);
		partita.rifiutaAcquisto();
		partita.fineTurno();
		// Luca è fuori dal gioco: si torna a Mario
		assertEquals(mario, partita.getGiocatoreCorrente());
	}

	@Test
	void vendita_automatica_degli_edifici_per_pagare_un_debito() {
		tabellone.getCasella(1).setProprietario(luca);
		tabellone.getCasella(3).setProprietario(luca);
		partita.costruisciCasetta(1);
		partita.costruisciCasetta(3);
		luca.setPortafoglio(100);

		partita.lanciaDadi(1, 3); // Tassa Patrimoniale: 200 euro

		assertFalse(luca.isInBancarotta());
		assertEquals(0, tabellone.getCasella(1).getNumeroDiCasetteSullaCasella());
		assertEquals(0, tabellone.getCasella(3).getNumeroDiCasetteSullaCasella());
		assertTrue(tabellone.getCasella(1).isIpotecata());
		assertTrue(tabellone.getCasella(3).isIpotecata());
		// 100 + 25 + 25 (vendita case) + 30 + 30 (ipoteche) - 200
		assertEquals(10, luca.getPortafoglio());
		assertEquals(32, partita.getCasetteDisponibili());
	}

	@Test
	void termina_partita_vince_il_patrimonio_piu_alto() {
		tabellone.getCasella(39).setProprietario(mario);
		partita.terminaPartita();
		assertEquals(mario, partita.getVincitore());
		assertEquals(FasePartita.TERMINATA, partita.getFase());
		assertThrows(MossaNonValidaException.class, () -> partita.lanciaDadi(1, 2));
	}

}
