package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Il tabellone dell'edizione italiana in euro: 40 caselle con i relativi
 * contratti e i mazzi di Imprevisti e Probabilità (nell'ordine di stampa).
 */
public class Tabellone {

	public static final int NUMERO_CASELLE = 40;
	public static final int POSIZIONE_PRIGIONE = 10;

	private static final int CASELLE_PER_LATO = 10;
	/** Una casa costa 50€ sul primo lato, 100€ sul secondo, 150€ sul terzo e 200€ sul quarto. */
	private static final int COSTO_CASA_PER_LATO = 50;
	private static final int PREZZO_STAZIONE = 200;
	private static final int PREZZO_SOCIETA = 150;
	private static final List<Integer> RENDITE_STAZIONE = List.of(25, 50, 100, 200);
	private static final List<Integer> MOLTIPLICATORI_SOCIETA = List.of(4, 10);

	private final List<Casella> caselle = new ArrayList<>();
	private final Map<GruppoColore, List<Casella>> caselleDelGruppo = new EnumMap<>(GruppoColore.class);
	private final List<Imprevisto> imprevisti = new ArrayList<>();
	private final List<Probabilita> probabilita = new ArrayList<>();

	public Tabellone() {

		speciale("Via", TipoCasella.VIA);
		terreno("Vicolo Corto", GruppoColore.FUCSIA, 60, 2, 10, 30, 90, 160, 250);
		speciale("Probabilità", TipoCasella.PROBABILITA);
		terreno("Vicolo Stretto", GruppoColore.FUCSIA, 60, 4, 20, 60, 180, 320, 450);
		tassa("Tassa Patrimoniale", 200);
		stazione("Stazione Sud");
		terreno("Bastioni Gran Sasso", GruppoColore.AZZURRO, 100, 6, 30, 90, 270, 400, 550);
		speciale("Imprevisti", TipoCasella.IMPREVISTI);
		terreno("Viale Monterosa", GruppoColore.AZZURRO, 100, 6, 30, 90, 270, 400, 550);
		terreno("Viale Vesuvio", GruppoColore.AZZURRO, 120, 8, 40, 100, 300, 450, 600);
		speciale("Prigione/Transito", TipoCasella.PRIGIONE);
		terreno("Via Accademia", GruppoColore.ARANCIONE, 140, 10, 50, 150, 450, 625, 750);
		societa("Società Elettrica");
		terreno("Corso Ateneo", GruppoColore.ARANCIONE, 140, 10, 50, 150, 450, 625, 750);
		terreno("Piazza Università", GruppoColore.ARANCIONE, 160, 12, 60, 180, 500, 700, 900);
		stazione("Stazione Ovest");
		terreno("Via Verdi", GruppoColore.MARRONE, 180, 14, 70, 200, 550, 750, 950);
		speciale("Probabilità", TipoCasella.PROBABILITA);
		terreno("Corso Raffaello", GruppoColore.MARRONE, 180, 14, 70, 200, 550, 750, 950);
		terreno("Piazza Dante", GruppoColore.MARRONE, 200, 16, 80, 220, 600, 800, 1000);
		speciale("Posteggio Gratuito", TipoCasella.POSTEGGIO);
		terreno("Via Marco Polo", GruppoColore.ROSSO, 220, 18, 90, 250, 700, 875, 1050);
		speciale("Imprevisti", TipoCasella.IMPREVISTI);
		terreno("Corso Magellano", GruppoColore.ROSSO, 220, 18, 90, 250, 700, 875, 1050);
		terreno("Largo Colombo", GruppoColore.ROSSO, 240, 20, 100, 300, 750, 925, 1100);
		stazione("Stazione Nord");
		terreno("Viale Costantino", GruppoColore.GIALLO, 260, 22, 110, 330, 800, 975, 1150);
		terreno("Viale Traiano", GruppoColore.GIALLO, 260, 22, 110, 330, 800, 975, 1150);
		societa("Società Acqua Potabile");
		terreno("Piazza Giulio Cesare", GruppoColore.GIALLO, 280, 24, 120, 360, 850, 1025, 1200);
		speciale("In prigione!", TipoCasella.VAI_IN_PRIGIONE);
		terreno("Via Roma", GruppoColore.VERDE, 300, 26, 130, 390, 900, 1100, 1275);
		terreno("Corso Impero", GruppoColore.VERDE, 300, 26, 130, 390, 900, 1100, 1275);
		speciale("Probabilità", TipoCasella.PROBABILITA);
		terreno("Largo Augusto", GruppoColore.VERDE, 320, 28, 150, 450, 1000, 1200, 1400);
		stazione("Stazione Est");
		speciale("Imprevisti", TipoCasella.IMPREVISTI);
		terreno("Viale dei Giardini", GruppoColore.VIOLA, 350, 35, 175, 500, 1100, 1300, 1500);
		tassa("Tassa di Lusso", 100);
		terreno("Parco della Vittoria", GruppoColore.VIOLA, 400, 50, 200, 600, 1400, 1700, 2000);

		imprevisti.add(new Imprevisto("Andate sino al Largo Colombo: se passate dal Via ritirate 200 euro.",
				EffettoCarta.VAI_A, 24));
		imprevisti.add(new Imprevisto("Andate in prigione direttamente e senza passare dal Via.",
				EffettoCarta.VAI_IN_PRIGIONE));
		imprevisti.add(new Imprevisto(
				"Avete tutti i vostri stabili da riparare: pagate 60 euro per ogni casa e 250 euro per ogni albergo.",
				EffettoCarta.RIPARAZIONI, 60, 250));
		imprevisti.add(new Imprevisto("Fate 3 passi indietro (con tanti auguri).", EffettoCarta.SPOSTA, -3));
		imprevisti.add(new Imprevisto("Andate sino a Via Accademia: se passate dal Via ritirate 200 euro.",
				EffettoCarta.VAI_A, 11));
		imprevisti.add(new Imprevisto("Versate 50 euro per beneficenza.", EffettoCarta.PAGA, 50));
		imprevisti.add(new Imprevisto("Andate alla Stazione Nord: se passate dal Via ritirate 200 euro.",
				EffettoCarta.VAI_A, 25));
		imprevisti.add(new Imprevisto("Multa di 40 euro per aver guidato senza patente.", EffettoCarta.PAGA, 40));
		imprevisti.add(new Imprevisto("Andate fino al Parco della Vittoria.", EffettoCarta.VAI_A, 39));
		imprevisti.add(new Imprevisto("Matrimonio in famiglia: spese impreviste 375 euro.", EffettoCarta.PAGA, 375));
		imprevisti.add(new Imprevisto(
				"Uscite gratis di prigione, se ci siete: potete conservare questo cartoncino sino al momento di servirvene (non si sa mai!).",
				EffettoCarta.ESCI_GRATIS_DI_PRIGIONE));
		imprevisti.add(new Imprevisto("Maturano le cedole delle vostre cartelle di rendita, ritirate 375 euro.",
				EffettoCarta.INCASSA, 375));
		imprevisti.add(new Imprevisto("La banca vi paga gli interessi del vostro conto corrente, ritirate 125 euro.",
				EffettoCarta.INCASSA, 125));
		imprevisti.add(new Imprevisto("Andate avanti sino al Via.", EffettoCarta.VAI_A, 0));
		imprevisti.add(new Imprevisto("Avete vinto un terno al lotto: ritirate 250 euro.", EffettoCarta.INCASSA, 250));
		imprevisti.add(new Imprevisto("Andate in prigione direttamente e senza passare dal Via.",
				EffettoCarta.VAI_IN_PRIGIONE));

		probabilita.add(new Probabilita("Ritornate al Vicolo Corto.", EffettoCarta.TORNA_A, 1));
		probabilita.add(new Probabilita("È maturata la cedola delle vostre azioni: ritirate 60 euro.",
				EffettoCarta.INCASSA, 60));
		probabilita.add(new Probabilita("Rimborso tassa sul reddito: ritirate 50 euro dalla banca.",
				EffettoCarta.INCASSA, 50));
		probabilita.add(new Probabilita("Avete vinto il secondo premio in un concorso di bellezza: ritirate 25 euro.",
				EffettoCarta.INCASSA, 25));
		probabilita.add(new Probabilita("Scade il vostro premio di assicurazione: pagate 125 euro.",
				EffettoCarta.PAGA, 125));
		probabilita.add(new Probabilita(
				"Uscite gratis di prigione, se ci siete: potete conservare questo cartoncino sino al momento di servirvene (non si sa mai!).",
				EffettoCarta.ESCI_GRATIS_DI_PRIGIONE));
		probabilita.add(new Probabilita("Avete vinto un premio di consolazione alla lotteria di Merano: ritirate 250 euro.",
				EffettoCarta.INCASSA, 250));
		probabilita.add(new Probabilita("Andate fino al Via.", EffettoCarta.VAI_A, 0));
		probabilita.add(new Probabilita("Pagate una multa di 25 euro.", EffettoCarta.PAGA, 25));
		probabilita.add(new Probabilita("Siete creditori verso la banca di 500 euro, ritirateli.",
				EffettoCarta.INCASSA, 500));
		probabilita.add(new Probabilita("Avete perso una causa: pagate 250 euro.", EffettoCarta.PAGA, 250));
		probabilita.add(new Probabilita("Andate in prigione direttamente e senza passare dal Via.",
				EffettoCarta.VAI_IN_PRIGIONE));
		probabilita.add(new Probabilita("Vi viene rimborsata la tassa sui redditi: incassate 20 euro.",
				EffettoCarta.INCASSA, 20));
		probabilita.add(new Probabilita(
				"Dovete pagare un contributo di miglioria stradale: 100 euro per ogni casa, 250 euro per ogni albergo che possedete.",
				EffettoCarta.RIPARAZIONI, 100, 250));
		probabilita.add(new Probabilita("Avete ceduto delle azioni: ricavate 125 euro.", EffettoCarta.INCASSA, 125));
		probabilita.add(new Probabilita("Pagate il conto del dottore: 125 euro.", EffettoCarta.PAGA, 125));

		for (GruppoColore gruppo : GruppoColore.values()) {
			caselleDelGruppo.put(gruppo, caselle.stream()
					.filter(casella -> casella.getGruppo() == gruppo)
					.collect(Collectors.toUnmodifiableList()));
		}
	}

	private void speciale(String testo, TipoCasella tipo) {
		caselle.add(Casella.speciale(caselle.size(), testo, tipo));
	}

	private void tassa(String testo, int importo) {
		caselle.add(Casella.tassa(caselle.size(), testo, importo));
	}

	private void terreno(String testo, GruppoColore gruppo, int prezzo, int nudo, int unaCasa, int dueCase,
			int treCase, int quattroCase, int albergo) {
		int lato = caselle.size() / CASELLE_PER_LATO;
		int costoCasetta = (lato + 1) * COSTO_CASA_PER_LATO;
		caselle.add(Casella.terreno(caselle.size(), testo, gruppo, prezzo, costoCasetta,
				Contratto.terreno(nudo, unaCasa, dueCase, treCase, quattroCase, albergo)));
	}

	private void stazione(String testo) {
		caselle.add(Casella.proprieta(caselle.size(), testo, TipoCasella.STAZIONE, PREZZO_STAZIONE,
				Contratto.conRendite(RENDITE_STAZIONE)));
	}

	private void societa(String testo) {
		caselle.add(Casella.proprieta(caselle.size(), testo, TipoCasella.SOCIETA, PREZZO_SOCIETA,
				Contratto.conRendite(MOLTIPLICATORI_SOCIETA)));
	}

	public List<Casella> getCaselle() {
		return Collections.unmodifiableList(caselle);
	}

	public Casella getCasella(int posizione) {
		return caselle.get(posizione);
	}

	public List<Imprevisto> getImprevisti() {
		return Collections.unmodifiableList(imprevisti);
	}

	public List<Probabilita> getProbabilita() {
		return Collections.unmodifiableList(probabilita);
	}

	public List<Casella> getCaselleDelGruppo(GruppoColore gruppo) {
		return caselleDelGruppo.get(gruppo);
	}

	public List<Casella> getProprietaDi(Giocatore giocatore) {
		return caselle.stream()
				.filter(casella -> casella.getProprietario() == giocatore)
				.collect(Collectors.toList());
	}

	public boolean possiedeTuttoIlGruppo(Giocatore giocatore, GruppoColore gruppo) {
		return getCaselleDelGruppo(gruppo).stream()
				.allMatch(casella -> casella.getProprietario() == giocatore);
	}

	public int contaProprietaDelTipo(Giocatore giocatore, TipoCasella tipo) {
		return (int) caselle.stream()
				.filter(casella -> casella.getTipo() == tipo && casella.getProprietario() == giocatore)
				.count();
	}
}
