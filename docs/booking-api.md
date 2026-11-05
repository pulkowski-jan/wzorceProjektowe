# API klientów i rezerwacji

Implementacja rozwija dwa zadania z README: zarządzanie rejestrem klientów oraz zapis
i odczyt koszyka rezerwacji. Zachowuje architekturę kontroler–usługa–repozytorium,
JDBC/SQLite i tekstową reprezentację kwot `Money`.

## Klienci

Obiekt klienta: `{"id":"UUID","name":"Jan Kowalski","email":"jan@example.org"}`.
Klient jest wpisem w rejestrze kupujących, nie kontem z hasłem.

| Metoda i ścieżka | Żądanie | Odpowiedź |
| --- | --- | --- |
| `POST /clients` | `{"name":"Jan Kowalski","email":"jan@example.org"}` | `201`, obiekt klienta, nagłówek `Location` |
| `GET /clients` | Brak | `200`, lista klientów |
| `GET /clients/{id}` | Brak | `200`, obiekt klienta; `404`, gdy nie istnieje |
| `PUT /clients/{id}` | Pełne `name` i `email` | `200`, zaktualizowany klient; `404`, gdy nie istnieje |
| `DELETE /clients/{id}` | Brak | `204`; `404`, gdy nie istnieje; `409`, gdy ma rezerwacje |

Identyfikator generuje serwer. `name` po usunięciu otaczających białych znaków ma
1–255 znaków. `email` jest wymagany, ma maksymalnie 254 znaki i podlega podstawowej
walidacji składniowej (bez weryfikacji dostarczalności). Otaczające białe znaki
są usuwane; wielkość liter pozostaje niezmieniona. Brak wymaganych pól lub błędne
wartości daje `400`. Aktualizacja nie zmienia UUID. Adres e-mail nie jest unikalnym
kluczem: dokumentacja nie wymaga blokowania wielu klientów o tym samym adresie.

Klient powiązany z rezerwacją nie może zostać usunięty. Można zmienić jego dane
kontaktowe; rezerwacja przechowuje jego identyfikator, a nie kopię danych klienta.
Listy nie mają jeszcze paginacji.

## Rezerwacje

| Metoda i ścieżka | Żądanie | Odpowiedź |
| --- | --- | --- |
| `POST /reservations` | UUID klienta i niepusta lista pozycji | `201`, rezerwacja, nagłówek `Location` |
| `GET /reservations/{id}` | Brak | `200`, rezerwacja; `404`, gdy nie istnieje |
| `GET /reservations?clientId={id}` | UUID istniejącego klienta | `200`, jego rezerwacje, także pusta lista; `404`, gdy klient nie istnieje |

Przykładowe żądanie utworzenia:

```json
{
  "clientId": "11111111-1111-4111-8111-111111111111",
  "items": [
    {
      "offerId": "22222222-2222-4222-8222-222222222222",
      "quantity": 2,
      "participant": {
        "name": "Jan Kowalski",
        "email": "jan@example.org"
      }
    }
  ]
}
```

UUID-y muszą wskazywać istniejące rekordy. `quantity` jest dodatnią liczbą całkowitą
z zakresu typu Java `int`; suma ilości dla jednej oferty również musi mieścić się
w tym zakresie. `participant` można pominąć lub ustawić na `null`. Jeśli go podano,
`name` jest wymagane i walidowane jak nazwa klienta; `email` jest opcjonalne, ale
podana wartość musi mieć poprawną składnię.

Serwer zwraca:

```json
{
  "id": "33333333-3333-4333-8333-333333333333",
  "clientId": "11111111-1111-4111-8111-111111111111",
  "items": [
    {
      "lineId": "44444444-4444-4444-8444-444444444444",
      "offerSnapshot": {
        "id": "22222222-2222-4222-8222-222222222222",
        "name": "Koncert",
        "vendor": {"name": "Organizator"},
        "price": {"amount": "100.00", "currency": "PLN"},
        "description": "Bilet",
        "quantity": 10,
        "version": 1
      },
      "quantity": 2,
      "timestamp": "2026-11-05T12:00:00Z",
      "status": "RESERVED",
      "participant": {"name": "Jan Kowalski", "email": "jan@example.org"}
    }
  ],
  "createTimestamp": "2026-11-05T12:00:00Z",
  "lastModifiedTimestamp": "2026-11-05T12:00:00Z"
}
```

Całe `offerSnapshot`, UUID-y, status i czas pochodzą z serwera. Dodatkowe pola
żądania nie pozwalają nadpisać cen, zrzutów ani statusu. `offerSnapshot.quantity`
oznacza dostępność przed zakupem, natomiast `items[].quantity` liczbę rezerwowanych
sztuk. Powtarzająca się oferta zachowuje osobne pozycje, np. dla różnych uczestników,
ale ma wspólny zrzut i jedną zagregowaną operację odjęcia zapasu.

Każda pozycja powstaje ze statusem `RESERVED`. Model przewiduje również `PAID`
i `CANCELLED`, ale obecne API nie udostępnia zmiany statusu, płatności ani anulowania.
Znaczniki czasu są wartościami UTC `Instant`, z dokładnością do milisekund.
Przy utworzeniu wszystkie są równe; wersja rezerwacji w bazie wynosi `1` i nie
jest eksponowana przez API. `lastModifiedTimestamp` pozostaje równy czasowi utworzenia,
ponieważ edycja koszyka nie jest częścią tego zadania.

## Spójność i błędy

- **Transakcja:** utworzenie rekordu, odjęcie wszystkich ilości i zapis JSON pozycji
  wykonują się w jednej transakcji. Dowolny błąd cofa wszystkie te operacje.
- **SQLite:** pierwszą operacją bazodanową jest `INSERT … SELECT` rezerwacji dla
  istniejącego klienta. Uzyskuje blokadę zapisu przed odczytem ofert, zamiast
  podnosić blokadę odczytu do zapisu. Pusty koszyk roboczy nie jest zatwierdzany.
  Nie ma blokady JVM, więc ochrona nie zależy od jednej instancji aplikacji.
- **Inventory:** warunkowy `UPDATE` sprawdza ilość i wersję oferty, odejmuje zapas
  i zwiększa wersję. Starszy zapis katalogu nie może nadpisać tego odjęcia.
  Jednoosobowy zapas (`quantity=1`) może zostać przydzielony tylko raz.
- **Historia:** `offer_items_data` przechowuje JSON całych pozycji. Odczyt nie
  odtwarza ofert z aktualnego katalogu, więc późniejsze zmiany ceny, opisu czy
  usunięcie oferty nie zmieniają rezerwacji.
- **Relacje:** klucze obce SQLite są włączane na każdym połączeniu produkcyjnym.
  Usunięcie klienta i tworzenie rezerwacji są serializowane przez bazę.
- **Kody:** `400` oznacza niepoprawne żądanie; `404` brak klienta, oferty lub
  rezerwacji; `409` brak zapasu, konflikt wersji lub próbę usunięcia klienta
  z rezerwacjami. Niepoprawny identyfikator w ścieżce odczytu daje `404`;
  niepoprawny UUID w żądaniu tworzenia daje `400`.

## Uruchomienie i przykład

Wymagane: Java 21. Polecenia wykonuje się z katalogu głównego repozytorium.

```sh
./gradlew test
./gradlew bootRun
```

Przykład wymaga `curl` i `jq`; domyślny port to `8081`.

```sh
base=http://localhost:8081
client_id=$(curl -fsS "$base/clients" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Jan Kowalski","email":"jan@example.org"}' | jq -r .id)
offer_id=$(curl -fsS "$base/offer/create" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Koncert","vendor":{"name":"Organizator"},"price":{"amount":"100.00","currency":"PLN"},"quantity":10}' \
  | jq -r .id)
reservation=$(curl -fsS "$base/reservations" \
  -H 'Content-Type: application/json' \
  -d "$(jq -n --arg client "$client_id" --arg offer "$offer_id" \
    '{clientId:$client,items:[{offerId:$offer,quantity:2}]}')")
reservation_id=$(printf '%s' "$reservation" | jq -r .id)
curl -fsS "$base/reservations/$reservation_id"
```

## Granice implementacji

Reguły ofert grupowych/imiennych/nieimiennych, konkretne miejsca, płatności,
saldo, anulowanie, zwolnienie zapasu i wygasanie, zależności ofert oraz edycja
uczestników pozostają osobnymi zadaniami z README. Aktualne rezerwacje zajmują zapas
bezterminowo. Ponowiony `POST` tworzy nową rezerwację; brak jeszcze kluczy idempotencji.

Domyślna inicjalizacja demonstracyjna usuwa dane przy każdym starcie. Po pierwszym
utworzeniu schematu można wyłączyć ją przez `SPRING_SQL_INIT_MODE=never`; istniejące
instalacje wymagają osobnego zarządzania migracjami. W projekcie nie ma
uwierzytelniania, autoryzacji ani izolacji danych użytkowników. Endpointów z danymi
osobowymi nie należy wystawiać publicznie.
