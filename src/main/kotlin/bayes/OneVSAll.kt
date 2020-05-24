package bayes

import koma.*
import koma.extensions.*
import krangl.DataFrame
import java.util.ArrayList


class OneVSAll(private val alpha: Double = 1e-5) {

    val classes = listOf(
        "reading", "coding", "ide_start",
        "notifications", "rdb", "database",
        "ide_close", "vcs", "terminal", "settings"
    )

    val limit = mapOf(
        "reading" to 0, "coding" to 0, "ide_start" to 0,
        "notifications" to 0, "rdb" to 0, "database" to 9,
        "ide_close" to 0, "vcs" to 0, "terminal" to 3, "settings" to 0
    )

    val top = mapOf(
        "reading" to 0, "coding" to 0, "ide_start" to 0,
        "notifications" to 0, "rdb" to 0, "database" to 1,
        "ide_close" to 4, "vcs" to 0, "terminal" to 1, "settings" to 0
    )

    var classifiers = ArrayList<NaiveBayesOneVAll>()

    fun initialize(df: DataFrame, n: Int = 1) {
        for (cls in classes) {
            val newClass = NaiveBayesOneVAll(cat = cls)
            newClass.initialize(df, limit[cls] ?: error(""), top[cls] ?: error(""), n)
            classifiers.add(newClass)
        }
    }

    fun fit(df: DataFrame) {
        for (cl in classifiers) {
            cl.fit(df)
        }
    }

    fun predict(df: DataFrame): Triple<ArrayList<Predicition>, ArrayList<Double>, ArrayList<Map<String, ArrayList<Double>>>> {
        val ROCs = ArrayList<Map<String, ArrayList<Double>>>()
        val AUCs = ArrayList<Double>()
        val preds = ArrayList<Predicition>()

        for (i in 0 until df.nrow)
            preds.add(Predicition())

        for (i in classes.indices) {
            val (X_bow, y_bow) = classifiers[i].transform(df)
            val prob = classifiers[i].prob(X_bow)
            val (roc, auc) = ROCAUC(prob, y_bow, classes[i])
            ROCs.add(roc)
            AUCs.add(auc)
            val (pos, negs) = classifiers[i].getPosNeg(X_bow)
            for (j in prob.indices) {
                preds[j].add(classes[i], prob[j], pos[j], negs[j])
            }
        }

        return Triple(preds, AUCs, ROCs)
    }

    private fun ROCAUC(
        prob: List<Double>,
        y: List<String>,
        cat: String
    ): Pair<Map<String, ArrayList<Double>>, Double> {
        val nexs = ArrayList<Double>()
        val pexs = ArrayList<Double>()
        val a = ArrayList<Pair<Double, Int>>()
        for (i in y.indices) {
            if (y[i].toLowerCase().contains(cat))
                a.add(Pair(prob[i], 1))
            else
                a.add(Pair(prob[i], 0))
        }

        a.sortBy { it.first }
        var c = 0.01
        val z = DoubleArray(2)
        nexs.add(0.0)
        pexs.add(0.0)

        for (v in a) {
            if (v.first > c) {
                c += 0.01
                nexs.add(z[0])
                pexs.add(z[1])
            }
            z[v.second]++
        }

        nexs.add(z[0])
        pexs.add(z[1])

        val pe = pexs[pexs.size - 1]
        val ne = nexs[nexs.size - 1]

        for (i in pexs.indices) {
            pexs[i] = pe - pexs[i]
            nexs[i] = ne - nexs[i]

        }
        val rec = ArrayList<Double>()
        val prec = ArrayList<Double>()

        for (i in pexs.indices) {
            rec.add(pexs[i] / pexs[0])
        }

        for (i in pexs.indices) {
            if (pexs[i] + nexs[i] == 0.0)
                prec.add(1.0)
            else
                prec.add(pexs[i] / (nexs[i] + pexs[i]))
        }

        val df = mapOf("recall" to rec, "precision" to prec)
        val auc = integrate(rec, prec)
        return Pair(df, auc)
    }



}