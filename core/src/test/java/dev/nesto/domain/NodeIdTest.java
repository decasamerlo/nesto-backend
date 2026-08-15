package dev.nesto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NodeIdTest {

  @Nested
  @DisplayName("NodeId.of")
  class Of {

    @Test
    @DisplayName("should accept a non blank string")
    void should_accept_a_non_blank_string() {
      var id = NodeId.of("01JZ5Z9WX6X1Y45C7ZHYV8NQKD");

      assertThat(id.value()).isEqualTo("01JZ5Z9WX6X1Y45C7ZHYV8NQKD");
    }

    @Test
    @DisplayName("should reject null values")
    void should_reject_null_values() {
      assertThatNullPointerException().isThrownBy(() -> NodeId.of(null));
    }

    @Test
    @DisplayName("should reject blank values")
    void should_reject_blank_values() {
      assertThatIllegalArgumentException().isThrownBy(() -> NodeId.of("   "));
    }
  }
}
