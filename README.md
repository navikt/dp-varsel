# dp-behov-brukernotifkasjon

Denne appen kan produsere brukernotifikasjoner som vises på Min Side og i Dekoratøren.

Appen har to modus:
* Produsere beskjeder og oppgaver som svar på behov
* Produsere en identisk beskjed til mange brukere samtidig

## Produsere beskjed til mange brukere samtidig
Appen har et REST-endepunkt, `.../beskjed/broadcast`, for å trigge genereringen av en hardkodet beskjed til mange brukere. 
Denne beskjeden går ut til alle brukere som er definert i kubernetes secret-en 
`brukernotifikasjon-broadcast-beskjed`.

### Hvordan sende ut ny beskjeder
1. Juster parametere for beskjeden i `NotifikasjonBroadcaster`.
2. Opprett en tekstfil med navnet `beskjedReceivers.txt`, og putt en ident per linje.
3. Opprett dette som en secret med navnet `brukernotifikasjon-broadcast-beskjed`, det kan gjøres slik:
   ```
   kubectl create secret generic brukernotifikasjon-broadcast-beskjed --from-file=/path/to/beskjedReceivers.txt
   ```
     * Hvis dette feiler, så kan det være fordi secret-en er definert fra før. Slett eventuelt den gamle og gjenta steg 3.
4. Gjør et POST-kall mot endepunktet `.../beskjed/broadcast`, med følgende body:
   ```
   { "dryRun": true }
   ```
   F.eks. i dev kan de gjøres slik:
   ```bash
   curl --location --request POST 'https://dp-behov-brukernotifikasjon.dev.intern.nav.no/beskjed/broadcast' \
    --header 'Content-Type: application/json' \
    --data-raw '{ "dryRun": true }'
   ```
   Dette vil sørge for at det kun gjøres en dry run. Man vi da kunne verifisere at appen får lest ut forventet antall 
   identer, men det vil ikke skje noen bestilling av eventer. Man kan da se etter følgende logginnslag i loggene:
   ```
   > Hentet X identer
   > Dry run, ville ha produsert X beskjeder.
   > Oppsummering: Oppsummering(success=0, feilet=0, skulleProdusert=X)
   ```
   Hvor X skal være samme antall identer som ligger i secret-en.
5. Gjør et nytt POST-kall mot endepunktet `.../beskjed/broadcast`, med følgende body:
   ```
   { "dryRun": false }
   ```
   F.eks. ved å endre dryRun-verdien i kallet i steg 4.
   Dette vil faktisk bestille beskjeder for alle identer som er definert i secret-en.
6. Verifiser at alt har gått som det skal ved å se i appens logger. Identen til alle beskjeder som bestilles logges i 
   sikkerlogg. Der er det per ident info om bestilligen gikk bra eller dårlig.
