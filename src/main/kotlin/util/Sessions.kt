package util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import koma.min
import krangl.*
import java.io.File
import java.lang.StringBuilder



class SessionFilter {
    companion object {

        fun ai(s: String, g: String, e: String, parser: Parser): String {
            val sb = StringBuilder(s)
            val jo = parser.parse(sb) as JsonObject
            val ret = jo.string(dt.getOrDefault(g, ""))
            if (ret != null)
                return ret
            return e
        }

        val dt: HashMap<String, String> = hashMapOf(
            "build.tools.actions" to "class",
            "file.types.usage" to "file_type",
            "toolwindow" to "id",
            "live.templates" to "key",
            "ide.self.update" to "event_id",
            "ui.settings" to "configurable",
            "lifecycle" to "event_id",
            "vcs.log.trigger" to "class",
            "productivity" to "id",
            "debugger.breakpoints.usage" to "type",
            "find" to "type",
            "gutter.icon.click" to "icon_id",
            "new.project.wizard" to "project_type",
            "intentions" to "id",
            "ui.dialogs" to "dialog_class",
            "toolbar" to "action_id",
            "searchEverywhere" to "contributor_ID",
            "ui.tips" to "type",
            "json.schema" to "schemekind",
            "actions" to "action_id",
            "build.maven.packagesearch" to "endpoint"
        )


        fun filter(path: String) {
            val df = DataFrame.readTSV(path)
            val parser: Parser = Parser.default()
            val c: ArrayList<String?> = ArrayList()
            for (i in 0 until df.nrow) {
                val s = df["event_data"][i].toString()
                val g =  df["group_id"][i].toString()
                val e = df["event_id"][i].toString()
                c.add(ai(s, g, e, parser))
            }
            val cc = StringCol("data", c)

            val ndf = (df.addColumn("data") { cc }).remove("event_data", "event_count", "technical_session_id")
            val pt = path.split(".")
            ndf.writeTSV(File(pt[0] + "_filt." + pt[1]))
        }
    }
}


class Action(val time: Long, val event_id: String, val group_id: String, val dt: String) {
    fun print() {
        println("$time $event_id $group_id $dt")
    }

    fun get_event(): String {
        return group_id + "_" + dt
    }
}


class Session(val id: String) {
    var actions: ArrayList<Action> = ArrayList()

    fun add(time: Long, event: String, group: String, action: String) {
        actions.add(Action(time, event, group, action))
    }

    fun print() {
        println(id)
        for (action in actions) {
            action.print()
        }
    }

    fun time(): Long {
        val t: Long = actions[actions.size-1].time
        return t / 100 / 60
    }

    fun events(): String {
        val events = ArrayList<String>()
        for (action in actions)
            events.add(action.get_event())
        return events.joinToString(separator = " , ")
    }
}

fun makeSessions(path: String, threshold: Long = 36000): ArrayList<Session> {
    val df = DataFrame.readTSV(path)
    val groups = df.groupBy("device_id").sortedBy("time_epoch").groups()
    val ses: ArrayList<Session> = ArrayList()
    var tot = -1
    for (group in groups) {
        var cnt: Int = -1
        var prev: Long = -1
        var start: Long = -1
        // ses.add(Session(groups[0].row(0)['device_id']))
        for (row in group.rows) {
            val dev = row.getOrDefault("device_id", "") as String
            val time = row.getOrDefault("time_epoch", 0) as Long
            val event = row.getOrDefault("event_id", "") as String
            val groupId = row.getOrDefault("group_id", "") as String
            val dt = row.getOrDefault("data", "") as String
            if (groupId == "lifecycle")
                if (event == "frame.activated" || event == "frame.deactivated")
                    continue
            if (prev == -1L || time - prev > threshold) {
                cnt++
                tot++
                start = time
                ses.add(Session(dev + "_" + cnt.toString()))
                ses[tot].add(time - start, event, groupId, dt)
            } else {
                ses[tot].add(time - start, event, groupId, dt)
            }
            prev = time

        }
    }
    return ses
}




fun testWrite(ses: ArrayList<Session>, path: String) {
    val ids = ArrayList<String>()
    val min = ArrayList<Long>()
    val events = ArrayList<String>()
    for (s in ses) {
        ids.add(s.id)
        min.add(s.time())
        events.add(s.events())
    }
    val ids_c = StringCol("session_id", ids)
    val min_c = LongCol("min", min)
    val events_c = StringCol("events", events)
    val ndf = dataFrameOf(ids_c, min_c, events_c)
    ndf.writeTSV(File(path))
}

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
