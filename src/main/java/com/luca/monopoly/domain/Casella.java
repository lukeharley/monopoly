package com.luca.monopoly.domain;

/**
 * Una delle 40 caselle del tabellone. Per le proprietà conserva anche lo stato
 * di gioco: proprietario, edifici e ipoteca, modificabili solo dal dominio.
 */
public class Casella {

	/** Dopo 4 case si costruisce l'albergo, che conta come quinto edificio. */
	public static final int MASSIMO_CASE = 4;
	public static final int ALBERGO = MASSIMO_CASE + 1;

	private static final int PERCENTUALE_IPOTECA = 50;
	private static final int INTERESSE_RISCATTO_PERCENTO = 10;
	private static final int PERCENTUALE_RIVENDITA_EDIFICIO = 50;

	private final int posizione;
	private final String testo;
	private final TipoCasella tipo;
	private final GruppoColore gruppo;
	private final int prezzo;
	private final int costoCasetta;
	private final int importoTassa;
	private final Contratto contratto;

	private int numeroEdifici;
	private Giocatore proprietario;
	private boolean ipotecata;

	private Casella(int posizione, String testo, TipoCasella tipo, GruppoColore gruppo, int prezzo, int costoCasetta,
			int importoTassa, Contratto contratto) {
		this.posizione = posizione;
		this.testo = testo;
		this.tipo = tipo;
		this.gruppo = gruppo;
		this.prezzo = prezzo;
		this.costoCasetta = costoCasetta;
		this.importoTassa = importoTassa;
		this.contratto = contratto;
	}

	static Casella speciale(int posizione, String testo, TipoCasella tipo) {
		return new Casella(posizione, testo, tipo, null, 0, 0, 0, null);
	}

	static Casella tassa(int posizione, String testo, int importo) {
		return new Casella(posizione, testo, TipoCasella.TASSA, null, 0, 0, importo, null);
	}

	static Casella terreno(int posizione, String testo, GruppoColore gruppo, int prezzo, int costoCasetta,
			Contratto contratto) {
		return new Casella(posizione, testo, TipoCasella.TERRENO, gruppo, prezzo, costoCasetta, 0, contratto);
	}

	static Casella proprieta(int posizione, String testo, TipoCasella tipo, int prezzo, Contratto contratto) {
		return new Casella(posizione, testo, tipo, null, prezzo, 0, 0, contratto);
	}

	// ---------------------------------------------------------------------
	// Lettura
	// ---------------------------------------------------------------------

	public int getPosizione() {
		return posizione;
	}

	public String getTesto() {
		return testo;
	}

	public TipoCasella getTipo() {
		return tipo;
	}

	public boolean isTerreno() {
		return tipo == TipoCasella.TERRENO;
	}

	public boolean isAcquistabile() {
		return tipo.isAcquistabile();
	}

	/** Solo per i terreni, altrimenti null. */
	public GruppoColore getGruppo() {
		return gruppo;
	}

	/** Prezzo di acquisto; 0 per le caselle che non si possono comprare. */
	public int getPrezzo() {
		return prezzo;
	}

	public int getCostoCasetta() {
		return costoCasetta;
	}

	/** Solo per le caselle di tipo TASSA, altrimenti 0. */
	public int getImportoTassa() {
		return importoTassa;
	}

	/** Solo per le proprietà, altrimenti null. */
	public Contratto getContratto() {
		return contratto;
	}

	public int getNumeroEdifici() {
		return numeroEdifici;
	}

	public boolean haAlbergo() {
		return numeroEdifici == ALBERGO;
	}

	public Giocatore getProprietario() {
		return proprietario;
	}

	public boolean isIpotecata() {
		return ipotecata;
	}

	public int getValoreIpotecario() {
		return prezzo * PERCENTUALE_IPOTECA / 100;
	}

	/** Valore dell'ipoteca più gli interessi, arrotondato per eccesso. */
	public int getCostoRiscatto() {
		return (getValoreIpotecario() * (100 + INTERESSE_RISCATTO_PERCENTO) + 99) / 100;
	}

	/** Quanto paga la banca per ogni edificio rivenduto. */
	public int getRicavoVenditaEdificio() {
		return costoCasetta * PERCENTUALE_RIVENDITA_EDIFICIO / 100;
	}

	// ---------------------------------------------------------------------
	// Modifica (solo dal dominio)
	// ---------------------------------------------------------------------

	void assegnaA(Giocatore nuovoProprietario) {
		this.proprietario = nuovoProprietario;
	}

	/** Torna libera: senza proprietario, senza ipoteca e senza edifici. */
	void restituisciAllaBanca() {
		proprietario = null;
		ipotecata = false;
		numeroEdifici = 0;
	}

	void aggiungiEdificio() {
		numeroEdifici++;
	}

	/** Togliendo l'albergo restano le 4 case su cui era costruito. */
	void rimuoviEdificio() {
		numeroEdifici--;
	}

	void rimuoviTuttiGliEdifici() {
		numeroEdifici = 0;
	}

	void ipoteca() {
		ipotecata = true;
	}

	void riscatta() {
		ipotecata = false;
	}

}
