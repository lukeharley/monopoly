# CustomScraper - panoramica

Questo documento descrive l'architettura iniziale per un CustomScraper in grado di:

1. Navigare pagine con paginazione o scroll infinito.
2. Estrarre dati in `ProductData` (inclusi i link prodotto).
3. Filtrare gli URL già noti.
4. Eseguire il match con un file di riferimento e proporre i risultati più simili.

## Flusso consigliato

1. **Navigazione sito**
   - `PaginationStrategy`: genera URL con `?page=N`.
   - `InfiniteScrollStrategy`: genera URL per chiamate di "scroll".
2. **Estrazione dati**
   - Salvare il risultato in CSV (`productName`, `commercialName`, `producer`, `format`, `url`, ...).
3. **Filtro URL già presenti**
   - `ExistingUrlLoader` carica gli URL già noti.
   - `UrlFilterService` filtra gli URL nuovi.
4. **Match con file di riferimento**
   - `ReferenceProductLoader` carica il file (CSV) di riferimento.
   - `MatchingService` calcola la similarità e ordina i risultati.

## CSV richiesti

### CSV di scraping (`ProductData`)
Colonne minime consigliate:
- `url`
- `productName`
- `commercialName`
- `producer`
- `format`

### CSV di riferimento
Colonne minime consigliate:
- `referenceId` (o `id`)
- `denomination`
- `commercialName`
- `producer`
- `format`

Ogni colonna aggiuntiva viene salvata in `attributes` e può essere usata per future logiche di match.

## Esempio di matching

1. Caricare i `ProductData` dal CSV di scraping.
2. Caricare i `ReferenceProduct` dal CSV di riferimento.
3. Chiamare `MatchingService#matchProducts`.
4. Mostrare i match ordinati per punteggio decrescente.
