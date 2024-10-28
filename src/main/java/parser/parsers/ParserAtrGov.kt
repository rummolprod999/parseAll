package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.logger.logger
import parser.tenderClasses.AtrGov
import parser.tenders.TendeAtrGov
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserAtrGov :
    ParserAbstract(),
    IParser {
    private val tendersS = mutableListOf<TendeAtrGov>()

    companion object WebCl {
        const val BaseUrl = "https://208.atr.gov.ru/bas/contracts/"
        const val timeoutB = 30L
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

    override fun parser() = parse { parserAtrGov() }

    private fun parserAtrGov() {
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
                    By.xpath("//strong[. = 'КОНКУРСНЫЙ отбор']"),
                ),
            )
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            tendersS.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in TenderZmo.parsing()", e.stackTrace, e, it.tn.href)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun getListTenders(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'js-store-grid-cont')]//div[contains(@class, 'js-product t-store__card')]"),
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
                    By.xpath("//div[contains(@class, 'js-store-grid-cont')]//div[contains(@class, 'js-product t-store__card')]"),
                )
            for (it in tenders) {
                try {
                    parserTender(it, driver)
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

    private fun parserTender(
        el: WebElement,
        driver: ChromeDriver,
    ) {
        // driver.switchTo().defaultContent()
        val urlT =
            el.findElementWithoutException(By.xpath(".//a"))?.getAttribute("href")?.trim {
                it <= ' '
            } ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender")
            throw Exception("cannot urlT in tender")
        }
        val name =
            el.findElementWithoutException(By.xpath(".//div[contains(@class, 'js-store-prod-name js-product-name')]"))?.text?.trim {
                it <=
                    ' '
            }
                ?: ""
        val purNum = urlT.getDataFromRegexp("tproduct\\/(\\d+-\\d+)")
        val tt = AtrGov(purNum, urlT, name)
        val t = TendeAtrGov(tt, driver)
        tendersS.add(t)
    }
}
