CREATE TABLE IF NOT EXISTS ferdigbehandlet_soknad
(
    ident      VARCHAR(11) NOT NULL,
    soknadId   uuid        NOT NULL,
    opprettet  TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (ident, soknadId)
);
