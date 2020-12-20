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
import parser.tenders.TenderNeftAst
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserNeftAst : IParser, ParserAbstract() {

    lateinit var drv: ChromeDriver
    var firstPage = true
    private lateinit var windowsSet: Set<String?>


    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserNeftAst() }
    private fun parserNeftAst() {
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
        executor.shutdown()
    }

    private fun parserSelen() {
        val options = ChromeOptions()
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        drv = ChromeDriver(options)
        drv.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        drv.manage().timeouts().setScriptTimeout(timeoutB, TimeUnit.SECONDS)
        drv.manage().deleteAllCookies()
        drv.get(BaseUrl)
        try {
            for (i in 1..CountPage) {
                parserList()
                parserPageS()
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            drv.quit()
        }
    }

    private fun parserList() {
        Thread.sleep(5000)
        if (!firstPage) {
            drv.switchTo().window(windowsSet.elementAt(0))
        } else {
            drv.switchTo().defaultContent()
        }
        val wait = WebDriverWait(drv, timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class = 'purch-reestr-tbl-div'][1]")))
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
                logger("error in parserTender", e.stackTrace, e, BaseUrl)
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
            logger("This tender exist on EIS")
        }
        val purNum =
            el.findElementWithoutException(By.xpath(".//span[@class = 'es-el-code-term']"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("can not find purNum in tender", el.text)
            return
        }
        try {
            val js = drv as JavascriptExecutor
            js.executeScript("document.querySelectorAll('#STRView')[$ind].click();")
        } catch (e: Exception) {
            logger("document.querySelectorAll('#STRView')[$ind].click();")
        }

    }

    private fun parserPageS() {
        val windowHandlers = drv.windowHandles
        //windowHandlers.removeAll(windowsSet)
        windowHandlers.forEach { t ->
            if (t == windowsSet.elementAt(0)) {
                return@forEach
            }
            try {
                /*val future: Future<Boolean> = executor.submit(Callable<Boolean> { parserPage(t) }) as Future<Boolean>
                try {
                    val s: Boolean = future.get(30, TimeUnit.SECONDS)
                } catch (ex: Exception) {
                    future.cancel(true)
                    throw ex
                } finally {
                    future.cancel(true)
                }*/
                parserPage(t)

            } catch (e: Exception) {
                logger(e)
            }
        }
    }

    private fun parserPage(window: String): Boolean {
        drv.switchTo().window(window)
        val tnd = TenderNeftAst(drv)
        try {
            ParserTender(tnd)
        } catch (e: Exception) {
            logger("error in ParserTender", e.stackTrace, e)
        }
        try {
            drv.close()
        } catch (e: Exception) {
            logger(e)
        }
        return true
    }

    companion object WebCl {
        const val BaseUrl = "http://utp.sberbank-ast.ru/Neft/List/PurchaseList"
        const val timeoutB = 60L
        const val CountPage = 10
        val executor = Executors.newSingleThreadExecutor()
    }
}