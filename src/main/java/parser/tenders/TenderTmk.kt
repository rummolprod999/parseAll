package parser.tenders

import org.openqa.selenium.chrome.ChromeDriver
import parser.tenderClasses.Tmk

class TenderTmk(val tn: Tmk, val driver: ChromeDriver) : TenderAbstract(), ITender {

    init {
        etpName = "ПАО «Трубная Металлургическая Компания»"
        etpUrl = "https://zakupki.tmk-group.com/"
    }

    override fun parsing() {

    }

    private fun addCounts(updated: Boolean) {
        if (updated) {
            UpdateTender++
        } else {
            AddTender++
        }
    }

    companion object TypeFz {
        const val typeFz = 186
    }
}