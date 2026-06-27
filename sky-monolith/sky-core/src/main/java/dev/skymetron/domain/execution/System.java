package dev.skymetron.domain.execution;

/**
 * A system in the Entity-Component-System architecture.
 *
 * <p>A system contains behavior and operates over all entities in an
 * {@link EntityStore} that match a component signature. Systems are
 * stateless Spring beans ({@code @Component}) — any shared state should
 * live in the store or in injected services.
 */
public interface System {

    /**
     * Process all matching entities in the store.
     */
    void execute(EntityStore store);
}
