CREATE TABLE IF NOT EXISTS nokkel
(
    id    BIGSERIAL PRIMARY KEY,
    ident VARCHAR(11) NOT NULL,
    eventId uuid NOT NULL UNIQUE,
    unique (ident, eventId)
);

CREATE TABLE IF NOT EXISTS beskjed
(
    nokkel                BIGSERIAL PRIMARY KEY references nokkel(id),
    tekst                 TEXT DEFAULT 'NB'::TEXT NOT NULL,
    opprettet             TIMESTAMP NOT NULL,
    sikkerhetsnivaa       INT NOT NULL,
    ekstern_varsling      BOOLEAN NOT NULL
);
