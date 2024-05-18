import processing.core.PApplet
import processing.core.PVector

fun main() {
    PApplet.runSketch(arrayOf("Sketch"), Sketch)
}

object Sketch : PApplet() {
    // Internal classes
    data class Edge(val from: Int, val to: Int)

    // Extensions
    operator fun PVector.plus(u: PVector): PVector = PVector.add(this, u)
    operator fun PVector.minus(u: PVector): PVector = PVector.sub(this, u)
    operator fun PVector.unaryMinus(): PVector = PVector(-x, -y, -z)
    private infix fun PVector.lineTo(u: PVector): Unit = line(x, y, u.x, u.y)

    // Constants
    private const val VARIANT = 3106
    private val n = intArrayOf(0, 3, 1, 0, 6)
    private val k = 1.0 - n[3] * 0.01 - n[4] * 0.005 - 0.15
    private val N = n[3] + 10

    // Don't forget to seed RNG!
    init {
        randomSeed(VARIANT.toLong())
    }

    // Graph data
    private val matrix = Array(N) { BooleanArray(N) { 1 <= random(2F) * k } }
    private val unimatrix = Array(N) { i -> BooleanArray(N) { j -> matrix[i][j] || matrix[j][i] } }

    private val points: Array<PVector> =
        (1..<N).map { TWO_PI * it / (N - 1) }.map { PVector(cos(it) * 280, sin(it) * 280) }.toTypedArray() + PVector(
            0F, 0F
        )

    // Global state
    private var directed = true

    private val visitedEdges = Array(N) { BooleanArray(N) { false } }
    private val visitedNodes = BooleanArray(N) { false }
    private val toVisit = ArrayDeque<Edge>()

    // Search routines
    private fun initSearch() {
        for (i in visitedEdges.indices) for (j in visitedEdges[i].indices) visitedEdges[i][j] = false
        for (i in visitedNodes.indices) visitedNodes[i] = false
        toVisit.clear()
        toVisit.add(matrix.withIndex().find { (_, bools) -> bools.any() }!!.index.let { Edge(it, it) })
    }

    private fun breadthSearchStep() {
        if (visitedNodes.all { it }) return
        val m = if (directed) matrix else unimatrix

        var v = toVisit.removeFirstOrNull()
        while (v?.let { visitedNodes[it.to] } == true) v = toVisit.removeFirstOrNull()

        visit(v, m)
    }

    private fun depthSearchStep() {
        if (visitedNodes.all { it }) return
        val m = if (directed) matrix else unimatrix

        var v = toVisit.removeLastOrNull()
        while (v?.let { visitedNodes[it.to] } == true) v = toVisit.removeLastOrNull()

        visit(v, m)
    }

    private fun visit(e: Edge?, m: Array<BooleanArray>) {
        val v = e?.to
        if (v != null) {
            visitedNodes[v] = true
            visitedEdges[e.from][e.to] = true

            val unvisitedNeighbours = m[v].withIndex().filter { (index, b) -> b && !visitedNodes[index] }
                .map { it.index }

            for (n in unvisitedNeighbours) toVisit.add(Edge(v, n))
        } else {
            visitedNodes.withIndex().find { (_, b) -> !b }?.let { toVisit.add(Edge(it.index, it.index)) }
        }
    }

    // Printing routines
    private fun displayMatrix(directed: Boolean = true) {
        val m = if (directed) matrix else unimatrix

        for (row in m) {
            for (i in row) print("${if (i) '1' else '0'} ")
            println()
        }
    }

    // Drawing routines
    override fun settings(): Unit = size(700, 700)

    override fun setup() {
        println("Directed graph:")
        displayMatrix()
        println("\nUndirected graph:")
        displayMatrix(directed = false)

        windowTitle("Circle Graph")

        colorMode(HSB, 360F, 100F, 100F)
        strokeWeight(2F)
        stroke(255)

        textAlign(CENTER, CENTER)
        textSize(40F)
    }

    override fun draw() {
        background(10)
        translate(width / 2F, height / 2F)

        for ((i, point) in points.withIndex()) {
            for ((j, edge) in matrix[i].withIndex()) {
                if (!edge || i == j) continue

                push()
                run {
                    translate(point.x, point.y)
                    if (visitedEdges[i][j] || (!directed && (visitedEdges[i][j] || visitedEdges[j][i]))) stroke(
                        color(
                            0,
                            100,
                            100
                        )
                    )

                    val lineOffset = if (matrix[i][j] && matrix[j][i] && directed) 3F else 0F
                    val end = points[j] - point
                    val dir = -end
                    val offset = dir.copy()
                    offset.setMag(30F)

                    rotate(end.heading())
                    if (directed) arrow(PI, PVector(end.mag() - 30, lineOffset))
                    line(0F, lineOffset, end.mag(), lineOffset)
                }
                pop()
            }
        }

        for ((i, point) in points.withIndex()) {
            push()
            run {
                translate(point.x, point.y)

                if (matrix[i][i]) {
                    noFill()
                    circle(0F, -40.83F, 40F)
                    if (directed) arrow(-1f, PVector(14f, -26.5f))
                }


                fill(100)
                if (visitedNodes[i]) stroke(color(0, 100, 100))
                circle(0F, 0F, 60F)
                fill(255)
                text((i + 1F).toInt(), 0F, 0F)
            }
            pop()
        }
    }

    private fun arrow(phi: Float, p: PVector) {
        p lineTo PVector(p.x + 15 * cos(phi + 0.3F), p.y + 15 * sin(phi + 0.3F))
        p lineTo PVector(p.x + 15 * cos(phi - 0.3F), p.y + 15 * sin(phi - 0.3F))
    }

    override fun keyPressed() {
        directed = if (key == ' ') !directed else directed
        if (key == 's') initSearch() else if (key == 'b') breadthSearchStep() else if (key == 'd') depthSearchStep()
    }
}
