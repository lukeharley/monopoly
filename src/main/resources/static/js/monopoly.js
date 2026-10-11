var COLORI_TERRENI = {
    fucsia: "#c2185b",
    azzurro: "#4fc3f7",
    arancione: "#fb8c00",
    marrone: "#8d5524",
    rosso: "#e53935",
    giallo: "#fdd835",
    verde: "#43a047",
    viola: "#5e35b1"
};

var COLORI_GIOCATORI = ["#1565c0", "#c62828", "#2e7d32", "#f9a825", "#6a1b9a", "#00838f", "#ef6c00", "#4e342e"];

var FACCE_DADO = ["", "⚀", "⚁", "⚂", "⚃", "⚄", "⚅"];

var stato = null;
var casellaSelezionata = null;

document.addEventListener("DOMContentLoaded", function () {
    if (!document.getElementById("partita")) {
        return;
    }
    document.getElementById("termina-button").addEventListener("click", function () {
        if (confirm("Terminare la partita? Vincerà il giocatore con il patrimonio più alto.")) {
            invia("/terminaPartita");
        }
    });
    fetch("/stato")
        .then(leggiRisposta)
        .then(aggiornaInterfaccia)
        .catch(mostraErrore);
});

function invia(url) {
    fetch(url, { method: "POST" })
        .then(leggiRisposta)
        .then(aggiornaInterfaccia)
        .catch(mostraErrore);
}

function leggiRisposta(risposta) {
    return risposta.json().then(function (dati) {
        if (!risposta.ok) {
            throw new Error(dati.errore || "Richiesta non riuscita. Errore " + risposta.status);
        }
        return dati;
    });
}

function mostraErrore(errore) {
    var box = document.getElementById("errore");
    box.textContent = errore.message;
    box.hidden = false;
}

function escape(testo) {
    var div = document.createElement("div");
    div.textContent = testo;
    return div.innerHTML;
}

function lanciaDadi() {
    invia("/lanciaDadi");
}

function aggiornaInterfaccia(data) {
    stato = data;
    document.getElementById("errore").hidden = true;

    disegnaTabellone();
    disegnaCentro();
    disegnaGiocatori();
    disegnaDettaglioCasella();
}

/* Posizione (riga, colonna) di ogni casella nella griglia 11x11 */
function cellaDellaGriglia(posizione) {
    if (posizione <= 10) {
        return { riga: 11, colonna: 11 - posizione };
    }
    if (posizione <= 20) {
        return { riga: 11 - (posizione - 10), colonna: 1 };
    }
    if (posizione <= 30) {
        return { riga: 1, colonna: 1 + (posizione - 20) };
    }
    return { riga: 1 + (posizione - 30), colonna: 11 };
}

function disegnaTabellone() {
    var tabellone = document.getElementById("tabellone");

    stato.caselle.forEach(function (casella) {
        var elemento = document.getElementById("casella_" + casella.posizione);
        if (!elemento) {
            elemento = document.createElement("div");
            elemento.id = "casella_" + casella.posizione;
            elemento.className = "casella";
            var cella = cellaDellaGriglia(casella.posizione);
            elemento.style.gridRow = cella.riga;
            elemento.style.gridColumn = cella.colonna;
            elemento.addEventListener("click", function () {
                selezionaCasella(casella.posizione);
            });
            tabellone.appendChild(elemento);
        }

        elemento.classList.toggle("ipotecata", casella.ipotecata);
        elemento.classList.toggle("selezionata", casella.posizione === casellaSelezionata);
        elemento.title = casella.testo;

        var html = "";
        if (casella.tipo === "TERRENO") {
            html += '<div class="banda" style="background:' + COLORI_TERRENI[casella.colore] + '">'
                + edifici(casella.casette) + "</div>";
        } else {
            html += '<div class="banda">' + iconaCasella(casella.tipo) + "</div>";
        }
        html += '<div class="nome">' + casella.testo + "</div>";
        if (casella.costo > 0) {
            html += '<div class="prezzo">' + casella.costo + "€</div>";
        }
        if (casella.proprietario !== null) {
            html += '<div class="proprietario" style="background:' + COLORI_GIOCATORI[casella.proprietario] + '"></div>';
        }

        html += '<div class="segnalini">';
        stato.giocatori.forEach(function (giocatore, indice) {
            if (!giocatore.inBancarotta && giocatore.posizione === casella.posizione) {
                html += '<img src="/images/segnalini/' + giocatore.segnalino.toLowerCase() + '.png"'
                    + ' class="' + (indice === stato.giocatoreCorrente ? "corrente" : "") + '"'
                    + ' title="' + escape(giocatore.nome) + '" alt="' + giocatore.segnalino + '">';
            }
        });
        html += "</div>";

        elemento.innerHTML = html;
    });
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

function iconaCasella(tipo) {
    switch (tipo) {
        case "VIA": return "➡️";
        case "STAZIONE": return "🚂";
        case "SOCIETA": return "💡";
        case "PROBABILITA": return "❓";
        case "IMPREVISTI": return "❗";
        case "TASSA": return "💰";
        case "PRIGIONE": return "🔒";
        case "POSTEGGIO": return "🅿️";
        case "VAI_IN_PRIGIONE": return "👮";
        default: return "";
    }
}

function disegnaCentro() {
    var giocatore = stato.giocatori[stato.giocatoreCorrente];
    var casellaCorrente = stato.caselle[giocatore.posizione];

    document.getElementById("dadi").textContent = stato.dado1 > 0
        ? FACCE_DADO[stato.dado1] + " " + FACCE_DADO[stato.dado2]
        : FACCE_DADO[6] + " " + FACCE_DADO[6];

    var turno = document.getElementById("turno");
    if (stato.fase === "TERMINATA") {
        var vincitore = stato.giocatori[stato.vincitore];
        turno.innerHTML = "🏆 " + escape(vincitore.nome) + " ha vinto la partita!";
    } else {
        turno.innerHTML = 'Turno di <span style="color:' + COLORI_GIOCATORI[stato.giocatoreCorrente] + '">'
            + escape(giocatore.nome) + "</span>" + (giocatore.inPrigione ? " (in prigione)" : "");
    }

    var carta = document.getElementById("carta");
    if (stato.ultimaCarta) {
        carta.innerHTML = "<b>" + stato.ultimaCarta.mazzo + "</b><br>" + stato.ultimaCarta.testo;
        carta.hidden = false;
    } else {
        carta.hidden = true;
    }

    var azioni = document.getElementById("azioni");
    azioni.innerHTML = "";

    switch (stato.fase) {
        case "LANCIO_DADI":
            if (giocatore.inPrigione) {
                aggiungiPulsante(azioni, "Paga " + stato.multaPrigione + "€ ed esci", "btn-warning",
                    function () { invia("/pagaUscitaDiPrigione"); }, giocatore.portafoglio < stato.multaPrigione);
                if (giocatore.carteUscitaDiPrigione > 0) {
                    aggiungiPulsante(azioni, "Usa carta \"Uscite gratis\"", "btn-warning",
                        function () { invia("/usaCartaUscitaDiPrigione"); });
                }
                aggiungiPulsante(azioni, "Tenta il doppio 🎲 (" + (giocatore.turniInPrigione + 1) + "/3)", "btn-success", lanciaDadi);
            } else {
                aggiungiPulsante(azioni, "Lancia dadi 🎲", "btn-success", lanciaDadi);
            }
            break;
        case "DECISIONE_ACQUISTO":
            aggiungiPulsante(azioni, "Compra " + casellaCorrente.testo + " (" + casellaCorrente.costo + "€)", "btn-primary",
                function () { invia("/compraProprieta"); }, giocatore.portafoglio < casellaCorrente.costo);
            aggiungiPulsante(azioni, "Non comprare", "btn-outline-secondary",
                function () { invia("/rifiutaAcquisto"); });
            break;
        case "FINE_TURNO":
            aggiungiPulsante(azioni, "Fine turno ➡️", "btn-primary", function () { invia("/fineTurno"); });
            break;
        case "TERMINATA":
            var nuova = document.createElement("a");
            nuova.className = "btn btn-success";
            nuova.href = "/resettaPartita";
            nuova.textContent = "Nuova partita";
            azioni.appendChild(nuova);
            document.getElementById("termina-button").disabled = true;
            break;
    }

    var eventi = document.getElementById("eventi");
    eventi.innerHTML = "";
    stato.eventi.slice().reverse().forEach(function (evento) {
        var li = document.createElement("li");
        li.textContent = evento;
        eventi.appendChild(li);
    });
}

function aggiungiPulsante(contenitore, testo, classe, azione, disabilitato) {
    var pulsante = document.createElement("button");
    pulsante.type = "button";
    pulsante.className = "btn btn-sm " + classe;
    pulsante.textContent = testo;
    pulsante.disabled = !!disabilitato;
    pulsante.addEventListener("click", azione);
    contenitore.appendChild(pulsante);
    return pulsante;
}

function disegnaGiocatori() {
    var contenitore = document.getElementById("giocatori");
    contenitore.innerHTML = "";

    stato.giocatori.forEach(function (giocatore, indice) {
        var card = document.createElement("div");
        card.className = "card giocatore-card mb-2"
            + (indice === stato.giocatoreCorrente && stato.fase !== "TERMINATA" ? " corrente" : "")
            + (giocatore.inBancarotta ? " bancarotta" : "");
        card.style.borderLeftColor = COLORI_GIOCATORI[indice];

        var stati = [];
        if (giocatore.inBancarotta) stati.push("💀 in bancarotta");
        if (giocatore.inPrigione) stati.push("🔒 in prigione");
        if (giocatore.carteUscitaDiPrigione > 0) stati.push("🎫 " + giocatore.carteUscitaDiPrigione + " carta uscita gratis");

        var html = '<div class="card-body p-2">'
            + '<div class="d-flex align-items-center gap-2">'
            + '<img src="/images/segnalini/' + giocatore.segnalino.toLowerCase() + '.png" alt="">'
            + "<div><b>" + escape(giocatore.nome) + "</b> <small class=\"text-muted\">(" + giocatore.segnalino + ")</small><br>"
            + "💶 <b>" + giocatore.portafoglio + "€</b> · patrimonio " + giocatore.patrimonio + "€</div></div>";
        if (stati.length > 0) {
            html += '<div class="small mt-1">' + stati.join(" · ") + "</div>";
        }
        html += '<div class="proprieta-giocatore"></div></div>';
        card.innerHTML = html;

        var elencoProprieta = card.querySelector(".proprieta-giocatore");
        stato.caselle.forEach(function (casella) {
            if (casella.proprietario === indice) {
                var etichetta = document.createElement("span");
                etichetta.textContent = casella.testo + (casella.ipotecata ? " (ipotecata)" : "");
                etichetta.style.background = casella.colore ? COLORI_TERRENI[casella.colore] : "#555";
                etichetta.addEventListener("click", function () {
                    selezionaCasella(casella.posizione);
                });
                elencoProprieta.appendChild(etichetta);
            }
        });

        contenitore.appendChild(card);
    });
}

function selezionaCasella(posizione) {
    casellaSelezionata = posizione;
    disegnaTabellone();
    disegnaDettaglioCasella();
}

function disegnaDettaglioCasella() {
    if (casellaSelezionata === null) {
        return;
    }
    var casella = stato.caselle[casellaSelezionata];
    var contenitore = document.getElementById("dettaglio-casella");

    var html = '<div class="card-header fw-bold" style="'
        + (casella.colore ? "background:" + COLORI_TERRENI[casella.colore] + ";color:#fff;text-shadow:0 0 3px #000" : "")
        + '">' + casella.testo + "</div><div class=\"card-body p-2 small\">";

    if (casella.tipo === "TERRENO" || casella.tipo === "STAZIONE" || casella.tipo === "SOCIETA") {
        var proprietario = casella.proprietario === null ? "nessuno (della banca)" : escape(stato.giocatori[casella.proprietario].nome);
        html += "Prezzo: <b>" + casella.costo + "€</b><br>"
            + "Proprietario: <b>" + proprietario + "</b><br>"
            + "Valore ipotecario: " + casella.valoreIpotecario + "€ (riscatto " + casella.costoRiscatto + "€)<br>";
        if (casella.tipo === "TERRENO") {
            html += "Costo di una casa: " + casella.costoCasetta + "€<br>"
                + "Edifici: " + (casella.casette === 5 ? "1 albergo" : casella.casette + " case") + "<br>";
        }
        if (casella.tipo === "SOCIETA") {
            html += "Affitto: 4 volte i dadi (10 volte con entrambe le società)<br>";
        } else if (casella.proprietario !== null) {
            html += "Affitto attuale: <b>" + casella.affitto + "€</b><br>";
        }
        if (casella.ipotecata) {
            html += '<span class="text-danger">Proprietà ipotecata: non rende affitto.</span><br>';
        }
    } else if (casella.tipo === "TASSA") {
        html += "Si pagano " + casella.costo + "€ alla banca.";
    } else {
        html += "Casella speciale.";
    }
    html += '<div class="d-flex flex-wrap gap-1 mt-2" id="azioni-casella"></div></div>';
    contenitore.innerHTML = html;

    var possedutaDalGiocatoreDiTurno = casella.proprietario === stato.giocatoreCorrente && stato.fase !== "TERMINATA";
    if (possedutaDalGiocatoreDiTurno) {
        var azioni = document.getElementById("azioni-casella");
        if (casella.tipo === "TERRENO" && !casella.ipotecata) {
            aggiungiPulsante(azioni, casella.casette === 4 ? "Costruisci albergo" : "Costruisci casa", "btn-success",
                function () { invia("/costruisciCasetta/" + casella.posizione); }, casella.casette === 5);
            aggiungiPulsante(azioni, "Vendi edificio", "btn-outline-success",
                function () { invia("/vendiCasetta/" + casella.posizione); }, casella.casette === 0);
        }
        if (casella.ipotecata) {
            aggiungiPulsante(azioni, "Riscatta ipoteca (" + casella.costoRiscatto + "€)", "btn-warning",
                function () { invia("/riscattaIpoteca/" + casella.posizione); });
        } else {
            aggiungiPulsante(azioni, "Ipoteca (+" + casella.valoreIpotecario + "€)", "btn-outline-warning",
                function () { invia("/ipoteca/" + casella.posizione); });
        }
    }
}
