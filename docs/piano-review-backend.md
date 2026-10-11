# Piano di review del backend Monopoly

**Stato:** ✅ rivisto (Opus 5.5) — **approvato con modifiche**: vedi §7, che prevale sulle sezioni precedenti.
**Branch analizzato:** `claude/tender-keller-j3u55n` al commit `c31479c`.
**Perimetro:** `src/main/java/com/luca/monopoly/**`, `src/main/resources/application*.yml`, `pom.xml`, `src/test/java/com/luca/monopoly/**`. Escluso il modulo `com.luca.customscraper` e il frontend.

**Metodo:** analisi diretta del codice, non eseguita da revisori automatici indipendenti (la review multi-agente è stata interrotta per contenere i costi). Ogni voce riporta file e riga: il revisore può verificarla aprendo il punto indicato. Dove un giudizio è opinabile è segnalato con ⚖️ *decisione*.

**Legenda gravità:** 🔴 alta (bug o rischio concreto) · 🟠 media (qualità/robustezza che un revisore senior bloccherebbe) · 🟡 bassa (pulizia). **Sforzo:** S < 1 h · M 1-3 h · L > 3 h.

---

## 1. Sintesi

Il motore di gioco è corretto nelle regole principali e i 49 test passano. I problemi trovati riguardano soprattutto **integrità dei dati, incapsulamento del dominio, codice morto e copertura dei test**. Nessun bug di regole bloccante è emerso dall'analisi; tre punti (A1, A2, A4) sono rischi reali in esecuzione.

| Area | Voci | Di cui 🔴 |
|---|---|---|
| A. Integrità dati e concorrenza | 5 | 3 |
| B. Regole di gioco | 3 | 0 |
| C. Clean Code / architettura | 10 | 0 |
| D. Performance | 3 | 0 |
| E. Sicurezza e API | 4 | 1 |
| F. Test e qualità | 5 | 0 |
| **Totale** | **30** | **4** |

Sforzo complessivo stimato: **3 PR, circa 2-3 giorni/persona** se si fa tutto; **mezza giornata** per le sole voci 🔴.

---

## 2. Segnalazioni

### A. Integrità dei dati e concorrenza

**A1 🔴 Enum `Segnalini` salvato per posizione (ordinale)** — `repository/JpaGiocatore.java:18-19`
Manca `@Enumerated(EnumType.STRING)`: JPA salva `0..7`. Aggiungere o riordinare una costante in `Segnalini` corrompe silenziosamente i giocatori già nel DB (il Cane diventa un Dinosauro). Il vincolo `unique` resta valido ma sul numero.
*Fix:* `@Enumerated(EnumType.STRING) @Column(name = "segnalino", unique = true, nullable = false, length = 20)`. Con `ddl-auto: update` la colonna esistente (INT) non viene convertita: serve una migrazione o un reset della tabella (vedi A5). — **S**

**A2 🔴 Ordine di turno dipendente da `findAll()` senza ordinamento** — `service/GiocatoreService.java:28-30`, usato da `MonopolyService.avviaPartita` (`:48-62`)
L'ordine dei giocatori nella partita (quindi chi inizia) è l'ordine in cui il DB restituisce le righe, che non è garantito. Su MySQL InnoDB coincide di solito con la chiave primaria, ma non è un contratto.
*Fix:* `List<JpaGiocatore> findAllByOrderByIdAsc()` nel repository e usarlo ovunque (`getGiocatori`, `salvaStato`). — **S**

**A3 🔴 Finestra di corsa tra lobby e avvio partita** — `HomeController.java:68-80` + `MonopolyService.java:48-62`
`aggiungiGiocatore` controlla `isPartitaPresente()` (sotto lock) e poi scrive sul DB **fuori** dal lock. Se `avviaPartita` corre in mezzo, un giocatore risulta registrato nel DB ma assente dalla partita: la lobby e la partita divergono finché non si resetta. Probabilità bassa (serve un clic simultaneo), ma è un difetto di progetto, non di fortuna.
*Fix:* spostare la registrazione dentro `MonopolyService` (metodo `synchronized aggiungiGiocatore`) così check e scrittura stanno sotto lo stesso lock. — **S**

**A4 🟠 I/O sul database dentro il lock della partita** — `MonopolyService.java:75-84` (`esegui`), `:61`
`salvaStato` (findAll + saveAll, transazione, commit) viene eseguito tenendo il monitor: ogni client in polling (`/stato`, ogni 1,5 s) resta bloccato durante il commit. Con 8 giocatori e un DB locale è impercettibile, ma un rallentamento del DB congela tutti. Inoltre lo stato salvato **non viene mai riletto** (`avviaPartita` riparte da 1500 €): è scrittura a perdere.
*Fix, ⚖️ decisione tra:*
- (a) **Eliminare `salvaStato`** e persistere solo la lobby (nome + segnalino). Rimuove I/O dal lock, il problema A5 e 20 righe. Perde la "fotografia" dei portafogli nel DB, che oggi nessuno legge.
- (b) Tenerlo, ma eseguire la scrittura **fuori** dal lock su una copia immutabile dei dati, salvando solo i giocatori cambiati.
Consiglio (a). — **S** / **M**

**A5 🟠 Dopo un riavvio la lobby mostra portafogli e posizioni della partita persa** — `MonopolyService.java` (stato in memoria) + `HomeController.getHome`
La partita vive in memoria; i giocatori nel DB conservano gli ultimi portafoglio/posizione. Al riavvio la lobby mostra "Luca 384 €" con la partita sparita. Scompare adottando A4(a); altrimenti azzerare al bootstrap (`@PostConstruct` o `ApplicationRunner`). — **S**

### B. Regole di gioco

**B1 🟠 Offerta all'asta limitata ai contanti, senza possibilità di ipotecare** — `domain/Asta.java:33-35`
Un giocatore con 50 € e 1.000 € di proprietà non può offrire 100 € anche se potrebbe procurarseli ipotecando (la gestione delle proprietà è vietata in fase `ASTA`, `Partita.java:33-34`). Regola ufficiale: si può pagare anche ipotecando. ⚖️ *decisione:* semplificazione accettabile per il progetto o da correggere?
*Fix:* ammettere le mosse di gestione (vendita/ipoteca, non costruzione) per l'**offerente di turno** durante l'asta, oppure documentare il limite nel README. — **M**

**B2 🟡 Dati inutilizzati e nome fuorviante in `Contratto`** — `domain/Contratto.java:11-15, 17-31`
`renditaUnaStazione…QuattroStazioni` (65/130/195/260) non sono mai letti: l'affitto delle stazioni usa `tassa` (25) raddoppiata (`CalcolatoreAffitto.java`, caso `STAZIONE`). Il campo `tassa` per le società (50) non è usato. Chi legge i contratti crede che le stazioni rendano 65 €.
*Fix:* rimuovere i quattro campi e il parametro; rinominare `tassa` in `renditaBase`; i contratti delle 12 caselle non acquistabili ("Segnaposto") diventano `Optional`/`null` espliciti o si passa a una `Map<Integer, Contratto>`. — **S**

**B3 🟡 `terminaPartita` durante asta o debito scarta silenziosamente lo stato** — `domain/Partita.java:272-282`
Se chiusa a metà asta o con un debito aperto, asta e debito vengono annullati senza registro eventi; il creditore di un debito non incassa. Accettabile ma va detto nel registro.
*Fix:* registrare "asta annullata" / "debito di X € verso Y annullato" oppure vietare la chiusura in `ASTA`/`SALDO_DEBITO`. — **S**

### C. Clean Code e architettura

**C1 🟠 Modello anemico: `Giocatore` e `Casella` espongono setter per ogni campo** — `domain/giocatore/Giocatore.java:30-86`, `domain/Casella.java:63-84`
`setPortafoglio`, `setInBancarotta`, `setInPrigione`, `setTurniInPrigione`, `setPosizione`, `setProprietario`, `setIpotecata` sono pubblici: qualunque classe può mettere il dominio in uno stato incoerente (in prigione a posizione 5). Le invarianti vivono in `Partita` invece che negli oggetti. I test (`PartitaTest`, 47 chiamate a setter) dipendono da questa apertura.
*Fix:* sostituire con metodi di intento (`entraInPrigione()`, `esciDiPrigione()`, `registraTurnoInPrigione()`, `dichiaraBancarotta()`, `spostaA(int)`, `Casella.assegnaA(Giocatore)`, `ipoteca()/riscatta()`, `aggiungiCasa()/rimuoviCasa()`), visibilità package-private per ciò che serve solo a `Partita`; spostare i test di dominio nel package `com.luca.monopoly.domain` (vedi F2). — **L**

**C2 🟠 Codice morto e classi vuote** —
- `domain/Albergo.java`, `domain/Casetta.java`: classi vuote; `Tabellone.getCasette()/getAlberghi()` allocano 32+12 oggetti solo per contarli (`Partita.java:37`, `Monopoly.java:17`).
- `domain/Monopoly.java`: usato solo per `getDadi().size()` in `HomeController.java:50`.
- `AppConfig.java`: `@ComponentScan` ridondante con `@SpringBootApplication`; richiede `allow-bean-definition-overriding: true` (`application.yml:26`), che nasconde conflitti di bean.
- `UserController.java` + `templates/user.html`: pagina vuota.
- `HomeForm.portafoglio` (`HomeForm.java:8`): mai letto.
*Fix:* eliminare tutto; costanti `Banca.CASETTE_TOTALI = 32`, `ALBERGHI_TOTALI = 12`; `HomeController` usa una costante `NUMERO_DADI`. — **S**

**C3 🟠 `Partita` resta una God Class (695 righe, 6 responsabilità)** — `domain/Partita.java`
Turni, movimento, carte, pagamenti/bancarotta, asta, gestione proprietà. Le sezioni sono già separate da commenti: sono i confini naturali di estrazione.
*Fix:* estrarre `ApplicatoreCarte` (righe 410-463) e `GestoreBancarotta`/`Tesoreria` (476-541: pagamento, trasferimento, bancarotta). `Partita` scende sotto le 450 righe e coordina. — **M**

**C4 🟠 Dipendenze costruite internamente, test seam pubblico** — `domain/Partita.java:36-42, 58-88`
`Tabellone`, `Banca`, dadi, mazzi, `Random` sono creati nel costruttore; `lanciaDadi(int, int)` è pubblico solo per i test e lancia `IllegalArgumentException` (unica eccezione non di dominio del package).
*Fix:* interfaccia `Dadi { Lancio lancia(); }` con implementazioni `DadiCasuali(Random)` e, nei test, `DadiPrestabiliti`; costruttore `Partita(List<Giocatore>, Tabellone, Dadi, Mazzo, Mazzo)` più una factory `Partita.nuova(giocatori)` per la produzione. `lanciaDadi(int,int)` sparisce. — **M**

**C5 🟠 Validazione del form fatta a mano nel service** — `service/GiocatoreService.java:33-45`, `HomeForm.java`
Nome vuoto/lunghezza e indice del segnalino sono controllati con `if` e `MossaNonValidaException` (che semanticamente è un errore di gioco, non di input).
*Fix:* `spring-boot-starter-validation`; `HomeForm` con `@NotBlank @Size(max = 30)` e `@NotNull`; `@Valid` nel controller con `BindingResult`; `HomeForm.segnalino` di tipo `Segnalini` (conversione automatica da nome) invece di `int` + `values()[i]`. — **S**

**C6 🟡 Struttura dei package piatta** — `com.luca.monopoly` contiene controller, form, view, advice, mescolati ad `Application`.
*Fix:* `web` (controller, `GestoreErroriApi`, `HomeForm`), `web.dto` (`StatoPartitaView`, `GiocatoreView`, `SelectOptionView`), `service`, `domain`, `persistence` (rinomina di `repository`). Rinominare `AjaxHomeController` → `PartitaApiController`, `JpaGiocatore` → `GiocatoreEntity`, `Segnalini` → `Segnalino` (è un singolo segnalino). — **M**

**C7 🟡 Numeri magici sparsi nelle regole** —
`4` e `5` (case prima dell'albergo / albergo) in `GestoreProprieta.java` e `Casella.haAlbergo`; `10`/`4` (moltiplicatori società) e `<< (stazioni - 1)` in `CalcolatoreAffitto.java`; `/ 2` (ipoteca) e `* 11 + 9) / 10` (riscatto) in `Casella.java`; `0.5` implicito nella vendita edifici.
*Fix:* costanti nominate (`CASE_PER_ALBERGO = 4`, `MOLTIPLICATORE_SOCIETA = 4`, `MOLTIPLICATORE_DUE_SOCIETA = 10`, `INTERESSE_RISCATTO_PERCENTO = 10`, …). — **S**

**C8 🟡 Colore del gruppo come `String`** — `domain/Casella.java`, `Tabellone.java` (`"fucsia"`, `"azzurro"`, …), `StatoPartitaView`, JS.
Un refuso in una stringa rompe il monopolio senza errori di compilazione.
*Fix:* `enum GruppoColore { FUCSIA, AZZURRO, … }` con `getNome()` per il JSON. — **S**

**C9 🟡 `Debito.passiDopoIlPagamento` codifica un comportamento con un intero** — `domain/Debito.java`, `Partita.java:227-233`
"0 = nessun movimento, >0 = multa di prigione: poi muovi" è una convenzione implicita.
*Fix:* `enum MotivoDebito { AFFITTO, TASSA, CARTA, MULTA_PRIGIONE }` + campo `passi` usato solo per `MULTA_PRIGIONE`, oppure un `Runnable azioneDopoIlPagamento` passato da chi crea il debito. — **S**

**C10 🟡 Piccole incoerenze** —
`Dado.NUMERO_FACCE` è un `final` d'istanza scritto come costante (`Dado.java:6`) → `static`. `FasePartita.MOVIMENTO` è uno stato transitorio esposto nell'API pubblica (`FasePartita.java`) → documentato, ma potrebbe diventare un valore di ritorno di `muovi`. `Giocatore` senza `equals/hashCode`: l'identità per riferimento è voluta (gli indici in `StatoPartitaView` si basano su `indexOf`) → aggiungere un commento o un `id`. — **S**

### D. Performance

**D1 🟠 La vista JSON viene ricostruita a ogni polling di ogni client** — `AjaxHomeController.java` (`/stato`), `StatoPartitaView.da` (`:55-104`)
Con N client, N ricostruzioni ogni 1,5 s dello stesso stato: 40 × `calcolaAffitto` (ognuno con `indexOf` O(40) in `Tabellone.getPosizione`, `:213-214`, più stream su 40 caselle) e 8 × `calcolaPatrimonio`. Oggi è dell'ordine dei microsecondi, ma è lavoro O(N·40²) ripetuto per uno stato immutato.
*Fix:* `MonopolyService` memorizza la `StatoPartitaView` dell'ultima versione (è un record immutabile) e la restituisce finché la versione non cambia; `Casella` conosce la propria `posizione` (eliminando `indexOf`). — **S**

**D2 🟡 Ricerche lineari ripetute nel dominio** — `Tabellone.java:202-231` (`getCaselleDelColore`, `getProprietaDi`, `possiedeTuttoIlGruppo`, `contaProprietaDelTipo`)
Ogni calcolo di affitto filtra 40 caselle più volte. Con 40 elementi è irrilevante, ma `getCaselleDelColore` può essere precalcolata una volta nel costruttore (`Map<GruppoColore, List<Casella>>`). — **S**

**D3 🟡 Registro eventi come `ArrayList` con `remove(0)`** — `Partita.java:611-616`
O(n) per inserimento con n = 100: trascurabile; `ArrayDeque` è la struttura corretta. — **S**

### E. Sicurezza e API

**E1 🔴 Nessuna protezione CSRF sui POST** — `HomeController.java` (form), `AjaxHomeController.java` (fetch), nessun filtro.
Una pagina esterna aperta da un giocatore può inviare `POST /resettaPartita` o `POST /lanciaDadi` al server (stessa rete/stesso host): cancella la partita o muove al posto suo. Non c'è Spring Security, quindi nessun token.
*Fix (leggera, senza Spring Security):* un `OncePerRequestFilter` che per i metodi non sicuri rifiuta (403) le richieste il cui header `Sec-Fetch-Site` è `cross-site` o il cui `Origin` non coincide con l'host; più `server.servlet.session.cookie.same-site: strict`. In alternativa `spring-boot-starter-security` con solo CSRF attivo e token nel form/fetch. — **S** / **M**

**E2 🟠 Nessuna identità dei giocatori** — by design (gioco allo stesso tavolo), documentato nel README.
Chiunque apra il link muove per il giocatore di turno. ⚖️ *decisione:* resta un limite dichiarato, oppure si lega ogni giocatore alla sessione HTTP che lo ha registrato (cookie di sessione → segnalino) e si rifiutano le mosse di altri. — **M**

**E3 🟡 Risposte JSON senza `Cache-Control`** — `AjaxHomeController.stato`
Un proxy intermedio potrebbe servire uno stato vecchio. *Fix:* `Cache-Control: no-store` sulle risposte di `/stato` (o `CacheControl.noStore()` in `ResponseEntity`). — **S**

**E4 🟡 Configurazione di produzione permissiva** — `application.yml:12` (`ddl-auto: update`), `:22` (`DB_USERNAME` default `luke`), `pom.xml:47` (devtools, che attiva anche la console H2 in `spring-boot:run`)
`update` modifica lo schema in produzione senza controllo; con A1 la colonna cambierebbe tipo senza migrare i dati. *Fix:* Flyway con `V1__giocatore.sql` e `ddl-auto: validate`; profilo `dev` tiene `create-drop`; `spring.h2.console.enabled: false` esplicito. — **M**

### F. Test e qualità

**F1 🟠 Copertura mancante su regole già implementate** — `PartitaTest.java`
Non testati: riparazioni con alberghi (`Partita.pagaRiparazioni`); società con due proprietà (×10); vendita forzata di un albergo quando la banca ha meno di 4 case (`GestoreProprieta.vendiEdificio`, ramo `venditaForzata`); esaurimento alberghi; asta a 3+ giocatori con ritiri e riassegnazione dell'indice (`Asta.ritirati`); bancarotta verso un giocatore con trasferimento delle carte "Uscite gratis"; `Mazzo` che ricicla le carte e trattiene `ESCI_GRATIS`; `terminaPartita` durante asta/debito; `StatoPartitaView` (indici, null). — **M**

**F2 🟠 I test di dominio manipolano lo stato con i setter** — `PartitaTest.java` (47 chiamate a `setPosizione`, `setInPrigione`, `setProprietario`, …)
Sono accoppiati all'implementazione: la fix C1 li rompe tutti. *Fix:* spostare `PartitaTest`/`MonopolyTest` nel package `com.luca.monopoly.domain` e introdurre un `ScenarioPartita` (builder di test) con metodi espressivi (`conProprietario(3, luca)`, `inPrigione(luca)`, `conContanti(luca, 50)`). — **M**

**F3 🟡 Test unitari assenti per le classi estratte** — `Asta`, `CalcolatoreAffitto`, `GestoreProprieta`, `Banca`, `Mazzo` sono testate solo attraverso `Partita`. *Fix:* una classe di test ciascuna (regole di uniformità, scorte, riciclo carte). — **M**

**F4 🟡 `MonopolyTest` usa `@Order` senza motivo** — `MonopolyTest.java` (10 annotazioni). I test sono indipendenti; l'ordinamento suggerisce il contrario. *Fix:* rimuovere. — **S**

**F5 🟡 Nessuna misura di copertura in CI** — `pom.xml`, `.github/workflows/ci.yml`. *Fix:* JaCoCo con soglia (es. 80 % sul package `domain`) in `mvn verify`. — **S**

---

## 3. Piano di esecuzione proposto

Tre PR separate, per tenere le review piccole e revertibili.

| PR | Contenuto | Voci | Sforzo | Rischio |
|---|---|---|---|---|
| **1 – Integrità e sicurezza** | enum STRING + migrazione, ordine giocatori, registrazione sotto lock, (eliminazione o spostamento di `salvaStato`), filtro CSRF, `no-store`, Flyway + `validate`, codice morto | A1-A5, E1, E3, E4, C2 | ½ giornata | basso: cambi locali, coperti da test d'integrazione |
| **2 – Dominio** | incapsulamento, estrazioni da `Partita`, iniezione dadi/tabellone, enum colore, costanti, `Debito` tipizzato, cache della vista | C1, C3, C4, C7-C10, D1-D3, B2, B3, (B1) | 1-1,5 giorni | medio: tocca quasi tutto il dominio; va fatta con i test spostati nel package (F2) |
| **3 – Qualità** | validazione con Bean Validation, riorganizzazione package, test mancanti, test unitari delle classi estratte, JaCoCo | C5, C6, F1-F5 | 1 giorno | basso |

Ordine consigliato: **1 → 3 (solo F2, come preparazione) → 2 → 3 (il resto)**.

## 4. Decisioni richieste al revisore

1. **A4/A5 – Persistenza dei portafogli:** eliminare `salvaStato` (consigliato) o tenerlo fuori dal lock?
2. **B1 – Asta:** permettere ipoteche durante l'offerta o documentare il limite?
3. **E2 – Identità:** restare "stesso tavolo" o legare i giocatori alla sessione?
4. **E1 – CSRF:** filtro leggero su `Origin`/`Sec-Fetch-Site` o Spring Security completo?
5. **E4 – Migrazioni:** introdurre Flyway (consigliato: è il prerequisito pulito per A1) o restare su `ddl-auto`?
6. **C6 – Package:** rinominare `Segnalini`→`Segnalino`, `JpaGiocatore`→`GiocatoreEntity`, `AjaxHomeController`→`PartitaApiController` ora (rumore nel diff) o in una PR a parte?

## 5. Cosa questa analisi non copre

- Non è stata eseguita una verifica indipendente delle segnalazioni: le righe citate vanno controllate.
- Nessuna misura di performance reale (profilazione): D1-D3 sono valutazioni sul codice.
- Frontend (`monopoly.js`), modulo `customscraper`, infrastruttura (Docker/deploy).
- Regole non implementate per scelta (scambi tra giocatori, limite di tempo): non trattate.

## 6. Checklist per il revisore

- [ ] Le 4 voci 🔴 (A1, A2, A3, E1) sono condivise e vanno in PR 1?
- [ ] Le 6 decisioni del §4 hanno una risposta.
- [ ] La suddivisione in 3 PR va bene, o si preferisce una sola PR?
- [ ] C'è qualche voce da scartare perché "gusto personale" e non standard del team?
- [ ] Manca qualcosa che il revisore considera bloccante?

---

## 7. Esito della review (Opus 5.5)

**Verdetto: approvato con modifiche.** Tutte le 30 voci sono state ricontrollate sul codice al commit `66bee5e`. Tre ipotesi sono state verificate eseguendo l'applicazione:

| Verifica pratica | Esito |
|---|---|
| `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (il comando avviato dal devcontainer), poi `GET /h2-console/` | **200, pagina "H2 Console"**: console esposta |
| `POST /resettaPartita` con `Origin: https://sito-malevolo.example` | **302**: partita resettata da un'origine esterna |
| Rimossi `AppConfig` e `allow-bean-definition-overriding`, poi `ApiPartitaIntegrationTest` | **8/8 verdi**: entrambi superflui |

### 7.1 Nuova voce (mancava nel piano)

**E5 🔴 Console H2 raggiungibile da Internet nel Codespace** — `application-dev.yml`, `pom.xml:47` (devtools), `.devcontainer/devcontainer.json`
Con `spring-boot:run` devtools è attivo e abilita per default `/h2-console`. Il port forwarding dei Codespaces si collega da `localhost`, quindi il controllo "solo connessioni locali" di H2 non protegge. Con la porta 8080 **Public** (come indicato nel README per giocare in più persone), chiunque abbia il link apre la console con utente `sa` e password vuota, esegue SQL arbitrario e, con `CREATE ALIAS`, codice Java nel container. Non serve nessun bug: è una funzione documentata di H2.
*Fix:* `spring.h2.console.enabled: false` esplicito in `application-dev.yml` (una proprietà esplicita prevale sui default di devtools) e un test d'integrazione che verifica che `/h2-console` non sia raggiungibile. — **S**

### 7.2 Esito per voce

Legenda: ✅ confermata · ✏️ confermata con correzioni · ⏸ rinviata · ❌ respinta.

| Voce | Esito | Gravità | Note del revisore |
|---|---|---|---|
| A1 | ✅ | 🔴 | Confermata (nessun `@Enumerated`, quindi ORDINAL). Su MySQL esistente la colonna resta INT: inserire `'CANE'` fallirebbe. La migrazione è obbligatoria (vedi E4). |
| A2 | ✏️ | 🔴→🟠 | Contratto JPA non garantito, ma su InnoDB e H2 l'ordine coincide in pratica con la chiave primaria. Rischio latente, fix banale. |
| A3 | ✏️ | 🔴→🟠 | Finestra reale ma richiede due clic quasi simultanei. **Da aggiungere:** due registrazioni simultanee con lo stesso segnalino superano entrambe `findBySegnalino` e la seconda viola il vincolo `unique`. La `DataIntegrityViolationException` non è gestita e produce una pagina 500. Fix: registrazione dentro il lock di `MonopolyService` e conversione della violazione in messaggio. |
| A4 | ✅ | 🟠 | Confermato che lo stato salvato non viene mai riletto (`avviaPartita` riparte da 1500 €). Decisione (a). |
| A5 | ✅ | 🟡 | Si risolve con A4(a). |
| B1 | ✏️ | 🟠→🟡 | Corretta come regola, ma vale per *qualsiasi* giocatore non di turno, non solo nell'asta. ⏸ Rinviata: documentare il limite nel README. |
| B2 | ✅ | 🟡 | Aggiornare `MonopolyTest.java:83-84`, che verifica i campi da eliminare. |
| B3 | ✏️ | 🟡 | Solo registrare l'annullamento nel registro eventi: vietare la chiusura non va bene, perché "Termina partita" deve funzionare sempre. |
| C1 | ✏️ | 🟠 | **Errore nel piano:** `Giocatore` è in `domain.giocatore` e `Partita` in `domain`, quindi la visibilità package-private non funziona. Spostare `Giocatore` in `domain` (il sottopackage contiene solo quella classe). `Giocatore.setNome/setSegnalino` non sono usati in produzione: eliminarli. Prerequisito: F2. |
| C2 | ✏️ | 🟠 | **Corretta l'affermazione:** `AppConfig` non richiede l'override dei bean; verificato che si possono rimuovere entrambi. Eliminando `Casetta`/`Albergo`, i test `test_casette`/`test_alberghi` passano a verificare le costanti di `Banca`. |
| C3 | ❌ | — | I metodi sono brevi (quasi tutti < 30 righe). Estrarre carte e bancarotta richiede un'interfaccia di callback sullo stato del turno (fase, debito, passaggio turno) e *aumenta* l'accoppiamento. Da rivalutare dopo C1, C7, C9 e B2, che riducono le dimensioni. |
| C4 | ✏️ | 🟡 | **Corretta la descrizione:** `Random` è già iniettabile (`Partita(List, Random, boolean)`). Il problema reale è `lanciaDadi(int, int)` pubblico con `IllegalArgumentException`. |
| C5 | ✅ | 🟡 | Il template deve passare dai valori ordinali ai nomi dell'enum. |
| C6 | ⏸ | — | Molti file toccati, poco valore. Eventualmente in una PR a parte, dopo le altre. |
| C7 | ✅ | 🟡 | |
| C8 | ✅ | 🟡 | Il JSON deve mantenere i nomi minuscoli: il frontend li usa come chiavi (`COLORI_TERRENI`). |
| C9 | ✅ | 🟡 | Enum, non `Runnable`: lo stato deve restare osservabile e serializzabile. |
| C10 | ✅ | 🟡 | |
| D1 | ✏️ | 🟠 | **Più grave del previsto:** `/stato` costruisce l'intera vista *anche quando poi risponde 204*, perché il confronto della versione avviene dopo. Prima fix: controllare la versione prima di costruire la vista. Cache facoltativa. |
| D2 | ✅ | 🟡 | Facoltativa. |
| D3 | ✏️ | 🟡 | Facoltativa: con 100 elementi la differenza non è misurabile. |
| E1 | ✅ | 🔴 | Verificata. **Correzione:** il suggerimento `same-site: strict` è inutile, perché gli endpoint non dipendono da cookie. La protezione è il filtro su `Origin`/`Sec-Fetch-Site`. |
| E2 | ✅ | — | Per scelta progettuale: resta un limite documentato. |
| E3 | ✏️ | 🟡 | Rischio quasi nullo (senza validatori non c'è caching euristico), ma `no-store` costa una riga. |
| E4 | ✏️ | 🟠 | La parte H2 diventa E5 🔴. **Correzione:** devtools *non* finisce nel jar di produzione (`optional` ed escluso dal repackage). Restano `ddl-auto: update` e il default `DB_USERNAME=luke`, da rimuovere. |
| F1 | ✅ | 🟠 | Verificato sui nomi dei 30 test di `PartitaTest`: mancano tutti i casi elencati. |
| F2 | ✅ | 🟠 | Prerequisito di C1. |
| F3 | ✅ | 🟡 | |
| F4 | ✅ | 🟡 | |
| F5 | ✅ | 🟡 | Soglia iniziale ragionevole: 80 % sul package `domain`. |

**Riepilogo:** 31 voci (30 + E5). 🔴 rivisti: **A1, E1, E5**. Respinte: C3. Rinviate: B1 (solo documentazione), C6. Facoltative: D2, D3.

### 7.3 Risposte alle decisioni del §4

1. **A4/A5:** opzione (a). Si elimina `salvaStato` e il DB conserva solo la lobby (nome + segnalino).
2. **B1:** si documenta il limite nel README; implementazione rinviata.
3. **E2:** resta "stesso tavolo", limite dichiarato.
4. **E1:** filtro leggero su `Origin`/`Sec-Fetch-Site`. Spring Security non serve, perché non c'è autenticazione da proteggere.
5. **E4:** Flyway sì. Con A4(a) lo schema è una sola tabella: `V1` la ricrea con `segnalino VARCHAR` (i dati della lobby sono temporanei, quindi non c'è nulla da migrare) e `ddl-auto: validate`.
6. **C6:** nessuna rinomina ora.

### 7.4 Piano di esecuzione rivisto

| PR | Contenuto, in ordine | Sforzo |
|---|---|---|
| **1 – Sicurezza e integrità** | E5 → E1 → A1 + E4 (Flyway, `validate`) → A4/A5 → A2 → A3 (+ vincolo unique) → C2 → D1 (risposta 204 senza costruire la vista) → E3 | ½ giornata |
| **2 – Dominio** | F2 (test nel package `domain` + `ScenarioPartita`) → C1 (+ spostamento di `Giocatore`) → C4 → C7 → C8 → C9 → C10 → B2 → B3 → D2 | 1 giornata |
| **3 – Qualità** | C5 → F1 → F3 → F4 → F5 → B1 (README) | ½ giornata |

Criterio di accettazione per ogni PR: `mvn verify` verde, nessuna regressione nel giro di prova end-to-end nel browser, e per PR 1 un test d'integrazione dedicato a ciascuna voce 🔴 (console H2 non raggiungibile, POST cross-origin rifiutato con 403, segnalino salvato come stringa).
