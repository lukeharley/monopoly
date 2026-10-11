package com.luca.monopoly;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.service.MonopolyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Consumer;

@RestController
public class AjaxHomeController {

    @Autowired
    private MonopolyService monopolyService;

    @GetMapping("/stato")
    public ResponseEntity<StatoPartitaView> stato() {
        Partita partita = monopolyService.getPartita();
        if (partita == null) {
            throw new MossaNonValidaException("Nessuna partita in corso.");
        }
        return ResponseEntity.ok(StatoPartitaView.da(partita));
    }

    @PostMapping("/lanciaDadi")
    public ResponseEntity<StatoPartitaView> lanciaDadi() {
        return esegui(Partita::lanciaDadi);
    }

    @PostMapping("/compraProprieta")
    public ResponseEntity<StatoPartitaView> compraProprieta() {
        return esegui(Partita::compraProprieta);
    }

    @PostMapping("/rifiutaAcquisto")
    public ResponseEntity<StatoPartitaView> rifiutaAcquisto() {
        return esegui(Partita::rifiutaAcquisto);
    }

    @PostMapping("/fineTurno")
    public ResponseEntity<StatoPartitaView> fineTurno() {
        return esegui(Partita::fineTurno);
    }

    @PostMapping("/pagaUscitaDiPrigione")
    public ResponseEntity<StatoPartitaView> pagaUscitaDiPrigione() {
        return esegui(Partita::pagaUscitaDiPrigione);
    }

    @PostMapping("/usaCartaUscitaDiPrigione")
    public ResponseEntity<StatoPartitaView> usaCartaUscitaDiPrigione() {
        return esegui(Partita::usaCartaUscitaDiPrigione);
    }

    @PostMapping("/costruisciCasetta/{posizione}")
    public ResponseEntity<StatoPartitaView> costruisciCasetta(@PathVariable int posizione) {
        return esegui(partita -> partita.costruisciCasetta(posizione));
    }

    @PostMapping("/vendiCasetta/{posizione}")
    public ResponseEntity<StatoPartitaView> vendiCasetta(@PathVariable int posizione) {
        return esegui(partita -> partita.vendiCasetta(posizione));
    }

    @PostMapping("/ipoteca/{posizione}")
    public ResponseEntity<StatoPartitaView> ipoteca(@PathVariable int posizione) {
        return esegui(partita -> partita.ipoteca(posizione));
    }

    @PostMapping("/riscattaIpoteca/{posizione}")
    public ResponseEntity<StatoPartitaView> riscattaIpoteca(@PathVariable int posizione) {
        return esegui(partita -> partita.riscattaIpoteca(posizione));
    }

    @PostMapping("/terminaPartita")
    public ResponseEntity<StatoPartitaView> terminaPartita() {
        return esegui(Partita::terminaPartita);
    }

    @ExceptionHandler(MossaNonValidaException.class)
    public ResponseEntity<Map<String, String>> mossaNonValida(MossaNonValidaException eccezione) {
        return ResponseEntity.badRequest().body(Map.of("errore", eccezione.getMessage()));
    }

    private ResponseEntity<StatoPartitaView> esegui(Consumer<Partita> mossa) {
        return ResponseEntity.ok(StatoPartitaView.da(monopolyService.esegui(mossa)));
    }
}
