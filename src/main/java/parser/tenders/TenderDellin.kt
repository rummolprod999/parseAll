package parser.tenders

import parser.tenderClasses.Dellin

class TenderDellin(
    val tn: Dellin,
) : TenderAbstract(),
    ITender {
    companion object TypeFz {
        val typeFz = 258
    }

    init {
        etpName = "Электронная тендерная площадка ООО «Деловые линии»"
        etpUrl = "https://etp.dellin.ru/"
    }

    override fun parsing() {}
}
