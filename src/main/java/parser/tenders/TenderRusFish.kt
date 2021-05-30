package parser.tenders

import parser.tenderClasses.Aomsz

class TenderRusFish(val tn: Aomsz) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "«Русская Рыбопромышленная Компания»"
        etpUrl = "https://russianfishery.ru/"
    }

    val typeFz by lazy {
        322
    }

    override fun parsing() {
        println(tn)
    }
}