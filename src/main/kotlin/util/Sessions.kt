package util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
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
                val g = df["group_id"][i].toString()
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


class Action(val time: Long, val group_id: String, val event_id: String, val dt: String) {
    fun print() {
        println("$time $group_id $event_id $dt")
    }

    fun getEvent(): String {
        return "$group_id; $event_id; $dt"
    }
}

class Session(val id: String, val startTime: Long = 0) {
    var actions: ArrayList<Action> = ArrayList()

    fun add(time: Long, group: String, event: String, action: String) {
        actions.add(Action(time,group,event, action))
    }

    fun print() {
        println(id)
        for (action in actions) {
            action.print()
        }
    }

    fun time(): Long {
        return actions[actions.size - 1].time
    }

    fun events(): String {
        val events = ArrayList<String>()
        for (action in actions)
            events.add(action.getEvent())
        return events.joinToString(separator = " , ")
    }

    fun times(): String {
        val times = ArrayList<String>()
        for (action in actions)
            times.add(action.time.toString())
        return times.joinToString(separator = " , ")
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
                ses.add(Session(dev + "_" + cnt.toString(), time))
                ses[tot].add(time - start, groupId, event, dt)
            } else {
                ses[tot].add(time - start, groupId, event, dt)
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
    val times = ArrayList<String>()

    for (s in ses) {
        ids.add(s.id)
        min.add(s.time())
        events.add(s.events())
        times.add(s.times())
    }

    val ids_c = StringCol("session_id", ids)
    val min_c = LongCol("min", min)
    val events_c = StringCol("events", events)
    val times_c = StringCol("timestamps", times)

    val ndf = dataFrameOf(ids_c, min_c, events_c, times_c)
    ndf.writeTSV(File("$path.tsv"))
}

fun sessionsFromTSV(path: String): ArrayList<Session> {
    val df = DataFrame.readTSV(path)
    val ses = ArrayList<Session>()
    for (i in 0 until df.nrow) {
        val events = df["events"][i].toString().split(" , ")
//        if (events.size < 3)
//            continue
        val times = df["timestamps"][i].toString().split(" , ")
        ses.add(Session(df["session_id"][i].toString()))
        for (j in events.indices) {
            val params = events[j].split(" ;")
            ses[ses.size - 1].add(times[j].toLong(), params[0], params[1], params[2])
        }
    }
    return ses
}