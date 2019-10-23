package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.logger.logger
import parser.tenders.TenderTransAst
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserTransAst : IParser, ParserAbstract() {

    lateinit var drv: ChromeDriver
    var firstPage = true
    private lateinit var windowsSet: Set<String?>

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserTransAst() }
    private fun parserTransAst() {
        var tr = 0
        while (true) {
            try {

                BaseUrl.forEach { parserSelen(it) }
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

    private fun parserSelen(urlPage: String) {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        drv = ChromeDriver(options)
        drv.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        drv.manage().deleteAllCookies()
        drv.get(urlPage)
        try {
            for (i in 1..CountPage) {
                parserList(urlPage)
            }
            parserPageS()
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            drv.quit()
        }
    }

    private fun parserList(urlPage: String) {
        Thread.sleep(5000)
        drv.switchTo().defaultContent()
        val wait = WebDriverWait(drv, timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class = 'purch-reestr-tbl-div'][20]")))
        drv.switchTo().defaultContent()
        if (firstPage) {
            windowsSet = drv.windowHandles
            firstPage = false
        }
        val tenders = drv.findElements(By.xpath("//div[@class = 'purch-reestr-tbl-div']"))
        for ((index, value) in tenders.withIndex()) {
            try {
                parserTender(value, index)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e, urlPage)
            }
        }
        try {
            val js = drv as JavascriptExecutor
            js.executeScript("var us = document.querySelectorAll('#pageButton > span.pagerElem'); us[us.length-2].click();")
        } catch (e: Exception) {
        }
    }

    private fun parserTender(el: WebElement, ind: Int) {
        val eis = el.findElementWithoutException(By.xpath(".//span[@class = 'oosSpan']"))?.text?.trim { it <= ' ' }
                ?: ""
        if (eis != "") {
            //logger("This tender exist on EIS, return")
            return
        }
        val purNum = el.findElementWithoutException(By.xpath(".//span[@class = 'es-el-code-term']"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("can not find purNum in tender", el.text)
            return
        }
        try {
            val js = drv as JavascriptExecutor
            js.executeScript("document.querySelectorAll('div.sendApplication + a')[$ind].click();")
        } catch (e: Exception) {
            logger("document.querySelectorAll('div.sendApplication + a')[$ind].click();")
        }

    }

    private fun parserPageS() {
        val windowHandlers = drv.windowHandles
        windowHandlers.removeAll(windowsSet)
        windowHandlers.forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger(e)
            }
        }
    }

    private fun parserPage(window: String) {
        drv.switchTo().window(window)
        val tnd = TenderTransAst(drv)
        try {
            //println(it)
            ParserTender(tnd)
        } catch (e: Exception) {
            logger("error in ParserTender", e.stackTrace, e)
        }
    }

    companion object WebCl {
        val BaseUrl = listOf("http://utp.sberbank-ast.ru/Transneft/List/PurchaseList", "http://utp.sberbank-ast.ru/Transneft/List/PurchaseListSMiSP")
        const val timeoutB = 120L
        const val CountPage = 10
    }
}