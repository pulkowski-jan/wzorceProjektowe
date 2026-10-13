# Wzorce Projektowe Projekt

REST API do rezerwowania biletów na wydarzenia (koncerty spektakle itp.)

# TODO
## Baza 

- [x] Inicjalizacja bazy danych i środowiska

      Skrypt SQL do tworzenia struktury tabel przy każdym uruchomieniu aplikacji.

- [x] Katalog dostępnych wydarzeń

      read/write dostęp do listy ofert.

- [ ] Zarządzanie rejestrem klientów

      moduł do obsługi danych kupujących

## Rezerwacje i Reguły Biznesowe

- [ ] Zapis i odczyt koszyka rezerwacji
      
      Zaimplementować tworzenie rezerwacji zawierającej oferty w momencie zakupu
      (ofertę zapakować we wrapper z timestampem i statusem (zaklepane/opłacone/anulowane) ilością sztuk, oraz opcjonalnymi danymi uczestnika 
      i dump listy tych itemów do bazy).

- [ ] Weryfikacja reguł uczestników

      Stworzyć mechanizm sprawdzający poprawność danych uczestnikóœ w zależności od typu oferty 
      (dla grupy (wtedy wystarczą dane klienta), imienne (dane osoby per item), nieimienny (dane niewymagane))

- [ ] Obsługa współbieżności i inventory

      Zabezpieczyć proces rezerwacji przed jednoczesnym zakupem tego samego miejsca przez wielu użytkowników i ponad inventory.

## Stany i Płatności

- [ ] Księgowanie transakcji płatniczych

      Stworzyć algorytm wyliczania aktualnego salda rezerwacji.

- [ ] Automatyczne potwierdzanie zamówień po płatności

       Obsłużyć zdarzenia finansowe i automatyczną zmianę stanu rezerwacji na opłaconą po zarejestrowaniu pełnej wpłaty.

- [ ] Asynchroniczne wygasanie nieopłaconych rezerwacji

      "cronjob" anulujący rezerwacje przekraczające limit czasu na płatność.

## Oferty pochodne – Miejsca i Zniżki

- [ ] Weryfikacja zależności między ofertami

      Zaimplementować katalog model i reguły dla ofert wymagających posiadania usługi bazowej (np. zniżka lub wybór miejsca).

- [ ] Kaskadowe usuwanie ofert pochodnych

      Anulowanie oferty bazowej musi wywołać anulowanie jej pochodnych

- [ ] Edycja danych uczestników na bilecie

      Udostępnić możliwość aktualizacji danych przypisanych do biletu o ile pozwala oferta.
