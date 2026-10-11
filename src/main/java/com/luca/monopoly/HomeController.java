package com.luca.monopoly;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalini;
import com.luca.monopoly.repository.JpaGiocatore;
import com.luca.monopoly.service.GiocatoreService;
import com.luca.monopoly.service.MonopolyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final GiocatoreService giocatoreService;

    private final MonopolyService monopolyService;

    public HomeController(GiocatoreService giocatoreService, MonopolyService monopolyService) {
        this.giocatoreService = giocatoreService;
        this.monopolyService = monopolyService;
    }

    @GetMapping("/")
    public String getHome(Model model) {

        List<JpaGiocatore> jpaGiocatori = giocatoreService.getGiocatori();

        List<GiocatoreView> giocatori = new ArrayList<>();
        List<Segnalini> segnaliniScelti = new ArrayList<>();
        for (JpaGiocatore jpaGiocatore : jpaGiocatori) {
            GiocatoreView giocatoreView = new GiocatoreView();
            giocatoreView.setNome(jpaGiocatore.getNome());
            giocatoreView.setPortafoglio(jpaGiocatore.getPortafoglio());
            giocatoreView.setSegnalino(jpaGiocatore.getSegnalino().getNome());
            giocatoreView.setValoreSegnalino(jpaGiocatore.getSegnalino().ordinal());

            giocatori.add(giocatoreView);
            segnaliniScelti.add(jpaGiocatore.getSegnalino());
        }
        model.addAttribute("giocatori", giocatori);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new HomeForm());
        }
        model.addAttribute("dadi", monopolyService.getMonopoly().getDadi().size());
        model.addAttribute("partitaInCorso", monopolyService.isPartitaPresente());

        List<SegnalinoView> segnalini = new ArrayList<>();
        Integer segnalinoProposto = null;
        for (Segnalini segnalino : Segnalini.values()) {
            boolean preso = segnaliniScelti.contains(segnalino);
            segnalini.add(new SegnalinoView(segnalino.ordinal(), segnalino.getNome(), preso));
            if (!preso && segnalinoProposto == null) {
                segnalinoProposto = segnalino.ordinal();
            }
        }

        model.addAttribute("segnalini", segnalini);
        model.addAttribute("segnalinoProposto", segnalinoProposto);
        model.addAttribute("portafoglioIniziale", Partita.PORTAFOGLIO_INIZIALE);

        return "home";

    }

    @PostMapping("/")
    public String aggiungiGiocatore(HomeForm form, RedirectAttributes redirectAttributes) {

        try {
            if (monopolyService.isPartitaPresente()) {
                throw new MossaNonValidaException(
                        "La partita è già iniziata: resettala per aggiungere nuovi giocatori.");
            }
            if (form.getSegnalino() < 0 || form.getSegnalino() >= Segnalini.values().length) {
                throw new MossaNonValidaException("Segnalino non valido.");
            }
            giocatoreService.aggiungiGiocatore(form.getNome(), Segnalini.values()[form.getSegnalino()]);
        } catch (MossaNonValidaException eccezione) {
            redirectAttributes.addFlashAttribute("messaggio", eccezione.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
        }

        return "redirect:/";
    }

    @PostMapping("/avviaPartita")
    public String avviaPartita(RedirectAttributes redirectAttributes) {
        try {
            monopolyService.avviaPartita();
        } catch (MossaNonValidaException eccezione) {
            redirectAttributes.addFlashAttribute("messaggio", eccezione.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/rimuoviGiocatore")
    public String rimuoviGiocatore(@RequestParam int segnalino, RedirectAttributes redirectAttributes) {
        try {
            if (segnalino < 0 || segnalino >= Segnalini.values().length) {
                throw new MossaNonValidaException("Segnalino non valido.");
            }
            monopolyService.rimuoviGiocatore(Segnalini.values()[segnalino]);
        } catch (MossaNonValidaException eccezione) {
            redirectAttributes.addFlashAttribute("messaggio", eccezione.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/nuovaPartita")
    public String nuovaPartita() {
        monopolyService.nuovaPartita();
        return "redirect:/";
    }

    @PostMapping("/resettaPartita")
    public String resettaPartita() {
        monopolyService.resettaPartita();
        return "redirect:/";
    }

}
