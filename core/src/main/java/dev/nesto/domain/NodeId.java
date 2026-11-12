package dev.nesto.domain;

import java.util.Arrays;
import java.util.Objects;

public record NodeId(String value) implements Comparable<NodeId> {

  public NodeId {
    Objects.requireNonNull(value, "NodeId must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("NodeId must not be blank");
    }
  }

  public static NodeId of(String value) {
    return new NodeId(value);
  }

  // By code point, the order both stores read tied siblings back in: COLLATE "C" orders id so in
  // Postgres. String's own order compares UTF-16 units, which puts a character above U+FFFF before
  // one in U+E000 to U+FFFF
  @Override
  public int compareTo(NodeId other) {
    return Arrays.compare(value.codePoints().toArray(), other.value.codePoints().toArray());
  }
}
