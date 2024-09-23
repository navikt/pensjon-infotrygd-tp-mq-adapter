# Pensjon Infotrygd TP MQ Adapter

Applikasjonen mottar meldinger på kø fra Infotrygd og gjør REST-kall til TP for å hente informasjon. Meldingene fra
Infotrygd kommer på CopyBook-format med EBCDIC karaketersett.

## Kjøre applikasjonen lokalt mot testmiljø

> [!NOTE]
Dette krever tilgang til Nav sine testmiljø via _naisdevice_.

Opprett en `.env-fil` med hemmeligheter og konfigurasjon for miljøet ved å
kjøre følgende kommando.

```shell
./fetch-secrets.sh
```

Om du ønsker å kjøre mot et annet miljø kan du spesifisere dette som et argument til
`fetch-secrets.sh`. Om du for eksempel ønsker å kjøre mot Q1 så kan du kjøre følgende kommando

```shell
./fetch-secrets.sh Q1
```

I IntelliJ, naviger til klassen `Application`, trykk høyre museknapp på startikonet og
velg `Modify Run Configuration...` fra menyen. Under `Environment variables`
legger du til stien til `.env-filen` som ble opprettet. Om
`Environment variables` ikke vises legger du til dette valget ved å trykke på
`Modify options` og velge `Environment variables` fra menyen som vises.

> [!NOTE]
Instansene som allerede kjører i miljøet vil fortsette å lese meldinger fra kø. For å være sikker på at du mottar
> meldinger lokalt må du kjøre ned instansene i miljøet du vil kjøre mot
