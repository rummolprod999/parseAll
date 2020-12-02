package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.logger.logger
import parser.tenders.TenderOilb2b
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserRb2B : IParser, ParserAbstract() {

    private val tendersS = mutableListOf<TenderOilb2b>()

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserRb2b() }
    private fun parserRb2b() {
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
            //driver.manage().window().maximize()
            val wait = WebDriverWait(driver, timeoutB)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[. = 'Заявки на закупку']")))
            Thread.sleep(7000)
            driver.switchTo().defaultContent()
            val js = driver as JavascriptExecutor
            try {
                js.executeScript("document.querySelectorAll('#ext-gen76')[0].click()")
                js.executeScript("document.querySelectorAll('button.sp-btn-close')[0].click()")
            } catch (e: Exception) {
                logger("Error in parser function", e.stackTrace, e)
            }
            driver.switchTo().defaultContent()
            js.executeScript("document.querySelectorAll('span.x-tab-strip-text.icon-information')[1].click()")
            Thread.sleep(2000)
            driver.switchTo().defaultContent()
            driver.switchTo().frame("1505_IFrame")
            Thread.sleep(3000)
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
                    //println(it)
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

    private fun parserPageN(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@href, 'javascript:loadPage')]")))
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.querySelectorAll('a[href=\"javascript:loadPage(${i})\"]')[0].click()")
        i++
        return getListTenders(driver, wait)
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id = 'proc-list']/div[contains(@class, 'proc')][1]")))
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        var st = 2
        loop@ while (true) {
            driver.findElements(By.xpath("//a[contains(., 'Показать')]")).forEach {
                try {
                    it.click()
                    Thread.sleep(100)
                } catch (e: Exception) {
                    //logger("element is not clickable")
                }

            }
            driver.findElements(By.xpath("//a[span[contains(., 'Показать')]]")).forEach {
                try {
                    it.click()
                    Thread.sleep(100)
                } catch (e: Exception) {
                    //logger("element is not clickable")
                }

            }
            val tenders = driver.findElements(By.xpath("//div[@id = 'proc-list']/div[contains(@class, 'proc')]"))
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

    }

    companion object WebCl {
        val BaseUrl = "https://oilb2bcs.ru/?pageTo=RegAgent&params=%5bType=1"
        const val timeoutB = 30L
        const val CountPage = 10
        var i = 2
    }
}