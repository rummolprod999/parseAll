package parser.parsers

import parser.tenders.TenderAlrosa
import java.util.logging.Level

class ParserAlrosa : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderAlrosa>()

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserAlrosa() }
    private fun parserAlrosa() {

    }
}