package dev.skymetron.domain.execution;

/**
 * Marker interface for ECS components.
 *
 * <p>A component is a pure data holder (typically a Java record) attached to
 * an entity. Components have no behavior — that lives in {@link System}s.
 *
 * <p>Implementations should be immutable records so that component state is
 * replaced (not mutated) on update.
 */
public interface Component {
}
