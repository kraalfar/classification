package bayes

import krangl.*
import koma.*
import koma.extensions.*

class BoW(private val limit: Int = 5, private val n: Int = 1) {

    public var voc: Map<String, Int> = HashMap()
    public lateinit var id2word: List<String>


    fun initialize(df: DataFrame) {
        val events = df["events"]
        val eventsMap: MutableMap<String, Int> = HashMap()

        for (i in 0 until events.length) {
            val events_arr = df["events"][i].toString().split(" , ")
            for (j in 0 until events_arr.size - n + 1) {
                val ev = events_arr.slice(j until j + n).joinToString()
                if (eventsMap.containsKey(ev)) {
                    eventsMap[ev] = eventsMap.getValue(ev) + 1
                } else {
                    eventsMap[ev] = 1
                }
            }
        }

        id2word = eventsMap.filter { it.value > limit }.toSortedMap(compareBy({ eventsMap[it]?.times(-1) }, { it }))
            .keys.toList()

        voc = id2word.withIndex().toList().associate { it.value to it.index }
    }

    fun transform(df: DataFrame): Pair<koma.matrix.Matrix<Double>, List<String>> {
        val mat = zeros(df.nrow, voc.size + 2)
        val label = ArrayList<String>()
        for (i in 0 until df.nrow) {
            mat[i, voc.size] = df["ms"][i] as Int
            mat[i, voc.size + 1] = df["events"][i].toString().split(" , ").size
            label.add(df["Category"][i].toString())

            val events_arr = df["events"][i].toString().split(" , ")
            for (j in 0 until events_arr.size - n + 1) {
                val event = events_arr.slice(j..j + n - 1).joinToString()
                if (voc.containsKey(event)) {
                    mat[i, voc[event] as Int] += 1
                }
            }
        }
        return Pair(mat, label)
    }


    //     no n-gram support
    fun transform2(df: DataFrame): koma.matrix.Matrix<Double> {
        val mat = zeros(df.nrow, voc.size + 2)
        for (i in 0 until df.nrow) {
            mat[i, voc.size] = df["ms"][i] as Int
            mat[i, voc.size + 1] = df["events"][i].toString().split(" , ").size
            for (event in df["events"][i].toString().split(" , ")) {
                if (voc.containsKey(event)) {
                    mat[i, voc[event] as Int] += 1
                }
            }
        }
        return mat
    }

}