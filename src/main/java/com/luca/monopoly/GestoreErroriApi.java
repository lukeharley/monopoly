package com.luca.monopoly;

import com.luca.monopoly.domain.MossaNonValidaException;
import com.luca.monopoly.domain.PartitaAssenteException;
import com.luca.monopoly.domain.StatoCambiatoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Traduce le eccezioni dell'API JSON in risposte con un codice HTTP coerente e
 * un messaggio leggibile nel campo "errore".
 */
@RestControllerAdvice(assignableTypes = AjaxHomeController.class)
public class GestoreErroriApi {

    private static final Logger LOG = LoggerFactory.getLogger(GestoreErroriApi.class);

    /** La mossa non è ammessa dalle regole nello stato attuale della partita. */
    @ExceptionHandler(MossaNonValidaException.class)
    public ResponseEntity<Map<String, String>> mossaNonValida(MossaNonValidaException eccezione) {
        return errore(HttpStatus.UNPROCESSABLE_ENTITY, eccezione.getMessage());
    }

    @ExceptionHandler(StatoCambiatoException.class)
    public ResponseEntity<Map<String, String>> statoCambiato(StatoCambiatoException eccezione) {
        return errore(HttpStatus.CONFLICT, eccezione.getMessage());
    }

    @ExceptionHandler(PartitaAssenteException.class)
    public ResponseEntity<Map<String, String>> partitaAssente(PartitaAssenteException eccezione) {
        return errore(HttpStatus.NOT_FOUND, eccezione.getMessage());
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, String>> richiestaNonValida(Exception eccezione) {
        return errore(HttpStatus.BAD_REQUEST, "Richiesta non valida: " + eccezione.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> erroreInatteso(Exception eccezione) {
        LOG.error("Errore inatteso durante una mossa", eccezione);
        return errore(HttpStatus.INTERNAL_SERVER_ERROR, "Errore inatteso del server.");
    }

    private ResponseEntity<Map<String, String>> errore(HttpStatus stato, String messaggio) {
        return ResponseEntity.status(stato).body(Map.of("errore", messaggio));
    }
}
