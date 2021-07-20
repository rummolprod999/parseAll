package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.Berel
import parser.tenders.TenderBerel
import parser.tools.formatterOnlyDate
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserBerel : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderBerel>()
    lateinit var driver: ChromeDriver
    lateinit var wait: WebDriverWait
    lateinit var options: ChromeOptions

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://berelcomp.ru/purchase/purchase-list/"
        const val timeoutB = 30L
    }

    override fun parser() = parse {
        try {
            parserBerel()
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

    private fun parserBerel() {
        var tr = 0
        while (true) {
            try {
                options = getchromeOptions()
                driver = ChromeDriver(options)
                try {
                    createTenderList()
                } catch (e: Exception) {
                    logger("error in createTenderList", e.stackTrace, e)
                }
                parserTenderList()
                break
            } catch (e: Exception) {
                tr++
                if (tr > 4) {
                    logger("Количество попыток истекло, выходим из программы")
                    break
                }
                logger("Error in ParserBerel function", e.stackTrace, e)
                e.printStackTrace()
            } finally {
                if (this::driver.isInitialized)
                    driver.quit()
            }
        }

    }

    private fun parserTenderList() {
        tendersList.forEach {
            try {
                ParserTender(it)
            } catch (e: Exception) {
                logger("error in TenderBerel.parsing()", e.stackTrace, e)
            }
        }
    }

    private fun createTenderList() {
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        driver.get(BaseUrl)
        driver.switchTo().defaultContent()
        wait = WebDriverWait(driver, timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class, 'purchase_item')]")))
        getListTenders()
    }

    private fun getListTenders() {
        Thread.sleep(2000)
        driver.switchTo().defaultContent()
        val tenders = driver.findElements(By.xpath("//div[@class = 'purchase_item']"))
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
            el.findElementWithoutException(By.xpath(".//div[@class = 'purchase_num']"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            return
        }
        val urlT = el.findElementWithoutException(By.xpath(".//a[@class = 'purchase_item_attachment left']"))
            ?.getAttribute("href")?.trim { it <= ' ' }
            ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender", purNum)
            throw Exception("cannot urlT in tender")
        }
        val purObj =
            el.findElementWithoutException(By.xpath(".//div[@class = 'purchase_item_text']"))?.text?.trim { it <= ' ' }
                ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath(".//div[@class = 'purchase_item_date']/span"))?.text?.trim()
                ?.trim { it <= ' ' }
                ?: ""
        val dateEndTmp =
            el.findElementWithoutException(By.xpath(".//div[@class = 'purchase_item_date purchase_item_marg']/span"))?.text?.trim()
                ?.trim { it <= ' ' }
                ?: ""
        val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
        val dateEnd = dateEndTmp.getDateFromString(formatterOnlyDate)
        val tt = Berel(purNum, urlT, purObj, datePub, dateEnd)
        val t = TenderBerel(tt)
        tendersList.add(t)

    }
}