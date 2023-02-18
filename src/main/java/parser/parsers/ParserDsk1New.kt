package parser.parsers

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteDoubleWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.Dsk1
import parser.tenders.TenderDsk1New
import parser.tools.formatterGpn
import parser.tools.formatterOnlyDate

class ParserDsk1New : IParser, ParserAbstract() {
    private val tendersS = mutableListOf<TenderDsk1New>()

    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserDsk1() }
    private fun parserDsk1() {
        var tr = 0
        while (true) {
            try {
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
            }
        }
    }

    private fun parserSelen() {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        val driver = ChromeDriver(options)
        try {
            driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
            driver.manage().deleteAllCookies()
            driver.get(BaseUrl)
            driver.switchTo().defaultContent()
            // driver.manage().window().maximize()
            val wait = WebDriverWait(driver, timeoutB)
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[. = 'Тендеры']"))
            )
            Thread.sleep(7000)

            getListTenders(driver, wait)

            tendersS.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in Tender.parsing()", e.stackTrace, e, it.tn.href)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[@class = 'table-item'][1]")
                )
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders = driver.findElements(By.xpath("//a[@class = 'table-item']"))
        for (it in tenders) {
            try {
                parserTender(it, driver)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
        return true
    }

    private fun parserTender(el: WebElement, driver: ChromeDriver) {
        val purName =
            el.findElementWithoutException(By.xpath(".//span[1]"))?.text?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val href =
            el?.getAttribute("href")?.trim { it <= ' ' }
                ?: run {
                    logger("href not found")
                    return
                }
        val purNum = href.getDataFromRegexp("about/tenders/(\\d+)")
        val cusName =
            el.findElementWithoutException(By.xpath(".//span[3]"))?.text?.trim { it <= ' ' } ?: ""
        val datePubT =
            el.findElementWithoutException(By.xpath(".//time[1]"))
                ?.text
                ?.trim { it <= ' ' }
                ?.deleteDoubleWhiteSpace()
                ?: ""
        var datePubR = datePubT.getDataFromRegexp("""c\s+(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
        var datePub = datePubR.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            datePubR = datePubT.getDataFromRegexp("""c\s+(\d{2}\.\d{2}\.\d{4})""")
            datePub = datePubR.getDateFromString(formatterOnlyDate)
        }

        val dateEndT =
            el.findElementWithoutException(By.xpath(".//time[2]"))
                ?.text
                ?.trim { it <= ' ' }
                ?.deleteDoubleWhiteSpace()
                ?: ""
        var dateEndR = dateEndT.getDataFromRegexp("""до\s+(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
        var dateEnd = dateEndR.getDateFromString(formatterGpn)
        if (dateEnd == Date(0L)) {
            dateEndR = dateEndT.getDataFromRegexp("""до\s+(\d{2}\.\d{2}\.\d{4})""")
            dateEnd = dateEndR.getDateFromString(formatterOnlyDate)
        }
        val tn = Dsk1(purNum, href, purName, datePub, dateEnd, cusName)
        val t = TenderDsk1New(tn, driver)
        tendersS.add(t)
    }

    companion object WebCl {
        const val BaseUrl = "https://www.dsk1.ru/about/tenders"
        const val timeoutB = 30L
        const val CountPage = 10
        var i = 2
    }
}
