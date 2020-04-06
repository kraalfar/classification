package bayes

import krangl.*


fun trainTestSplit(path: String, fraction: Double = 0.8): Pair<DataFrame, DataFrame> {
    val df = DataFrame.readTSV(path)
    val trainCount: Int = kotlin.math.ceil(fraction * df.nrow).toInt()
    val shuffledDf = df.shuffle()
    return Pair(shuffledDf.slice(0..trainCount), shuffledDf.slice(trainCount + 1..shuffledDf.nrow))
}


fun getArrayList(size: Int = 10, value: Double = 0.1): ArrayList<Double> {
    var list = ArrayList<Double>()
    for (i in 0 until size) {
        list.add(value)
    }
    return list
}
