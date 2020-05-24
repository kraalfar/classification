import bayes.ClassTest
import bayes.trainTestSplit

fun main() {
//    val ses = sessionsFromTSV("data/UI_3-4buckets_9_march_session.tsv")


    val (train, test) = trainTestSplit("data/test/test_data.tsv")
    val cat = "database"
    val cl = ClassTest(cat = cat)

    var best = 0.0
    var bi: Int = 0
    var bj: Int = 0

    for (i in 0 until 20) {
        for (j in 0 until 20) {
            cl.initialize(train, i, j)
            cl.fit(train)
            val (t, aucTrain) = cl.predict(train)
            val (o, aucTest) = cl.predict(test)
            if (aucTest > best) {
                best = aucTest
                bi = i
                bj = j
            }
            println("limit=$i, top=$j\ntrain=$aucTrain \ntest=$aucTest\n---------------------------")

        }
    }

    println("BEST\nlimit=$bi, top=$bj\ntest=$best")

//    smile.classification.KNN


//    val small = ArrayList<Session>()

//    for (s in ses) {
//        if (s.actions.size in 11..50) {
//            small.add(s)
//        }
//    }
//    val (train, test) = trainTestSplit("../data/test/test_data.tsv")
//    val bow = BoW(n=1)
//    bow.initialize(train)
//
//
//    val classificator = OneVSAll()
//    val (X_bow, y_bow) = bow.transform(train)
//
//    val (X_bow_test, y_bow_test) = bow.transform(test)
//    classificator.fit(X_bow, y_bow)
//
//    val pred = classificator.probs(X_bow)
//
//    val rec = ArrayList<Double>()
//    val prec = ArrayList<Double>()
//    val a =  ArrayList<Pair<Double, Int>>()
//
//    for (i in y_bow.indices) {
//        if (y_bow[i].toLowerCase().contains("coding"))
//            a.add(Pair(pred[i], 1))
//        else
//            a.add(Pair(pred[i], 0))
//    }
//
//    a.sortBy { it.first }
//


//    ses.shuffle()
//    val sl1 = ses.slice(0..999)
//    testWrite(sl1 as ArrayList<Session>, "data/data_to_label_3")
//
//    val sl2 = ses.slice(1000..1999)
//    testWrite(sl2 as ArrayList<Session>, "data/data_to_label_4")


//    val df = DataFrame.readTSV("data/UI_1-2buckets_7march_session.tsv")
//
//    val d = Array<DoubleArray> (ses.size) {DoubleArray(ses.size)}
////    val clusters = DBSCAN.fit(ses.toArray(), DWTDist(), 10,  0.2)
//    val clusters = HierarchicalClustering.fit(CompleteLinkage.of(ses.toArray(), DWTDist()))
//    val y = clusters.partition(7);
//
//    val ind = IntCol("clust", y)
//    val ids = ArrayList<String>()
//    val min = ArrayList<Long>()
//    val events = ArrayList<String>()
//    val times = ArrayList<String>()
//
//    for (s in ses) {
//        ids.add(s.id)
//        min.add(s.time())
//        events.add(s.events())
//        times.add(s.times())
//    }
//
//    val ids_c = StringCol("session_id", ids)
//    val min_c = LongCol("mili", min)
//    val events_c = StringCol("events", events)
//    val times_c = StringCol("timestamps", times)

//    val ndf = dataFrameOf(ids_c, min_c, events_c, times_c, ind)
//    ndf.writeTSV(File("data/dtw_2.tsv"))


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



