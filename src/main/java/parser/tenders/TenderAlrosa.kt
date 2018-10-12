package parser.tenders

import parser.tenderClasses.Alrosa

class TenderAlrosa(val tn: Alrosa) : TenderAbstract(), ITender {
    init {
        etpName = "ЭТП АЛРОСА"
        etpUrl = "https://zakupki.alrosa.ru"
    }

    override fun parsing() {

    }
}