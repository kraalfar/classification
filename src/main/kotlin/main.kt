import bayes.*

fun main() {
    val (train, test) = trainTestSplit("data/sh_sessions_multi.tsv")

    val bow = BoW(100)
    bow.initialize(train)

    val (X_bow, y_bow) = bow.transform(train)
    val (X_bow_test, y_bow_test) = bow.transform(test)
    val classificator = NaiveBayesCounter(1e-4)
    classificator.fit(X_bow, y_bow)

    val ans = classificator.predict(X_bow_test)
    var tot = 0.0
    for(i in y_bow_test.indices) {
        if (y_bow_test[i].contains(ans[i])) {
            tot += 1
        }
    }
    println(tot / y_bow_test.size)
//
//    ans = classificator.predict(X_bow_test, mode = "min")
//    tot = 0.0
//    for(i in y_bow_test.indices) {
//        if (y_bow_test[i].contains(ans[i])) {
//            tot += 1
//        }
//    }
//    println(tot / y_bow_test.size)
}
