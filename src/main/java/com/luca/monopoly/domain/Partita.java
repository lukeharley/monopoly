package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.luca.monopoly.domain.giocatore.Giocatore;

/**
 * Motore di gioco: gestisce i turni, il lancio dei dadi e ciò che accade quando
 * un segnalino arriva su una casella. Le regole su affitti ed edifici sono
 * delegate a {@link CalcolatoreAffitto} e {@link GestoreProprieta}.
 *
 * Ogni mossa pubblica parte da una fase stabile di {@link FasePartita} e
 * termina in un'altra fase stabile; una mossa non ammessa nella fase corrente
 * lancia {@link MossaNonValidaException} senza modificare lo stato.
 */
public class Partita {

	public static final int PORTAFOGLIO_INIZIALE = 1500;
	public static final int PREMIO_VIA = 200;
	public static final int MULTA_PRIGIONE = 125;
	public static final int MAX_TURNI_IN_PRIGIONE = 3;
	private static final int MAX_DOPPI_CONSECUTIVI = 3;
	private static final int MAX_EVENTI = 100;

	/** Fasi in cui il giocatore di turno può vendere edifici e ipotecare. */
	private static final Set<FasePartita> FASI_DI_GESTIONE = EnumSet.of(FasePartita.LANCIO_DADI,
			FasePartita.DECISIONE_ACQUISTO, FasePartita.FINE_TURNO, FasePartita.SALDO_DEBITO);

	private final Tabellone tabellone = new Tabellone();
	private final Banca banca = new Banca(tabellone.getCasette().size(), tabellone.getAlberghi().size());
	private final List<Giocatore> giocatori;
	private final List<Dado> dadi = List.of(new Dado(), new Dado());
	private final Mazzo<Imprevisto> mazzoImprevisti;
	private final Mazzo<Probabilita> mazzoProbabilita;
	private final CalcolatoreAffitto calcolatoreAffitto = new CalcolatoreAffitto(tabellone);
	private final GestoreProprieta gestoreProprieta;
	private final Random random;
	private final List<String> eventi = new ArrayList<>();

	private int indiceGiocatoreCorrente;
	private FasePartita fase = FasePartita.LANCIO_DADI;
	private int dado1;
	private int dado2;
	private int doppiConsecutivi;
	private boolean rilancioConcesso;
	private Carta ultimaCartaPescata;
	private Asta asta;
	private Debito debito;
	private Giocatore vincitore;

	public Partita(List<Giocatore> giocatori) {
		this(giocatori, new Random(), true);
	}

	public Partita(List<Giocatore> giocatori, Random random, boolean mescolaMazzi) {
		if (giocatori.size() < 2) {
			throw new MossaNonValidaException("Servono almeno 2 giocatori per iniziare la partita.");
		}
		this.giocatori = new ArrayList<>(giocatori);
		this.random = random;
		this.mazzoImprevisti = new Mazzo<>(tabellone.getImprevisti(), random, mescolaMazzi);
		this.mazzoProbabilita = new Mazzo<>(tabellone.getProbabilita(), random, mescolaMazzi);
		this.gestoreProprieta = new GestoreProprieta(tabellone, banca, this::registra);

		registra("La partita ha inizio! Tocca a " + getGiocatoreCorrente().getNome() + ".");
	}

	// ---------------------------------------------------------------------
	// Turno
	// ---------------------------------------------------------------------

	public void lanciaDadi() {
		lanciaDadi(dadi.get(0).lancia(random), dadi.get(1).lancia(random));
	}

	/** Lancio con risultato prestabilito: usato dai test e da {@link #lanciaDadi()}. */
	public void lanciaDadi(int risultatoDado1, int risultatoDado2) {
		verificaFase(FasePartita.LANCIO_DADI, "Non è il momento di lanciare i dadi.");
		if (risultatoDado1 < 1 || risultatoDado1 > 6 || risultatoDado2 < 1 || risultatoDado2 > 6) {
			throw new IllegalArgumentException("Ogni dado vale da 1 a 6.");
		}

		Giocatore giocatore = getGiocatoreCorrente();
		dado1 = risultatoDado1;
		dado2 = risultatoDado2;
		ultimaCartaPescata = null;
		rilancioConcesso = false;

		boolean doppio = dado1 == dado2;
		int risultatoDadi = dado1 + dado2;
		registra(giocatore.getNome() + " lancia i dadi: " + dado1 + " + " + dado2 + " = " + risultatoDadi
				+ (doppio ? " (doppio!)" : "") + ".");

		if (giocatore.isInPrigione()) {
			lancioInPrigione(giocatore, doppio, risultatoDadi);
			return;
		}

		if (doppio) {
			doppiConsecutivi++;
			if (doppiConsecutivi == MAX_DOPPI_CONSECUTIVI) {
				registra(giocatore.getNome() + " ha fatto tre doppi consecutivi e va in prigione!");
				vaiInPrigione(giocatore);
				fase = FasePartita.FINE_TURNO;
				return;
			}
			rilancioConcesso = true;
		}

		muoviERisolvi(giocatore, risultatoDadi);
	}

	private void lancioInPrigione(Giocatore giocatore, boolean doppio, int risultatoDadi) {
		if (doppio) {
			registra(giocatore.getNome() + " ha fatto un doppio ed esce di prigione.");
			esciDiPrigione(giocatore);
			muoviERisolvi(giocatore, risultatoDadi);
			return;
		}

		giocatore.setTurniInPrigione(giocatore.getTurniInPrigione() + 1);
		if (giocatore.getTurniInPrigione() < MAX_TURNI_IN_PRIGIONE) {
			registra(giocatore.getNome() + " resta in prigione.");
			fase = FasePartita.FINE_TURNO;
			return;
		}

		registra(giocatore.getNome() + " è al terzo turno in prigione: deve pagare " + MULTA_PRIGIONE
				+ "€ di multa per uscire.");
		fase = FasePartita.MOVIMENTO;
		if (pagamento(giocatore, null, MULTA_PRIGIONE, risultatoDadi)) {
			esciDiPrigione(giocatore);
			muoviERisolvi(giocatore, risultatoDadi);
		}
	}

	public void compraProprieta() {
		verificaFase(FasePartita.DECISIONE_ACQUISTO, "Non c'è nessuna proprietà da comprare.");
		Giocatore giocatore = getGiocatoreCorrente();
		Casella casella = tabellone.getCasella(giocatore.getPosizione());

		if (giocatore.getPortafoglio() < casella.getCostoProprieta()) {
			throw new MossaNonValidaException("Non hai abbastanza denaro per comprare " + casella.getTesto()
					+ ": puoi ipotecare altre proprietà o rinunciare all'acquisto.");
		}

		giocatore.paga(casella.getCostoProprieta());
		casella.setProprietario(giocatore);
		registra(giocatore.getNome() + " compra " + casella.getTesto() + " per " + casella.getCostoProprieta()
				+ "€.");
		concludiAzione();
	}

	/** Chi rinuncia all'acquisto mette la proprietà all'asta tra tutti i giocatori. */
	public void rifiutaAcquisto() {
		verificaFase(FasePartita.DECISIONE_ACQUISTO, "Non c'è nessuna proprietà da comprare.");
		Giocatore giocatore = getGiocatoreCorrente();
		Casella casella = tabellone.getCasella(giocatore.getPosizione());

		List<Giocatore> partecipanti = new ArrayList<>();
		for (int i = 1; i <= giocatori.size(); i++) {
			Giocatore partecipante = giocatori.get((indiceGiocatoreCorrente + i) % giocatori.size());
			if (!partecipante.isInBancarotta()) {
				partecipanti.add(partecipante);
			}
		}
		asta = new Asta(casella, partecipanti);
		fase = FasePartita.ASTA;
		registra(giocatore.getNome() + " rinuncia a comprare " + casella.getTesto() + ": si va all'asta! Offre per primo "
				+ asta.getOfferenteDiTurno().getNome() + ".");
	}

	public void offri(int importo) {
		verificaFase(FasePartita.ASTA, "Non c'è nessuna asta in corso.");
		Giocatore offerente = asta.getOfferenteDiTurno();
		asta.offri(offerente, importo);
		registra(offerente.getNome() + " offre " + importo + "€.");
		chiudiAstaSeConclusa();
	}

	public void ritiratiDallAsta() {
		verificaFase(FasePartita.ASTA, "Non c'è nessuna asta in corso.");
		Giocatore offerente = asta.getOfferenteDiTurno();
		asta.ritirati(offerente);
		registra(offerente.getNome() + " si ritira dall'asta.");
		chiudiAstaSeConclusa();
	}

	private void chiudiAstaSeConclusa() {
		if (!asta.isConclusa()) {
			return;
		}
		Casella casella = asta.getCasella();
		Giocatore aggiudicatario = asta.getMigliorOfferente();
		if (aggiudicatario == null) {
			registra("Nessuna offerta: " + casella.getTesto() + " resta alla banca.");
		} else {
			aggiudicatario.paga(asta.getOffertaMassima());
			casella.setProprietario(aggiudicatario);
			registra(aggiudicatario.getNome() + " si aggiudica " + casella.getTesto() + " per "
					+ asta.getOffertaMassima() + "€.");
		}
		asta = null;
		concludiAzione();
	}

	public void pagaDebito() {
		verificaFase(FasePartita.SALDO_DEBITO, "Non hai debiti da saldare.");
		Giocatore giocatore = getGiocatoreCorrente();
		if (giocatore.getPortafoglio() < debito.importo()) {
			throw new MossaNonValidaException("Ti mancano ancora " + (debito.importo() - giocatore.getPortafoglio())
					+ "€: vendi edifici o ipoteca altre proprietà.");
		}

		Debito saldato = debito;
		debito = null;
		trasferisci(giocatore, saldato.creditore(), saldato.importo());
		registra(giocatore.getNome() + " salda il debito di " + saldato.importo() + "€.");

		if (saldato.passiDopoIlPagamento() > 0) {
			// era la multa per uscire di prigione: il segnalino ora si muove
			esciDiPrigione(giocatore);
			muoviERisolvi(giocatore, saldato.passiDopoIlPagamento());
		} else {
			concludiAzione();
		}
	}

	public void dichiaraBancarotta() {
		verificaFase(FasePartita.SALDO_DEBITO, "Puoi dichiarare bancarotta solo se hai un debito da saldare.");
		Giocatore creditore = debito.creditore();
		debito = null;
		bancarotta(getGiocatoreCorrente(), creditore);
	}

	public void fineTurno() {
		verificaFase(FasePartita.FINE_TURNO, "Non puoi ancora passare il turno.");
		passaAlProssimoGiocatore();
	}

	public void pagaUscitaDiPrigione() {
		Giocatore giocatore = verificaInPrigionePrimaDelLancio();
		if (giocatore.getPortafoglio() < MULTA_PRIGIONE) {
			throw new MossaNonValidaException("Non hai abbastanza denaro per pagare la multa.");
		}
		giocatore.paga(MULTA_PRIGIONE);
		esciDiPrigione(giocatore);
		registra(giocatore.getNome() + " paga " + MULTA_PRIGIONE + "€ ed esce di prigione.");
	}

	public void usaCartaUscitaDiPrigione() {
		Giocatore giocatore = verificaInPrigionePrimaDelLancio();
		if (giocatore.getCarteUscitaDiPrigione().isEmpty()) {
			throw new MossaNonValidaException("Non possiedi carte \"Uscite gratis di prigione\".");
		}
		restituisciCartaAlMazzo(giocatore.getCarteUscitaDiPrigione().remove(0));
		esciDiPrigione(giocatore);
		registra(giocatore.getNome() + " usa la carta \"Uscite gratis di prigione\".");
	}

	/**
	 * Chiude la partita in anticipo: vince il giocatore con il patrimonio più alto
	 * (denaro + valore di proprietà ed edifici).
	 */
	public void terminaPartita() {
		if (fase == FasePartita.TERMINATA) {
			throw new MossaNonValidaException("La partita è già terminata.");
		}
		asta = null;
		debito = null;
		vincitore = getGiocatoriAttivi().stream().max(Comparator.comparingInt(this::calcolaPatrimonio)).get();
		fase = FasePartita.TERMINATA;
		registra("Partita terminata: vince " + vincitore.getNome() + " con un patrimonio di "
				+ calcolaPatrimonio(vincitore) + "€!");
	}

	// ---------------------------------------------------------------------
	// Gestione delle proprietà del giocatore di turno
	// ---------------------------------------------------------------------

	public void costruisciCasetta(int posizione) {
		Casella casella = verificaGestione(posizione, "Non puoi costruire mentre hai un debito da saldare.");
		gestoreProprieta.costruisci(getGiocatoreCorrente(), casella);
	}

	public void vendiCasetta(int posizione) {
		Casella casella = verificaGestione(posizione, null);
		gestoreProprieta.vendiEdificio(getGiocatoreCorrente(), casella, false);
	}

	public void ipoteca(int posizione) {
		Casella casella = verificaGestione(posizione, null);
		gestoreProprieta.ipoteca(getGiocatoreCorrente(), casella);
	}

	public void riscattaIpoteca(int posizione) {
		Casella casella = verificaGestione(posizione, "Non puoi riscattare ipoteche mentre hai un debito da saldare.");
		gestoreProprieta.riscatta(getGiocatoreCorrente(), casella);
	}

	/**
	 * @param erroreInSaldoDebito se non null, la mossa è vietata durante il saldo
	 *                            di un debito (perché spende denaro)
	 */
	private Casella verificaGestione(int posizione, String erroreInSaldoDebito) {
		if (!FASI_DI_GESTIONE.contains(fase)) {
			throw new MossaNonValidaException("Ora non puoi gestire le proprietà.");
		}
		if (fase == FasePartita.SALDO_DEBITO && erroreInSaldoDebito != null) {
			throw new MossaNonValidaException(erroreInSaldoDebito);
		}
		if (posizione < 0 || posizione >= Tabellone.NUMERO_CASELLE) {
			throw new MossaNonValidaException("Casella inesistente.");
		}
		Casella casella = tabellone.getCasella(posizione);
		if (casella.getProprietario() != getGiocatoreCorrente()) {
			throw new MossaNonValidaException(
					casella.getTesto() + " non appartiene a " + getGiocatoreCorrente().getNome() + ".");
		}
		return casella;
	}

	// ---------------------------------------------------------------------
	// Movimento e risoluzione delle caselle
	// ---------------------------------------------------------------------

	/**
	 * Muove il segnalino e risolve la casella di arrivo. Se la risoluzione non
	 * richiede altre decisioni (acquisto, asta, debito) l'azione si conclude.
	 */
	private void muoviERisolvi(Giocatore giocatore, int passi) {
		fase = FasePartita.MOVIMENTO;
		muovi(giocatore, passi);
		if (fase == FasePartita.MOVIMENTO) {
			concludiAzione();
		}
	}

	private void muovi(Giocatore giocatore, int passi) {
		int nuovaPosizione = giocatore.getPosizione() + passi;
		if (nuovaPosizione >= Tabellone.NUMERO_CASELLE) {
			nuovaPosizione %= Tabellone.NUMERO_CASELLE;
			giocatore.incassa(PREMIO_VIA);
			registra(giocatore.getNome() + " passa dal Via e ritira " + PREMIO_VIA + "€.");
		}
		spostaSenzaPassareDalVia(giocatore, nuovaPosizione);
	}

	private void spostaSenzaPassareDalVia(Giocatore giocatore, int nuovaPosizione) {
		giocatore.setPosizione(nuovaPosizione);
		String casella = tabellone.getCasella(nuovaPosizione).getTesto();
		registra(giocatore.getNome() + " arriva su " + casella + (casella.endsWith("!") ? "" : "."));
		risolviCasella(giocatore);
	}

	private void risolviCasella(Giocatore giocatore) {
		Casella casella = tabellone.getCasella(giocatore.getPosizione());

		switch (casella.getTipo()) {
			case VAI_IN_PRIGIONE:
				registra(giocatore.getNome() + " va in prigione!");
				vaiInPrigione(giocatore);
				break;
			case TASSA:
				registra(giocatore.getNome() + " deve pagare " + casella.getTesto() + ": "
						+ casella.getCostoProprieta() + "€.");
				pagamento(giocatore, null, casella.getCostoProprieta(), 0);
				break;
			case IMPREVISTI:
				applicaCarta(giocatore, mazzoImprevisti.pesca());
				break;
			case PROBABILITA:
				applicaCarta(giocatore, mazzoProbabilita.pesca());
				break;
			case TERRENO:
			case STAZIONE:
			case SOCIETA:
				risolviProprieta(giocatore, casella);
				break;
			default:
				// Via, Prigione/Transito e Posteggio Gratuito: non succede nulla
				break;
		}
	}

	private void risolviProprieta(Giocatore giocatore, Casella casella) {
		Giocatore proprietario = casella.getProprietario();

		if (proprietario == null) {
			fase = FasePartita.DECISIONE_ACQUISTO;
			registra(casella.getTesto() + " è libera: costa " + casella.getCostoProprieta() + "€.");
		} else if (proprietario == giocatore) {
			registra(casella.getTesto() + " è già di " + giocatore.getNome() + ".");
		} else if (casella.isIpotecata()) {
			registra(casella.getTesto() + " è ipotecata: nessun affitto da pagare.");
		} else {
			int affitto = calcolaAffitto(casella);
			registra(giocatore.getNome() + " deve " + affitto + "€ di affitto a " + proprietario.getNome() + ".");
			pagamento(giocatore, proprietario, affitto, 0);
		}
	}

	private void applicaCarta(Giocatore giocatore, Carta carta) {
		ultimaCartaPescata = carta;
		String mazzo = carta instanceof Imprevisto ? "Imprevisti" : "Probabilità";
		registra(giocatore.getNome() + " pesca una carta " + mazzo + ": \"" + carta.getTesto() + "\"");

		switch (carta.getEffetto()) {
			case INCASSA:
				giocatore.incassa(carta.getValore());
				break;
			case PAGA:
				pagamento(giocatore, null, carta.getValore(), 0);
				break;
			case VAI_A:
				int passi = Math.floorMod(carta.getValore() - giocatore.getPosizione(), Tabellone.NUMERO_CASELLE);
				muovi(giocatore, passi == 0 ? Tabellone.NUMERO_CASELLE : passi);
				break;
			case TORNA_A:
				spostaSenzaPassareDalVia(giocatore, carta.getValore());
				break;
			case SPOSTA:
				spostaSenzaPassareDalVia(giocatore,
						Math.floorMod(giocatore.getPosizione() + carta.getValore(), Tabellone.NUMERO_CASELLE));
				break;
			case VAI_IN_PRIGIONE:
				vaiInPrigione(giocatore);
				break;
			case ESCI_GRATIS_DI_PRIGIONE:
				giocatore.getCarteUscitaDiPrigione().add(carta);
				break;
			case RIPARAZIONI:
				pagaRiparazioni(giocatore, carta);
				break;
		}
	}

	private void pagaRiparazioni(Giocatore giocatore, Carta carta) {
		int casette = 0;
		int alberghi = 0;
		for (Casella casella : tabellone.getProprietaDi(giocatore)) {
			if (casella.haAlbergo()) {
				alberghi++;
			} else {
				casette += casella.getNumeroDiCasetteSullaCasella();
			}
		}
		int importo = casette * carta.getValore() + alberghi * carta.getValoreAlbergo();
		if (importo == 0) {
			registra(giocatore.getNome() + " non possiede edifici: non paga nulla.");
			return;
		}
		registra(giocatore.getNome() + " deve " + importo + "€ per " + casette + " case e " + alberghi
				+ " alberghi.");
		pagamento(giocatore, null, importo, 0);
	}

	// ---------------------------------------------------------------------
	// Pagamenti, debiti e bancarotta
	// ---------------------------------------------------------------------

	/**
	 * Paga subito se i contanti bastano. Altrimenti, se vendendo edifici e
	 * ipotecando il giocatore può raccogliere la somma, apre la fase
	 * SALDO_DEBITO; se nemmeno così può pagare, va in bancarotta.
	 *
	 * @return true se il pagamento è avvenuto subito
	 */
	private boolean pagamento(Giocatore debitore, Giocatore creditore, int importo, int passiDopoIlPagamento) {
		if (debitore.getPortafoglio() >= importo) {
			trasferisci(debitore, creditore, importo);
			return true;
		}
		if (gestoreProprieta.valoreLiquidabile(debitore) < importo) {
			registra(debitore.getNome() + " non può pagare " + importo + "€ nemmeno vendendo tutto.");
			bancarotta(debitore, creditore);
			return false;
		}
		debito = new Debito(creditore, importo, passiDopoIlPagamento);
		fase = FasePartita.SALDO_DEBITO;
		registra(debitore.getNome() + " ha solo " + debitore.getPortafoglio() + "€ contanti: deve vendere edifici o "
				+ "ipotecare proprietà per pagare " + importo + "€.");
		return false;
	}

	private void trasferisci(Giocatore debitore, Giocatore creditore, int importo) {
		debitore.paga(importo);
		if (creditore != null) {
			creditore.incassa(importo);
		}
	}

	/**
	 * Gli edifici tornano alla banca a metà prezzo; denaro, proprietà e carte
	 * passano al creditore, oppure tornano alla banca se il creditore è la banca.
	 */
	private void bancarotta(Giocatore giocatore, Giocatore creditore) {
		gestoreProprieta.liquidaEdifici(giocatore);
		giocatore.setInBancarotta(true);
		registra(giocatore.getNome() + " è in bancarotta ed esce dal gioco!");

		for (Casella casella : tabellone.getProprietaDi(giocatore)) {
			if (creditore != null) {
				casella.setProprietario(creditore);
			} else {
				casella.setProprietario(null);
				casella.setIpotecata(false);
			}
		}
		for (Carta carta : giocatore.getCarteUscitaDiPrigione()) {
			if (creditore != null) {
				creditore.getCarteUscitaDiPrigione().add(carta);
			} else {
				restituisciCartaAlMazzo(carta);
			}
		}
		giocatore.getCarteUscitaDiPrigione().clear();
		if (creditore != null) {
			creditore.incassa(giocatore.getPortafoglio());
			registra("Tutti i beni di " + giocatore.getNome() + " passano a " + creditore.getNome() + ".");
		}
		giocatore.setPortafoglio(0);
		giocatore.setInPrigione(false);

		List<Giocatore> attivi = getGiocatoriAttivi();
		if (attivi.size() == 1) {
			vincitore = attivi.get(0);
			fase = FasePartita.TERMINATA;
			registra(vincitore.getNome() + " è l'ultimo giocatore rimasto e VINCE la partita!");
		} else {
			// il debitore è sempre il giocatore di turno
			passaAlProssimoGiocatore();
		}
	}

	// ---------------------------------------------------------------------
	// Prigione, carte e turni
	// ---------------------------------------------------------------------

	private void vaiInPrigione(Giocatore giocatore) {
		giocatore.setPosizione(Tabellone.POSIZIONE_PRIGIONE);
		giocatore.setInPrigione(true);
		giocatore.setTurniInPrigione(0);
		doppiConsecutivi = 0;
		rilancioConcesso = false;
	}

	private void esciDiPrigione(Giocatore giocatore) {
		giocatore.setInPrigione(false);
		giocatore.setTurniInPrigione(0);
	}

	private void restituisciCartaAlMazzo(Carta carta) {
		if (carta instanceof Imprevisto imprevisto) {
			mazzoImprevisti.rimettiInFondo(imprevisto);
		} else if (carta instanceof Probabilita probabilita) {
			mazzoProbabilita.rimettiInFondo(probabilita);
		}
	}

	/** Dopo il lancio (ed eventuale acquisto, asta o debito) si rilancia solo con un doppio. */
	private void concludiAzione() {
		Giocatore giocatore = getGiocatoreCorrente();
		if (rilancioConcesso && !giocatore.isInPrigione()) {
			fase = FasePartita.LANCIO_DADI;
			registra(giocatore.getNome() + " ha fatto un doppio: lancia di nuovo!");
		} else {
			fase = FasePartita.FINE_TURNO;
		}
	}

	private void passaAlProssimoGiocatore() {
		do {
			indiceGiocatoreCorrente = (indiceGiocatoreCorrente + 1) % giocatori.size();
		} while (giocatori.get(indiceGiocatoreCorrente).isInBancarotta());

		doppiConsecutivi = 0;
		rilancioConcesso = false;
		ultimaCartaPescata = null;
		fase = FasePartita.LANCIO_DADI;

		Giocatore giocatore = getGiocatoreCorrente();
		registra("Tocca a " + giocatore.getNome() + (giocatore.isInPrigione() ? " (in prigione)." : "."));
	}

	private void verificaFase(FasePartita faseAttesa, String messaggio) {
		if (fase == FasePartita.TERMINATA) {
			throw new MossaNonValidaException("La partita è terminata.");
		}
		if (fase != faseAttesa) {
			throw new MossaNonValidaException(messaggio);
		}
	}

	private Giocatore verificaInPrigionePrimaDelLancio() {
		verificaFase(FasePartita.LANCIO_DADI, "Puoi uscire di prigione solo prima di lanciare i dadi.");
		Giocatore giocatore = getGiocatoreCorrente();
		if (!giocatore.isInPrigione()) {
			throw new MossaNonValidaException(giocatore.getNome() + " non è in prigione.");
		}
		return giocatore;
	}

	private void registra(String evento) {
		eventi.add(evento);
		if (eventi.size() > MAX_EVENTI) {
			eventi.remove(0);
		}
	}

	// ---------------------------------------------------------------------
	// Interrogazioni sullo stato della partita
	// ---------------------------------------------------------------------

	public int calcolaAffitto(Casella casella) {
		return calcolatoreAffitto.calcola(casella, dado1 + dado2);
	}

	public int calcolaPatrimonio(Giocatore giocatore) {
		int patrimonio = giocatore.getPortafoglio();
		for (Casella casella : tabellone.getProprietaDi(giocatore)) {
			patrimonio += casella.isIpotecata() ? casella.getValoreIpotecario() : casella.getCostoProprieta();
			patrimonio += casella.getNumeroDiCasetteSullaCasella() * casella.getCostoCasetta();
		}
		return patrimonio;
	}

	public List<Casella> getProprietaDi(Giocatore giocatore) {
		return tabellone.getProprietaDi(giocatore);
	}

	public List<Giocatore> getGiocatoriAttivi() {
		return giocatori.stream().filter(giocatore -> !giocatore.isInBancarotta()).collect(Collectors.toList());
	}

	public Giocatore getGiocatoreCorrente() {
		return giocatori.get(indiceGiocatoreCorrente);
	}

	public List<Giocatore> getGiocatori() {
		return Collections.unmodifiableList(giocatori);
	}

	public Tabellone getTabellone() {
		return tabellone;
	}

	public FasePartita getFase() {
		return fase;
	}

	public int getDado1() {
		return dado1;
	}

	public int getDado2() {
		return dado2;
	}

	public Carta getUltimaCartaPescata() {
		return ultimaCartaPescata;
	}

	public Asta getAsta() {
		return asta;
	}

	public Debito getDebito() {
		return debito;
	}

	public Giocatore getVincitore() {
		return vincitore;
	}

	public int getCasetteDisponibili() {
		return banca.getCasetteDisponibili();
	}

	public int getAlberghiDisponibili() {
		return banca.getAlberghiDisponibili();
	}

	public List<String> getEventi() {
		return Collections.unmodifiableList(eventi);
	}

}
