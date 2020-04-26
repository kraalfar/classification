import krangl.*
import smile.clustering.DBSCAN
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage.CompleteLinkage
import util.*
import java.io.File


fun main() {
    val ses = sessionsFromTSV("data/UI_1-2buckets_7march_session.tsv")

    val df = DataFrame.readTSV("data/UI_1-2buckets_7march_session.tsv")


//    val clusters = DBSCAN.fit(ses.toArray(), DWTDist(), 10,  0.2)
    val clusters = HierarchicalClustering.fit(CompleteLinkage.of(ses.toArray(), DWTDist()))
    val y = clusters.partition(7);

    val ind = IntCol("clust", y)
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
    val min_c = LongCol("mili", min)
    val events_c = StringCol("events", events)
    val times_c = StringCol("timestamps", times)

    val ndf = dataFrameOf(ids_c, min_c, events_c, times_c, ind)
    ndf.writeTSV(File("data/dtw_2.tsv"))







//    val files = File("data").list()

//    SessionFilter.filter("data/UI_1-2buckets_6march.tsv")
//    for (file in files) {
//        if (file.toString().contains("filt")) {
//            val s = makeSessions("data/$file")
//            val name = file.toString().replace("_filt", "_session")
//            testWrite(s, "data/$name")
//        }
//    }
//    val ses = makeSessions("data/UI_1-2buckets_6march_filt.tsv")
//    testWrite(ses, "data/sessions.tsv")
//    ses.shuffle()
//    val df = DataFrame.readTSV("data/sh_sessions_multi.tsv")
//    val ses = ArrayList<String>()
//    val lab = ArrayList<String>()
//
//    for (i in 0 until df.nrow) {
//        ses.add(df["events"][i].toString())
//        lab.add(df["category"][i].toString())
//    }
//    val s: ArrayList<Session> = ses.slice(0 .. 1000) as ArrayList<Session>




//
//
//
//    for (i in 1 until 11) {
//        for (j in 1 until 11) {
//            val clusters = DBSCAN.fit(ses.toArray(), DistSes(), i, j * 0.1)
//            val ind = IntCol("clust", clusters.y)
//            val ndf = df.addColumn("clust") {ind}
//            ndf.writeTSV(File("data/test/test_$i _$j.tsv"))
//
//        }
//    }

}