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
import parser.tenders.TenderKomos
import parser.tools.formatterGpn
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserKomos :
    ParserAbstract(),
    IParser {

    companion object WebCl {
        const val BaseUrl = "https://komos.suppman.ru/"
        const val timeoutB = 30L
        const val CountPage = 10
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

    override fun parser() = parse { parserKom() }

    private fun parserKom() {
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
                    By.xpath("//div[@class = 'card-body']"),
                ),
            )
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            (1..CountPage).forEach { _ ->
                try {
                    //val res = parserPageN(driver, wait)
                    //if (!res) return@forEach
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

    private fun parserPageN(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                    "(//button[@class = 'page-link'])[last()]",
                ),
            ),
        )
        val js = driver as JavascriptExecutor
        js.executeScript("document.getElementsByClassName('page-link')[document.getElementsByClassName('page-link').length-1].click()")
        driver.switchTo().defaultContent()
        return getListTenders(driver, wait)
    }

    private fun getListTenders(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        Thread.sleep(2000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[@class = 'card-body']"),
                ),
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
                    By.xpath("//div[@class = 'card-body']"),
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
        val purNum =
            el.findElementWithoutException(By.xpath(".//div[@class = 'mt-1']/b"))?.text?.trim { it <= ' ' }
                ?.replace("№", "") ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            return
        }
        var urlT =
            el.getAttribute("onclick")?.replace("window.open('", "")?.replace("window.open('", "')")?.trim {
                it <= ' '
            } ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender", purNum)
            throw Exception("cannot urlT in tender")
        }
        urlT = "https://komos.suppman.ru${urlT}"
        val purObj = el.findElementWithoutException(By.xpath(".//a"))?.text?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath(".//span[contains(.,'Дата начала сбора предложений')]"))?.text?.replace(
                "Дата начала сбора предложений:",
                ""
            )?.trim { it <= ' ' } ?: ""
        val datePub = datePubTmp.getDateFromString(formatterGpn)
        val dateEndT = el.findElementWithoutException(By.xpath(".//span[contains(.,'Идут торги до')]"))?.text?.replace(
            "Идут торги до",
            ""
        )?.trim { it <= ' ' } ?: ""
        val dateEnd = dateEndT.getDateFromString(formatterGpn)
        val contactP = ""
        val tn = TenderKomos(urlT, contactP, purNum, purObj, "", datePub, dateEnd)
        tn.parsing()
    }
}