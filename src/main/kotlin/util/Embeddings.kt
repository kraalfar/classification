package util

import krangl.DataFrame
import krangl.readTSV
import smile.math.distance.DynamicTimeWarping
import java.io.File




class Embeddings(path: String) {
    var embd =  HashMap<String, List<Double>>()

    fun getEmbeddings(path: String) {
        File(path).forEachLine { x ->
            embd[x.split("\t", limit = 2)[0]] =
                    x.split("\t", limit = 2)[1].split("\t").map { it.toDouble() }
        }
    }

    fun getDist(s1: String, s2: String): Double {
        if ((s1 in embd) && (s2 in embd)) {
            return cosineSimilarity(embd[s1]!!.toDoubleArray(), embd[s2]!!.toDoubleArray())
        }
        return 0.0
    }

}

class EmbeddingDist(val embeddings: Embeddings): smile.math.distance.Distance<Action> {
    override fun d(x: Action, y: Action): Double {
        val s1 = "${x.group_id}_${x.event_id}_${x.dt}"
        val s2 = "${y.group_id}_${y.event_id}_${y.dt}"
        return embeddings.getDist(s1, s2)
    }
}

val embeddings = Embeddings("data/test/embd_res.tsv")
val dtwE = DynamicTimeWarping(EmbeddingDist(embeddings))

fun DTWESession(s1: Session, s2: Session): Double {
    val a1 = Array(s1.actions.size) { i -> s1.actions[i] }
    val a2 = Array(s2.actions.size) { i -> s2.actions[i] }
//    return dtw.apply(a1, a2) / (a1.size + a2.size)
    return dtwE.apply(a1, a2)
}


class DTWEDist<T> : smile.math.distance.Distance<T> {
    override fun d(x: T, y: T): Double {
        if (x is Session && y is Session)
            return DTWESession(x, y)
        return 1.0
    }

}

class DTWEKernel<T>: smile.math.kernel.MercerKernel<T> {
    override fun k(x: T, y: T): Double {
        if (x is Session && y is Session)
            return DTWESession(x, y)
        return 1.0
    }

}

fun distEmbd(s1: Session, s2: Session): Double {
    val x = DoubleArray(200)
    var cnt1 = 0
    for (a in s1.actions) {
        val ev = "${a.group_id}_${a.event_id}_${a.dt}"
        if (ev in embeddings.embd) {
            cnt1++
            for (j in embeddings.embd[ev]!!.indices)
                x[j] += embeddings.embd[ev]!![j]
        }
    }

    val y = DoubleArray(200)
    var cnt2 = 0
    for (a in s2.actions) {
        val ev = "${a.group_id}_${a.event_id}_${a.dt}"
        if (ev in embeddings.embd) {
            cnt2++
            for (j in embeddings.embd[ev]!!.indices)
                x[j] += embeddings.embd[ev]!![j]
        }
    }
    for (i in x.indices) {
        x[i] = x[i] / cnt1
        y[i] = y[i] / cnt2
    }
    return cosineSimilarity(x, y)
}

class EKernel<T>: smile.math.kernel.MercerKernel<T> {
    override fun k(x: T, y: T): Double {
        if (x is Session && y is Session)
            return distEmbd(x, y)
        return 1.0
    }

}