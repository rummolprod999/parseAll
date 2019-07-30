import parser.tenderClasses.Uzex
import parser.tenders.ITender
import parser.tenders.TenderAbstract

class TenderUzex(val tn: Uzex) : TenderAbstract(), ITender {

    init {
        etpName = "АО \"Узбекская Республиканская товарно-сырьевая биржа\""
        etpUrl = "https://dxarid.uzex.uz/ru"
    }

    override fun parsing() {

    }
}