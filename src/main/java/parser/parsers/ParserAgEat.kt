package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.clickerExp
import parser.extensions.findElementWithoutException
import parser.logger.logger
import parser.tenderClasses.AgEat
import parser.tenders.TenderAgEat
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserAgEat : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderAgEat>()
    lateinit var driver: ChromeDriver
    lateinit var wait: WebDriverWait
    lateinit var options: ChromeOptions

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://agregatoreat.ru/purchases/all"
        const val timeoutB = 20L
        const val CountPage = 170
    }

    override fun parser() = parse {
        try {
            parserAgEat()
        } catch (e: Exception) {
            logger("Error in parserSelen function", e.stackTrace, e)
        }
    }

    private fun parserAgEat() {
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
                if (this::driver.isInitialized)
                    driver.quit()
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
        wait = WebDriverWait(driver, timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@href= '/purchases/all']")))
        try {
            driver.clickerExp("//a[@href= '/purchases/all']")
        } catch (e: Exception) {
            logger("Error in clickAll function", e.stackTrace, e)
            return true
        }
        getListTenders()
        (2..CountPage).forEach {
            try {
                getNextPage(it)
            } catch (e: Exception) {
                logger("Error in getNextPage function", e.stackTrace, e)
            }
        }
        return false
    }

    fun parserTenderList() {
        tendersList.forEach {
            try {
                ParserTender(it)
            } catch (e: Exception) {
                logger("error in TenderAgEat.parsing()", e.stackTrace, e)
            }
        }
    }

    private fun getchromeOptions(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        return options
    }

    private fun getNextPage(num: Int) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@class, 'ui-paginator-page') and .= '$num']")))
        try {
            val paginator = driver.findElementByXPath("//a[contains(@class, 'ui-paginator-page') and .= '$num']")
            paginator.click()
            getListTenders()
        } catch (e: Exception) {
            logger("Bad clicker", e.stackTrace, e)
        }
    }

    private fun getListTenders() {
        Thread.sleep(5000)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class = 'registry-sorting' and contains(., 'Сортировать:')]")))
        driver.switchTo().defaultContent()
        val tenders =
            driver.findElements(By.xpath("//article[@class = 'card p-grid']"))
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
            el.findElementWithoutException(By.xpath(".//h3[@id = 'tradeNumber']/a"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("can not purNum in tender")
            return
        }
        val purObj =
            el.findElementWithoutException(By.xpath(".//label[. = 'Наименование']/following-sibling::p"))?.text?.trim { it <= ' ' }
                ?: ""
        val status =
            el.findElementWithoutException(By.xpath(".//div[@id = 'purchaseStateDescription']//span"))?.text?.trim { it <= ' ' }
                ?: ""
        val urlT =
            el.findElementWithoutException(By.xpath(".//a[@id = 'tradeInfoLink']"))
                ?.getAttribute("href")?.trim { it <= ' ' }
                ?: ""
        if (urlT == "") {
            logger("can not urlT in tender", purNum)
            return
        }
        val tt = AgEat(purNum, urlT, purObj, status)
        val t = TenderAgEat(tt, driver)
        tendersList.add(t)
    }
}