package knn

import koma.*
import koma.extensions.*
import krangl.DataFrame
import smile.classification.KNN
import util.CosDist
import util.filter
import java.util.ArrayList

class BoWKNN(val cat: String = "coding") {

    var voc: Map<String, Int> = HashMap()
    lateinit var id2word: List<String>
    private var n: Int = 0

    lateinit var knn: KNN<DoubleArray>

    fun initialize(df: DataFrame, limit: Int = 5, top: Int = 0, n: Int = 1) {
        this.n = n
        val events = df["events"]
        val eventsMap: MutableMap<String, Int> = HashMap()

        for (i in 0 until events.length) {
            val eventsArr = filter(df["events"][i].toString().split(" , "))
            for (j in 0 until eventsArr.size - n + 1) {
                val ev = eventsArr.slice(j until j + n).joinToString()
                if (eventsMap.containsKey(ev)) {
                    eventsMap[ev] = eventsMap.getValue(ev) + 1
                } else {
                    eventsMap[ev] = 1
                }
            }
        }

        id2word = eventsMap.filter { it.value > limit }
            .toSortedMap(compareBy({ eventsMap[it]?.times(-1) }, { it })).keys.toList()

        id2word = id2word.slice(top until id2word.size)
        voc = id2word.withIndex().toList().associate { it.value to it.index }
    }

    fun transform(df: DataFrame): Pair<Array<DoubleArray>, IntArray> {
        val res = ArrayList<DoubleArray>()
        val label = IntArray(df.nrow)

        for (i in 0 until df.nrow) {
            label[i] = if (df["Category"][i].toString().toLowerCase().contains(cat)) 1 else 0
            val cm = DoubleArray(voc.size) {i -> 0.0}

            val eventsArr = filter(df["events"][i].toString().split(" , "))
            for (j in 0 until eventsArr.size - n + 1) {
                val event = eventsArr.slice(j until j + n).joinToString()
                if (voc.containsKey(event)) {
                    cm[voc[event] as Int] = cm[voc[event] as Int] + 1
                }
            }
            for (k in cm.indices) {
                cm[k] = if (cm[k] == 0.0) 0.0 else 1.0 + ln(cm[k]) // ln(10)
            }
            res.add(cm)
        }
        return Pair(Array(res.size) {i -> res[i] }, label)
    }

    fun fit(df: DataFrame, k: Int) {
        val (X, y) = transform(df)
        knn = KNN.fit(X, y, CosDist(), k)
    }

    fun predict(df: DataFrame): IntArray {
        val (X, y) = transform(df)
        return knn.predict(X)
    }

    fun predictF1(df: DataFrame): Double {
        val (X, y) = transform(df)
        val res = knn.predict(X)
        var tp = 0.0
        var tn = 0.0
        var fp = 0.0
        var fn = 0.0
        for (i in y.indices) {
            if (y[i] == 0) {
                if (res[i] == 0) {
                    tn++
                } else {
                    fn++
                }
            } else {
                if (res[i] == 1) {
                    tp++
                } else {
                    fp++
                }
            }
        }
        val prec = tp / (tp + fn)
        val rec = tp / (tp + fp)
        val f1 = 2 * prec * rec / (prec + rec)
        return if (f1 == f1) f1 else 0.0
    }
}