package dev.nesto.domain;

public record Position(int value) {

  public Position {
    if (value < 0) {
      throw new IllegalArgumentException("Position must not be negative");
    }
  }

  public static Position of(int value) {
    return new Position(value);
  }
}
