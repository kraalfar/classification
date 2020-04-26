package bayes

import koma.*
import koma.extensions.*
import java.util.ArrayList

class NaiveBayesCounter(private val alpha: Double = 1e-5) {

    private var classProb: MutableMap<String, Double> = HashMap()
    private var classes: MutableList<String> = ArrayList()
    private var probs: MutableMap<String, koma.matrix.Matrix<Double>> = HashMap()


    fun fit(X: koma.matrix.Matrix<Double>, y: List<String>) {
        for (i in y.indices) {
            for (cls in y[i].toLowerCase().split(", ")) {
                if (!classProb.containsKey(cls)) {
                    classProb[cls] = 1.0 / y.size
                    classes.add(cls)
                    probs[cls] = ones(X.shape()[1] - 2, 11) * alpha
                } else {
                    classProb[cls] = classProb.getValue(cls) + 1.0 / y.size
                }
                for (j in 0 until X.shape()[1] - 2) {
                    val ind:Int = if (X[i, j] < 10.0) X[i, j].toInt() else 10
                    probs.getOrDefault(cls, zeros(0, 0))[j, ind] += 1
                }
            }

        }
        for (cls in classes) {
            var probsMatrix = probs.getOrDefault(cls, zeros(0, 0))
            val den = probsMatrix.getRow(0).elementSum().toInt()
            probsMatrix /= den
            probs[cls] = probsMatrix
        }
    }




    fun logProb(X: koma.matrix.Matrix<Double>): koma.matrix.Matrix<Double> {
        var ans = zeros(X.shape()[0], classes.size)
        for (i in 0 until X.shape()[0]) {
            for (j in classes.indices) {
                val cls = classes[j]
                ans[i, j] = kotlin.math.ln(classProb.getOrDefault(cls, alpha))
                for (k in 0 until X.shape()[1] - 2) {
                    val ind = if (X[i, k] < 10.0) X[i, k].toInt() else 10
                    ans[i, j] += kotlin.math.ln(probs.getOrDefault(cls, zeros(0, 0))[k, ind])
                }
            }
        }
        val p = fill(ans.shape()[0], ans.shape()[1]) {i, j -> 1 / (exp(ans[i, 0..ans.shape()[1]-1] - ans[i, j]).mean() * ans.shape()[1]) }
        return p
    }

    fun predict(X: koma.matrix.Matrix<Double>): ArrayList<ArrayList<String>> {
        val probs = logProb(X)
        val ans = ArrayList<ArrayList<String>>()

        for (i in 0 until probs.shape()[0]) {
            ans.add(ArrayList<String>())
        }
        probs.forEachIndexed { row, col, ele -> if (ele > 0.05) {ans[row].add(classes[col])}}

        return ans
    }
}