package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.ZmoKursk
import parser.tenders.TenderZmoYalta
import parser.tools.formatterGpn
import parser.tools.formatterOnlyDate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserZmoYalta : IParser, ParserAbstract() {
    private val tendersS = mutableListOf<TenderZmoYalta>()

    companion object WebCl {
        const val BaseUrl = "https://yalta-zmo.rts-tender.ru/"
        const val timeoutB = 30L
        const val CountPage = 10
    }

    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserYalta() }

    private fun parserYalta() {
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
                    By.xpath("//div[@class = 'wg-selectbox']/div[@class = 'select']")
                )
            )
            val js = driver as JavascriptExecutor
            js.executeScript("document.querySelectorAll('div.wg-selectbox div.select')[0].click()")
            js.executeScript(
                "document.querySelectorAll('div.wg-selectbox ul li:last-child')[0].click()"
            )
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            run mt@{
                (1..CountPage).forEach { _ ->
                    try {
                        val res = parserPageN(driver, wait)
                        if (!res) return@mt
                    } catch (e: Exception) {
                        logger("Error in parserPageN function", e.stackTrace, e)
                    }
                }
            }
            tendersS.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in TenderZmo.parsing()", e.stackTrace, e, it.tn.url)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun parserPageN(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        driver.switchTo().defaultContent()
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(
                        "//div[@class = 'paginator__page-selector']/a[contains(@class, 'paginator__next')]"
                    )
                )
            )
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.getElementsByClassName('paginator__next')[0].click()")
        driver.switchTo().defaultContent()
        return getListTenders(driver, wait)
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//table[@id = 'jqGrid']/tbody/tr[not(@class = 'jqgfirstrow')][1]")
                )
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        var st = 2
        loop@ while (true) {
            driver.switchTo().defaultContent()
            val tenders =
                driver.findElements(
                    By.xpath("//table[@id = 'jqGrid']/tbody/tr[not(@class = 'jqgfirstrow')]")
                )
            for (it in tenders) {
                try {
                    parserTender(it)
                } catch (e: Exception) {
                    st--
                    if (st == 0) {
                        logger("error in parserTender", e.stackTrace, e)
                        break@loop
                    }
                    logger("error in parserTender", e.stackTrace, e)
                    continue@loop
                }
            }
            return true
        }
        return true
    }

    private fun parserTender(el: WebElement) {
        // driver.switchTo().defaultContent()
        val purNum =
            el.findElementWithoutException(By.xpath("./td[2]/p"))?.text?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            throw Exception("cannot purNum in tender")
        }
        val urlT =
            el.findElementWithoutException(By.xpath("./td[4]/a"))?.getAttribute("href")?.trim {
                it <= ' '
            } ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender", purNum)
            throw Exception("cannot urlT in tender")
        }
        val purObj =
            el.findElementWithoutException(By.xpath("./td[4]/a"))?.text?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath("./td[6]/span"))?.text?.trim()?.trim {
                it <= ' '
            } ?: ""
        val dateEndTmp =
            el.findElementWithoutException(By.xpath("./td[7]/span"))?.text?.trim()?.trim {
                it <= ' '
            } ?: ""
        val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
        val dateEnd = dateEndTmp.getDateFromString(formatterGpn)
        val status =
            el.findElementWithoutException(By.xpath("./td[9]"))?.text?.trim { it <= ' ' } ?: ""
        val nmck =
            el.findElementWithoutException(By.xpath("./td[5]"))
                ?.text
                ?.replace(',', '.')
                ?.deleteAllWhiteSpace()
                ?.trim { it <= ' ' } ?: ""
        if (datePub == Date(0L) || dateEnd == Date(0L)) {
            logger("cannot find pubDate or dateEnd on page", urlT, purNum)
            return
        }
        val tt = ZmoKursk(status, purNum, purObj, nmck, datePub, dateEnd, urlT)
        val t = TenderZmoYalta(tt)
        tendersS.add(t)
    }
}
