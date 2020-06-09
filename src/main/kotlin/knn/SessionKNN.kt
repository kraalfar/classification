package knn

import koma.*
import koma.extensions.*
import krangl.DataFrame
import smile.classification.KNN
import util.DWTDist
import util.Session
import util.sessionsFromDF
import java.util.ArrayList

class SessionKNN(val cat: String = "coding") {

    lateinit var knn: KNN<Session>



    fun fit(df: DataFrame, k: Int) {
        val X = sessionsFromDF(df)
        val y = IntArray (X.size) { i -> if (X[i].cat.contains(cat)) 1 else 0}
        knn = KNN.fit(X, y, DWTDist(),  k)
    }

    fun predict(df: DataFrame): IntArray {
        val X = sessionsFromDF(df)
        val y = Array<Int> (X.size) {i -> if (X[i].cat.contains(cat)) 1 else 0}
        return knn.predict(X)
    }

    fun predictF1(df: DataFrame): Double {
        val X = sessionsFromDF(df)
        val y = Array<Int> (X.size) {i -> if (X[i].cat.contains(cat)) 1 else 0}
        val res = knn.predict(X)
        var tp = 0.0
        var tn = 0.0
        var fp = 0.0
        var fn = 0.0
        for (i in y.indices) {
            if (y[i] == 0) {
                if (res[i] == 0) {
                    tn++
                } else {
                    fn++
                }
            } else {
                if (res[i] == 1) {
                    tp++
                } else {
                    fp++
                }
            }
        }
        val prec = tp / (tp + fn)
        val rec = tp / (tp + fp)
        if (prec + rec == 0.0)
            return 0.0
        return 2 * prec * rec / (prec + rec)
    }
}