package dev.nesto.port.in;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import dev.nesto.domain.NodeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SetNodeStatusUseCaseTest {

  @Nested
  @DisplayName("SetNodeStatusUseCase.Command")
  class CommandConstructor {

    @Test
    @DisplayName("should reject null nodeId")
    void should_reject_null_node_id() {
      assertThatNullPointerException()
          .isThrownBy(() -> new SetNodeStatusUseCase.Command(null, Optional.empty()));
    }

    @Test
    @DisplayName("should reject null status")
    void should_reject_null_status() {
      assertThatNullPointerException()
          .isThrownBy(() -> new SetNodeStatusUseCase.Command(NodeMother.NODE_ID, null));
    }
  }
}
