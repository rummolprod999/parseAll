package parser.tenders

import parser.tenderClasses.ZakazRf

class TenderZakazRf(val tn: ZakazRf) : TenderAbstract(), ITender {
    init {
        etpName = "ЭТП \"Биржевая площадка\""
        etpUrl = "http://bp.zakazrf.ru/"
    }

    companion object TypeFz {
        const val typeFz = 297
    }

    override fun parsing() {
        println(tn)
    }
}