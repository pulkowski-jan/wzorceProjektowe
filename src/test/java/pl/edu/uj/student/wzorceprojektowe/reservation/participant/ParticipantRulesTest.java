package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.*;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ParticipantRulesTest {
  private static final Offer OFFER =
      new Offer("offer-1", "Concert", new Vendor("V"), new Money("PLN", "10.00"), "", 10, 1);

  private static List<Participant> people(String... names) {
    List<Participant> result = new ArrayList<>();
    Arrays.stream(names).forEach(name -> result.add(name == null ? null : new Participant(name)));
    return result;
  }

  @Nested
  class Named {
    private final ParticipantRule rule = new NamedParticipantRule(new PolicyExecutor<>());

    @Test
    void acceptsOneValidParticipantPerTicket() {
      assertThatCode(() -> rule.validate(OFFER, 2, people("Ada", "Alan"))).doesNotThrowAnyException();
    }

    @Test
    void acceptsDuplicateNamesBecauseTwoPeopleCanShareAName() {
      assertThatCode(() -> rule.validate(OFFER, 2, people("Ada", "Ada"))).doesNotThrowAnyException();
    }

    @Test
    void acceptsNameOfMaximumLength() {
      assertThatCode(() -> rule.validate(OFFER, 1, people("x".repeat(255))))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void rejectsWrongNumberOfParticipants(int given) {
      List<Participant> participants = people(new String[given]);
      for (int i = 0; i < given; i++) {
        participants.set(i, new Participant("Person " + i));
      }
      assertThatThrownBy(() -> rule.validate(OFFER, 2, participants))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("requires 2 participant(s)")
          .hasMessageContaining("%d given".formatted(given));
    }

    @Test
    void rejectsInvalidParticipants() {
      assertThatThrownBy(() -> rule.validate(OFFER, 1, people((String) null)))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("must not be null");
      assertThatThrownBy(() -> rule.validate(OFFER, 1, List.of(new Participant(null))))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("name must not be blank");
      assertThatThrownBy(() -> rule.validate(OFFER, 1, people("   ")))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("name must not be blank");
      assertThatThrownBy(() -> rule.validate(OFFER, 1, people("x".repeat(256))))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("may not exceed 255");
    }

    @Test
    void reportsItsMode() {
      assertThat(rule.mode()).isEqualTo(ParticipantMode.NAMED);
    }
  }

  @Nested
  class WithoutParticipants {
    @ParameterizedTest
    @ValueSource(strings = {"NONE", "GROUP"})
    void acceptEmptyListAndRejectAnyParticipant(String modeName) {
      ParticipantMode mode = ParticipantMode.valueOf(modeName);
      ParticipantRule rule =
          mode == ParticipantMode.NONE ? new NoneParticipantRule() : new GroupParticipantRule();

      assertThat(rule.mode()).isEqualTo(mode);
      assertThatCode(() -> rule.validate(OFFER, 3, List.of())).doesNotThrowAnyException();
      assertThatThrownBy(() -> rule.validate(OFFER, 1, people("Ada")))
          .isInstanceOf(RequestValidationException.class)
          .hasMessageContaining("does not accept participants")
          .hasMessageContaining(modeName);
    }
  }

  @Nested
  class Registry {
    @Test
    void picksTheRuleForEachMode() {
      var registry =
          new ParticipantRuleRegistry(
              List.of(
                  new GroupParticipantRule(),
                  new NamedParticipantRule(new PolicyExecutor<>()),
                  new NoneParticipantRule()));

      for (ParticipantMode mode : ParticipantMode.values()) {
        assertThat(registry.forMode(mode).mode()).isEqualTo(mode);
      }
    }

    @Test
    void refusesToStartWhenAModeHasNoRule() {
      assertThatThrownBy(
          () ->
              new ParticipantRuleRegistry(
                  List.of(new NoneParticipantRule(), new GroupParticipantRule())))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("NAMED");
    }

    @Test
    void refusesToStartWhenAModeHasTwoRules() {
      assertThatThrownBy(
          () ->
              new ParticipantRuleRegistry(
                  List.of(
                      new NoneParticipantRule(),
                      new NoneParticipantRule(),
                      new GroupParticipantRule(),
                      new NamedParticipantRule(new PolicyExecutor<>()))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Duplicate")
          .hasMessageContaining("NONE");
    }
  }
}
