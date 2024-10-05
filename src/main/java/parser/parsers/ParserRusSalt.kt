package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.RusSalt
import parser.tenders.TenderRusSalt
import parser.tools.formatterRs
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserRusSalt :
    ParserAbstract(),
    IParser {
    private val tendersList = mutableListOf<TenderRusSalt>()
    lateinit var driver: ChromeDriver
    lateinit var wait: WebDriverWait
    lateinit var options: ChromeOptions

    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog",
        )
        java.util.logging.Logger
            .getLogger("org.openqa.selenium")
            .level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://russalt.ru/tendery/"
        const val timeoutB = 30L
        const val CountPage = 10
    }

    override fun parser() =
        parse {
            try {
                parserRussalt()
            } catch (e: Exception) {
                logger("Error in parserSelen function", e.stackTrace, e)
            }
        }

    private fun getchromeOptions(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        return options
    }

    private fun parserRussalt() {
        var tr = 0
        while (true) {
            try {
                options = getchromeOptions()
                driver = ChromeDriver(options)
                driver.manage().window().size = Dimension(1920, 1080)
                // driver.manage().window().fullscreen()
                parserSelen()
                break
            } catch (e: Exception) {
                tr++
                if (tr > 4) {
                    logger("Количество попыток истекло, выходим из программы")
                    break
                }
                logger("Error in parserSelen function", e.stackTrace, e)
                e.printStackTrace()
            } finally {
                if (this::driver.isInitialized) driver.quit()
            }
        }
    }

    private fun parserTenderList() {
        tendersList.forEach {
            try {
                ParserTender(it)
            } catch (e: Exception) {
                logger("error in TenderRusSalt.parsing()", e.stackTrace, e)
            }
        }
    }

    private fun parserSelen() {
        try {
            if (createTenderList()) return
            parserTenderList()
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
            throw e
        }
    }

    fun createTenderList(): Boolean {
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        driver.get(BaseUrl)
        driver.switchTo().defaultContent()
        wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
        Thread.sleep(3000)
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class = 'tenders-item tenders-grid']"),
            ),
        )
        getListTenders()
        (1..CountPage).forEach { _ ->
            try {
                getNextPage()
            } catch (e: Exception) {
                logger("Error in getNextPage function", e.stackTrace, e)
            }
        }
        return false
    }

    private fun getNextPage() {
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//a[@class = 'next page-numbers']"),
            ),
        )
        try {
            val paginator = driver.findElement(By.xpath("//a[@class = 'next page-numbers']"))
            paginator.click()
            Thread.sleep(3000)
            getListTenders()
        } catch (e: Exception) {
            logger("Bad clicker", e.stackTrace, e)
        }
    }

    private fun getListTenders() {
        Thread.sleep(2000)
        driver.switchTo().defaultContent()
        val tenders = driver.findElements(By.xpath("//div[@class = 'tenders-item tenders-grid']"))
        tenders.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: WebElement) {
        val href =
            el.findElementWithoutException(By.xpath(".//a"))?.getAttribute("href")?.trim {
                it <= ' '
            } ?: ""
        if (href == "") {
            logger("cannot href in tender")
            return
        }
        val purNum =
            el
                .findElementWithoutException(
                    By.xpath(".//div[@class = 'tenders-item__text tenders-number']"),
                )?.text
                ?.replace("№", "")
                ?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender $href")
            return
        }
        val purName =
            el.findElementWithoutException(By.xpath(".//a"))?.text?.trim { it <= ' ' } ?: ""
        if (purName == "") {
            logger("cannot purName in tender $href")
            return
        }
        val status =
            el
                .findElementWithoutException(By.xpath(".//div[contains(@class, 'tenders-status')]"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val divT =
            el
                .findElementWithoutException(By.xpath(".//div[contains(@class, 'tenders-curator')]"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el
                .findElementWithoutException(By.xpath(".//div[contains(@class, 'tenders-start')]"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val datePub = datePubTmp.getDateFromString(formatterRs)
        if (datePub == Date(0L)) {
            logger("cannot find datePub on page", href, purNum)
            return
        }
        val dateEndTmp =
            el
                .findElementWithoutException(By.xpath(".//div[contains(@class, 'tenders-finish')]"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val dateEndR = dateEndTmp.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{2})""")
        val dateEnd = dateEndR.getDateFromString(formatterRs)
        val tt = RusSalt(purNum, href, purName, dateEnd, datePub, status, divT)
        val t = TenderRusSalt(tt, driver)
        tendersList.add(t)
    }
}
