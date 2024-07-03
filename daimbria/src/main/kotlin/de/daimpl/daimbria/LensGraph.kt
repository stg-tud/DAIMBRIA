package de.daimpl.daimbria

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph

fun lensGraph(rootLens: Lens, block: LensGraph.() -> Unit): LensGraph {
    val graph = LensGraph(rootLens)
    graph.block()
    return graph
}

typealias LensVertex = Pair<String, String>

class LensGraph(initLens: Lens) {
    private val graph: DirectedMultigraph<String, DefaultEdge> = DirectedMultigraph(DefaultEdge::class.java)
    private val lensMap: MutableMap<LensVertex, Lens> = mutableMapOf()
    private val emptyVertex = initLens.source

    init {
        graph.addVertex(initLens.source)
        graph.addVertex(initLens.destination)
        graph.addEdge(initLens.source, initLens.destination)
        lensMap[initLens.source to initLens.destination] = initLens
    }

    operator fun Lens.unaryPlus() = registerLens(this)

    private fun registerLens(lens: Lens) {
        val (from, to) = lens.source to lens.destination

        if (!graph.containsVertex(from)) {
            throw IllegalArgumentException("Unknown schema: $from")
        }
        if (graph.containsEdge(from, to)) {
            return
        }

        val historyBeforeNewLens = lensFromTo(emptyVertex, lens.source)
        val historyWithNewLens = historyBeforeNewLens + lens.operations
        // todo use model for error message?
        val jsonRepresentation = LensDslValidation.createJsonRepresentation(
            validationModel = JsonNodeFactory.instance.objectNode(),
            ops = historyWithNewLens
        )

        // add target version as vertex if not existent already
        if (!graph.containsVertex(to)) {
            graph.addVertex(to)
        }

        // add edges in both directions
        graph.addEdge(from, to)
        graph.addEdge(to, from)
        lensMap[from to to] = lens
        lensMap[to to from] = lens.reverse()
    }

    fun lensFromTo(from: String, to: String): List<LensOp> {
        require(!(!graph.containsVertex(from) || !graph.containsVertex(to))) {
            "Couldn't find schema in graph: $from or $to"
        }

        // todo use lookup instead of costly path algorithm + op concatenation

        val dijkstraAlg = DijkstraShortestPath(graph)
        val path = dijkstraAlg.getPath(from, to) ?: throw IllegalArgumentException("No path found from $from to $to")

        return buildList {
            path.edgeList.forEach { edge ->
                val source = graph.getEdgeSource(edge)
                val target = graph.getEdgeTarget(edge)
                val lens = lensMap[source to target]
                    ?: throw IllegalStateException("Lens not found for edge: $source to $target")
                addAll(lens.operations)
            }
        }
    }
}
