package com.luca.monopoly.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Il contenuto della scatola del Monopoly: tabellone, banca, segnalini e dadi.
 */
public class Monopoly {

	private final Tabellone tabellone;
	private final Banca banca;

	public Monopoly() {
		this.tabellone = new Tabellone();
		this.banca = new Banca();
	}

	public Tabellone getTabellone() {
		return tabellone;
	}

	public Banca getBanca() {
		return banca;
	}

	public List<Segnalini> getSegnalini() {
		return Arrays.asList(Segnalini.values());
	}

	public List<Dado> getDadi() {

		List<Dado> dadi = new ArrayList<>();

		for (int i = 1; i <= 2; i++) {
			dadi.add(new Dado());
		}

		return dadi;
	}
}
