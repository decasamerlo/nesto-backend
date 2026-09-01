package dev.nesto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PositionTest {

  @Nested
  @DisplayName("Position.of")
  class Of {

    @Test
    @DisplayName("should accept zero")
    void should_accept_zero() {
      var position = Position.of(0);

      assertThat(position.value()).isZero();
    }

    @Test
    @DisplayName("should accept positive values")
    void should_accept_positive_values() {
      var position = Position.of(42);

      assertThat(position.value()).isEqualTo(42);
    }

    @Test
    @DisplayName("should reject negative values")
    void should_reject_negative_values() {
      assertThatIllegalArgumentException().isThrownBy(() -> Position.of(-1));
    }
  }
}
