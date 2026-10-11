package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.luca.monopoly.domain.giocatore.Giocatore;

/**
 * Motore di gioco: gestisce i turni, il lancio dei dadi e tutte le regole che
 * si applicano quando un segnalino arriva su una casella.
 *
 * Ogni turno attraversa le fasi descritte in {@link FasePartita}: il giocatore
 * lancia i dadi, eventualmente decide se comprare la proprietà su cui è
 * arrivato e infine passa il turno. In qualsiasi momento del proprio turno può
 * costruire, vendere edifici e ipotecare le proprie proprietà.
 */
public class Partita {

	public static final int PORTAFOGLIO_INIZIALE = 1500;
	public static final int PREMIO_VIA = 200;
	public static final int MULTA_PRIGIONE = 125;
	public static final int MAX_TURNI_IN_PRIGIONE = 3;
	private static final int MAX_DOPPI_CONSECUTIVI = 3;
	private static final int MAX_EVENTI = 100;

	private final Tabellone tabellone = new Tabellone();
	private final List<Giocatore> giocatori;
	private final List<Dado> dadi;
	private final Mazzo<Imprevisto> mazzoImprevisti;
	private final Mazzo<Probabilita> mazzoProbabilita;
	private final Random random;
	private final List<String> eventi = new ArrayList<>();

	private int indiceGiocatoreCorrente;
	private FasePartita fase = FasePartita.LANCIO_DADI;
	private int dado1;
	private int dado2;
	private int doppiConsecutivi;
	private boolean rilancioConcesso;
	private int casetteDisponibili;
	private int alberghiDisponibili;
	private Carta ultimaCartaPescata;
	private int cartePescate;
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
		this.dadi = new Monopoly().getDadi();
		this.mazzoImprevisti = new Mazzo<>(tabellone.getImprevisti(), random, mescolaMazzi);
		this.mazzoProbabilita = new Mazzo<>(tabellone.getProbabilita(), random, mescolaMazzi);
		this.casetteDisponibili = tabellone.getCasette().size();
		this.alberghiDisponibili = tabellone.getAlberghi().size();

		registra("La partita ha inizio! Tocca a " + getGiocatoreCorrente().getNome() + ".");
	}

	// ---------------------------------------------------------------------
	// Azioni del giocatore di turno
	// ---------------------------------------------------------------------

	public void lanciaDadi() {
		lanciaDadi(dadi.get(0).lancia(random), dadi.get(1).lancia(random));
	}

	/** Lancio con risultato prestabilito: usato dai test e da {@link #lanciaDadi()}. */
	public void lanciaDadi(int risultatoDado1, int risultatoDado2) {
		verificaFase(FasePartita.LANCIO_DADI, "Non è il momento di lanciare i dadi.");

		Giocatore giocatore = getGiocatoreCorrente();
		dado1 = risultatoDado1;
		dado2 = risultatoDado2;
		ultimaCartaPescata = null;
		rilancioConcesso = false;

		boolean doppio = dado1 == dado2;
		int risultatoDado = dado1 + dado2;
		registra(giocatore.getNome() + " lancia i dadi: " + dado1 + " + " + dado2 + " = " + risultatoDado
				+ (doppio ? " (doppio!)" : "") + ".");

		if (giocatore.isInPrigione()) {
			if (doppio) {
				registra(giocatore.getNome() + " ha fatto un doppio ed esce di prigione.");
				esciDiPrigione(giocatore);
			} else {
				giocatore.setTurniInPrigione(giocatore.getTurniInPrigione() + 1);
				if (giocatore.getTurniInPrigione() < MAX_TURNI_IN_PRIGIONE) {
					registra(giocatore.getNome() + " resta in prigione.");
					fase = FasePartita.FINE_TURNO;
					return;
				}
				registra(giocatore.getNome() + " è al terzo turno in prigione: paga " + MULTA_PRIGIONE
						+ "€ di multa ed esce.");
				if (!pagamento(giocatore, null, MULTA_PRIGIONE)) {
					return;
				}
				esciDiPrigione(giocatore);
			}
		} else if (doppio) {
			doppiConsecutivi++;
			if (doppiConsecutivi == MAX_DOPPI_CONSECUTIVI) {
				registra(giocatore.getNome() + " ha fatto tre doppi consecutivi e va in prigione!");
				vaiInPrigione(giocatore);
				fase = FasePartita.FINE_TURNO;
				return;
			}
			rilancioConcesso = true;
		}

		muovi(giocatore, risultatoDado);

		if (fase == FasePartita.LANCIO_DADI && giocatore == getGiocatoreCorrente()
				&& !giocatore.isInBancarotta()) {
			concludiAzione();
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

	public void rifiutaAcquisto() {
		verificaFase(FasePartita.DECISIONE_ACQUISTO, "Non c'è nessuna proprietà da comprare.");
		Giocatore giocatore = getGiocatoreCorrente();
		Casella casella = tabellone.getCasella(giocatore.getPosizione());
		registra(giocatore.getNome() + " rinuncia a comprare " + casella.getTesto() + ".");
		concludiAzione();
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

	// ---------------------------------------------------------------------
	// Gestione delle proprietà (case, alberghi, ipoteche)
	// ---------------------------------------------------------------------

	public void costruisciCasetta(int posizione) {
		Giocatore giocatore = getGiocatoreCorrente();
		Casella casella = verificaProprietaDelGiocatoreCorrente(posizione);

		if (!casella.isTerreno()) {
			throw new MossaNonValidaException("Si può costruire solo sui terreni.");
		}
		List<Casella> gruppo = tabellone.getCaselleDelColore(casella.getColore());
		if (!possiedeTuttoIlGruppo(giocatore, casella.getColore())) {
			throw new MossaNonValidaException(
					"Per costruire devi possedere tutti i terreni del gruppo " + casella.getColore() + ".");
		}
		if (gruppo.stream().anyMatch(Casella::isIpotecata)) {
			throw new MossaNonValidaException("Non puoi costruire: un terreno del gruppo è ipotecato.");
		}

		int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
		if (casella.haAlbergo()) {
			throw new MossaNonValidaException("Su " + casella.getTesto() + " c'è già un albergo.");
		}
		int minimoDelGruppo = gruppo.stream().mapToInt(Casella::getNumeroDiCasetteSullaCasella).min().orElse(0);
		if (numeroCasette > minimoDelGruppo) {
			throw new MossaNonValidaException(
					"Devi costruire in modo uniforme: prima aggiungi una casa sugli altri terreni del gruppo.");
		}
		if (giocatore.getPortafoglio() < casella.getCostoCasetta()) {
			throw new MossaNonValidaException("Non hai abbastanza denaro per costruire.");
		}

		if (numeroCasette == 4) {
			if (alberghiDisponibili == 0) {
				throw new MossaNonValidaException("La banca ha esaurito gli alberghi.");
			}
			alberghiDisponibili--;
			casetteDisponibili += 4;
		} else {
			if (casetteDisponibili == 0) {
				throw new MossaNonValidaException("La banca ha esaurito le casette.");
			}
			casetteDisponibili--;
		}

		giocatore.paga(casella.getCostoCasetta());
		casella.setNumeroDiCasetteSullaCasella(numeroCasette + 1);
		registra(giocatore.getNome() + (casella.haAlbergo() ? " costruisce un albergo su " : " costruisce una casa su ")
				+ casella.getTesto() + " per " + casella.getCostoCasetta() + "€.");
	}

	public void vendiCasetta(int posizione) {
		Casella casella = verificaProprietaDelGiocatoreCorrente(posizione);
		vendiEdificio(getGiocatoreCorrente(), casella, false);
	}

	public void ipoteca(int posizione) {
		Casella casella = verificaProprietaDelGiocatoreCorrente(posizione);
		ipotecaProprieta(getGiocatoreCorrente(), casella);
	}

	public void riscattaIpoteca(int posizione) {
		Giocatore giocatore = getGiocatoreCorrente();
		Casella casella = verificaProprietaDelGiocatoreCorrente(posizione);

		if (!casella.isIpotecata()) {
			throw new MossaNonValidaException(casella.getTesto() + " non è ipotecata.");
		}
		if (giocatore.getPortafoglio() < casella.getCostoRiscatto()) {
			throw new MossaNonValidaException("Non hai abbastanza denaro per riscattare l'ipoteca.");
		}
		giocatore.paga(casella.getCostoRiscatto());
		casella.setIpotecata(false);
		registra(giocatore.getNome() + " riscatta l'ipoteca su " + casella.getTesto() + " pagando "
				+ casella.getCostoRiscatto() + "€.");
	}

	/**
	 * Chiude la partita in anticipo: vince il giocatore con il patrimonio più alto
	 * (denaro + valore di proprietà ed edifici).
	 */
	public void terminaPartita() {
		if (fase == FasePartita.TERMINATA) {
			throw new MossaNonValidaException("La partita è già terminata.");
		}
		vincitore = getGiocatoriAttivi().stream().max(Comparator.comparingInt(this::calcolaPatrimonio)).get();
		fase = FasePartita.TERMINATA;
		registra("Partita terminata: vince " + vincitore.getNome() + " con un patrimonio di "
				+ calcolaPatrimonio(vincitore) + "€!");
	}

	// ---------------------------------------------------------------------
	// Regole
	// ---------------------------------------------------------------------

	private void muovi(Giocatore giocatore, int passi) {
		int nuovaPosizione = giocatore.getPosizione() + passi;
		if (nuovaPosizione >= Tabellone.NUMERO_CASELLE) {
			nuovaPosizione %= Tabellone.NUMERO_CASELLE;
			giocatore.incassa(PREMIO_VIA);
			registra(giocatore.getNome() + " passa dal Via e ritira " + PREMIO_VIA + "€.");
		}
		giocatore.setPosizione(nuovaPosizione);
		registra(giocatore.getNome() + " arriva su " + tabellone.getCasella(nuovaPosizione).getTesto() + ".");
		risolviCasella(giocatore);
	}

	private void muoviIndietro(Giocatore giocatore, int nuovaPosizione) {
		giocatore.setPosizione(nuovaPosizione);
		registra(giocatore.getNome() + " arriva su " + tabellone.getCasella(nuovaPosizione).getTesto() + ".");
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
				registra(giocatore.getNome() + " paga " + casella.getTesto() + ": " + casella.getCostoProprieta()
						+ "€.");
				pagamento(giocatore, null, casella.getCostoProprieta());
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
			registra(giocatore.getNome() + " paga " + affitto + "€ di affitto a " + proprietario.getNome() + ".");
			pagamento(giocatore, proprietario, affitto);
		}
	}

	public int calcolaAffitto(Casella casella) {
		Giocatore proprietario = casella.getProprietario();
		if (proprietario == null || casella.isIpotecata()) {
			return 0;
		}
		Contratto contratto = tabellone.getContratto(tabellone.getCaselle().indexOf(casella));

		switch (casella.getTipo()) {
			case TERRENO:
				int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
				if (numeroCasette == 0 && possiedeTuttoIlGruppo(proprietario, casella.getColore())) {
					// Monopolio: la rendita del terreno nudo raddoppia
					return contratto.getRenditaTerreno() * 2;
				}
				return contratto.calcolaAffitto(numeroCasette);
			case STAZIONE:
				// 25€ con una stazione, raddoppia per ogni altra stazione posseduta
				int stazioni = contaProprietaDelTipo(proprietario, TipoCasella.STAZIONE);
				return contratto.getTassa() << (stazioni - 1);
			case SOCIETA:
				int societa = contaProprietaDelTipo(proprietario, TipoCasella.SOCIETA);
				return (dado1 + dado2) * (societa == 2 ? 10 : 4);
			default:
				return 0;
		}
	}

	private void applicaCarta(Giocatore giocatore, Carta carta) {
		ultimaCartaPescata = carta;
		cartePescate++;
		String mazzo = carta instanceof Imprevisto ? "Imprevisti" : "Probabilità";
		registra(giocatore.getNome() + " pesca una carta " + mazzo + ": \"" + carta.getTesto() + "\"");

		switch (carta.getEffetto()) {
			case INCASSA:
				giocatore.incassa(carta.getValore());
				break;
			case PAGA:
				pagamento(giocatore, null, carta.getValore());
				break;
			case VAI_A:
				int passi = Math.floorMod(carta.getValore() - giocatore.getPosizione(), Tabellone.NUMERO_CASELLE);
				muovi(giocatore, passi == 0 ? Tabellone.NUMERO_CASELLE : passi);
				break;
			case TORNA_A:
				// lo spostamento all'indietro non fa passare dal Via
				muoviIndietro(giocatore, carta.getValore());
				break;
			case SPOSTA:
				muoviIndietro(giocatore,
						Math.floorMod(giocatore.getPosizione() + carta.getValore(), Tabellone.NUMERO_CASELLE));
				break;
			case VAI_IN_PRIGIONE:
				vaiInPrigione(giocatore);
				break;
			case ESCI_GRATIS_DI_PRIGIONE:
				giocatore.getCarteUscitaDiPrigione().add(carta);
				break;
			case RIPARAZIONI:
				int casette = 0;
				int alberghi = 0;
				for (Casella casella : getProprietaDi(giocatore)) {
					if (casella.haAlbergo()) {
						alberghi++;
					} else {
						casette += casella.getNumeroDiCasetteSullaCasella();
					}
				}
				int importo = casette * carta.getValore() + alberghi * carta.getValoreAlbergo();
				if (importo > 0) {
					registra(giocatore.getNome() + " paga " + importo + "€ per " + casette + " case e " + alberghi
							+ " alberghi.");
					pagamento(giocatore, null, importo);
				} else {
					registra(giocatore.getNome() + " non possiede edifici: non paga nulla.");
				}
				break;
		}
	}

	/**
	 * Trasferisce denaro dal debitore al creditore (o alla banca se il creditore è
	 * null). Se il debitore non ha abbastanza contanti vende automaticamente gli
	 * edifici e ipoteca le proprietà; se non basta va in bancarotta.
	 *
	 * @return true se il pagamento è andato a buon fine
	 */
	private boolean pagamento(Giocatore debitore, Giocatore creditore, int importo) {
		if (debitore.getPortafoglio() < importo) {
			raccogliDenaro(debitore, importo);
		}
		if (debitore.getPortafoglio() < importo) {
			bancarotta(debitore, creditore);
			return false;
		}
		debitore.paga(importo);
		if (creditore != null) {
			creditore.incassa(importo);
		}
		return true;
	}

	private void raccogliDenaro(Giocatore giocatore, int importo) {
		registra(giocatore.getNome() + " non ha abbastanza contanti: vende edifici e ipoteca proprietà.");
		while (giocatore.getPortafoglio() < importo) {
			Casella conEdifici = getProprietaDi(giocatore).stream()
					.filter(casella -> casella.getNumeroDiCasetteSullaCasella() > 0)
					.max(Comparator.comparingInt(Casella::getNumeroDiCasetteSullaCasella))
					.orElse(null);
			if (conEdifici != null) {
				vendiEdificio(giocatore, conEdifici, true);
				continue;
			}
			Casella daIpotecare = getProprietaDi(giocatore).stream()
					.filter(casella -> !casella.isIpotecata())
					.findFirst()
					.orElse(null);
			if (daIpotecare == null) {
				return;
			}
			ipotecaProprieta(giocatore, daIpotecare);
		}
	}

	private void bancarotta(Giocatore giocatore, Giocatore creditore) {
		giocatore.setInBancarotta(true);
		registra(giocatore.getNome() + " è in bancarotta ed esce dal gioco!");

		for (Casella casella : getProprietaDi(giocatore)) {
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
		} else if (giocatore == getGiocatoreCorrente()) {
			passaAlProssimoGiocatore();
		}
	}

	private void vendiEdificio(Giocatore giocatore, Casella casella, boolean venditaForzata) {
		int numeroCasette = casella.getNumeroDiCasetteSullaCasella();
		if (!casella.isTerreno() || numeroCasette == 0) {
			throw new MossaNonValidaException("Su " + casella.getTesto() + " non ci sono edifici da vendere.");
		}
		int massimoDelGruppo = tabellone.getCaselleDelColore(casella.getColore()).stream()
				.mapToInt(Casella::getNumeroDiCasetteSullaCasella).max().orElse(0);
		if (!venditaForzata && numeroCasette < massimoDelGruppo) {
			throw new MossaNonValidaException(
					"Devi vendere in modo uniforme: prima vendi gli edifici sui terreni del gruppo che ne hanno di più.");
		}

		int ricavo = casella.getCostoCasetta() / 2;
		if (casella.haAlbergo()) {
			if (casetteDisponibili >= 4) {
				// l'albergo viene sostituito da 4 casette
				casetteDisponibili -= 4;
				casella.setNumeroDiCasetteSullaCasella(4);
			} else if (venditaForzata) {
				ricavo *= 5;
				casella.setNumeroDiCasetteSullaCasella(0);
			} else {
				throw new MossaNonValidaException(
						"La banca non ha 4 casette per sostituire l'albergo: non puoi venderlo ora.");
			}
			alberghiDisponibili++;
			registra(giocatore.getNome() + " vende l'albergo su " + casella.getTesto() + " e incassa " + ricavo
					+ "€.");
		} else {
			casetteDisponibili++;
			casella.setNumeroDiCasetteSullaCasella(numeroCasette - 1);
			registra(giocatore.getNome() + " vende una casa su " + casella.getTesto() + " e incassa " + ricavo
					+ "€.");
		}
		giocatore.incassa(ricavo);
	}

	private void ipotecaProprieta(Giocatore giocatore, Casella casella) {
		if (casella.isIpotecata()) {
			throw new MossaNonValidaException(casella.getTesto() + " è già ipotecata.");
		}
		if (casella.isTerreno() && tabellone.getCaselleDelColore(casella.getColore()).stream()
				.anyMatch(terreno -> terreno.getNumeroDiCasetteSullaCasella() > 0)) {
			throw new MossaNonValidaException("Prima di ipotecare devi vendere gli edifici del gruppo.");
		}
		casella.setIpotecata(true);
		giocatore.incassa(casella.getValoreIpotecario());
		registra(giocatore.getNome() + " ipoteca " + casella.getTesto() + " e incassa "
				+ casella.getValoreIpotecario() + "€.");
	}

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
		if (carta instanceof Imprevisto) {
			mazzoImprevisti.rimettiInFondo(carta);
		} else {
			mazzoProbabilita.rimettiInFondo(carta);
		}
	}

	/** Dopo il lancio (ed eventuale acquisto) si rilancia solo se è uscito un doppio. */
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

	private Casella verificaProprietaDelGiocatoreCorrente(int posizione) {
		if (fase == FasePartita.TERMINATA) {
			throw new MossaNonValidaException("La partita è terminata.");
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

	private void registra(String evento) {
		eventi.add(evento);
		if (eventi.size() > MAX_EVENTI) {
			eventi.remove(0);
		}
	}

	// ---------------------------------------------------------------------
	// Interrogazioni sullo stato della partita
	// ---------------------------------------------------------------------

	public boolean possiedeTuttoIlGruppo(Giocatore giocatore, String colore) {
		return tabellone.getCaselleDelColore(colore).stream()
				.allMatch(casella -> casella.getProprietario() == giocatore);
	}

	private int contaProprietaDelTipo(Giocatore giocatore, TipoCasella tipo) {
		return (int) tabellone.getCaselleDelTipo(tipo).stream()
				.filter(casella -> casella.getProprietario() == giocatore)
				.count();
	}

	public List<Casella> getProprietaDi(Giocatore giocatore) {
		return tabellone.getCaselle().stream()
				.filter(casella -> casella.getProprietario() == giocatore)
				.collect(Collectors.toList());
	}

	public int calcolaPatrimonio(Giocatore giocatore) {
		int patrimonio = giocatore.getPortafoglio();
		for (Casella casella : getProprietaDi(giocatore)) {
			patrimonio += casella.isIpotecata() ? casella.getValoreIpotecario() : casella.getCostoProprieta();
			patrimonio += casella.getNumeroDiCasetteSullaCasella() * casella.getCostoCasetta();
		}
		return patrimonio;
	}

	public List<Giocatore> getGiocatoriAttivi() {
		return giocatori.stream().filter(giocatore -> !giocatore.isInBancarotta()).collect(Collectors.toList());
	}

	public Giocatore getGiocatoreCorrente() {
		return giocatori.get(indiceGiocatoreCorrente);
	}

	public List<Giocatore> getGiocatori() {
		return giocatori;
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

	/** Numero di carte pescate dall'inizio della partita: l'interfaccia lo usa per mostrare ogni carta una volta sola. */
	public int getCartePescate() {
		return cartePescate;
	}

	public Giocatore getVincitore() {
		return vincitore;
	}

	public int getCasetteDisponibili() {
		return casetteDisponibili;
	}

	public int getAlberghiDisponibili() {
		return alberghiDisponibili;
	}

	public List<String> getEventi() {
		return eventi;
	}

}
