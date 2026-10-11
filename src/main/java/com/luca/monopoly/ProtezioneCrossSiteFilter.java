package com.luca.monopoly;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Protezione CSRF: rifiuta le richieste che modificano lo stato (POST, PUT, ...)
 * quando il browser le invia da un altro sito. Senza questo filtro una pagina
 * qualsiasi aperta dal giocatore potrebbe resettare la partita o muovere al suo posto.
 *
 * Non servono token: l'applicazione non ha login, e i browser moderni dichiarano
 * da dove arriva la richiesta con l'header {@code Sec-Fetch-Site} (Fetch Metadata).
 * Per i browser che non lo inviano si confronta l'header {@code Origin} con l'host.
 * Le richieste senza nessuno dei due non arrivano da un browser (curl, test) e
 * non possono essere sfruttate per un attacco CSRF, quindi passano.
 */
@Component
public class ProtezioneCrossSiteFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ProtezioneCrossSiteFilter.class);

    private static final Set<String> METODI_SICURI = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /** "none": richiesta avviata dall'utente (barra degli indirizzi, segnalibro). */
    private static final Set<String> PROVENIENZE_AMMESSE = Set.of("same-origin", "none");

    private static final String ERRORE_JSON = "{\"errore\":\"Richiesta proveniente da un altro sito: rifiutata.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest richiesta, HttpServletResponse risposta, FilterChain catena)
            throws ServletException, IOException {
        if (METODI_SICURI.contains(richiesta.getMethod()) || isStessaOrigine(richiesta)) {
            catena.doFilter(richiesta, risposta);
            return;
        }
        LOG.warn("Richiesta {} {} rifiutata: Sec-Fetch-Site={}, Origin={}", richiesta.getMethod(),
                richiesta.getRequestURI(), richiesta.getHeader("Sec-Fetch-Site"), richiesta.getHeader("Origin"));
        risposta.setStatus(HttpStatus.FORBIDDEN.value());
        risposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        risposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        risposta.getWriter().write(ERRORE_JSON);
    }

    private boolean isStessaOrigine(HttpServletRequest richiesta) {
        String provenienza = richiesta.getHeader("Sec-Fetch-Site");
        if (provenienza != null) {
            return PROVENIENZE_AMMESSE.contains(provenienza);
        }
        String origine = richiesta.getHeader("Origin");
        if (origine == null) {
            return true;
        }
        return hostDellOrigine(origine).equalsIgnoreCase(hostRichiesto(richiesta));
    }

    /** Host e porta dell'header Origin; stringa vuota se non valido (per esempio "null"). */
    private static String hostDellOrigine(String origine) {
        try {
            String host = new URI(origine).getRawAuthority();
            return host == null ? "" : host;
        } catch (URISyntaxException eccezione) {
            return "";
        }
    }

    /**
     * Dietro un proxy (Codespaces) l'host pubblico arriva in X-Forwarded-Host; con più
     * proxy in catena il primo valore è quello del client. Un sito esterno non può
     * impostare questo header: i form non lo permettono e fetch richiederebbe CORS.
     */
    private static String hostRichiesto(HttpServletRequest richiesta) {
        String inoltrato = richiesta.getHeader("X-Forwarded-Host");
        if (inoltrato != null) {
            return inoltrato.split(",")[0].trim();
        }
        return String.valueOf(richiesta.getHeader("Host"));
    }
}
