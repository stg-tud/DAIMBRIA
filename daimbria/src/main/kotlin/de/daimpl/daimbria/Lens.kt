package de.daimpl.daimbria

/**
 * A Lens defines schema migrations applied to certain schema version to transform into another version.
 * This can be seen as an edge from [source] version to [destination] version in the [LensGraph].
 *
 * @param source schema version to apply migrations to
 * @param destination schema version where migrations have been applied already
 * @param operations list of lens operations which define the migration
 */
data class Lens(val source: String, val destination: String, val operations: List<LensOp>) {
    fun reverse(): Lens = Lens(destination, source, operations.map(LensOp::reverse).reversed())
}
