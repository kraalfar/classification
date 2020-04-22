import krangl.*
import smile.clustering.DBSCAN
import smile.clustering.KMeans
import util.*
import java.io.File

fun main() {
//    SessionFilter.filter("data/UI_1-2buckets_6march.tsv")
//    val ses = makeSessions("data/UI_1-2buckets_6march_filt.tsv")
//    testWrite(ses, "data/test.tsv")
//    ses.shuffle()
    val df = DataFrame.readTSV("data/sh_sessions_multi.tsv")
    val ses = ArrayList<String>()
    val lab = ArrayList<String>()

    for (i in 0 until df.nrow) {
        ses.add(df["events"][i].toString())
        lab.add(df["category"][i].toString())
    }
//    val s: ArrayList<Session> = ses.slice(0 .. 1000) as ArrayList<Session>
//    val s2: Array<Session> = s.toArray() as Array<Session>
//
//
//    val rnn = smile.neighbor.LinearSearch<Session>(s2, DistSes())
//
    for (i in 1 until 11) {
        for (j in 1 until 11) {
            val clusters = DBSCAN.fit(ses.toArray(), DistSes(), i, j * 0.1)
            val ind = IntCol("clust", clusters.y)
            val ndf = df.addColumn("clust") {ind}
            ndf.writeTSV(File("data/test/test_$i _$j.tsv"))

        }
    }

}
