package util

import koma.min
import smile.math.distance.DynamicTimeWarping
import java.lang.Math.max
import java.lang.Math.sqrt


/**
 * Расстояние между сеессиям, рассчитаное из их пересечения
 */
fun dist(s1: Session, s2: Session): Double {
    val a1 = ArrayList<String>()
    val a2 = ArrayList<String>()

    for (a in s1.actions) {
        a1.add(a.event_id + " " + a.group_id + " " + a.dt)
    }

    for (a in s2.actions) {
        a2.add(a.event_id + " " + a.group_id + " " + a.dt)
    }

    val h1 = a1.groupingBy { it }.eachCount()
    val h2 = a2.groupingBy { it }.eachCount()

    val keys = HashSet<String>()
    keys.addAll(h1.keys)
    keys.addAll(h2.keys)

    val h3 = HashMap<String, Int>()
    for (k in keys) {
        h3[k] = min(h1.getOrDefault(k, 0), h2.getOrDefault(k, 0))
    }

    val v1 = h1.values.sum()
    val v2 = h2.values.sum()
    val v3 = h3.values.sum()

    return 1.0 * v3 / (min(v1, v2))

}

/**
 * Расстояние между сеессиям, рассчитаное из их пересечения
 */
fun dist(s1: String, s2: String): Double {
    val a1 = s1.split(" , ")
    val a2 = s2.split(" , ")

    val h1 = a1.groupingBy { it }.eachCount()
    val h2 = a2.groupingBy { it }.eachCount()

    val keys = HashSet<String>()
    keys.addAll(h1.keys)
    keys.addAll(h2.keys)

    val h3 = HashMap<String, Int>()
    for (k in keys) {
        h3[k] = min(h1.getOrDefault(k, 0), h2.getOrDefault(k, 0))
    }

    val v1 = h1.values.sum()
    val v2 = h2.values.sum()
    val v3 = h3.values.sum()

    val f1: Double = (1.0 * v3) / v1
    val f2: Double = (1.0 * v3) / v2

    if (f1 + f2 == 0.0)
        return 0.0
    return 2 * f1 * f2 / (f1 + f2)

}

/**
 * Класс обертка для расстояния между действиями
 */
class ActionDist : smile.math.distance.Distance<Action> {
    override fun d(x: Action, y: Action): Double {

        if (x.group_id == "actions" || y.group_id == "actions") {
            // если действие action, то расстояние определяется только по event_data
            if (x.dt == y.dt)
                return 0.0
            return 1.0
        } else if (x.group_id == "productivity" || y.group_id == "productivity") {
            // если действие productivity, то расстояние определяется только по event_data
            if (x.dt == y.dt)
                return 0.0
            return 1.0
        } else if (x.group_id == "file.types.usage" || y.group_id == "file.types.usage") {
            // если действие file.types.usage, то расстояние вычисляется только по event_id
            if (x.event_id == y.event_id)
                return 0.0
            return 1.0
        } else {
            // иначе за каждое свопадение группы, ивента или ивент даты расстояние уменьшается
            var score = 1.0
            if (x.group_id != y.group_id)
                return score
            score -= 1.0 / 10
            if (x.event_id != y.event_id)
                return score
            score -= 1.0 / 10

            if (x.dt != y.dt)
                return score
            return 0.0
        }

    }

}

// dtw обертка для ActionDist
val dtw = DynamicTimeWarping(ActionDist())

/**
 * Скалярное произведение двух массивов
 */
fun prod(s1: DoubleArray, s2: DoubleArray): Double {
    var prod = 0.0
    for (i in s1.indices) {
        prod += s1[i] * s2[i]
    }
    return prod
}


/**
 * Косинусное расстояние между двумя массивами
 */
fun cosineSimilarity(s1: DoubleArray, s2: DoubleArray): Double {
    val norm1 = kotlin.math.sqrt(prod(s1, s1))
    val norm2 = kotlin.math.sqrt(prod(s2, s2))
    if (norm1 == 0.0 || norm2 == 0.0) {
        return 1.0
    }
    return -prod(s1, s2) / norm1 / norm2


}

/**
 * dtw принимает массивы, поэтому эта обертка преобразует сессии в массив
 */
fun DTWSession(s1: Session, s2: Session): Double {
    val a1 = Array(s1.actions.size) { i -> s1.actions[i] }
    val a2 = Array(s2.actions.size) { i -> s2.actions[i] }
//    return dtw.apply(a1, a2) / (a1.size + a2.size)
    return dtw.apply(a1, a2)
}


/**
 * Финальная обертка dist для применения в knn
 */
class DistSes<T> : smile.math.distance.Distance<T> {
    override fun d(x: T, y: T): Double {
        if (x is Session && y is Session)
            return 1 - dist(x, y)
        if (x is String && y is String)
            return 1 - dist(x, y)
        return 1.0
    }

}

/**
 * Обертка для dtw для применения в knn
 */
class DTWDist<T> : smile.math.distance.Distance<T> {

    override fun d(x: T, y: T): Double {
        if (x is Session && y is Session)
            return DTWSession(x, y)
        return 1.0
    }

}


/**
 * Обертка для косиносного расстояния для применения в knn
 */
class CosDist<T> : smile.math.distance.Distance<T> {

    override fun d(x: T, y: T): Double {
        if (x is DoubleArray && y is DoubleArray)
            return cosineSimilarity(x, y)
        return 1.0
    }
}

/**
 * Обертка dtw расстояния для svm
 */
class DTWKernel<T>: smile.math.kernel.MercerKernel<T> {
    override fun k(x: T, y: T): Double {
        if (x is Session && y is Session)
            return DTWSession(x, y)
        return 1.0
    }

}

/**
 * Обертка dist для svm
 */
class IntersectionKernel<T>: smile.math.kernel.MercerKernel<T> {
    override fun k(x: T, y: T): Double {
        if (x is Session && y is Session)
            return 1 - dist(x, y)
        if (x is String && y is String)
            return 1 - dist(x, y)
        return 1.0
    }

}


/**
 * Обертка косинусного расстояния для svm
 */
class CosKernel<T>: smile.math.kernel.MercerKernel<T> {
    override fun k(x: T, y: T): Double {
        if (x is DoubleArray && y is DoubleArray)
            return cosineSimilarity(x, y)
        return 1.0
    }

}
