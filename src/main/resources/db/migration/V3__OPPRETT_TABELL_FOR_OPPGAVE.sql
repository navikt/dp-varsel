CREATE TABLE IF NOT EXISTS oppgave
(
    nokkel                BIGSERIAL PRIMARY KEY references nokkel(id),
    tekst                 TEXT DEFAULT 'NB'::TEXT NOT NULL,
    opprettet             TIMESTAMP NOT NULL,
    sikkerhetsnivaa       INT NOT NULL,
    ekstern_varsling      BOOLEAN NOT NULL,
    link                  TEXT NOT NULL
);
