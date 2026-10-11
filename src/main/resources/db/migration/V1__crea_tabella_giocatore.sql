-- Lobby: i giocatori registrati prima di avviare la partita.
-- La tabella creata in passato da Hibernate salvava il segnalino come numero (ordinale)
-- e conteneva portafoglio e posizione, mai riletti: i dati della lobby sono temporanei,
-- quindi si ricrea la tabella invece di migrarli.
DROP TABLE IF EXISTS giocatore;

CREATE TABLE giocatore (
    id        INT          NOT NULL AUTO_INCREMENT,
    nome      VARCHAR(30)  NOT NULL,
    segnalino VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_giocatore PRIMARY KEY (id),
    CONSTRAINT uk_giocatore_segnalino UNIQUE (segnalino)
);
