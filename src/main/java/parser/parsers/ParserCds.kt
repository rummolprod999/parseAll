package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
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
import parser.tenderClasses.Cds
import parser.tenders.TenderCds
import parser.tools.formatterOnlyDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserCds :
    ParserAbstract(),
    IParser {
    private val tendersS = mutableListOf<TenderCds>()

    private var c = 0

    companion object WebCl {
        const val BaseUrl = "https://tender.cds.spb.ru/Tenders"
        const val timeoutB = 30L
        const val CountPage = 3
    }

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

    override fun parser() = parse { parserCds() }

    private fun parserCds() {
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
            val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//tr[contains(@id, 'gvTendersGrid')][position() > 2]"),
                ),
            )
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            (1..CountPage).forEach { _ ->
                try {
                    parserPageN(driver, wait)
                } catch (e: Exception) {
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }
            tendersS.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in ParserTender()", e.stackTrace, e, it.tn.purNum)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun parserPageN(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ) {
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a.dxp-button.dxp-bi")),
        )
        val js = driver as JavascriptExecutor
        if (c > 0) {
            js.executeScript("document.querySelectorAll('a.dxp-button.dxp-bi')[1].click()")
        } else {
            js.executeScript("document.querySelectorAll('a.dxp-button.dxp-bi')[0].click()")
        }
        c++
        Thread.sleep(5000)
        driver.switchTo().defaultContent()
        getListTenders(driver, wait)
    }

    private fun getListTenders(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ) {
        Thread.sleep(5000)
        driver.switchTo().defaultContent()
        val tenders =
            driver.findElements(By.xpath("//tr[contains(@id, 'gvTendersGrid')][position() > 2]"))
        for (it in tenders) {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: WebElement) {
        val purNum =
            el.findElementWithoutException(By.xpath("./td[2]"))?.text?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot find purNum in tender")
            throw Exception("cannot find purNum in tender")
        }
        val urlT = BaseUrl
        val purObj =
            el.findElementWithoutException(By.xpath("./td[3]"))?.text?.trim { it <= ' ' } ?: ""
        val delivTerm =
            el.findElementWithoutException(By.xpath("./td[6]"))?.text?.trim { it <= ' ' } ?: ""
        val delivPlace =
            el.findElementWithoutException(By.xpath("./td[4]"))?.text?.trim { it <= ' ' } ?: ""
        val contact =
            el.findElementWithoutException(By.xpath("./td[7]"))?.text?.trim { it <= ' ' } ?: ""
        val dates =
            el.findElementWithoutException(By.xpath("./td[5]"))?.text?.trim { it <= ' ' } ?: ""
        val datePubR =
            dates.getDataFromRegexp("""c\s+(\d{2}\.\d{2}\.\d{4})""").deleteDoubleWhiteSpace().trim()
        val datePub = datePubR.getDateFromString(formatterOnlyDate)
        if (datePub == Date(0L)) {
            run {
                logger("datePub was not found", purObj)
                throw Exception("datePub was not found")
            }
        }
        val dateEndR =
            dates
                .getDataFromRegexp("""по\s+(\d{2}\.\d{2}\.\d{4})""")
                .deleteDoubleWhiteSpace()
                .trim()
        var dateEnd = dateEndR.getDateFromString(formatterOnlyDate)
        if (dateEnd == Date(0)) {
            dateEnd =
                Date.from(
                    datePub
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusDays(2)
                        .toInstant(),
                )
        }
        val tt =
            Cds(
                purNum = purNum,
                href = urlT,
                purName = purObj,
                contact = contact,
                pubDate = datePub,
                endDate = dateEnd,
                delivTerm = delivTerm,
                delivPlace = delivPlace,
            )
        val t = TenderCds(tt)
        tendersS.add(t)
    }
}
