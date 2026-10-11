package com.luca.monopoly.web;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.Partita;
import com.luca.monopoly.domain.Segnalino;
import com.luca.monopoly.persistence.GiocatoreEntity;
import com.luca.monopoly.service.GiocatoreService;
import com.luca.monopoly.service.MonopolyService;
import com.luca.monopoly.web.dto.GiocatoreView;
import com.luca.monopoly.web.dto.SegnalinoView;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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

        List<GiocatoreEntity> giocatoriRegistrati = giocatoreService.getGiocatori();

        List<GiocatoreView> giocatori = new ArrayList<>();
        List<Segnalino> segnaliniScelti = new ArrayList<>();
        for (GiocatoreEntity registrato : giocatoriRegistrati) {
            GiocatoreView giocatoreView = new GiocatoreView();
            giocatoreView.setNome(registrato.getNome());
            giocatoreView.setSegnalino(registrato.getSegnalino().getNome());
            giocatoreView.setValoreSegnalino(registrato.getSegnalino().name());

            giocatori.add(giocatoreView);
            segnaliniScelti.add(registrato.getSegnalino());
        }
        model.addAttribute("giocatori", giocatori);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new HomeForm());
        }
        model.addAttribute("dadi", Partita.NUMERO_DADI);
        model.addAttribute("partitaInCorso", monopolyService.isPartitaPresente());

        List<SegnalinoView> segnalini = new ArrayList<>();
        String segnalinoProposto = null;
        for (Segnalino segnalino : Segnalino.values()) {
            boolean preso = segnaliniScelti.contains(segnalino);
            segnalini.add(new SegnalinoView(segnalino.name(), segnalino.getNome(), preso));
            if (!preso && segnalinoProposto == null) {
                segnalinoProposto = segnalino.name();
            }
        }

        model.addAttribute("segnalini", segnalini);
        model.addAttribute("segnalinoProposto", segnalinoProposto);
        model.addAttribute("portafoglioIniziale", Partita.PORTAFOGLIO_INIZIALE);

        return "home";

    }

    @PostMapping("/")
    public String aggiungiGiocatore(@Valid HomeForm form, BindingResult validazione,
            RedirectAttributes redirectAttributes) {

        try {
            if (validazione.hasErrors()) {
                throw new MossaNonValidaException(messaggioDiErrore(validazione));
            }
            monopolyService.aggiungiGiocatore(form.getNome(), form.getSegnalino());
        } catch (MossaNonValidaException eccezione) {
            redirectAttributes.addFlashAttribute("messaggio", eccezione.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
        }

        return "redirect:/";
    }

    /** Il primo errore del form; un valore non convertibile (segnalino inesistente) ha un messaggio proprio. */
    private static String messaggioDiErrore(BindingResult validazione) {
        FieldError errore = validazione.getFieldError();
        if (errore == null || errore.isBindingFailure()) {
            return "Segnalino non valido.";
        }
        return errore.getDefaultMessage();
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
    public String rimuoviGiocatore(@RequestParam Segnalino segnalino, RedirectAttributes redirectAttributes) {
        try {
            monopolyService.rimuoviGiocatore(segnalino);
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
