import java.util.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.pow

val start = 0
val end = 14

val startPosition = start
val endPosition = end

val random = Random()

val startPheromone = 1.0
val pheromoneInsertConstant = 10.0
val pheromoneEvaporationConstant = 0.01
val iterationsCount = 10_000
val greediness = 2
val herdInstinct = 2
val antCount = 50

fun main(args: Array<String>) {
    val graph: List<Edge> = initGraph()
    println(graph)
    val dijkstra = solveDijkstra(graph)
    println("Time of Dijkstra: ${dijkstra.third}")
    println("Solution: ${dijkstra.first}")
    val optimalWeight = dijkstra.second.sumBy { it.weight }
    println("Total weight: $optimalWeight")

    val antSolutions = List(100) { solveAnt(graph) }
    println("Min Time of Ants: ${antSolutions.map { it.third }.min()}")
    println("Mean Time of Ants: ${antSolutions.map { it.third }.average()}")
    println("Max Time of Ants: ${antSolutions.map { it.third }.max()}")
    println("Ratio of right solutions for Ants: ${antSolutions.map { it.first }.count { it == dijkstra.first } / antSolutions.size.toDouble()}")
    val averageAntWeight = antSolutions.map { it.second }.map { it.sumBy { it.weight } }.average()
    println("Mean absolute difference between optimal weight: ${averageAntWeight - optimalWeight}")
    println("Mean relative difference between optimal weight: ${(averageAntWeight / optimalWeight) - 1}")
}

fun solveDijkstra(graph: List<Edge>): Triple<List<Int>, List<Edge>, Long> {
    val before = System.currentTimeMillis()
    val q = (start..end).toHashSet()
    val previous = arrayOfNulls<Int>(end + 1)
    val distances = IntArray(end + 1) { Int.MAX_VALUE }
    distances[startPosition] = 0
    while (q.isNotEmpty()) {
        var currentVertex: Int = q.first()
        q.forEach {
            if (distances[it] < distances[currentVertex]) currentVertex = it
        }
        q.remove(currentVertex)
        val neighbors = graph.filter { it.isApplicable(currentVertex) }
        neighbors.forEach {
            val altDistance = distances[currentVertex] + it.weight
            if (altDistance < distances[it.getOpposite(currentVertex)]) {
                distances[it.getOpposite(currentVertex)] = altDistance
                previous[it.getOpposite(currentVertex)] = currentVertex
            }
        }
    }
    val after = System.currentTimeMillis()

    val (positions, passed) = composeDijkstraSolution(previous, graph)
    return Triple(positions, passed, after - before)
}

fun composeDijkstraSolution(references: Array<Int?>, graph: List<Edge>): Pair<List<Int>, List<Edge>> {
    var current = endPosition
    val passedPositions = mutableListOf(current)

    while (current != startPosition) {
        val previous = references[current]
        if (previous != null) {
            passedPositions.add(0, previous)
            current = previous
        } else {
            println("Something went wrong")
            return emptyList<Int>() to emptyList()
        }

    }
    val passed = passedPositions.windowed(2) { (from, to) -> graph.find { it.matches(from, to) }!! }
    return passedPositions to passed
}

fun solveAnt(graph: List<Edge>): Triple<List<Int>, List<Edge>, Long> {
    graph.forEach { it.pheromone = startPheromone }
    val before = System.currentTimeMillis()
    val ants = (0 until antCount).map { Ant() }
    repeat(iterationsCount) {
        ants.forEach { it.move(graph) }
        graph.forEach { it.pheromone *= 1 - pheromoneEvaporationConstant }
    }
    val after = System.currentTimeMillis()
    val (positions, passed) = composeAntSolution(graph)
    return Triple(positions, passed, after - before)
}

fun composeAntSolution(graph: List<Edge>): Pair<List<Int>, List<Edge>> {
    var current = startPosition
    val passedPositions = mutableListOf(current)
    val passed = mutableListOf<Edge>()
    while (current != endPosition) {
        val edge = graph.filter { !passed.contains(it) && it.isApplicable(current) }.maxBy { it.pheromone }
        if (edge != null) {
            passed += edge
            current = edge.getOpposite(current)
            passedPositions += current
        } else {
            println("Something went wrong")
            return emptyList<Int>() to emptyList()
        }
    }
    return passedPositions to passed
}

fun initGraph() = buildSequence {
    (start..end).forEach { from ->
        (from..end).filter { from != it }.forEach { yield(Edge(from, it, random.nextInt(100) + 21)) }
    }
}.toList()

class Edge(private val from: Int, private val to: Int, val weight: Int, var pheromone: Double = startPheromone) {
    fun matches(from: Int, to: Int) = (from == this.from && to == this.to) || (from == this.to && to == this.from)
    fun isApplicable(point: Int) = point == from || point == to
    fun getOpposite(point: Int) = if (point == from) to else if (point == to) from else -1
    fun getProbability(edges: List<Edge>): Double = getProbabilityConstant() / edges.sumByDouble { it.getProbabilityConstant() }
    override fun toString() = "(from: $from, to: $to, w: $weight)"
    private fun getProbabilityConstant() = pheromone.pow(herdInstinct) * (1.0 / weight).pow(greediness)
}

class Ant(private var currentPosition: Int = startPosition, private val passedEdges: MutableList<Edge> = mutableListOf()) {
    fun move(graph: List<Edge>) {
        val edges = graph.filter { it.isApplicable(currentPosition) }.filter { !passedEdges.contains(it) }
        val nextEdge = edges.maxBy { it.getProbability(edges) * random.nextDouble() }
        if (nextEdge == null) {
            restart()
            return
        }
        currentPosition = nextEdge.getOpposite(currentPosition)
        passedEdges += nextEdge
        if (currentPosition == endPosition) {
            putPheromone()
            restart()
        }
    }

    private fun putPheromone() {
        passedEdges.forEach { it.pheromone += pheromoneInsertConstant / passedEdges.sumBy { it.weight } }
    }

    private fun restart() {
        currentPosition = startPosition
        passedEdges.clear()
    }
}