package dev.nesto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.stream.Stream;
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

  @Nested
  @DisplayName("NodeId.compareTo")
  class CompareTo {

    // The contract suite's tied ids, plus ab, handed in reverse. B before a, where a case-blind
    // order puts a first, a before ab, a prefix first as COLLATE "C" has it, and U+FF5E before
    // U+1F600, where String's UTF-16 order puts U+1F600 first
    @Test
    @DisplayName("should order by code point")
    void should_order_by_code_point() {
      var ids = Stream.of("\uD83D\uDE00", "\uFF5E", "c", "ab", "a", "B").map(NodeId::of).sorted();

      assertThat(ids)
          .extracting(NodeId::value)
          .containsExactly("B", "a", "ab", "c", "\uFF5E", "\uD83D\uDE00");
      assertThat(NodeId.of("a")).isEqualByComparingTo(NodeId.of("a"));
    }
  }
}
