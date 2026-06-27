package dev.skymetron.domain.execution;

/**
 * An entity in the Entity-Component-System architecture.
 *
 * <p>An entity is a bare identifier — it carries no data of its own.
 * All state is held by {@link Component}s attached to it via an
 * {@link EntityStore}. Systems operate on entities that have a
 * specific set of components.
 */
public interface Entity {

    EntityId id();
}
