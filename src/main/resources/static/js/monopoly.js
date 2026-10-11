var COLORI_TERRENI = {
    fucsia: "#b8336a",
    azzurro: "#7cc0e0",
    arancione: "#e3892b",
    marrone: "#8a5a34",
    rosso: "#cf3b2e",
    giallo: "#e8c339",
    verde: "#3f8a4f",
    viola: "#5b3f8f"
};

/* Gruppi chiari: sulla testata del contratto serve l'inchiostro scuro */
var TERRENI_CHIARI = ["giallo", "azzurro"];

var COLORE_STAZIONE = "#2a2119";
var COLORE_SOCIETA = "#8f846f";

var COLORI_GIOCATORI = ["#2a5c9a", "#b23a30", "#3d7a46", "#c98a16", "#6b3d8f", "#2b8a8a", "#c96a26", "#5b4636"];

/* Indici dei puntini accesi nella griglia 3x3 di ogni faccia del dado */
var PUNTI_DADO = { 1: [4], 2: [0, 8], 3: [0, 4, 8], 4: [0, 2, 6, 8], 5: [0, 2, 4, 6, 8], 6: [0, 2, 3, 5, 6, 8] };

/* Testo degli angoli: [sopra, grande, sotto, dimensione, colore, rotazione] */
var ANGOLI = {
    VIA: ["ritirate 200€", "VIA", "← passando", "34px", "#a8322a", "-45deg"],
    PRIGIONE: ["solo transito", "PRIGIONE", "", "17px", "#2a2119", "45deg"],
    POSTEGGIO: ["posteggio", "GRATUITO", "", "17px", "#2a2119", "-45deg"],
    VAI_IN_PRIGIONE: ["andate", "IN PRIGIONE!", "", "15px", "#a8322a", "45deg"]
};

/* Fasi in cui si possono gestire le proprietà (come FASI_DI_GESTIONE in Partita.java) */
var FASI_DI_GESTIONE = ["LANCIO_DADI", "DECISIONE_ACQUISTO", "FINE_TURNO", "SALDO_DEBITO", "ASTA"];
/* Fasi in cui si può solo raccogliere denaro: niente costruzioni né riscatti */
var FASI_SENZA_SPESE = ["SALDO_DEBITO", "ASTA"];

var INTERVALLO_AGGIORNAMENTO_MS = 1500;

var stato = null;
var casellaSelezionata = null;
var carteViste = null;
var vincitoreNascosto = false;
var dadiInLancio = false;
var richiestaInCorso = false;

document.addEventListener("DOMContentLoaded", function () {
    if (!document.getElementById("partita")) {
        return;
    }
    document.getElementById("termina-button").addEventListener("click", function () {
        if (confirm("Terminare la partita? Vincerà il giocatore con il patrimonio più alto.")) {
            invia("/terminaPartita");
        }
    });
    document.addEventListener("keydown", function (evento) {
        if (evento.key === "Escape" && casellaSelezionata !== null) {
            chiudiContratto();
        }
    });
    caricaStato();
    // chi guarda da un altro dispositivo vede le mosse degli altri giocatori
    setInterval(aggiornaSeCambiato, INTERVALLO_AGGIORNAMENTO_MS);
});

function caricaStato() {
    return fetch("/stato")
        .then(leggiRisposta)
        .then(aggiornaInterfaccia)
        .catch(gestisciErrore);
}

function aggiornaSeCambiato() {
    if (richiestaInCorso || !stato) {
        return;
    }
    fetch("/stato?versione=" + stato.versione)
        .then(function (risposta) {
            return risposta.status === 204 ? null : leggiRisposta(risposta);
        })
        .then(function (dati) {
            if (dati) {
                aggiornaInterfaccia(dati);
            }
        })
        .catch(gestisciErrore);
}

/* Invia una mossa con la versione dello stato su cui il giocatore ha deciso. */
function richiesta(url, parametri) {
    var query = new URLSearchParams(parametri || {});
    if (stato) {
        query.set("versione", stato.versione);
    }
    return fetch(url + "?" + query.toString(), { method: "POST" }).then(leggiRisposta);
}

function invia(url, parametri) {
    if (richiestaInCorso) {
        return;
    }
    impostaAttesa(true);
    richiesta(url, parametri)
        .then(aggiornaInterfaccia)
        .catch(gestisciErrore)
        .finally(function () {
            impostaAttesa(false);
        });
}

function impostaAttesa(inAttesa) {
    richiestaInCorso = inAttesa;
    document.body.classList.toggle("in-attesa", inAttesa);
}

function leggiRisposta(risposta) {
    return risposta.json()
        .catch(function () {
            return {};
        })
        .then(function (dati) {
            if (!risposta.ok) {
                var errore = new Error(dati.errore || "Richiesta non riuscita. Errore " + risposta.status);
                errore.status = risposta.status;
                throw errore;
            }
            return dati;
        });
}

function gestisciErrore(errore) {
    if (errore.status === 404) {
        // la partita è stata resettata: si torna alla registrazione dei giocatori
        window.location.reload();
        return;
    }
    if (errore.status === 409) {
        // un altro giocatore ha mosso nel frattempo: mostra lo stato aggiornato
        fetch("/stato")
            .then(leggiRisposta)
            .then(function (dati) {
                aggiornaInterfaccia(dati);
                mostraErrore(errore);
            });
        return;
    }
    mostraErrore(errore);
}

function mostraErrore(errore) {
    // Con il contratto aperto l'errore va mostrato lì, altrimenti resterebbe coperto
    var box = document.getElementById(casellaSelezionata !== null ? "errore-contratto" : "errore")
        || document.getElementById("errore");
    box.textContent = errore.message;
    box.hidden = false;
}

function escape(testo) {
    var div = document.createElement("div");
    div.textContent = testo;
    return div.innerHTML;
}

function immagineSegnalino(segnalino) {
    return "/images/segnalini/" + segnalino.toLowerCase() + ".png";
}

function aggiornaInterfaccia(data) {
    stato = data;
    document.getElementById("errore").hidden = true;
    if (carteViste === null) {
        // Al primo caricamento non si ripropone una carta già letta
        carteViste = stato.cartePescate;
    }

    disegnaTabellone();
    disegnaCentro();
    disegnaGiocatori();
    disegnaRegistro();
    disegnaContratto();
    disegnaCarta();
    disegnaVincitore();
}

/* ------------------------------------------------------------------ */
/* Tabellone                                                           */
/* ------------------------------------------------------------------ */

/* Posizione (riga, colonna) e lato di ogni casella nella griglia 11x11 */
function cellaDellaGriglia(posizione) {
    if (posizione <= 10) {
        return { riga: 11, colonna: 11 - posizione, lato: "b" };
    }
    if (posizione <= 20) {
        return { riga: 11 - (posizione - 10), colonna: 1, lato: "l" };
    }
    if (posizione <= 30) {
        return { riga: 1, colonna: 1 + (posizione - 20), lato: "t" };
    }
    return { riga: 1 + (posizione - 30), colonna: 11, lato: "r" };
}

function isAcquistabile(casella) {
    return casella.tipo === "TERRENO" || casella.tipo === "STAZIONE" || casella.tipo === "SOCIETA";
}

function coloreCasella(casella) {
    if (casella.tipo === "TERRENO") {
        return COLORI_TERRENI[casella.colore];
    }
    return casella.tipo === "STAZIONE" ? COLORE_STAZIONE : COLORE_SOCIETA;
}

function disegnaTabellone() {
    var tabellone = document.getElementById("tabellone");

    stato.caselle.forEach(function (casella) {
        var cella = cellaDellaGriglia(casella.posizione);
        var angolare = casella.posizione % 10 === 0;
        var elemento = document.getElementById("casella_" + casella.posizione);
        if (!elemento) {
            elemento = document.createElement("div");
            elemento.id = "casella_" + casella.posizione;
            elemento.className = "casella lato-" + cella.lato + (angolare ? " angolare" : "");
            elemento.style.gridRow = cella.riga;
            elemento.style.gridColumn = cella.colonna;
            elemento.setAttribute("role", "button");
            elemento.tabIndex = 0;
            elemento.addEventListener("click", function () {
                selezionaCasella(casella.posizione);
            });
            elemento.addEventListener("keydown", function (evento) {
                if (evento.key === "Enter" || evento.key === " ") {
                    evento.preventDefault();
                    selezionaCasella(casella.posizione);
                }
            });
            tabellone.appendChild(elemento);
        }

        elemento.classList.toggle("ipotecata", casella.ipotecata);
        elemento.classList.toggle("selezionata", casella.posizione === casellaSelezionata);
        elemento.title = casella.testo;
        elemento.setAttribute("aria-label", casella.testo);

        var html = "";
        if (angolare) {
            html += contenutoAngolo(casella.tipo);
        } else {
            if (isAcquistabile(casella)) {
                html += '<div class="banda" style="background:' + coloreCasella(casella) + '">'
                    + (casella.casette > 0 ? edifici(casella.casette) : etichettaBanda(casella.tipo)) + "</div>";
            }
            html += '<div class="casella-corpo">'
                + '<div class="casella-nome">' + escape(casella.testo) + "</div>"
                + simboloCasella(casella.tipo)
                + '<div class="casella-prezzo">' + prezzoCasella(casella) + "</div>"
                + "</div>";
        }
        if (casella.ipotecata) {
            html += '<div class="timbro-ipoteca">IPOTECATA</div>';
        }
        if (casella.proprietario !== null) {
            html += '<div class="proprietario" title="' + escape(stato.giocatori[casella.proprietario].nome) + '"'
                + ' style="background:' + COLORI_GIOCATORI[casella.proprietario] + '"></div>';
        }

        html += '<div class="segnalini">';
        stato.giocatori.forEach(function (giocatore, indice) {
            if (!giocatore.inBancarotta && giocatore.posizione === casella.posizione) {
                var corrente = indice === stato.giocatoreCorrente && stato.fase !== "TERMINATA";
                html += '<img src="' + immagineSegnalino(giocatore.segnalino) + '"'
                    + (corrente ? ' class="corrente"' : "")
                    + ' title="' + escape(giocatore.nome) + '" alt="' + giocatore.segnalino + '">';
            }
        });
        html += "</div>";

        elemento.innerHTML = html;
    });
}

function contenutoAngolo(tipo) {
    var angolo = ANGOLI[tipo];
    return '<div class="angolo"><div class="angolo-testo" style="transform:rotate(' + angolo[5] + ')">'
        + '<div class="angolo-piccolo">' + angolo[0] + "</div>"
        + '<div class="angolo-grande" style="font-size:' + angolo[3] + ";color:" + angolo[4] + '">' + angolo[1] + "</div>"
        + '<div class="angolo-piccolo">' + angolo[2] + "</div>"
        + "</div></div>";
}

function edifici(casette) {
    if (casette === 5) {
        return '<div class="albergo"></div>';
    }
    var html = "";
    for (var i = 0; i < casette; i++) {
        html += '<div class="casa"></div>';
    }
    return html;
}

function etichettaBanda(tipo) {
    var testo = tipo === "STAZIONE" ? "FERROVIE" : tipo === "SOCIETA" ? "SERVIZI" : "";
    return testo ? '<span class="banda-etichetta">' + testo + "</span>" : "";
}

function simboloCasella(tipo) {
    var simboli = { IMPREVISTI: ["!", "#e07a2e"], PROBABILITA: ["?", "#3c79a8"], TASSA: ["€", "#6b5d4c"] };
    var simbolo = simboli[tipo];
    return simbolo ? '<div class="casella-simbolo" style="color:' + simbolo[1] + '">' + simbolo[0] + "</div>" : "";
}

function prezzoCasella(casella) {
    if (casella.tipo === "TASSA") {
        return "paga " + casella.costo + "€";
    }
    return casella.costo > 0 ? casella.costo + "€" : "";
}

/* ------------------------------------------------------------------ */
/* Centro: dadi, turno e azioni                                        */
/* ------------------------------------------------------------------ */

function disegnaDadi(faccia1, faccia2, inLancio) {
    var rotazioni = inLancio ? ["-11deg", "9deg"] : ["-3deg", "4deg"];
    document.getElementById("dadi").innerHTML = [faccia1, faccia2].map(function (faccia, indice) {
        var punti = "";
        for (var i = 0; i < 9; i++) {
            punti += PUNTI_DADO[faccia].indexOf(i) >= 0 ? '<span class="punto"></span>' : "<span></span>";
        }
        return '<div class="dado" style="transform:rotate(' + rotazioni[indice] + ')">' + punti + "</div>";
    }).join("");
}

function lanciaDadi() {
    if (dadiInLancio || richiestaInCorso) {
        return;
    }
    dadiInLancio = true;
    impostaAttesa(true);
    disegnaCentro();

    // I dadi rotolano per qualche fotogramma mentre il server decide il risultato
    var fotogrammi = 0;
    var animazione = new Promise(function (fine) {
        var intervallo = setInterval(function () {
            disegnaDadi(facciaCasuale(), facciaCasuale(), true);
            if (++fotogrammi >= 7) {
                clearInterval(intervallo);
                fine();
            }
        }, 60);
    });

    Promise.all([richiesta("/lanciaDadi"), animazione])
        .then(function (risultati) {
            dadiInLancio = false;
            aggiornaInterfaccia(risultati[0]);
        })
        .catch(function (errore) {
            dadiInLancio = false;
            disegnaCentro();
            gestisciErrore(errore);
        })
        .finally(function () {
            impostaAttesa(false);
        });
}

function facciaCasuale() {
    return 1 + Math.floor(Math.random() * 6);
}

function disegnaCentro() {
    var terminata = stato.fase === "TERMINATA";
    var giocatore = stato.giocatori[stato.giocatoreCorrente];
    var vincitore = terminata && stato.vincitore !== null ? stato.giocatori[stato.vincitore] : null;
    var casellaCorrente = stato.caselle[giocatore.posizione];

    if (!dadiInLancio) {
        disegnaDadi(stato.dado1 || 6, stato.dado2 || 6, false);
    }

    // durante l'asta il protagonista è chi deve offrire, non il giocatore di turno
    var indiceProtagonista = vincitore ? stato.vincitore
        : stato.fase === "ASTA" ? stato.asta.offerenteDiTurno : stato.giocatoreCorrente;
    var protagonista = stato.giocatori[indiceProtagonista];
    var nomeProtagonista = '<span style="color:' + COLORI_GIOCATORI[indiceProtagonista] + '">'
        + escape(protagonista.nome) + "</span>";
    document.getElementById("turno-segnalino").src = immagineSegnalino(protagonista.segnalino);
    document.getElementById("turno").innerHTML = vincitore ? nomeProtagonista + " ha vinto!"
        : stato.fase === "ASTA" ? "Offre " + nomeProtagonista
        : "Turno di " + nomeProtagonista;

    var testoStato = "";
    if (!terminata) {
        if (stato.fase === "ASTA") {
            testoStato = statoAsta();
        } else if (stato.fase === "SALDO_DEBITO") {
            testoStato = statoDebito(giocatore);
        } else if (giocatore.inPrigione) {
            testoStato = "In prigione · tentativo " + (giocatore.turniInPrigione + 1) + " di 3 · multa " + stato.multaPrigione + "€";
        } else if (stato.fase === "DECISIONE_ACQUISTO") {
            testoStato = casellaCorrente.testo + " è libera. La compri?";
        } else if (stato.fase === "LANCIO_DADI" && /lancia di nuovo/.test(ultimoEvento())) {
            testoStato = "Doppio! Lancia di nuovo.";
        }
    }
    var turnoStato = document.getElementById("turno-stato");
    turnoStato.textContent = testoStato;
    turnoStato.hidden = !testoStato;

    var azioni = document.getElementById("azioni");
    azioni.innerHTML = "";

    switch (stato.fase) {
        case "LANCIO_DADI":
            if (giocatore.inPrigione) {
                aggiungiPulsante(azioni, "Paga " + stato.multaPrigione + "€ ed esci", "btn-oro",
                    function () { invia("/pagaUscitaDiPrigione"); }, giocatore.portafoglio < stato.multaPrigione || dadiInLancio);
                if (giocatore.carteUscitaDiPrigione > 0) {
                    aggiungiPulsante(azioni, "Usa carta «Uscite gratis»", "btn-oro",
                        function () { invia("/usaCartaUscitaDiPrigione"); }, dadiInLancio);
                }
                aggiungiPulsante(azioni, "Tenta il doppio (" + (giocatore.turniInPrigione + 1) + "/3)", "btn-verde",
                    lanciaDadi, dadiInLancio);
            } else {
                aggiungiPulsante(azioni, "Lancia i dadi", "btn-verde", lanciaDadi, dadiInLancio);
            }
            break;
        case "DECISIONE_ACQUISTO":
            aggiungiPulsante(azioni, "Compra per " + casellaCorrente.costo + "€", "btn-rosso",
                function () { invia("/compraProprieta"); }, giocatore.portafoglio < casellaCorrente.costo);
            aggiungiPulsante(azioni, "Non comprare", "",
                function () { invia("/rifiutaAcquisto"); });
            break;
        case "ASTA":
            disegnaAzioniAsta(azioni);
            break;
        case "SALDO_DEBITO":
            disegnaAzioniDebito(azioni, giocatore);
            break;
        case "FINE_TURNO":
            aggiungiPulsante(azioni, "Fine turno →", "btn-rosso", function () { invia("/fineTurno"); });
            break;
        case "TERMINATA":
            azioni.appendChild(formNuovaPartita("btn-verde"));
            break;
    }

    document.getElementById("termina-button").disabled = terminata;
    document.getElementById("ultimo-evento").textContent = ultimoEvento();
}

/* ------------------------------------------------------------------ */
/* Asta e saldo del debito                                             */
/* ------------------------------------------------------------------ */

function statoAsta() {
    var asta = stato.asta;
    var casella = stato.caselle[asta.posizione];
    var offerente = stato.giocatori[asta.offerenteDiTurno];
    var migliore = asta.migliorOfferente === null ? "nessuna offerta"
        : "offerta più alta " + asta.offertaMassima + "€ di " + stato.giocatori[asta.migliorOfferente].nome;
    return casella.testo + " all'asta (valore " + casella.costo + "€) · " + migliore
        + " · contanti di " + offerente.nome + ": " + offerente.portafoglio + "€";
}

function disegnaAzioniAsta(azioni) {
    var asta = stato.asta;
    var offerente = stato.giocatori[asta.offerenteDiTurno];
    var offertaMinima = asta.offertaMassima + 1;
    var puoOffrire = offerente.portafoglio >= offertaMinima;

    var campo = document.createElement("label");
    campo.className = "campo-offerta";
    campo.innerHTML = '<input type="number" id="importo-offerta" aria-label="Importo dell\'offerta"><span>€</span>';
    var importo = campo.querySelector("input");
    importo.min = offertaMinima;
    importo.max = offerente.portafoglio;
    importo.value = Math.min(Math.max(offertaMinima, asta.offertaMassima + 10), offerente.portafoglio);
    importo.disabled = !puoOffrire;
    importo.addEventListener("keydown", function (evento) {
        if (evento.key === "Enter" && puoOffrire) {
            invia("/offerta", { importo: importo.value });
        }
    });
    azioni.appendChild(campo);

    aggiungiPulsante(azioni, "Offri", "btn-rosso", function () {
        invia("/offerta", { importo: importo.value });
    }, !puoOffrire);
    aggiungiPulsante(azioni, "Ritirati", "", function () {
        invia("/ritiratiDallAsta");
    });
}

function statoDebito(giocatore) {
    var debito = stato.debito;
    var creditore = debito.creditore === null ? "alla banca" : "a " + stato.giocatori[debito.creditore].nome;
    return "Deve pagare " + debito.importo + "€ " + creditore + " ma ha " + giocatore.portafoglio
        + "€ in contanti. Apri le tue proprietà per vendere edifici o ipotecarle.";
}

function disegnaAzioniDebito(azioni, giocatore) {
    var debito = stato.debito;
    aggiungiPulsante(azioni, "Paga " + debito.importo + "€", "btn-rosso", function () {
        invia("/pagaDebito");
    }, giocatore.portafoglio < debito.importo);
    aggiungiPulsante(azioni, "Dichiara bancarotta", "", function () {
        if (confirm("Dichiarare bancarotta? Uscirai dal gioco.")) {
            invia("/dichiaraBancarotta");
        }
    });
}

function ultimoEvento() {
    return stato.eventi.length > 0 ? stato.eventi[stato.eventi.length - 1] : "";
}

function aggiungiPulsante(contenitore, testo, classe, azione, disabilitato) {
    var pulsante = document.createElement("button");
    pulsante.type = "button";
    pulsante.className = "btn " + classe;
    pulsante.textContent = testo;
    pulsante.disabled = !!disabilitato;
    pulsante.addEventListener("click", azione);
    contenitore.appendChild(pulsante);
    return pulsante;
}

/* "Nuova partita" tiene al tavolo gli stessi giocatori e torna al setup */
function formNuovaPartita(classe) {
    var form = document.createElement("form");
    form.method = "post";
    form.action = "/nuovaPartita";
    form.innerHTML = '<button type="submit" class="btn ' + classe + '">Nuova partita</button>';
    return form;
}

/* ------------------------------------------------------------------ */
/* Barra laterale: giocatori e registro                                */
/* ------------------------------------------------------------------ */

function disegnaGiocatori() {
    var contenitore = document.getElementById("giocatori");
    contenitore.innerHTML = "";

    stato.giocatori.forEach(function (giocatore, indice) {
        var corrente = indice === stato.giocatoreCorrente && stato.fase !== "TERMINATA";
        var card = document.createElement("div");
        card.className = "giocatore" + (corrente ? " corrente" : "") + (giocatore.inBancarotta ? " bancarotta" : "");

        var stati = [];
        if (giocatore.inBancarotta) stati.push("In bancarotta");
        if (giocatore.inPrigione) stati.push("In prigione");
        if (giocatore.carteUscitaDiPrigione > 0) stati.push(giocatore.carteUscitaDiPrigione + " carta «Uscite gratis»");

        var html = '<img src="' + immagineSegnalino(giocatore.segnalino) + '" alt="">'
            + '<div class="giocatore-info">'
            + '<div class="giocatore-riga">'
            + '<div class="giocatore-colore" style="background:' + COLORI_GIOCATORI[indice] + '"></div>'
            + '<div class="giocatore-nome">' + escape(giocatore.nome) + "</div>"
            + '<div class="giocatore-segnalino">' + giocatore.segnalino + "</div>"
            + (corrente ? '<div class="badge-turno">TURNO</div>' : "")
            + "</div>"
            + '<div class="giocatore-soldi">'
            + '<div class="giocatore-portafoglio">' + giocatore.portafoglio + "€</div>"
            + '<div class="giocatore-patrimonio">patrimonio ' + giocatore.patrimonio + "€</div>"
            + "</div>";
        if (stati.length > 0) {
            html += '<div class="giocatore-stato">' + stati.join(" · ") + "</div>";
        }
        html += '<div class="giocatore-proprieta"></div></div>';
        card.innerHTML = html;

        var elencoProprieta = card.querySelector(".giocatore-proprieta");
        stato.caselle.forEach(function (casella) {
            if (casella.proprietario === indice) {
                var etichetta = document.createElement("span");
                etichetta.title = casella.testo + (casella.ipotecata ? " (ipotecata)" : "")
                    + (casella.casette > 0 ? " · " + (casella.casette === 5 ? "albergo" : casella.casette + " case") : "");
                etichetta.style.background = coloreCasella(casella);
                etichetta.classList.toggle("ipotecata", casella.ipotecata);
                etichetta.addEventListener("click", function () {
                    selezionaCasella(casella.posizione);
                });
                elencoProprieta.appendChild(etichetta);
            }
        });
        elencoProprieta.hidden = elencoProprieta.children.length === 0;

        contenitore.appendChild(card);
    });
}

function disegnaRegistro() {
    var eventi = document.getElementById("eventi");
    eventi.innerHTML = "";
    stato.eventi.slice().reverse().forEach(function (evento) {
        var riga = document.createElement("div");
        riga.textContent = evento;
        eventi.appendChild(riga);
    });
}

/* ------------------------------------------------------------------ */
/* Contratto di proprietà                                              */
/* ------------------------------------------------------------------ */

function selezionaCasella(posizione) {
    casellaSelezionata = posizione;
    disegnaTabellone();
    disegnaContratto();
}

function chiudiContratto() {
    casellaSelezionata = null;
    disegnaTabellone();
    disegnaContratto();
}

function possiedeTuttoIlGruppo(casella) {
    return stato.caselle.every(function (altra) {
        return altra.colore !== casella.colore || altra.proprietario === casella.proprietario;
    });
}

function contaStazioni(proprietario) {
    return stato.caselle.filter(function (altra) {
        return altra.tipo === "STAZIONE" && altra.proprietario === proprietario;
    }).length;
}

function rigaContratto(etichetta, valore, attuale, separata) {
    return '<div class="contratto-riga' + (attuale ? " attuale" : "") + (separata ? " separata" : "") + '">'
        + "<span>" + etichetta + "</span><span>" + valore + "</span></div>";
}

function righeContratto(casella) {
    var posseduta = casella.proprietario !== null;
    var rendite = casella.rendite;

    if (casella.tipo === "TERRENO") {
        var casette = posseduta ? casella.casette : -1;
        var gruppo = posseduta && possiedeTuttoIlGruppo(casella);
        return rigaContratto("Affitto", rendite[0] + "€", casette === 0 && !gruppo)
            + rigaContratto("Con il gruppo completo", rendite[1] + "€", casette === 0 && gruppo)
            + rigaContratto("Con 1 casa", rendite[2] + "€", casette === 1)
            + rigaContratto("Con 2 case", rendite[3] + "€", casette === 2)
            + rigaContratto("Con 3 case", rendite[4] + "€", casette === 3)
            + rigaContratto("Con 4 case", rendite[5] + "€", casette === 4)
            + rigaContratto("Con albergo", rendite[6] + "€", casette === 5)
            + rigaContratto("Prezzo", casella.costo + "€", false, true)
            + rigaContratto("Costo di una casa", casella.costoCasetta + "€")
            + rigaContratto("Valore ipotecario", casella.valoreIpotecario + "€");
    }
    if (casella.tipo === "STAZIONE") {
        var stazioni = posseduta ? contaStazioni(casella.proprietario) : 0;
        return rigaContratto("Affitto", rendite[0] + "€", stazioni === 1)
            + rigaContratto("Con 2 stazioni", rendite[1] + "€", stazioni === 2)
            + rigaContratto("Con 3 stazioni", rendite[2] + "€", stazioni === 3)
            + rigaContratto("Con 4 stazioni", rendite[3] + "€", stazioni === 4)
            + rigaContratto("Prezzo", casella.costo + "€", false, true)
            + rigaContratto("Valore ipotecario", casella.valoreIpotecario + "€");
    }
    if (casella.tipo === "SOCIETA") {
        return rigaContratto("Con una società", "4 × dadi")
            + rigaContratto("Con entrambe", "10 × dadi")
            + rigaContratto("Prezzo", casella.costo + "€", false, true)
            + rigaContratto("Valore ipotecario", casella.valoreIpotecario + "€");
    }
    var descrizioni = {
        VIA: ["Passando dal Via", "+200€"],
        PRIGIONE: ["Multa per uscire", stato.multaPrigione + "€"],
        POSTEGGIO: ["Sosta libera", "—"],
        VAI_IN_PRIGIONE: ["Andate dritti in prigione", "—"],
        TASSA: ["Da pagare alla banca", casella.costo + "€"],
        IMPREVISTI: ["Pescate una carta", "Imprevisti"],
        PROBABILITA: ["Pescate una carta", "Probabilità"]
    };
    var descrizione = descrizioni[casella.tipo];
    return rigaContratto(descrizione[0], descrizione[1]);
}

function disegnaContratto() {
    var velo = document.getElementById("contratto");
    if (casellaSelezionata === null) {
        velo.hidden = true;
        velo.innerHTML = "";
        return;
    }
    var casella = stato.caselle[casellaSelezionata];
    var acquistabile = isAcquistabile(casella);
    var proprietario = casella.proprietario === null ? null : stato.giocatori[casella.proprietario];

    var testata = "#ece1c5";
    var inchiostro = "#2a2119";
    if (acquistabile) {
        testata = coloreCasella(casella);
        inchiostro = casella.tipo === "TERRENO" && TERRENI_CHIARI.indexOf(casella.colore) >= 0 ? "#2a2119" : "#fbf6e9";
    }

    var nota = "";
    if (casella.ipotecata) {
        nota = "Proprietà ipotecata: non rende affitto.";
    } else if (casella.tipo === "TERRENO" && casella.casette > 0) {
        nota = "Edifici: " + (casella.casette === 5 ? "1 albergo" : casella.casette + (casella.casette === 1 ? " casa" : " case"));
    }

    var html = '<div class="contratto">'
        + '<button type="button" class="contratto-chiudi" title="Chiudi">×</button>'
        + '<div class="contratto-testata" style="background:' + testata + ";color:" + inchiostro + '">'
        + '<div class="contratto-tipo">' + (acquistabile ? "Contratto di proprietà" : "Casella speciale") + "</div>"
        + '<div class="contratto-nome">' + escape(casella.testo) + "</div>"
        + "</div>"
        + '<div class="contratto-righe">' + righeContratto(casella) + "</div>"
        + '<div class="contratto-proprietario">'
        + (proprietario ? '<img src="' + immagineSegnalino(proprietario.segnalino) + '" alt="">' : "")
        + "<span>Proprietario: <b>"
        + (!acquistabile ? "—" : proprietario ? escape(proprietario.nome) : "nessuno (della banca)")
        + "</b></span></div>"
        + (nota ? '<div class="contratto-nota">' + nota + "</div>" : "")
        + '<div id="errore-contratto" class="contratto-nota" hidden></div>'
        + '<div class="contratto-azioni" id="azioni-casella"></div>'
        + '<div class="contratto-motivi" id="motivi-contratto" hidden></div>'
        + "</div>";
    velo.innerHTML = html;
    velo.hidden = false;

    velo.onclick = function (evento) {
        if (evento.target === velo) {
            chiudiContratto();
        }
    };
    velo.querySelector(".contratto-chiudi").addEventListener("click", chiudiContratto);

    var azioni = document.getElementById("azioni-casella");
    var motivi = [];
    var gestibile = casella.proprietario === indiceDiChiGestisce() && FASI_DI_GESTIONE.indexOf(stato.fase) >= 0;
    if (gestibile) {
        if (casella.tipo === "TERRENO" && !casella.ipotecata) {
            var edificio = casella.casette === 4 ? "Costruisci albergo" : "Costruisci casa";
            aggiungiAzioneContratto(azioni, motivi, edificio + " (" + casella.costoCasetta + "€)", "btn-verde",
                "/costruisciCasetta/", casella, motivoNonCostruibile(casella));
            aggiungiAzioneContratto(azioni, motivi, "Vendi edificio", "",
                "/vendiCasetta/", casella, motivoNonVendibile(casella));
        }
        if (casella.ipotecata) {
            aggiungiAzioneContratto(azioni, motivi, "Riscatta ipoteca (" + casella.costoRiscatto + "€)", "btn-oro",
                "/riscattaIpoteca/", casella, motivoNonRiscattabile(casella));
        } else {
            aggiungiAzioneContratto(azioni, motivi, "Ipoteca (+" + casella.valoreIpotecario + "€)", "btn-oro",
                "/ipoteca/", casella, motivoNonIpotecabile(casella));
        }
    }
    azioni.hidden = azioni.children.length === 0;

    var elencoMotivi = document.getElementById("motivi-contratto");
    elencoMotivi.innerHTML = motivi.map(function (motivo) {
        return "<div>" + motivo + "</div>";
    }).join("");
    elencoMotivi.hidden = motivi.length === 0;
}

/* Un pulsante del contratto: se la mossa non è consentita resta disattivato e il motivo va sotto */
function aggiungiAzioneContratto(contenitore, motivi, testo, classe, url, casella, motivo) {
    var pulsante = aggiungiPulsante(contenitore, testo, classe + " btn-piccolo",
        function () { invia(url + casella.posizione); }, !!motivo);
    if (motivo) {
        pulsante.title = motivo;
        motivi.push(testo.replace(/ \(.*\)$/, "") + ": " + motivo.charAt(0).toLowerCase() + motivo.slice(1));
    }
}

/* ------------------------------------------------------------------ */
/* Regole di costruzione e ipoteca                                     */
/* Rispecchiano i controlli di Partita.java, che resta l'arbitro:      */
/* qui servono solo a non offrire mosse che il server rifiuterebbe.    */
/* ------------------------------------------------------------------ */

function terreniDelGruppo(casella) {
    return stato.caselle.filter(function (altra) {
        return altra.tipo === "TERRENO" && altra.colore === casella.colore;
    });
}

/* Durante l'asta gestisce le proprietà chi deve offrire (come getGiocatoreCheGestisce in Partita.java) */
function indiceDiChiGestisce() {
    return stato.fase === "ASTA" ? stato.asta.offerenteDiTurno : stato.giocatoreCorrente;
}

function portafoglioCorrente() {
    return stato.giocatori[indiceDiChiGestisce()].portafoglio;
}

function motivoSenzaSpese() {
    if (stato.fase === "SALDO_DEBITO") {
        return "Mentre hai un debito da saldare puoi solo vendere edifici o ipotecare.";
    }
    return stato.fase === "ASTA" ? "Durante l'asta puoi solo vendere edifici o ipotecare." : "";
}

function motivoNonCostruibile(casella) {
    if (FASI_SENZA_SPESE.indexOf(stato.fase) >= 0) {
        return motivoSenzaSpese();
    }
    var gruppo = terreniDelGruppo(casella);
    if (!possiedeTuttoIlGruppo(casella)) {
        return "Serve il gruppo completo (" + gruppo.length + " terreni " + casella.colore + ").";
    }
    if (gruppo.some(function (terreno) { return terreno.ipotecata; })) {
        return "Un terreno del gruppo è ipotecato.";
    }
    if (casella.casette === 5) {
        return "C'è già un albergo.";
    }
    var minimo = Math.min.apply(null, gruppo.map(function (terreno) { return terreno.casette; }));
    if (casella.casette > minimo) {
        return "Costruisci in modo uniforme: prima una casa sugli altri terreni del gruppo.";
    }
    if (portafoglioCorrente() < casella.costoCasetta) {
        return "Non hai abbastanza denaro.";
    }
    if (casella.casette === 4 && stato.alberghiDisponibili === 0) {
        return "La banca ha finito gli alberghi.";
    }
    if (casella.casette < 4 && stato.casetteDisponibili === 0) {
        return "La banca ha finito le case.";
    }
    return "";
}

function motivoNonVendibile(casella) {
    if (casella.casette === 0) {
        return "Non ci sono edifici da vendere.";
    }
    var massimo = Math.max.apply(null, terreniDelGruppo(casella).map(function (terreno) { return terreno.casette; }));
    if (casella.casette < massimo) {
        return "Vendi in modo uniforme: prima sui terreni del gruppo che hanno più edifici.";
    }
    if (casella.casette === 5 && stato.casetteDisponibili < 4) {
        return "La banca non ha 4 case per sostituire l'albergo.";
    }
    return "";
}

function motivoNonIpotecabile(casella) {
    if (casella.tipo === "TERRENO" && terreniDelGruppo(casella).some(function (terreno) { return terreno.casette > 0; })) {
        return "Prima vendi gli edifici del gruppo.";
    }
    return "";
}

function motivoNonRiscattabile(casella) {
    if (FASI_SENZA_SPESE.indexOf(stato.fase) >= 0) {
        return motivoSenzaSpese();
    }
    return portafoglioCorrente() < casella.costoRiscatto ? "Non hai abbastanza denaro." : "";
}

/* ------------------------------------------------------------------ */
/* Carta Imprevisti / Probabilità                                      */
/* ------------------------------------------------------------------ */

function disegnaCarta() {
    var velo = document.getElementById("carta");
    if (!stato.ultimaCarta || stato.cartePescate <= carteViste) {
        velo.hidden = true;
        return;
    }
    var imprevisti = stato.ultimaCarta.mazzo === "Imprevisti";
    velo.innerHTML = '<div class="carta ' + (imprevisti ? "imprevisti" : "probabilita") + '">'
        + '<div class="carta-interno">'
        + '<div class="carta-mazzo"><div class="carta-simbolo">' + (imprevisti ? "!" : "?") + "</div>"
        + "<div>" + stato.ultimaCarta.mazzo + "</div></div>"
        + '<div class="carta-testo">' + escape(stato.ultimaCarta.testo) + "</div>"
        + '<button type="button" class="btn btn-scuro">Continua</button>'
        + "</div></div>";
    velo.hidden = false;
    velo.querySelector("button").addEventListener("click", function () {
        carteViste = stato.cartePescate;
        velo.hidden = true;
    });
}

/* ------------------------------------------------------------------ */
/* Fine partita                                                        */
/* ------------------------------------------------------------------ */

function disegnaVincitore() {
    var velo = document.getElementById("vincitore");
    if (stato.fase !== "TERMINATA" || stato.vincitore === null || vincitoreNascosto) {
        velo.hidden = true;
        return;
    }
    var vincitore = stato.giocatori[stato.vincitore];

    var classifica = stato.giocatori
        .map(function (giocatore, indice) { return { giocatore: giocatore, indice: indice }; })
        .sort(function (a, b) {
            return (b.indice === stato.vincitore) - (a.indice === stato.vincitore)
                || b.giocatore.patrimonio - a.giocatore.patrimonio;
        })
        .map(function (voce, posizione) {
            var giocatore = voce.giocatore;
            return '<div class="classifica-riga' + (giocatore.inBancarotta ? " bancarotta" : "") + '">'
                + '<div class="classifica-posizione">' + (posizione + 1) + "</div>"
                + '<img src="' + immagineSegnalino(giocatore.segnalino) + '" alt="">'
                + '<div class="classifica-nome">' + escape(giocatore.nome) + "</div>"
                + '<div class="classifica-valore">' + (giocatore.inBancarotta ? "bancarotta" : giocatore.patrimonio + "€") + "</div>"
                + "</div>";
        }).join("");

    velo.innerHTML = '<div class="vincitore">'
        + '<div class="etichetta">Partita terminata</div>'
        + '<div class="vincitore-medaglia"><img src="' + immagineSegnalino(vincitore.segnalino) + '" alt=""></div>'
        + '<div class="vincitore-titolo">' + escape(vincitore.nome) + " ha vinto la partita!</div>"
        + '<div class="vincitore-patrimonio">Patrimonio finale <b>' + vincitore.patrimonio + "€</b></div>"
        + '<div class="classifica">' + classifica + "</div>"
        + '<div class="vincitore-azioni"></div>'
        + "</div>";

    var azioni = velo.querySelector(".vincitore-azioni");
    azioni.appendChild(formNuovaPartita("btn-rosso"));
    aggiungiPulsante(azioni, "Guarda il tabellone", "btn-vuoto", function () {
        vincitoreNascosto = true;
        velo.hidden = true;
    });
    velo.hidden = false;
}
