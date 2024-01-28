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
import parser.tenderClasses.KurganKhim
import parser.tenders.TenderKurganKhim
import parser.tools.formatterGpn
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserKurganKhim : IParser, ParserAbstract() {

    private val tendersS = mutableListOf<TenderKurganKhim>()

    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://kurgankhimmash-zaproscen.ru/purchases-all/active"
        const val timeoutB = 30L
        const val CountPage = 7
    }

    override fun parser() = parse { parserKurgan() }

    private fun parserKurgan() {
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
                    By.xpath("//tbody/tr/div[contains(@class, 'v-card')]")
                )
            )

            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            (1..CountPage).forEach { _ ->
                try {
                    val res = parserPageN(driver, wait)
                    if (!res) return@forEach
                } catch (e: Exception) {
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }
            tendersS.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in parsing()", e.stackTrace, e, it.tn.href)
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
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@aria-label = 'Следующая страница']")
            )
        )
        val js = driver as JavascriptExecutor
        js.executeScript(
            "document.querySelectorAll('button[aria-label = \"Следующая страница\"]')[0].click()"
        )
        driver.switchTo().defaultContent()
        return getListTenders(driver, wait)
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//tbody/tr/div[contains(@class, 'v-card')]")
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
                driver.findElements(By.xpath("//tbody/tr/div[contains(@class, 'v-card')]"))
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

    private fun parserTender(el: WebElement, driver: ChromeDriver) {
        val purNum =
            el.findElementWithoutException(By.xpath(".//div/b[@class = 'font_size16']"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            throw Exception("cannot purNum in tender")
        }

        val urlTender =
            el.findElementWithoutException(By.xpath(".//div/b[@class = 'font_size16']/a"))
                ?.getAttribute("href")
                ?.trim { it <= ' ' } ?: ""
        if (urlTender == "") {
            logger("cannot urlT in tender", purNum)
            throw Exception("cannot urlT in tender")
        }
        val purName =
            el.findElementWithoutException(By.xpath(".//div/b[@class = 'font_size16']/a"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val orgName =
            el.findElementWithoutException(
                    By.xpath(".//div[b[@class = 'font_size16']]/following-sibling::div")
                )
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath(".//div[b[. = 'Дата начала подачи заявок:']]"))
                ?.text
                ?.replace("Дата начала подачи заявок:", "")
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val dateEndTmp =
            el.findElementWithoutException(
                    By.xpath(".//div[b[. = 'Дата окончания подачи заявок:']]")
                )
                ?.text
                ?.replace("Дата окончания подачи заявок:", "")
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val datePub = datePubTmp.getDateFromString(formatterGpn)
        val dateEnd = dateEndTmp.getDateFromString(formatterGpn)
        val attachments = mutableMapOf<String, String>()
        el.findElements(By.xpath(".//a[@title = 'Скачать']")).forEach {
            val urlAtt = "${it.getAttribute("href")}"
            val attName = it.text.trim { it <= ' ' }
            if (attName != "") {
                attachments[attName] = urlAtt
            }
        }
        val tn = KurganKhim(purNum, urlTender, purName, datePub, dateEnd, attachments, orgName)
        val t = TenderKurganKhim(tn, driver)
        tendersS.add(t)
    }
}
