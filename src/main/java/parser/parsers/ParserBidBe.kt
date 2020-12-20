package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.BidBe
import parser.tenders.TenderBidBe
import parser.tools.formatterGpn
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserBidBe : IParser, ParserAbstract() {

    private val tendersS = mutableListOf<TenderBidBe>()

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserBidBe() }
    private fun parserBidBe() {
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
            val wait = WebDriverWait(driver, timeoutB)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//div[@class = 'v-card__text' and div[@class = 'row row--dense']])[position() > 1]")))
            Thread.sleep(5000)
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            tendersS.forEach {
                try {
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in ParserTender.parsing()", e.stackTrace, e, it.tn.href)
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
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//div[@class = 'v-card__text' and div[@class = 'row row--dense']])[position() > 1]")))
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders =
            driver.findElements(By.xpath("(//div[@class = 'v-card__text' and div[@class = 'row row--dense']])[position() > 1]"))
        for (it in tenders) {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
        return true
    }

    private fun parserTender(el: WebElement) {
        val href = el.findElementWithoutException(By.xpath(".//a[@class = 'с-link--line']"))?.getAttribute("href")
            ?.trim { it <= ' ' }
            ?: run { logger("href not found"); return }
        val purName =
            el.findElementWithoutException(By.xpath(".//a[@class = 'с-link--line']"))?.text?.trim { it <= ' ' }
                ?: run { logger("purName not found ${href}"); return }
        val purNumT =
            el.findElementWithoutException(By.xpath(".//div[@class = 'col col-auto' and contains(., 'Заказ')]"))?.text?.trim { it <= ' ' }
                ?: run { logger("purNumT not found ${href}"); return }
        val purNum = purNumT.getDataFromRegexp("Заказ #(\\d+)")
        val payMethod =
            el.findElementWithoutException(By.xpath(".//div[. = 'Способ оплаты']/following-sibling::div"))?.text?.trim { it <= ' ' }
                ?: ""
        val status =
            el.findElementWithoutException(By.xpath(".//div[. = 'Статус заказа']/following-sibling::div"))?.text?.trim { it <= ' ' }
                ?: ""
        val delivPlace =
            el.findElementWithoutException(By.xpath(".//div[. = 'Регион доставки']/following-sibling::div"))?.text?.trim { it <= ' ' }
                ?: ""
        val pubDateT =
            el.findElementWithoutException(By.xpath(".//div[. = 'Опубликовано (Мск)']/following-sibling::div"))?.text?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found ${href}"); return }
        val datePub = pubDateT.getDateFromString(formatterGpn)
        val endDateT =
            el.findElementWithoutException(By.xpath(".//div[@class = 'grey--text text--darken-1' and contains(. , 'Окончание приёма (Мск)')]/following-sibling::div"))?.text?.trim { it <= ' ' }
                ?: run { logger("endDateT not found ${href}"); return }
        val dateEnd = endDateT.getDateFromString(formatterGpn)
        val tt = BidBe(
            purNum,
            href,
            purName,
            datePub,
            dateEnd,
            payMethod,
            delivPlace,
            status
        )
        val t = TenderBidBe(tt)
        tendersS.add(t)

    }

    companion object WebCl {
        const val BaseUrl = "https://bidbe.ru/trade"
        const val timeoutB = 30L
    }
}