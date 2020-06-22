package util

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import krangl.*
import java.io.File
import java.lang.StringBuilder

/**
 * Содержит функции для преобразования сырых ивентов
 */
class SessionFilter {
    companion object {

        /**
         * Возвращает строчное преобразование ивента
         * @param s event_data
         * @param g group_id
         * @param e event_id
         * @param parser парсер для json
         * @return строчное преобразовние
         */
        fun ai(s: String, g: String, e: String, parser: Parser): String {
            val sb = StringBuilder(s)
            val jo = parser.parse(sb) as JsonObject
            val ret = jo.string(dt.getOrDefault(g, ""))
            if (ret != null)
                return ret
            return e
        }

        // Сопоставление group_id и нужной части event_data
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

        /**
         * Преобразует логи, убирает ненужные столбцы и упрощает информацию о событии. Сохраняет в тот же файл с тем
         * же названием и суффиксом  _filt.
         * @param path путь до логов
         */
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

/**
 * Класс одного действия
 * @property time начало действия
 * @property dt информация из event_data
 */
class Action(val time: Long, val group_id: String, val event_id: String, val dt: String) {
    fun print() {
        println("$time $group_id $event_id $dt")
    }

    fun getEvent(): String {
        return group_id + "_" + event_id + "_" + dt
    }
}


/**
 * Класс сессии
 * @property id id сессии, составляется из id пользователя и номера его сессии
 * @property startTime время начала сессии
 */
class Session(val id: String, val startTime: Long = 0) {
    var actions: ArrayList<Action> = ArrayList()
    var cat: String = ""

    /**
     * Добавляет событие в сессию
     * @param time время совершения действия
     * @param group group_id
     * @param event event_id
     * @param action информация из event_data
     */
    fun add(time: Long, group: String, event: String, action: String) {
        actions.add(Action(time,group,event, action))
    }

    /**
     * Устанавливает категорию сессии
     * @param cat категория
     */
    fun category(cat: String) {
        this.cat = cat
    }

    fun print() {
        println(id)
        for (action in actions) {
            action.print()
        }
    }

    /**
     * @return время конца сессии
     */
    fun time(): Long {
        return actions[actions.size - 1].time
    }

    /**
     * @return строка из всех действий в сессии
     */
    fun events(): String {
        val events = ArrayList<String>()
        for (action in actions)
            events.add(action.getEvent())
        return events.joinToString(separator = " , ")
    }

    /**
     * @return время, которое занимает сессия
     */
    fun times(): String {
        val times = ArrayList<String>()
        for (action in actions)
            times.add(action.time.toString())
        return times.joinToString(separator = " , ")
    }
}

/**
 * Составляет сессии из логов
 * @param path путь до логов
 * @param threshold минимальное время между деййствиями в одной сессии
 * @return массив из сессий
 */
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


/**
 * Записывает сессии по данному пути
 * @param ses массив из сессий
 * @param path путь до файла
 */
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

    val idsCol = StringCol("session_id", ids)
    val minCol = LongCol("ms", min)
    val eventsCol = StringCol("events", events)
//    val times_c = StringCol("timestamps", times)

    val ndf = dataFrameOf(idsCol, minCol, eventsCol)
    ndf.writeTSV(File("$path.tsv"))
}

/**
 * Достает сессии из TSV файла
 * @param path путь по файла
 * @return массив сессий
 */
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

/**
 * Достает сессии из таблицы
 * @param df таблица с сессиями
 * @return массив сессий
 */
fun sessionsFromDF(df: DataFrame): Array<Session>{
    val ses = ArrayList<Session>()
    for (i in 0 until df.nrow) {
        val events = df["events"][i].toString().split(" , ")
//        if (events.size < 3)
//            continue
        ses.add(Session(df["session_id"][i].toString()))
        ses[ses.size - 1].category(df["Category"][i].toString().toLowerCase())
        for (j in events.indices) {
            val params = events[j].split("_", limit=3)
            ses[ses.size - 1].add(0, params[0], params[1], params[2])
        }
    }
    return Array(ses.size) {i -> ses[i]}
}


/**
 * Обобщает действия в сессии
 */
fun filter(events: List<String>): ArrayList<String> {
    val res = ArrayList<String>()
    for (event in events) {
        if (event.startsWith("file.types.usage_open"))
            res.add("file.types.usage_open")
        else if (event.startsWith("file.types.usage_edit"))
            res.add("file.types.usage_edit")
        else
            res.add(event)
    }
    return res
}
