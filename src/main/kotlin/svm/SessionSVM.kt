package svm

import krangl.DataFrame
import smile.classification.KNN
import smile.classification.SVM
import util.*

/**
 * Класс реализующий классификатор SVM один против всех для метрик между сессиями, через класс Session
 * @property cat имя категории
 */
class SessionSVM(val cat: String = "coding") {


    lateinit var knn: SVM<Session>

    /**
     *  Инициализирует SVM
     *  @param df train data
     *  @param c the soft margin penalty parameter
     *  @param tol the tolerance of convergence test
     */
    fun fit(df: DataFrame, c: Double, tol: Double) {
        val X = sessionsFromDF(df)
        val y = IntArray (X.size) { i -> if (X[i].cat.contains(cat)) 1 else -1}

        // для того, чтобы поменять метрику нужно заменить IntersectionKernel на нужную метрику
        knn = SVM.fit(X, y, IntersectionKernel(),  c, tol)
    }

    /**
     * Предсказывает метки для переданных сессий
     * @param df data for prediction
     * @return Массив меток
     */
    fun predict(df: DataFrame): IntArray {
        val X = sessionsFromDF(df)
        return knn.predict(X)
    }

    /**
     * Возвращает f1-score предсказания
     * @param df data for prediction
     * @return Массив меток
     */
    fun predictF1(df: DataFrame): Double {
        val X = sessionsFromDF(df)
        val y = Array<Int> (X.size) {i -> if (X[i].cat.contains(cat)) 1 else -1}
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
        val f1 = 2 * prec * rec / (prec + rec)
        if (f1 == f1)
            return f1
        return 0.0
    }
}