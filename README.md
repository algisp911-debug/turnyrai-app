# ⚽ Turnyrai – Android programėlė

Programėlė vaikų futbolo grupėms: pagal **grupės kodą** atidaro tos grupės taurių eilę.
Kiekviena grupė savo duomenis laiko savo Google paskyroje (Google Sheets + Apps Script), o ši programėlė tik leidžia patogiai prie jų prisijungti.

## Parsisiųsti

**[Turnyrai.apk – naujausia versija](https://github.com/algisp911-debug/turnyrai-app/releases/latest/download/Turnyrai.apk)**

Įdiegimas Android telefone:
1. Atsidarykite nuorodą telefone ir parsisiųskite `Turnyrai.apk`.
2. Atidarykite parsisiųstą failą. Jei telefonas paklaus, leiskite diegti programas iš šio šaltinio (naršyklės).
3. Atidarykite programėlę **Turnyrai** ir įveskite grupės kodą, kurį gavote iš trenerio.

iPhone šios programėlės įdiegti negalima. iPhone naudotojai atsidaro grupės nuorodą naršyklėje ir prideda ją prie pradžios ekrano.

## Kaip pridėti naują grupę

1. Grupė įsidiegia savo lentelę ir programėlę pagal instrukciją (rinkinys „Turnyrai_programele.zip“).
2. Į failą [`grupes.json`](grupes.json) įrašoma eilutė su grupės kodu, pavadinimu ir `/exec` nuoroda:

```json
{
  "grupes": {
    "ZALGIRIS2018": { "pavadinimas": "Žalgiris 2018", "url": "https://script.google.com/macros/s/…/exec" }
  }
}
```

3. Išsaugojus failą, naujas kodas programėlėje veikia iš karto. APK iš naujo diegti nereikia.

Grupės kodas neskiria didžiųjų ir mažųjų raidžių. Vietoj kodo galima įklijuoti ir pilną `/exec` nuorodą.

## Kaip naudotis

- **Atgal** mygtukas pagrindiniame lange atidaro meniu: *Atnaujinti*, *Keisti grupę*, *Išeiti*.
- Duomenis keisti gali tik žinantys tos grupės PIN kodą (įvedamas programėlės viršuje, „🔒 Peržiūra“).

## Techninė informacija

- Kodas: `android/` (Java, be papildomų bibliotekų, minSdk 24).
- APK surenkamas automatiškai per GitHub Actions (`.github/workflows/build-apk.yml`), kai pasikeičia `android/` aplankas arba paleidus rankiniu būdu. Rezultatas keliamas į **Releases**, todėl Actions vietos neužima.
- APK pasirašomas raktu, saugomu repozitorijos paslaptyse (`KEYSTORE_BASE64` ir `KEYSTORE_PASSWORD`). **Rakto nepameskite**: be jo naujų versijų nebus galima įdiegti ant senųjų.
