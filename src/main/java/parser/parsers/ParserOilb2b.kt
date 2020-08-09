package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.logger.logger
import parser.tenders.TenderZmoYalta
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserOilb2b : IParser, ParserAbstract() {
    private val tendersS = mutableListOf<TenderZmoYalta>()

    companion object WebCl {
        const val BaseUrl = "https://oilb2bcs.ru/?pageTo=RegAgent&params=%5bType=1"
        const val timeoutB = 30L
        const val CountPage = 10
        var i = 2
    }

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserOilb2b() }

    private fun parserOilb2b() {
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
        //options.addArguments("headless")
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
            sleep(7000)
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
            sleep(2000)
            driver.switchTo().defaultContent()
            driver.switchTo().frame("1505_IFrame")
            sleep(3000)
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
        driver.switchTo().frame("1505_IFrame")
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@href, 'javascript:loadPage')]")))
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.querySelectorAll('a[href='javascript:loadPage($i)']')[0].click()")
        i++
        driver.switchTo().frame("1505_IFrame")
        return getListTenders(driver, wait)
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        sleep(5000)
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id = 'proc-list']/div[contains(@class, 'proc')][1]")))
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        var st = 2
        loop@ while (true) {
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
        println(el.text)
    }

}