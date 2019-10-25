package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.logger.logger
import parser.tenderClasses.Evraz
import parser.tenders.TenderEvraz
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserEvraz : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderEvraz>()
    lateinit var driver: ChromeDriver
    lateinit var wait: WebDriverWait
    lateinit var options: ChromeOptions

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "https://supply.evraz.com/?hello_token=0"
        const val timeoutB = 30L
    }

    override fun parser() = parse {
        try {
            parserEvraz()
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

    private fun parserEvraz() {
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
                logger("Error in parserEvraz function", e.stackTrace, e)
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
                logger("error in TenderEvraz.parsing()", e.stackTrace, e)
            }
        }
    }

    private fun createTenderList() {
        //return //TODO change next week
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        driver.get(BaseUrl)
        driver.switchTo().defaultContent()
        wait = WebDriverWait(driver, timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class, 'company-name')]")))
        val collapsed = driver.findElementsByXPath("//div[contains(@class, 'company-name')]")
        collapsed.forEach {
            it.click()
            Thread.sleep(10000)
        }
        val collapsed1 = driver.findElementsByXPath("//div[contains(@class, 'plot-name')]")
        collapsed1.forEach {
            it.click()
            Thread.sleep(10000)
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@class, 'open_lot')]")))
        getListTenders()
    }

    private fun getListTenders() {
        Thread.sleep(2000)
        driver.switchTo().defaultContent()
        val tenders = driver.findElements(By.xpath("//a[contains(@class, 'open_lot')]"))
        tenders.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {

                logger("error in parserTender", e.stackTrace, e)

            }
        }
    }

    private fun parserTender(el: WebElement) {
        val purName = el.text.trim { it <= ' ' }
        if (purName == "") {
            logger("can not purName in tender")
            return
        }
        val urlT = el.getAttribute("url") ?: run {
            logger("can not urlT in tender $purName")
            return
        }
        val idiblock = el.getAttribute("idiblock") ?: run {
            logger("can not idiblock in tender $purName")
            return
        }
        val iblock_applick = el.getAttribute("iblock-applick") ?: run {
            logger("can not iblock_applick in tender $purName")
            return
        }
        val purNum = el.getAttribute("idelement") ?: run {
            logger("can not purNum in tender $urlT")
            return
        }
        val href = "https://supply.evraz.com/lot/index.php?ID=${purNum}&IBLOCK_ID=${idiblock}&IBLOCK_ID_APPLIK=${iblock_applick}&HISTORY_APPLIK=Y&modalWindow=Y"
        val tt = Evraz(purNum, href, purName)
        val t = TenderEvraz(tt, driver)
        tendersList.add(t)
    }
}