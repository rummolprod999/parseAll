package parser.parsers

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.logger.logger
import parser.tenders.TenderTmk
import java.util.logging.Level

class ParserTmk : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderTmk>()
    lateinit var driver: ChromeDriver
    lateinit var wait: WebDriverWait
    lateinit var options: ChromeOptions

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://zakupki.tmk-group.com/#com/procedure/index"
        const val timeoutB = 30L
        const val CountPage = 5
    }

    override fun parser() = parse {
        try {
            parserTmk()
        } catch (e: Exception) {
            logger("Error in parserSelen function", e.stackTrace, e)
        }
    }

    private fun parserTmk() {

    }
}