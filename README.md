# Futterbock App 🍽️🏕️

Die Futterbock App unterstützt die Essensplanung für Pfadfinderlager. Sie hilft dabei, Mahlzeiten zu
organisieren, Mengen zu berechnen und Einkaufslisten zu erstellen.

Funktionen:

- ✅ Erstellen und Verwalten von Mahlzeiten für das Lager
- ✅ Automatische Berechnung von Zutatenmengen basierend auf der Teilnehmerzahl
- ✅ Generierung von Einkaufslisten
- ✅ Zuordnung von Teilnehmern zu verschiedenen Mahlzeiten

Die App erleichtert die Küchenplanung und sorgt dafür, dass alle satt werden – ohne unnötigen
Aufwand! 🚀

Die App basiert dabei auf dem hier erhältlichem Futterbock: https://bockbuecher.de/futterbock

Fragen oder Feedback gerne an: <a href="mailto:kontakt@bockbuecher.de">kontakt@bockbuecher.de</a>

### 👶 Teilnehmer-Faktor nach Alter

Teilnehmer werden abhängig vom Geburtsjahr unterschiedlich stark gewichtet:           
- **Babys (unter 4 Jahren)** zählen **0.4x** 
- **Kinder (unter 10 Jahren)** zählen **0.7x** 
- **Jugendliche (11–14 Jahre)** zählen **1.0x**
- **junge Erwachsene (15–23 Jahre)** zählen **1.2x**
- **Erwachsene (ab 14 Jahren)** zählen **1.0x**

Diese Gewichtung fließt z. B. in die Berechnung von Portionen, Mengen und teilweise auch
Materialbedarf mit ein.  
So wird vermieden, dass für kleinere Kinder zu viel geplant oder eingekauft wird.

### Import von Teilnehmern

- Die App ermöglicht das Importieren von Teilnehmern über eine CSV-Datei
- Als Beispiel für eine solche Datei ist die [sample_participants.csv](https://github.com/fkischewski99/futterbock/blob/main/sample_participants.csv) in dem Repository enthalten
- Die Datei muss die folgenden Spalten besitzen: Vorname, Nachname (optional), Ernährungsweise,
  Geburtsjahr)
- 💡
  Tipp: [Importieren oder Exportieren von Textdateien (TXT oder CSV) – Microsoft-Support](https://support.microsoft.com/de-de/office/importieren-oder-exportieren-von-textdateien-txt-oder-csv-5250ac4c-663c-47ce-937b-339e391393ba)

## Geplante Features

- Gäste zu Rezepten hinzufügen

## Installation & Setup

### macOS: Datenschutz und Sicherheit

Beim ersten Start der App auf macOS kann eine Sicherheitswarnung erscheinen. Um die App
freizuschalten:

1. **Sicherheitswarnung beim ersten Start:**
    - Wenn die Meldung "App kann nicht geöffnet werden" erscheint, klicken Sie auf **"Abbrechen"**

2. **Systemeinstellungen öffnen:**
    - Gehen Sie zu **Systemeinstellungen** > **Datenschutz und Sicherheit**
    - Oder drücken Sie `⌘ + Leertaste` und suchen nach "Datenschutz"

3. **App freischalten:**
    - Scrollen Sie zum Bereich **"Sicherheit"**
    - Sie sehen eine Meldung: *"Die App wurde blockiert, da sie von einem nicht verifizierten
      Entwickler stammt"*
    - Klicken Sie auf **"Trotzdem öffnen"**

4. **Bestätigung:**
    - Bei der nächsten Sicherheitsabfrage klicken Sie auf **"Öffnen"**
    - Die App startet nun und kann zukünftig normal verwendet werden

> **Hinweis:** Diese Schritte sind nur beim ersten Start erforderlich. Danach kann die App normal
> über das Dock oder den Finder gestartet werden.

## Technical

### App starten

```shell
./gradlew :composeApp:desktopRun -PmainClass=MainKt
```

### Oder direkt mit

```shell
./gradlew run
```





