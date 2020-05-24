package bayes

import krangl.*


fun trainTestSplit(path: String, fraction: Double = 0.8): Pair<DataFrame, DataFrame> {
    val df = DataFrame.readTSV(path)
    val trainCount: Int = kotlin.math.ceil(fraction * df.nrow).toInt()
    val shuffledDf = df.shuffle()
    return Pair(shuffledDf.slice(0..trainCount), shuffledDf.slice(trainCount + 1..shuffledDf.nrow))
}


fun getArrayList(size: Int = 10, value: Double = 0.1): ArrayList<Double> {
    val list = ArrayList<Double>()
    for (i in 0 until size) {
        list.add(value)
    }
    return list
}


fun integrate(x: ArrayList<Double>, y: ArrayList<Double>): Double {
    var res = 0.0
    for (i in 0 until y.size - 1) {
        res -= (x[i + 1] - x[i]) * (y[i + 1] + y[i]) / 2
    }
    return res
}


class Predicition(
    val probs: HashMap<String, Double> = HashMap(),
    val pos: HashMap<String, List<String>> = HashMap(),
    val negs: HashMap<String, List<String>> = HashMap()
) {
    fun add(cat: String, prob: Double, ps: List<String>, ns: List<String>) {
        probs[cat] = prob
        pos[cat] = ps
        negs[cat] = ns
    }
}