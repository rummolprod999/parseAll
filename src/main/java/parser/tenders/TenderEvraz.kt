package parser.tenders

import org.openqa.selenium.chrome.ChromeDriver
import parser.tenderClasses.Evraz

class TenderEvraz(val tn: Evraz, val driver: ChromeDriver) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 189
    }

    init {
        etpName = "ЕВРАЗ"
        etpUrl = "http://supply.evraz.com/"
    }

    override fun parsing() {
        println(tn)
    }
}