package simplex

import java.util.*
import kotlin.collections.ArrayList

fun main(args: Array<String>) {
    val sc = Scanner(System.`in`)
    val nOfVariables = sc.nextInt()
    val nOfRows = sc.nextInt()
    val targetFunction = IntArray(nOfVariables)
    for (i in 0 until nOfVariables) {
        targetFunction[i] = sc.nextInt()
    }
    val simplexMatrix = Array(nOfRows + 1) { DoubleArray(nOfVariables + 1) }
    for (i in 0 until nOfRows) {
        for (j in 0 until nOfVariables) {
            simplexMatrix[i][j] = sc.nextDouble()
        }
        simplexMatrix[i][nOfVariables] = sc.nextDouble()
    }
    var q = 0.0
    if (sc.hasNext()) {
        q = sc.nextDouble()
    }
    sc.close()
    simplexMatrix[nOfRows][nOfVariables] = q
    System.arraycopy(targetFunction, 0, simplexMatrix[nOfRows], 0, nOfVariables)
    var recalcTable = recalcTable(simplexMatrix, nOfRows, nOfVariables)
    var i = 0
    while (simplexMatrix[nOfRows].any { it < 0 } && i < 100) {
        recalcTable = recalcTable(recalcTable, nOfRows, nOfVariables)
        i++
    }
    println((1..nOfVariables).zip(recalcTable[nOfRows].asList()).map { it.first * it.second }.sum()
            + recalcTable[nOfRows][nOfVariables])
}

private fun recalcTable(simplexMatrix: Array<DoubleArray>, nOfRows: Int, nOfVariables: Int): Array<DoubleArray> {
    val min = simplexMatrix[nOfRows].minBy { it < 0 }
    val column = simplexMatrix[nOfRows].indexOfFirst { it == simplexMatrix[nOfRows].find { it == min } }
    val coeffs = ArrayList<Double>()
    simplexMatrix[nOfVariables].forEachIndexed { index, i ->
        if (simplexMatrix[column][index] != 0.0 && i > 0) {
            coeffs.add(simplexMatrix[nOfVariables][index] / simplexMatrix[column][index])
        } else {
            coeffs.add(Double.MAX_VALUE)
        }
    }
    val newMatrix = Array(nOfRows + 1) { DoubleArray(nOfVariables + 1) }

    val row = coeffs.indexOf(coeffs.min())
    val elem = simplexMatrix[row][column]
    simplexMatrix[row][column] = 1 / elem
    (0..nOfVariables).forEachIndexed { index, _ ->
        if (index != column) {
            newMatrix[row][index] = simplexMatrix[row][index] / elem
        }
    }
    (0..nOfRows).forEachIndexed { index, _ ->
        if (index != column) {
            newMatrix[row][index] = simplexMatrix[row][index] / elem * -1
        }
    }

    (0..nOfRows).forEachIndexed { i, _ ->
        (0..nOfVariables).forEachIndexed { j, _ ->
            if (i != row && j != column) {
                newMatrix[i][j] = (simplexMatrix[i][j] * elem -
                        simplexMatrix[row][j] * simplexMatrix[i][column]) / elem
            }
        }
    }
    return newMatrix
}
