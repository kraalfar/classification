package bayes

import koma.*
import koma.extensions.*
import java.util.ArrayList

class NaiveBayes(private val alpha: Double = 1e-5) {

    private var classProb: MutableMap<String, Double> = HashMap()
    private var classes: MutableList<String> = ArrayList()
    private var probs: MutableMap<String, DoubleArray> = HashMap()


    fun fit(X: koma.matrix.Matrix<Double>, y: List<String>) {
        for (i in y.indices) {
            for (cls in y[i].toLowerCase().split(", ")) {
                if (!classProb.containsKey(cls)) {
                    classProb[cls] = 1.0 / y.size
                    classes.add(cls)
                    probs[cls] = DoubleArray(X.shape()[1] - 1) { alpha }
                    probs.getOrDefault(cls, DoubleArray(0))[X.shape()[1] - 2] = 0.0
                } else {
                    classProb[cls] = classProb.getValue(cls) + 1.0 / y.size
                }
                for (j in 0 until X.shape()[1] - 2) {
                    probs.getOrDefault(cls, DoubleArray(0))[j] += X[i, j]
                }
                probs.getOrDefault(cls, DoubleArray(0))[X.shape()[1] - 2] += X[i, X.shape()[1] - 2]
            }

        }
        for (cls in classes) {
            val den = probs.getOrDefault(cls, DoubleArray(0)).sum()
            for (j in 0 until X.shape()[1] - 2) {
                probs.getOrDefault(cls, DoubleArray(0))[j] /= den
            }
            probs.getOrDefault(cls, DoubleArray(0))[X.shape()[1] - 2] =
                classProb.getOrDefault(cls, 0.0) * y.size / probs.getOrDefault(cls, DoubleArray(0))[X.shape()[1] - 2]
        }
    }

    fun logProb2(X: koma.matrix.Matrix<Double>): koma.matrix.Matrix<Double> {
        val ans = zeros(X.shape()[0], classes.size)
        for (i in 0 until X.shape()[0]) {
            for (j in classes.indices) {
                val cls = classes[j]
                ans[i, j] = kotlin.math.ln(classProb.getOrDefault(cls, alpha))
                val timeProb = probs.getOrDefault(cls, DoubleArray(0))[X.shape()[1] - 2]
                ans[i, j] += (X[i, X.shape()[1] - 2]) * kotlin.math.ln(1 - timeProb) + kotlin.math.ln(timeProb)
                for (k in 0 until X.shape()[1] - 2) {
                    ans[i, j] += X[i, k] * kotlin.math.ln(probs.getOrDefault(cls, DoubleArray(0))[k])
                }
            }
        }
        return ans
    }


    fun logProb(X: koma.matrix.Matrix<Double>): koma.matrix.Matrix<Double> {
        val ans = zeros(X.shape()[0], classes.size)
        for (i in 0 until X.shape()[0]) {
            for (j in classes.indices) {
                val cls = classes[j]
                ans[i, j] = kotlin.math.ln(classProb.getOrDefault(cls, alpha))
                for (k in 0 until X.shape()[1] - 2) {
                    ans[i, j] += X[i, k] * kotlin.math.ln(probs.getOrDefault(cls, DoubleArray(0))[k])
                }
            }
        }
        return ans
    }

    fun predict(X: koma.matrix.Matrix<Double>, mode: String = "nm"): List<String> {
        val probs = if (mode == "nm") {
            logProb(X)
        } else {
            logProb2(X)
        }
        val ans = ArrayList<String>()

        probs.forEachRow { ans.add(classes[it.argMax()]) }
        return ans
    }
}