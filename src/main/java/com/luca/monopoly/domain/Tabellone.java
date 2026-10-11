package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Tabellone {

	public static final int NUMERO_CASELLE = 40;
	public static final int POSIZIONE_PRIGIONE = 10;

	private final int NUMERO_MAX_PEZZI_CASETTA = 32;
	private final int NUMERO_MAX_PEZZI_ALBERGO = 12;

	private final List<Contratto> contratti = new ArrayList<>();
	private final List<Casella> caselle = new ArrayList<>();
	private final List<Imprevisto> imprevisti = new ArrayList<>();
	private final List<Probabilita> probabilita = new ArrayList<>();

	public Tabellone() {

		caselle.add(new Casella("Via", TipoCasella.VIA));
		caselle.add(terreno("Vicolo Corto", "fucsia", 60));
		caselle.add(new Casella("Probabilità", TipoCasella.PROBABILITA));
		caselle.add(terreno("Vicolo Stretto", "fucsia", 60));
		caselle.add(new Casella("Tassa Patrimoniale", TipoCasella.TASSA, null, 200, 0));
		caselle.add(new Casella("Stazione Sud", TipoCasella.STAZIONE, null, 200, 0));
		caselle.add(terreno("Bastioni Gran Sasso", "azzurro", 100));
		caselle.add(new Casella("Imprevisti", TipoCasella.IMPREVISTI));
		caselle.add(terreno("Viale Monterosa", "azzurro", 100));
		caselle.add(terreno("Viale Vesuvio", "azzurro", 120));
		caselle.add(new Casella("Prigione/Transito", TipoCasella.PRIGIONE));
		caselle.add(terreno("Via Accademia", "arancione", 140));
		caselle.add(new Casella("Società Elettrica", TipoCasella.SOCIETA, null, 150, 0));
		caselle.add(terreno("Corso Ateneo", "arancione", 140));
		caselle.add(terreno("Piazza Università", "arancione", 160));
		caselle.add(new Casella("Stazione Ovest", TipoCasella.STAZIONE, null, 200, 0));
		caselle.add(terreno("Via Verdi", "marrone", 180));
		caselle.add(new Casella("Probabilità", TipoCasella.PROBABILITA));
		caselle.add(terreno("Corso Raffaello", "marrone", 180));
		caselle.add(terreno("Piazza Dante", "marrone", 200));
		caselle.add(new Casella("Posteggio Gratuito", TipoCasella.POSTEGGIO));
		caselle.add(terreno("Via Marco Polo", "rosso", 220));
		caselle.add(new Casella("Imprevisti", TipoCasella.IMPREVISTI));
		caselle.add(terreno("Corso Magellano", "rosso", 220));
		caselle.add(terreno("Largo Colombo", "rosso", 240));
		caselle.add(new Casella("Stazione Nord", TipoCasella.STAZIONE, null, 200, 0));
		caselle.add(terreno("Viale Costantino", "giallo", 260));
		caselle.add(terreno("Viale Traiano", "giallo", 260));
		caselle.add(new Casella("Società Acqua Potabile", TipoCasella.SOCIETA, null, 150, 0));
		caselle.add(terreno("Piazza Giulio Cesare", "giallo", 280));
		caselle.add(new Casella("In prigione!", TipoCasella.VAI_IN_PRIGIONE));
		caselle.add(terreno("Via Roma", "verde", 300));
		caselle.add(terreno("Corso Impero", "verde", 300));
		caselle.add(new Casella("Probabilità", TipoCasella.PROBABILITA));
		caselle.add(terreno("Largo Augusto", "verde", 320));
		caselle.add(new Casella("Stazione Est", TipoCasella.STAZIONE, null, 200, 0));
		caselle.add(new Casella("Imprevisti", TipoCasella.IMPREVISTI));
		caselle.add(terreno("Viale dei Giardini", "viola", 350));
		caselle.add(new Casella("Tassa di Lusso", TipoCasella.TASSA, null, 100, 0));
		caselle.add(terreno("Parco della Vittoria", "viola", 400));

		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Vicolo Corto", 2, 10, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Vicolo Stretto", 4, 20, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Stazione Sud", 0, 0, 65, 25));
		contratti.add(new Contratto("Bastioni Gran Sasso", 6, 30, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Viale Monterosa", 6, 30, 0, 0));
		contratti.add(new Contratto("Viale Vesuvio", 8, 40, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Via Accademia", 10, 50, 0, 0));
		contratti.add(new Contratto("Società Elettrica", 0, 0, 50, 50));
		contratti.add(new Contratto("Corso Ateneo", 10, 50, 0, 0));
		contratti.add(new Contratto("Piazza Università", 12, 60, 0, 0));
		contratti.add(new Contratto("Stazione Ovest", 0, 0, 65, 25));
		contratti.add(new Contratto("Via Verdi", 14, 70, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Corso Raffaello", 14, 70, 0, 0));
		contratti.add(new Contratto("Piazza Dante", 16, 80, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Via Marco Polo", 18, 90, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Corso Magellano", 18, 90, 0, 0));
		contratti.add(new Contratto("Largo Colombo", 20, 100, 0, 0));
		contratti.add(new Contratto("Stazione Nord", 0, 0, 65, 25));
		contratti.add(new Contratto("Viale Costantino", 22, 110, 0, 0));
		contratti.add(new Contratto("Viale Traiano", 22, 110, 0, 0));
		contratti.add(new Contratto("Società Acqua Potabile", 0, 0, 50, 50));
		contratti.add(new Contratto("Piazza Giulio Cesare", 24, 120, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Via Roma", 26, 130, 0, 0));
		contratti.add(new Contratto("Corso Impero", 26, 130, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Largo Augusto", 28, 150, 0, 0));
		contratti.add(new Contratto("Stazione Est", 0, 0, 65, 25));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Viale dei Giardini", 35, 175, 0, 0));
		contratti.add(new Contratto("Segnaposto", 0, 0, 0, 0));
		contratti.add(new Contratto("Parco della Vittoria", 50, 200, 0, 0));

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

	}

	/*
	 * Il costo di una casetta dipende dal lato del tabellone su cui si trova il
	 * terreno: 50 euro per il primo lato, 100 per il secondo, 150 per il terzo e
	 * 200 per il quarto.
	 */
	private Casella terreno(String testo, String colore, int costoProprieta) {
		int lato = caselle.size() / 10;
		int costoCasetta = (lato + 1) * 50;
		return new Casella(testo, TipoCasella.TERRENO, colore, costoProprieta, costoCasetta);
	}

	public List<Casella> getCaselle() {
		return caselle;
	}

	public Casella getCasella(int posizione) {
		return caselle.get(posizione);
	}

	public List<Contratto> getContratti() {
		return contratti;
	}

	public Contratto getContratto(int posizione) {
		return contratti.get(posizione);
	}

	public List<Imprevisto> getImprevisti() {
		return imprevisti;
	}

	public List<Probabilita> getProbabilita() {
		return probabilita;
	}

	public List<Casella> getCaselleDelColore(String colore) {
		return caselle.stream()
				.filter(casella -> casella.getColore() != null && casella.getColore().equals(colore))
				.collect(Collectors.toList());
	}

	public List<Casella> getCaselleDelTipo(TipoCasella tipo) {
		return caselle.stream()
				.filter(casella -> casella.getTipo() == tipo)
				.collect(Collectors.toList());
	}

	public List<Casetta> getCasette() {

		List<Casetta> listaDiCasette = new ArrayList<>();

		for (int i = 1; i <= NUMERO_MAX_PEZZI_CASETTA; i++) {
			listaDiCasette.add(new Casetta());
		}

		return listaDiCasette;
	}

	public List<Albergo> getAlberghi() {

		List<Albergo> listaDiAlberghi = new ArrayList<>();

		for (int i = 1; i <= NUMERO_MAX_PEZZI_ALBERGO; i++) {
			listaDiAlberghi.add(new Albergo());
		}

		return listaDiAlberghi;
	}

}
