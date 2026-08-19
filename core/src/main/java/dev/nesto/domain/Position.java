package dev.nesto.domain;

public record Position(int value) implements Comparable<Position> {

  public Position {
    if (value < 0) {
      throw new IllegalArgumentException("Position must not be negative");
    }
  }

  public static Position of(int value) {
    return new Position(value);
  }

  @Override
  public int compareTo(Position other) {
    return Integer.compare(value, other.value);
  }
}
