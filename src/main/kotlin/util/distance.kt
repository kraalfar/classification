package util

import koma.min
import smile.math.distance.DynamicTimeWarping

class DistSes<T> : smile.math.distance.Distance<T> {
    override fun d(x: T, y: T): Double {
        if (x is Session && y is Session)
            return 1 - dist(x, y)
        if (x is String && y is String)
            return 1 - dist(x, y)
        return 1.0
    }

}

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

    val f1: Double = (1.0 * v3) / v1
    val f2: Double = (1.0 * v3) / v2

    if (f1 + f2 == 0.0)
        return 0.0
    return 2 * f1 * f2 / (f1 + f2)

}

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

class ActionDist : smile.math.distance.Distance<Action> {
    override fun d(x: Action, y: Action): Double {
        var score = 1.0
        if (x.group_id != y.group_id)
            return score
        score -= 1.0 / 2
        if (x.event_id != y.event_id)
            return score
        score -= 1.0 / 4

        if (x.dt != y.dt)
            return score
        return 0.0
    }

}

val dtw = DynamicTimeWarping(ActionDist())

fun DTWSession(s1: Session, s2: Session): Double {
    val a1 = Array(s1.actions.size) { i -> s1.actions[i]}
    val a2 = Array(s2.actions.size) { i -> s2.actions[i]}
//    return dtw.apply(a1, a2) / (a1.size + a2.size)
    return dtw.apply(a1, a2)
}

class DWTDist<T> : smile.math.distance.Distance<T> {
    override fun d(x: T, y: T): Double {
        if (x is Session && y is Session)
            return DTWSession(x, y)
        return 1.0
    }

}