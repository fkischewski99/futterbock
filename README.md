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

## Geplante Features

### Auf jeden Fall

- Verwaltung von Teilnehmenden (Stammesmitglieder)
- Hinzufügen von Unverträglichkeiten (Gluten, Laktose etc)
- Hinzufügen von Unverträglichkeiten gegen einzelne Lebensmittel

### Möglicherweise

- Erstellung von Küchendienstplänen?
- Erweiterung der PDFs --> Teilnehmendenliste mit Unverträglichkeiten + Ernährungsweise

## Installation & Setup

### macOS: Datenschutz und Sicherheit

Beim ersten Start der App auf macOS kann eine Sicherheitswarnung erscheinen. Um die App freizuschalten:

1. **Sicherheitswarnung beim ersten Start:**
   - Wenn die Meldung "App kann nicht geöffnet werden" erscheint, klicken Sie auf **"Abbrechen"**

2. **Systemeinstellungen öffnen:**
   - Gehen Sie zu **Systemeinstellungen** > **Datenschutz und Sicherheit**
   - Oder drücken Sie `⌘ + Leertaste` und suchen nach "Datenschutz"

3. **App freischalten:**
   - Scrollen Sie zum Bereich **"Sicherheit"**
   - Sie sehen eine Meldung: *"Die App wurde blockiert, da sie von einem nicht verifizierten Entwickler stammt"*
   - Klicken Sie auf **"Trotzdem öffnen"**

4. **Bestätigung:**
   - Bei der nächsten Sicherheitsabfrage klicken Sie auf **"Öffnen"**
   - Die App startet nun und kann zukünftig normal verwendet werden

> **Hinweis:** Diese Schritte sind nur beim ersten Start erforderlich. Danach kann die App normal über das Dock oder den Finder gestartet werden.

## Technical

### App starten
```shell
./gradlew :composeApp:desktopRun -PmainClass=MainKt
```

### Oder direkt mit
```shell
./gradlew run
```





