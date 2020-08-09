package parser.tenders

import parser.tenderClasses.Oilb2b

class TenderOilb2b(val tn: Oilb2b) : TenderAbstract(), ITender {
    init {
        etpName = "«НЕФТЬ-B2B»"
        etpUrl = "https://oilb2bcs.ru/"
    }

    override fun parsing() {

    }
}