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
import parser.tenderClasses.Tmk
import parser.tenders.TenderTmk
import parser.tools.formatterGpn
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserTmk :
    ParserAbstract(),
    IParser {
    private val tendersList = mutableListOf<TenderTmk>()
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
        const val BaseUrl = "https://zakupki.tmk-group.com/#com/procedure/index"
        const val timeoutB = 30L
        const val CountPage = 10
    }

    override fun parser() =
        parse {
            try {
                parserTmk()
            } catch (e: Exception) {
                logger("Error in parserSelen function", e.stackTrace, e)
            }
        }

    private fun getchromeOptions(): ChromeOptions {
        val options = ChromeOptions()
        // options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        return options
    }

    private fun parserTmk() {
        var tr = 0
        while (true) {
            try {
                options = getchromeOptions()
                driver = ChromeDriver(options)
                driver.manage().window().size = Dimension(1280, 1024)
                driver.manage().window().fullscreen()
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
                logger("error in TenderTmk.parsing()", e.stackTrace, e)
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
        Thread.sleep(5000)
        clickException()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class = 'x-grid3-body']/div[contains(@class, 'x-grid3-row')][25]"),
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

    private fun clickException() {
        try {
            val el = driver.findElement(By.xpath("//button[. = 'OK']"))
            el.click()
        } catch (e: Exception) {
            Thread.sleep(1000)
        }
    }

    private fun getNextPage() {
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[contains(@class, 'x-tbar-page-next')]"),
            ),
        )
        try {
            val paginator =
                driver.findElement(By.xpath("//button[contains(@class, 'x-tbar-page-next')]"))
            paginator.click()
            Thread.sleep(5000)
            clickException()
            getListTenders()
        } catch (e: Exception) {
            logger("Bad clicker", e.stackTrace, e)
        }
    }

    private fun getListTenders() {
        Thread.sleep(2000)
        driver.switchTo().defaultContent()
        val tenders =
            driver.findElements(
                By.xpath(
                    "//div[@class = 'x-grid3-body']/div[contains(@class, 'x-grid3-row')]//tr[1]",
                ),
            )
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
            el
                .findElementWithoutException(By.xpath(".//td[last()]/div/a[1]"))
                ?.getAttribute("href")
                ?.trim { it <= ' ' } ?: ""
        if (href == "") {
            logger("cannot href in tender")
            return
        }
        val purNum =
            el.findElementWithoutException(By.xpath(".//td[3]/div"))?.text?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender $href")
            return
        }
        val nameOrg =
            el.findElementWithoutException(By.xpath(".//td[7]/div"))?.text?.trim { it <= ' ' } ?: ""
        val purName =
            el.findElementWithoutException(By.xpath(".//td[8]/div"))?.text?.trim { it <= ' ' } ?: ""
        if (purName == "") {
            logger("cannot purName in tender $href")
            return
        }
        val status =
            el.findElementWithoutException(By.xpath(".//td[last()-1]/div"))?.text?.trim {
                it <= ' '
            } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath(".//td[10]/div"))?.text?.trim { it <= ' ' }
                ?: ""
        val datePub = datePubTmp.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            logger("cannot find datePub on page", href, purNum)
            return
        }
        val dateEndTmp =
            el.findElementWithoutException(By.xpath(".//td[12]/div"))?.text?.trim { it <= ' ' }
                ?: ""
        val dateEndR = dateEndTmp.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
        val dateEnd = dateEndR.getDateFromString(formatterGpn)
        val tt = Tmk(purNum, href, purName, dateEnd, datePub, status, nameOrg)
        val t = TenderTmk(tt, driver)
        tendersList.add(t)
    }
}
