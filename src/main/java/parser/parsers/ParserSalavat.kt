package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.Salavat
import parser.tenders.TenderSalavat
import parser.tools.formatterGpn
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserSalavat : IParser, ParserAbstract() {
    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "http://salavat-neftekhim.gazprom.ru/tenders"
        const val timeoutB = 120L
        const val CountPage = 5
    }

    override fun parser() = parse { parserSalavat() }

    fun parserSalavat() {
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

    fun parserSelen() {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        val driver = ChromeDriver(options)
        val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        try {
            driver.get(BaseUrl)
            try {
                parserPageN(driver, wait)
            } catch (e: Exception) {
                logger("Error in parserPageN function", e.stackTrace, e)
            }
            (1..CountPage).forEach {
                try {
                    parserPageN(driver, wait, it)
                } catch (e: Exception) {
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun parserPageN(driver: ChromeDriver, wait: WebDriverWait, np: Int = 0) {
        if (np != 0) {
            try {
                val js = driver as JavascriptExecutor
                js.executeScript(
                    "document.querySelectorAll('div.dataTables_paginate span[data-href = \"$np\"]')[0].click()"
                )
            } catch (e: Exception) {}
        }
        Thread.sleep(5000)
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                    "//section[@class = 'dataTables_wrapper']//table[contains(@class, 'text_data')]/tbody"
                )
            )
        )
        driver.switchTo().defaultContent()
        val tenders =
            driver.findElements(
                By.xpath(
                    "//section[@class = 'dataTables_wrapper']//table[contains(@class, 'text_data')]/tbody/tr"
                )
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
        val purNum =
            el.findElementWithoutException(By.xpath("./td[2]/p/a/span"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            return
        }
        val urlT =
            el.findElementWithoutException(By.xpath("./td[2]/p/a"))?.getAttribute("href")?.trim {
                it <= ' '
            } ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender", purNum)
            return
        }
        val purObj =
            el.findElementWithoutException(By.xpath("./td[2]/p[2]"))?.text?.trim { it <= ' ' } ?: ""
        val pwName =
            el.findElementWithoutException(By.xpath("./td[2]/p/span[@class = 'tender_type']"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val dateEndTmp =
            el.findElementWithoutException(By.xpath("./td[1]/p/span"))
                ?.text
                ?.trim()
                ?.replace("в ", "")
                ?.trim { it <= ' ' } ?: ""
        val dateEnd = dateEndTmp.getDateFromString(formatterGpn)
        if (dateEnd == Date(0L)) {
            logger("cannot find dateEnd on page", urlT, purNum)
            return
        }
        val cusName =
            el.findElementWithoutException(By.xpath("./td[3]/p"))?.text?.trim { it <= ' ' } ?: ""
        val orgName =
            el.findElementWithoutException(By.xpath("./td[4]/p"))?.text?.trim { it <= ' ' } ?: ""
        val tt = Salavat(purNum, urlT, purObj, dateEnd, pwName, cusName, orgName)
        val t = TenderSalavat(tt)
        ParserTender(t)
    }
}
