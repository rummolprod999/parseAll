package parser.tenders

import parser.tenderClasses.AgEat

class TenderAgEat(val tn: AgEat) : TenderAbstract(), ITender {

    init {
        etpName = "ЕАТ"
        etpUrl = "https://agregatoreat.ru/"
    }

    override fun parsing() {
        println(tn)
    }

    companion object TypeFz {
        const val typeFz = 112
    }
}