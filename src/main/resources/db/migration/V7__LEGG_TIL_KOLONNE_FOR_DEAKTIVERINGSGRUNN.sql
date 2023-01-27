ALTER TABLE oppgave
    ADD COLUMN
        deaktiveringsgrunn VARCHAR(255) NULL;

UPDATE oppgave SET deaktiveringsgrunn = 'FERDIG' WHERE aktiv = false;
