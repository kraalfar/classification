package bayes

import koma.*
import koma.extensions.*
import krangl.DataFrame
import util.filter
import java.util.ArrayList


/**
 * Класс, реализующий классификатор наивного байеса один против всех.
 * @property alpha константа регуляризации
 * @property cat категория которую хотим предсказывать
 * @property p априорная вероятность встретить катгорию
 * @property probs распределение вероятностей для каждого слова из словаря
 * @property voc Словарь из оставшихся действий (действию соотвествуюет его номер)
 * @property id2word Массив из всех слов в словаре
 * @property n размер n-грам для словаря
 */
class NaiveBayes(private val alpha: Double = 1e-5, val cat: String = "coding") {


    var p: Double = 0.0
    var probs: ArrayList<koma.matrix.Matrix<Double>> = ArrayList()

    var voc: Map<String, Int> = HashMap()
    lateinit var id2word: List<String>
    private var n: Int = 0

    /**
     * Инициализирует словарь
     * @param df таблица с сессиями
     * @param limit порог, по которомы выкидываются редкие слова
     * @param top количество популярных слов, выкидываемых из словаря
     * @param n размер n-грам
     */

    fun initialize(df: DataFrame, limit: Int = 5, top: Int = 0, n: Int = 1) {
        this.n = n
        val events = df["events"]
        val eventsMap: MutableMap<String, Int> = HashMap()

        for (i in 0 until events.length) {
            val eventsArr = filter(df["events"][i].toString().split(" , "))
            for (j in 0 until eventsArr.size - n + 1) {
                val ev = eventsArr.slice(j until j + n).joinToString()
                if (eventsMap.containsKey(ev)) {
                    eventsMap[ev] = eventsMap.getValue(ev) + 1
                } else {
                    eventsMap[ev] = 1
                }
            }
        }

        id2word = eventsMap.filter { it.value > limit }
            .toSortedMap(compareBy({ eventsMap[it]?.times(-1) }, { it })).keys.toList()

        id2word = id2word.slice(top until id2word.size)
        voc = id2word.withIndex().toList().associate { it.value to it.index }
    }

    /**
     * Преобразует сессии в мешок слов
     * @param df таблица с сессиями
     * @return пара из массива мешков слов и меток категории сессии
     */
    fun transform(df: DataFrame): Pair<koma.matrix.Matrix<Double>, List<String>> {
        val mat = zeros(df.nrow, voc.size + 2)
        val label = ArrayList<String>()
        for (i in 0 until df.nrow) {
            label.add(df["Category"][i].toString())

            val eventsArr = filter(df["events"][i].toString().split(" , "))
            for (j in 0 until eventsArr.size - n + 1) {
                val event = eventsArr.slice(j until j + n).joinToString()
                if (voc.containsKey(event)) {
                    mat[i, voc[event] as Int] += 1
                }
            }
        }
        return Pair(mat, label)
    }

    /**
     * Приближает распределение параметров по тестовой выборке
     * @param df таблица с тестовыми данными
     */
    fun fit (df: DataFrame) {
        val (X, y) = transform(df)

        probs.add(ones(X.shape()[1], 11) * alpha)
        probs.add(ones(X.shape()[1], 11) * alpha)

        for (i in y.indices) {
            var cls = 0
            if (!y[i].toLowerCase().contains(cat))
                cls = 1
            else
                p++


            for (j in 0 until X.shape()[1]) {
                val ind: Int = if (X[i, j] < 10.0) X[i, j].toInt() else 10
                probs[cls][j, ind] += 1

            }

        }

        p /= y.size
        for (cls in 0 until 2) {
            var probsMatrix = probs[cls]
            val den = (1 + alpha) * probsMatrix.getRow(0).elementSum().toInt()
            probsMatrix /= den
            probs[cls] = probsMatrix
        }
    }

    /**
     * Вычисляет вероятность каждой сессии иметь категорию cat
     * @param X матрица, каждая строчка которой представляет из себя мешок слов для отдельной сессии
     * @return массив вероятностей
     */
    fun prob(X: koma.matrix.Matrix<Double>): List<Double> {
        val ans = zeros(X.shape()[0], 2)
        for (i in 0 until X.shape()[0]) {
            for (cls in 0 until 2) {
                ans[i, cls] = kotlin.math.ln(p * (1 - cls) + (1 - p) * cls)
                for (k in 0 until X.shape()[1]) {
                    val ind = if (X[i, k] < 10.0) X[i, k].toInt() else 10
                    ans[i, cls] += kotlin.math.ln(probs[cls][k, ind])
                }
            }
        }
        val q = fill(
            ans.shape()[0],
            ans.shape()[1]
        ) { i, j -> 1 / (exp(ans[i, 0 until ans.shape()[1]] - ans[i, j]).mean() * ans.shape()[1]) }
        return q.getCol(0).toList()
    }

    /**
     * Для каждого слова из словаря считает вклад его в полученное предсказание
     * @param X матрица, каждая строчка которой представляет из себя мешок слов для отдельной сессии
     * @return массив для каждой сессии из запроса, состоящий из массив вкладов для каждого из слов словаря
     */
    fun expl(X: koma.matrix.Matrix<Double>): ArrayList<List<Double>> {
        val ans = ArrayList<List<Double>>()

        for (i in 0 until X.shape()[0]) {
            val semi = zeros(X.shape()[1], 1)

            for (k in 0 until X.shape()[1]) {
                val ind = if (X[i, k] < 10.0) X[i, k].toInt() else 10
                semi[k] += kotlin.math.ln(probs[0][k, ind]) - kotlin.math.ln(probs[1][k, ind])
            }
            ans.add(semi.toList())
        }

        return ans
    }

    /**
     * Находит самые значимые событий по вкладам
     * @param expl массив для каждой сессии из запроса, состоящий из массив вкладов для каждого из слов словаря
     * @param n топ самых значимых действий
     * @return Пару из двух списков, в которых хранятся все действия, которые попадали в топ n самых
     * положительно/отрицательно значимых. Если действие попадало несколько раз, то и будет встречаться
     * оно несколько раз.
     */
    private fun impact(
        expl: ArrayList<List<Double>>,
        n: Int = 5
    ): Pair<ArrayList<List<String>>, ArrayList<List<String>>> {
        val pos = ArrayList<List<String>>()
        val negs = ArrayList<List<String>>()

        for (row in expl) {
            val cv = ArrayList<Pair<String, Double>>()

            for (j in id2word.indices) {
                cv.add(Pair(id2word[j], row[j]))
            }

            cv.sortBy { -it.second }
            pos.add(cv.slice(0 until n).map { it.first })
            negs.add(cv.slice(cv.size - n until cv.size).map { it.first })
        }
        return Pair(pos, negs)
    }

    /**
     * Объединяет expl и impact
     */
    fun getPosNeg(X: koma.matrix.Matrix<Double>): Pair<ArrayList<List<String>>, ArrayList<List<String>>> {
        val expl = expl(X)
        return impact(expl)
    }

}