package bayes

import krangl.*


/**
 * Разделяет датасет на треин и тест
 * @param path путь до датасета
 * @param fraction для попадающая в трейн
 */
fun trainTestSplit(path: String, fraction: Double = 0.8): Pair<DataFrame, DataFrame> {
    val df = DataFrame.readTSV(path)
    val trainCount: Int = kotlin.math.ceil(fraction * df.nrow).toInt()
    val shuffledDf = df.shuffle()
    return Pair(shuffledDf.slice(0..trainCount), shuffledDf.slice(trainCount + 1..shuffledDf.nrow))
}


/**
 * Интегрирование
 */
fun integrate(x: ArrayList<Double>, y: ArrayList<Double>): Double {
    var res = 0.0
    for (i in 0 until y.size - 1) {
        res -= (x[i + 1] - x[i]) * (y[i + 1] + y[i]) / 2
    }
    return res
}


/**
 * Класс для предсказания наивным байесом
 * @property probs словарь вероятностей каждой категории
 * @property pos словарь действий положительно влияющих на принятие решения для каждой категории
 * @property negs словарь действий отрицательно влияющих на принятие решения для каждой категории
 */
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