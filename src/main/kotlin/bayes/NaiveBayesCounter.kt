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
        val ans = zeros(X.shape()[0], classes.size)
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
        return ans
    }

    fun predict(X: koma.matrix.Matrix<Double>): List<String> {
        val probs = logProb(X)

        val ans = ArrayList<String>()

        probs.forEachRow { ans.add(classes[it.argMax()]) }
        return ans
    }
}