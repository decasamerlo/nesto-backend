package dev.nesto.domain;

import java.util.Objects;

public record NodeId(String value) {

  public NodeId {
    Objects.requireNonNull(value, "NodeId must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("NodeId must not be blank");
    }
  }

  public static NodeId of(String value) {
    return new NodeId(value);
  }
}
