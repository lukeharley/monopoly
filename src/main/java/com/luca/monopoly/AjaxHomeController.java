package com.luca.monopoly;

import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.service.MonopolyService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Consumer;

/**
 * API JSON della partita. Ogni mossa accetta il parametro opzionale
 * {@code versione}: se non coincide con quella attuale la mossa viene
 * rifiutata con 409 (vedi {@link MonopolyService}).
 */
@RestController
public class AjaxHomeController {

    private final MonopolyService monopolyService;

    public AjaxHomeController(MonopolyService monopolyService) {
        this.monopolyService = monopolyService;
    }

    /**
     * Con {@code versione} uguale a quella attuale risponde 204 senza costruire la
     * vista: nulla è cambiato. Le risposte non vanno mai in cache, perché lo stato
     * cambia a ogni mossa.
     */
    @GetMapping("/stato")
    public ResponseEntity<StatoPartitaView> stato(@RequestParam(required = false) Long versione) {
        return monopolyService.leggiSeCambiata(versione, StatoPartitaView::da)
                .map(stato -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stato))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @PostMapping("/lanciaDadi")
    public StatoPartitaView lanciaDadi(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::lanciaDadi);
    }

    @PostMapping("/compraProprieta")
    public StatoPartitaView compraProprieta(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::compraProprieta);
    }

    @PostMapping("/rifiutaAcquisto")
    public StatoPartitaView rifiutaAcquisto(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::rifiutaAcquisto);
    }

    @PostMapping("/offerta")
    public StatoPartitaView offerta(@RequestParam int importo, @RequestParam(required = false) Long versione) {
        return esegui(versione, partita -> partita.offri(importo));
    }

    @PostMapping("/ritiratiDallAsta")
    public StatoPartitaView ritiratiDallAsta(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::ritiratiDallAsta);
    }

    @PostMapping("/pagaDebito")
    public StatoPartitaView pagaDebito(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::pagaDebito);
    }

    @PostMapping("/dichiaraBancarotta")
    public StatoPartitaView dichiaraBancarotta(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::dichiaraBancarotta);
    }

    @PostMapping("/fineTurno")
    public StatoPartitaView fineTurno(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::fineTurno);
    }

    @PostMapping("/pagaUscitaDiPrigione")
    public StatoPartitaView pagaUscitaDiPrigione(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::pagaUscitaDiPrigione);
    }

    @PostMapping("/usaCartaUscitaDiPrigione")
    public StatoPartitaView usaCartaUscitaDiPrigione(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::usaCartaUscitaDiPrigione);
    }

    @PostMapping("/costruisciCasetta/{posizione}")
    public StatoPartitaView costruisciCasetta(@PathVariable int posizione,
            @RequestParam(required = false) Long versione) {
        return esegui(versione, partita -> partita.costruisciCasetta(posizione));
    }

    @PostMapping("/vendiCasetta/{posizione}")
    public StatoPartitaView vendiCasetta(@PathVariable int posizione,
            @RequestParam(required = false) Long versione) {
        return esegui(versione, partita -> partita.vendiCasetta(posizione));
    }

    @PostMapping("/ipoteca/{posizione}")
    public StatoPartitaView ipoteca(@PathVariable int posizione, @RequestParam(required = false) Long versione) {
        return esegui(versione, partita -> partita.ipoteca(posizione));
    }

    @PostMapping("/riscattaIpoteca/{posizione}")
    public StatoPartitaView riscattaIpoteca(@PathVariable int posizione,
            @RequestParam(required = false) Long versione) {
        return esegui(versione, partita -> partita.riscattaIpoteca(posizione));
    }

    @PostMapping("/terminaPartita")
    public StatoPartitaView terminaPartita(@RequestParam(required = false) Long versione) {
        return esegui(versione, Partita::terminaPartita);
    }

    private StatoPartitaView esegui(Long versione, Consumer<Partita> mossa) {
        return monopolyService.esegui(versione, mossa, StatoPartitaView::da);
    }
}
