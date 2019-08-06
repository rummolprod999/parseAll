package parser.tenders

import parser.tenderClasses.Achi

class TenderAchi(val tn: Achi) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "Achizitii.md"
        etpUrl = "https://achizitii.md/"
    }

    val typeFz by lazy {
        207
    }

    override fun parsing() {
    }
}