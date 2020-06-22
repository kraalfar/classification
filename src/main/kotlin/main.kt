import bayes.trainTestSplit
import knn.BoWKNN

fun main() {

    val (train, test) = trainTestSplit("data/test/test_data.tsv")

    val cl = BoWKNN("coding")
    cl.initialize(train, 0, 0)
    cl.fit(train, 3)
    print(cl.predictF1(test))
}