package bayes

import krangl.*
import koma.*
import koma.extensions.*

class BoW(private val limit: Int = 100) {

    private var voc: Map<String, Int> = HashMap()

    fun initialize(df: DataFrame) {
        val events = df["events"]
        val eventsMap: MutableMap<String, Int> = HashMap()
        for (i in 0 until events.length) {
            for (ev in events[i].toString().split(" , ")) {
                if (eventsMap.containsKey(ev)) {
                    eventsMap[ev] = eventsMap.getValue(ev) + 1
                } else {
                    eventsMap[ev] = 1
                }
            }
        }
        val realLimit = if (eventsMap.size > limit) limit else eventsMap.size
        voc = eventsMap.toSortedMap(compareBy({ eventsMap[it]?.times(-1) }, { it })).keys.toList()
            .slice(0 until realLimit).withIndex().toList().associate {it.value to it.index}
    }

    fun transform(df: DataFrame): Pair<koma.matrix.Matrix<Double>, List<String>> {
        val mat = zeros(df.nrow, voc.size + 2)
        val label = ArrayList<String>()
        for (i in 0 until df.nrow) {
            mat[i, voc.size] = df["min"][i] as Int
            mat[i, voc.size + 1] = df["events"][i].toString().split(" , ").size
            label.add(df["category"][i].toString())
            for (event in df["events"][i].toString().split(" , ")) {
                if (voc.containsKey(event)) {
                    mat[i, voc[event] as Int] += 1
                }
            }
        }
        return Pair(mat, label)
    }

}