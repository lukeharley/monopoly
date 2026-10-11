package com.luca.monopoly.domain;

/**
 * Prepara le situazioni di gioco dei test usando solo le operazioni del dominio,
 * senza setter: lo stato resta sempre coerente con le regole.
 */
final class ScenarioPartita {

	private ScenarioPartita() {
	}

	static void conContanti(Giocatore giocatore, int contanti) {
		giocatore.incassa(contanti - giocatore.getPortafoglio());
	}

	/** In prigione da {@code turniGiaPassati} turni (0 = appena entrato). */
	static void inPrigione(Giocatore giocatore, int turniGiaPassati) {
		giocatore.entraInPrigione();
		for (int turno = 0; turno < turniGiaPassati; turno++) {
			giocatore.registraTurnoInPrigione();
		}
	}

	/** Mette gli edifici direttamente, senza passare dalle regole di costruzione. */
	static void conEdifici(Casella casella, int numeroEdifici) {
		for (int edificio = 0; edificio < numeroEdifici; edificio++) {
			casella.aggiungiEdificio();
		}
	}
}
