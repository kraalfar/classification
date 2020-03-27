import krangl.*
//import kmath.*

fun main() {
    val df = DataFrame.readTSV("data/sh_sessions_multi.tsv")

    val r = df["sec"]

    val a:Int = r[0] as Int
    println(r.min())
}
