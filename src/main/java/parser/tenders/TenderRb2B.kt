package parser.tenders

import parser.tenderClasses.Oilb2b

class TenderRb2B(val tn: Oilb2b) : TenderAbstract(), ITender {

    init {
        etpName = "RB2B Электронная торговая площадка"
        etpUrl = "https://zakupki.rb2b.ru/"
    }

    override fun parsing() {}

    private fun addCounts(updated: Boolean) {
        if (updated) {
            UpdateTender++
        } else {
            AddTender++
        }
    }

    companion object TypeFz {
        const val typeFz = 291
    }
}
