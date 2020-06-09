package bayes

import koma.*
import koma.extensions.*
import krangl.DataFrame
import java.util.ArrayList


class ClassTest(val cat: String) {

    var classifier = NaiveBayesMultinomial(cat = cat)

    fun initialize(df: DataFrame, limit: Int = 0, top: Int = 0, n: Int = 1) {
        classifier.initialize(df, limit, top, n)
    }

    fun fit(df: DataFrame) {
        classifier.fit(df)
    }

    fun predict(df: DataFrame): Pair<ArrayList<Double>, Double> {
        val (X_bow, y_bow) = classifier.transform(df)
        val prob = classifier.prob(X_bow)
        val (f1s, auc) = ROCAUC(prob, y_bow, cat)
        return Pair(f1s, auc)
    }

    private fun ROCAUC(
        prob: List<Double>,
        y: List<String>,
        cat: String
    ): Pair<ArrayList<Double>, Double> {
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
        var ind = 0
        val z = DoubleArray(2)


        for (i in 0 until 101) {
            val c = i / 100.0
            while (ind < a.size && a[ind].first < c) {
                z[a[ind].second]++
                ind++
            }
            nexs.add(z[0])
            pexs.add(z[1])
        }


        val pe = pexs[pexs.size - 1]
        val ne = nexs[nexs.size - 1]

        for (i in pexs.indices) {
            pexs[i] = pe - pexs[i]
            nexs[i] = ne - nexs[i]

        }
        val rec = ArrayList<Double>()
        val prec = ArrayList<Double>()

        for (i in pexs.indices) {
            rec.add(1.0 * pexs[i] / pexs[0])
        }

        for (i in pexs.indices) {
            if (pexs[i] + nexs[i] == 0.0)
                prec.add(1.0)
            else
                prec.add(pexs[i] / (nexs[i] + pexs[i]))
        }

        val f1s = ArrayList<Double>()
        for (i in 1 until prec.size) {
            f1s.add(2 * prec[i] * rec[i] / (prec[i] + rec[i]))
        }
        val auc = integrate(rec, prec)


        return Pair(f1s,  auc)
    }



}