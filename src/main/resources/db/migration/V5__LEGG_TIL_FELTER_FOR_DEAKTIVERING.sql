ALTER TABLE oppgave
    ADD COLUMN
        aktiv boolean NULL default true,
    ADD COLUMN
        deaktiveringstidspunkt TIMESTAMP NULL;
