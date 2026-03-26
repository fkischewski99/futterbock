# Ergänzung der Datenschutzerklärung für die Futterbock App

Die nachfolgenden Bestimmungen ergänzen die allgemeine Datenschutzerklärung der Bock Bücher UG (haftungsbeschränkt) unter [https://bockbuecher.de/datenschutz/](https://bockbuecher.de/datenschutz/) um die Verarbeitung personenbezogener Daten bei Nutzung der **Futterbock App** (Android, iOS, Desktop).

Die verantwortliche Stelle ist dieselbe wie in der allgemeinen Datenschutzerklärung angegeben.

---

## 1. Geltungsbereich

Diese Ergänzung gilt für die Nutzung der Futterbock App auf allen unterstützten Plattformen (Android, iOS, Desktop/JVM). Die Bestimmungen der allgemeinen Datenschutzerklärung (insbesondere Betroffenenrechte, Verantwortliche Stelle, SSL-/TLS-Verschlüsselung) gelten ergänzend.

---

## 2. Registrierung und Nutzerkonto

Bei der Registrierung in der App werden folgende Daten erhoben und in Firebase Authentication gespeichert:

- **E-Mail-Adresse** (als Login-Kennung)
- **Passwort** (wird durch Firebase gehasht gespeichert; wir haben keinen Zugriff auf das Klartext-Passwort)
- **Gruppenzugehörigkeit (Stamm)** (zur organisatorischen Zuordnung Ihrer Daten)

**Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung – Bereitstellung des App-Dienstes)

Sie können Ihr Nutzerkonto jederzeit in der App löschen. Dabei werden Ihre Kontodaten aus Firebase Authentication sowie Ihre Nutzerdaten aus der Datenbank entfernt.

---

## 3. Verarbeitung von Teilnehmerdaten

Zur Erfüllung des Hauptzwecks der App (Verpflegungsplanung für Lager und Veranstaltungen) werden folgende personenbezogene Daten von Teilnehmern erhoben und verarbeitet:

| Datenkategorie | Zweck |
|---|---|
| Vorname, Nachname | Identifikation und Zuordnung zu Veranstaltungen |
| Geburtsdatum | Berechnung altersabhängiger Portionsgrößen |
| Ernährungsweise (vegan, vegetarisch, pescetarisch, omnivor) | Rezeptplanung und Einkaufslistenerstellung |
| Lebensmittelunverträglichkeiten (Laktose, Fructose, Gluten, Nüsse) | Sicherstellung verträglicher Mahlzeiten |
| Allergien | Sicherstellung verträglicher Mahlzeiten |
| Kochgruppenzuordnung | Organisation der Verpflegung |

**Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung) sowie Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an der sicheren Verpflegungsplanung, insbesondere bei gesundheitsrelevanten Daten wie Allergien und Unverträglichkeiten).

**Hinweis zu Gesundheitsdaten:** Angaben zu Ernährungsweise, Unverträglichkeiten und Allergien können Gesundheitsdaten im Sinne von Art. 9 DSGVO darstellen. Die Verarbeitung erfolgt auf Grundlage der ausdrücklichen Einwilligung der betroffenen Person bzw. des Erziehungsberechtigten (Art. 9 Abs. 2 lit. a DSGVO), die durch die freiwillige Eingabe dieser Daten in die App erteilt wird.

---

## 4. CSV-Import von Teilnehmerdaten

Die App ermöglicht den Import von Teilnehmerdaten über CSV-Dateien. Dabei können Vorname, Nachname, Geburtsdatum, Ernährungsweise und Kochgruppenzuordnung importiert werden.

Der Nutzer, der den Import durchführt, ist dafür verantwortlich, dass er die erforderliche Berechtigung zur Verarbeitung der importierten personenbezogenen Daten besitzt (z. B. Einwilligung der Betroffenen oder deren Erziehungsberechtigten).

---

## 5. Datenverarbeitung durch Google Firebase

Die App nutzt **Google Firebase** als Backend-Dienst für Authentifizierung und Datenspeicherung. Anbieter ist die Google Ireland Limited, Gordon House, Barrow Street, Dublin 4, Irland.

### Eingesetzte Firebase-Dienste:

- **Firebase Authentication** – Verwaltung von Nutzerkonten (E-Mail/Passwort)
- **Firebase Cloud Firestore** – Speicherung aller App-Daten (Teilnehmer, Veranstaltungen, Rezepte, Einkaufslisten)

### Nicht eingesetzte Firebase-Dienste:

- Firebase Analytics ist **deaktiviert**
- Firebase Crashlytics ist **nicht aktiv**
- Es findet **kein Tracking, keine Verhaltensanalyse und keine Werbeauswertung** durch die App statt

**Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung) und Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an einer zuverlässigen, skalierbaren Infrastruktur).

**Auftragsverarbeitung:** Google verarbeitet Daten in unserem Auftrag gemäß Art. 28 DSGVO. Die Nutzungsbedingungen von Firebase enthalten entsprechende Auftragsverarbeitungsvereinbarungen.

Weitere Informationen zum Datenschutz bei Firebase:
[https://firebase.google.com/support/privacy](https://firebase.google.com/support/privacy)

---

## 6. Datenübermittlung in Drittländer

Durch die Nutzung von Google Firebase können personenbezogene Daten auf Servern außerhalb der Europäischen Union verarbeitet werden, insbesondere in den USA. Die Übermittlung erfolgt auf Grundlage des Angemessenheitsbeschlusses der Europäischen Kommission gemäß Art. 45 DSGVO (EU-US Data Privacy Framework) bzw. auf Basis von Standardvertragsklauseln (Art. 46 Abs. 2 lit. c DSGVO).

---

## 7. Update-Prüfung (nur Desktop-Version)

Die Desktop-Version der App prüft automatisch auf verfügbare Updates über die GitHub API (`https://api.github.com`). Dabei werden **keine personenbezogenen Daten** übermittelt. Es wird lediglich ein technischer User-Agent-Header („Futterbock-App-UpdateChecker") gesendet.

**Rechtsgrundlage:** Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an der Bereitstellung aktueller Software).

---

## 8. Lokale Datenverarbeitung

### PDF-Generierung

Die App ermöglicht die Erstellung von PDF-Dokumenten (Einkaufslisten, Rezeptpläne, Materiallisten). Die PDF-Generierung erfolgt **ausschließlich lokal** auf dem Gerät des Nutzers. Es werden keine Daten an externe Dienste zur PDF-Erstellung übermittelt.

### Protokollierung (Logging)

Die App verwendet eine lokale Protokollierung zu Entwicklungs- und Fehlerbehebungszwecken. Protokolldaten verbleiben auf dem Gerät und werden **nicht an externe Server übermittelt**.

---

## 9. App-Berechtigungen

### Android
Die App benötigt ausschließlich folgende Berechtigungen:
- **Internetzugriff** (`INTERNET`) – Kommunikation mit Firebase
- **Netzwerkstatus** (`ACCESS_NETWORK_STATE`) – Prüfung der Internetverbindung

Die App fordert **keine** Berechtigungen für Kamera, Mikrofon, Standort, Kontakte oder andere sensible Gerätedaten an.

### iOS und Desktop
Keine besonderen Geräteberechtigungen über den Internetzugriff hinaus erforderlich.

---

## 10. Datenspeicherung und Löschung

### Speicherdauer
Personenbezogene Daten werden gespeichert, solange Ihr Nutzerkonto besteht bzw. solange die Daten für den Zweck der Verarbeitung erforderlich sind.

### Löschungsmöglichkeiten
- **Nutzerkonto:** Kann jederzeit in der App gelöscht werden. Dabei werden die Kontodaten aus Firebase Authentication und die zugehörigen Nutzerdaten aus Firestore entfernt.
- **Teilnehmerdaten:** Einzelne Teilnehmer können jederzeit gelöscht werden. Die Löschung umfasst auch die Zuordnung zu Veranstaltungen und Mahlzeiten.
- **Veranstaltungsdaten:** Veranstaltungen können mitsamt aller zugehörigen Daten (Mahlzeiten, Einkaufslisten, Materialien, Teilnehmerzuordnungen) gelöscht werden.

---

## 11. Datensicherheit in der App

- Alle Datenübertragungen zwischen App und Firebase erfolgen **verschlüsselt** (TLS/SSL)
- Passwörter werden durch Firebase Authentication **gehasht** gespeichert
- Datenzugriffe sind auf die eigene Organisation (Stamm/Gruppe) beschränkt — Nutzer können nur Daten ihrer eigenen Gruppe einsehen und bearbeiten
- Es werden **keine Geräte-IDs, Werbe-IDs oder Standortdaten** erhoben

---

## 12. Rechte der betroffenen Personen

Die in der allgemeinen Datenschutzerklärung aufgeführten Betroffenenrechte (Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit, Widerspruch) gelten auch für die in der App verarbeiteten Daten.

Zur Ausübung Ihrer Rechte wenden Sie sich bitte an: **kontakt@bockbücher.de**

---

*Stand: März 2026*
*Quelle der allgemeinen Datenschutzerklärung: [https://www.e-recht24.de](https://www.e-recht24.de)*
