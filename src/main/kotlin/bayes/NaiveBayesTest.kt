package bayes

import krangl.DataFrame
import java.util.ArrayList

/**
 * Класс для тестирование наивного байесса
 */
class NaiveBayesTest(val cat: String) {

    var classifier = NaiveBayes(cat = cat)

    /**
     * Инициализирует словарь
     * @param df таблица с сессиями
     * @param limit порог, по которомы выкидываются редкие слова
     * @param top количество популярных слов, выкидываемых из словаря
     * @param n размер n-грам
     */
    fun initialize(df: DataFrame, limit: Int = 0, top: Int = 0, n: Int = 1) {
        classifier.initialize(df, limit, top, n)
    }

    /**
     * Подбириает параметры распределения по тестовым данным
     * @param df таблица с тестовыми данными
     */
    fun fit(df: DataFrame) {
        classifier.fit(df)
    }

    /**
     * Возвращает f1-score для разных трешхолдов предсказаний и auc
     * @param df data for prediction
     * @return пара из массива f1-score и auc
     */
    fun predictF1(df: DataFrame): Pair<ArrayList<Double>, Double> {
        val (X_bow, y_bow) = classifier.transform(df)
        val prob = classifier.prob(X_bow)
        val (f1s, auc) = ROCAUC(prob, y_bow, cat)
        return Pair(f1s, auc)
    }

    /**
     * Вычисляет ROCAUC
     * @param prob Массив вероятнейстей принадлежности сессии к классу
     * @param y массив из настоящих категорий сессий
     * @param cat нужная нам категория
     * @return пара из массива f1-score и auc
     */
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