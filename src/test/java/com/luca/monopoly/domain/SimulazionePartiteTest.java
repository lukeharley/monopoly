package com.luca.monopoly.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gioca molte partite complete con giocatori automatici che scelgono mosse a
 * caso (comprare, rifiutare, offrire, costruire, ipotecare, uscire di prigione...)
 * e dopo ogni mossa verifica le invarianti del gioco. Le mosse rifiutate con
 * {@link MossaNonValidaException} sono ammesse; qualsiasi altra eccezione o
 * invariante violata fa fallire il test indicando il seme della partita.
 */
class SimulazionePartiteTest {

	private static final int PARTITE = Integer.getInteger("simulazione.partite", 300);
	private static final int MAX_MOSSE = 20_000;

	@Test
	void le_partite_casuali_da_2_a_8_giocatori_rispettano_le_regole() {
		for (long seme = 1; seme <= PARTITE; seme++) {
			gioca(seme, 2 + (int) (seme % 7));
		}
	}

	/**
	 * Senza scambi tra giocatori un gruppo completo si ottiene solo con acquisti e
	 * aste: in due succede quasi sempre e la partita finisce in bancarotta. Con più
	 * giocatori molte partite restano aperte e vanno chiuse con "Termina partita".
	 */
	@Test
	void in_due_quasi_tutte_le_partite_finiscono_con_un_vincitore() {
		int concluse = 0;
		for (long seme = 1; seme <= PARTITE; seme++) {
			if (gioca(seme, 2)) {
				concluse++;
			}
		}
		assertTrue(concluse >= PARTITE * 8 / 10, "Partite concluse con un vincitore: " + concluse + "/" + PARTITE);
	}

	/** @return true se la partita si è conclusa con un vincitore entro {@link #MAX_MOSSE} */
	private boolean gioca(long seme, int numeroGiocatori) {
		Random caso = new Random(seme);
		List<Giocatore> giocatori = new ArrayList<>();
		for (int i = 0; i < numeroGiocatori; i++) {
			giocatori.add(new Giocatore("G" + i, Segnalino.values()[i]));
		}
		Partita partita = new Partita(giocatori, new Random(seme * 31), true);

		for (int mossa = 0; mossa < MAX_MOSSE; mossa++) {
			if (partita.getFase() == FasePartita.TERMINATA) {
				verificaInvarianti(partita, seme);
				return true;
			}
			try {
				giocaUnaMossa(partita, caso);
			} catch (MossaNonValidaException rifiutata) {
				// mossa scelta a caso e non ammessa: lo stato non deve cambiare
			} catch (RuntimeException errore) {
				throw new AssertionError("Seme " + seme + ", fase " + partita.getFase() + ": " + errore, errore);
			}
			verificaInvarianti(partita, seme);
		}
		return false;
	}

	private void giocaUnaMossa(Partita partita, Random caso) {
		Giocatore gestore = partita.getGiocatoreCheGestisce();

		// a volte, prima della mossa principale, si gestisce una proprietà a caso
		if (caso.nextInt(4) == 0) {
			List<Casella> proprieta = partita.getProprietaDi(gestore);
			if (!proprieta.isEmpty()) {
				int posizione = proprieta.get(caso.nextInt(proprieta.size())).getPosizione();
				switch (caso.nextInt(10)) {
					case 0 -> partita.vendiCasetta(posizione);
					case 1 -> partita.ipoteca(posizione);
					case 2, 3 -> partita.riscattaIpoteca(posizione);
					default -> partita.costruisciCasetta(posizione);
				}
				return;
			}
		}

		switch (partita.getFase()) {
			case LANCIO_DADI -> {
				if (gestore.isInPrigione() && caso.nextInt(3) == 0) {
					if (gestore.getNumeroCarteUscitaDiPrigione() > 0) {
						partita.usaCartaUscitaDiPrigione();
					} else {
						partita.pagaUscitaDiPrigione();
					}
				} else {
					partita.lanciaDadi();
				}
			}
			case DECISIONE_ACQUISTO -> {
				if (caso.nextInt(10) < 7) {
					partita.compraProprieta();
				} else {
					partita.rifiutaAcquisto();
				}
			}
			case ASTA -> {
				Asta asta = partita.getAsta();
				if (caso.nextBoolean() && gestore.getPortafoglio() > asta.getOffertaMassima()) {
					int margine = gestore.getPortafoglio() - asta.getOffertaMassima();
					partita.offri(asta.getOffertaMassima() + 1 + caso.nextInt(Math.min(margine, 100)));
				} else if (asta.getMigliorOfferente() != gestore) {
					partita.ritiratiDallAsta();
				} else {
					fail("Il miglior offerente non dovrebbe essere di turno nell'asta");
				}
			}
			case SALDO_DEBITO -> {
				if (gestore.getPortafoglio() >= partita.getDebito().importo()) {
					partita.pagaDebito();
				} else if (caso.nextInt(20) == 0) {
					partita.dichiaraBancarotta();
				} else {
					raccogliDenaro(partita, gestore, caso);
				}
			}
			case FINE_TURNO -> partita.fineTurno();
			default -> fail("Fase inattesa tra una mossa e l'altra: " + partita.getFase());
		}
	}

	/** Vende un edificio o ipoteca una proprietà, scegliendo tra quelle ammesse. */
	private void raccogliDenaro(Partita partita, Giocatore giocatore, Random caso) {
		List<Casella> proprieta = new ArrayList<>(partita.getProprietaDi(giocatore));
		Collections.shuffle(proprieta, caso);
		for (Casella casella : proprieta) {
			try {
				if (casella.getNumeroEdifici() > 0) {
					partita.vendiCasetta(casella.getPosizione());
				} else {
					partita.ipoteca(casella.getPosizione());
				}
				return;
			} catch (MossaNonValidaException nonAmmessa) {
				// si prova con la prossima proprietà
			}
		}
		// nessuna vendita possibile (es. albergo senza casette in banca): resta la bancarotta
		partita.dichiaraBancarotta();
	}

	private void verificaInvarianti(Partita partita, long seme) {
		String contesto = "Seme " + seme + ": ";
		Tabellone tabellone = partita.getTabellone();
		FasePartita fase = partita.getFase();

		assertNotEquals(FasePartita.MOVIMENTO, fase, contesto + "fase transitoria visibile");
		assertEquals(fase == FasePartita.ASTA, partita.getAsta() != null, contesto + "asta e fase incoerenti");
		assertEquals(fase == FasePartita.SALDO_DEBITO, partita.getDebito() != null,
				contesto + "debito e fase incoerenti");
		assertEquals(fase == FasePartita.TERMINATA, partita.getVincitore() != null,
				contesto + "vincitore e fase incoerenti");

		if (fase != FasePartita.TERMINATA) {
			assertFalse(partita.getGiocatoreCorrente().isInBancarotta(), contesto + "di turno un giocatore eliminato");
			assertTrue(partita.getGiocatoriAttivi().size() >= 2, contesto + "partita aperta con un solo giocatore");
		}
		if (fase == FasePartita.DECISIONE_ACQUISTO) {
			Casella casella = tabellone.getCasella(partita.getGiocatoreCorrente().getPosizione());
			assertTrue(casella.isAcquistabile() && casella.getProprietario() == null,
					contesto + "decisione d'acquisto su una casella non libera");
		}

		int carteImprevistiTenute = 0;
		int carteProbabilitaTenute = 0;
		for (Giocatore giocatore : partita.getGiocatori()) {
			assertTrue(giocatore.getPortafoglio() >= 0, contesto + giocatore + " ha contanti negativi");
			assertTrue(giocatore.getPosizione() >= 0 && giocatore.getPosizione() < Tabellone.NUMERO_CASELLE,
					contesto + "posizione fuori dal tabellone");
			if (giocatore.isInPrigione()) {
				assertEquals(Tabellone.POSIZIONE_PRIGIONE, giocatore.getPosizione(), contesto + "prigioniero fuori cella");
				assertTrue(giocatore.getTurniInPrigione() <= Partita.MAX_TURNI_IN_PRIGIONE,
						contesto + "troppi turni in prigione");
			}
			if (giocatore.isInBancarotta()) {
				assertEquals(0, giocatore.getPortafoglio(), contesto + "eliminato con denaro");
				assertTrue(partita.getProprietaDi(giocatore).isEmpty(), contesto + "eliminato con proprietà");
				assertEquals(0, giocatore.getNumeroCarteUscitaDiPrigione(), contesto + "eliminato con carte");
			}
			for (Carta carta : giocatore.cediCarteUscitaDiPrigione()) {
				if (carta instanceof Imprevisto) {
					carteImprevistiTenute++;
				} else {
					carteProbabilitaTenute++;
				}
				giocatore.riceviCartaUscitaDiPrigione(carta);
			}
		}
		assertTrue(carteImprevistiTenute <= 1 && carteProbabilitaTenute <= 1,
				contesto + "carte \"Uscite gratis\" duplicate");

		int caseSulTabellone = 0;
		int alberghiSulTabellone = 0;
		for (Casella casella : tabellone.getCaselle()) {
			if (!casella.isAcquistabile()) {
				assertNull(casella.getProprietario(), contesto + casella.getTesto() + " ha un proprietario");
				continue;
			}
			if (casella.getProprietario() == null) {
				assertFalse(casella.isIpotecata(), contesto + casella.getTesto() + " della banca ma ipotecata");
				assertEquals(0, casella.getNumeroEdifici(), contesto + casella.getTesto() + " della banca con edifici");
			}
			if (casella.getNumeroEdifici() > 0) {
				GruppoColore gruppo = casella.getGruppo();
				assertTrue(tabellone.possiedeTuttoIlGruppo(casella.getProprietario(), gruppo),
						contesto + "edifici su " + casella.getTesto() + " senza il gruppo completo");
				List<Casella> delGruppo = tabellone.getCaselleDelGruppo(gruppo);
				assertTrue(delGruppo.stream().noneMatch(Casella::isIpotecata),
						contesto + "edifici in un gruppo con terreni ipotecati");
				int minimo = delGruppo.stream().mapToInt(Casella::getNumeroEdifici).min().getAsInt();
				int massimo = delGruppo.stream().mapToInt(Casella::getNumeroEdifici).max().getAsInt();
				assertTrue(massimo - minimo <= 1, contesto + "costruzione non uniforme nel gruppo " + gruppo);
			}
			assertTrue(casella.getNumeroEdifici() <= Casella.ALBERGO, contesto + "troppi edifici");
			if (casella.haAlbergo()) {
				alberghiSulTabellone++;
			} else {
				caseSulTabellone += casella.getNumeroEdifici();
			}
		}
		assertEquals(Banca.CASETTE_TOTALI, caseSulTabellone + partita.getCasetteDisponibili(),
				contesto + "casette create o perse");
		assertEquals(Banca.ALBERGHI_TOTALI, alberghiSulTabellone + partita.getAlberghiDisponibili(),
				contesto + "alberghi creati o persi");
		assertTrue(partita.getEventi().size() <= 100, contesto + "registro eventi oltre il limite");
	}
}
