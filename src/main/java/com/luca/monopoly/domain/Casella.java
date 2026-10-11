package com.luca.monopoly.domain;

import com.luca.monopoly.domain.giocatore.Giocatore;

public class Casella {
	private final String testo;
	private final TipoCasella tipo;
	private final String colore;
	/*
	 * Per terreni, stazioni e società è il prezzo di acquisto; per le caselle di
	 * tipo TASSA è l'importo da pagare alla banca.
	 */
	private final int costoProprieta;
	private final int costoCasetta;
	private int numeroDiCasetteSullaCasella;
	private Giocatore proprietario;
	private boolean ipotecata;

	public Casella(String testo, TipoCasella tipo, String colore, int costoProprieta, int costoCasetta) {
		this.testo = testo;
		this.tipo = tipo;
		this.colore = colore;
		this.costoProprieta = costoProprieta;
		this.costoCasetta = costoCasetta;
	}

	public Casella(String testo, TipoCasella tipo) {
		this(testo, tipo, null, 0, 0);
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

	public String getColore() {
		return colore;
	}

	public int getCostoProprieta() {
		return costoProprieta;
	}

	public int getCostoCasetta() {
		return costoCasetta;
	}

	public int getNumeroDiCasetteSullaCasella() {
		return numeroDiCasetteSullaCasella;
	}

	public void setNumeroDiCasetteSullaCasella(int numeroDiCasetteSullaCasella) {
		this.numeroDiCasetteSullaCasella = numeroDiCasetteSullaCasella;
	}

	/** 5 casette equivalgono a un albergo. */
	public boolean haAlbergo() {
		return numeroDiCasetteSullaCasella == 5;
	}

	public Giocatore getProprietario() {
		return proprietario;
	}

	public void setProprietario(Giocatore proprietario) {
		this.proprietario = proprietario;
	}

	public boolean isIpotecata() {
		return ipotecata;
	}

	public void setIpotecata(boolean ipotecata) {
		this.ipotecata = ipotecata;
	}

	public int getValoreIpotecario() {
		return costoProprieta / 2;
	}

	/** Valore dell'ipoteca + 10% di interessi, arrotondato per eccesso. */
	public int getCostoRiscatto() {
		return (getValoreIpotecario() * 11 + 9) / 10;
	}

}
