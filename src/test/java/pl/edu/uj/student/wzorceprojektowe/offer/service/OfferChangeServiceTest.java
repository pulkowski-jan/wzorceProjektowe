package pl.edu.uj.student.wzorceprojektowe.offer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

@ExtendWith(MockitoExtension.class)
class OfferChangeServiceTest {
  @Mock private OfferRepository repository;
  @InjectMocks private OfferChangeService service;

  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void invalidQuantityIsRejectedBeforeAnyDatabaseAccess(int quantity) {
    assertThatThrownBy(() -> service.reserveQuantity("offer-id", quantity))
        .isInstanceOf(RequestValidationException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void insufficientStockIsRejectedInJavaWithoutAttemptingAnUpdate() {
    when(repository.findById("offer-id")).thenReturn(Optional.of(offer(2)));

    assertThatThrownBy(() -> service.reserveQuantity("offer-id", 3))
        .isInstanceOf(RequestConflictException.class);

    verify(repository, never()).update(any());
  }

  @Test
  void missingOfferIsReportedWithoutAttemptingAnUpdate() {
    when(repository.findById("offer-id")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reserveQuantity("offer-id", 1))
        .isInstanceOf(OfferNotFoundException.class);

    verify(repository, never()).update(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void returnsOriginalSnapshotAndUsesVersionedUpdate(int quantity) {
    Offer original = offer(3);
    when(repository.findById("offer-id")).thenReturn(Optional.of(original));

    assertThat(service.reserveQuantity("offer-id", quantity)).isEqualTo(original);

    verify(repository).update(offer(3 - quantity));
  }

  @Test
  void concurrentChangesAreNotSilentlyOverwritten() {
    when(repository.findById("offer-id")).thenReturn(Optional.of(offer(3)));
    when(repository.update(any())).thenThrow(new OptimisticLockingFailureException("stale version"));

    assertThatThrownBy(() -> service.reserveQuantity("offer-id", 1))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  private Offer offer(int quantity) {
    return new Offer("offer-id", "Concert", new Vendor("Vendor"),
        new Money("PLN", "10.00"), "", quantity, 7);
  }
}
