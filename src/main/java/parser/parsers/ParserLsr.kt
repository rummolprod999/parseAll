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
import parser.tenderClasses.Lsr
import parser.tenders.TenderLsr
import parser.tools.formatterGpn
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserLsr : IParser, ParserAbstract() {
    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "http://zakupki.lsr.ru/Tenders#page="
        const val timeoutB = 120L
        const val CountPage = 30
    }

    override fun parser() = parse { parserLsr() }

    fun parserLsr() {
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
        //val wait = WebDriverWait(driver, timeoutB)
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        try {
            for (i in 1..CountPage) {
                val urlT = "$BaseUrl$i"
                try {
                    parserList(urlT, driver)
                } catch (e: Exception) {
                    logger("Error in parserList function", e.stackTrace, e)
                }
            }

        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun parserList(urlT: String, driver: ChromeDriver) {
        driver.get(urlT)
        driver.switchTo().defaultContent()
        val wait = WebDriverWait(driver, timeoutB)
        Thread.sleep(15000)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@role, 'row') and @id][10]")))
        val tenders = driver.findElements(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@role, 'row') and @id]"))
        for ((index, value) in tenders.withIndex()) {
            try {
                parserTender(value, index + 1)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e, urlT)
            }
        }
    }

    private fun parserTender(el: WebElement, ind: Int) {
        val purNum = el.findElementWithoutException(By.xpath(".//td[1]/a"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("can not find purNum in tender")
            return
        }
        val hrefL = el.findElementWithoutException(By.xpath(".//td[1]/a"))?.getAttribute("href")?.trim { it <= ' ' }
                ?: ""
        val hrefT = el.findElementWithoutException(By.xpath(".//td[2]/a"))?.getAttribute("href")?.trim { it <= ' ' }
                ?: ""
        if (hrefL == "" || hrefT == "") {
            logger("can not find hrefs in tender", purNum)
            return
        }
        var purName = el.findElementWithoutException(By.xpath(".//td[2]/a"))?.text?.trim { it <= ' ' }
                ?: ""
        val purNameL = el.findElementWithoutException(By.xpath(".//td[3]/a"))?.text?.trim { it <= ' ' }
                ?: ""
        if (purName != purNameL) {
            purName = "$purName $purNameL"
        }
        val pubDate = Date()
        var endDateT = el.findElementWithoutException(By.xpath(".//td[5]"))?.text?.trim { it <= ' ' }
                ?: ""
        if (endDateT == "") {
            endDateT = el.findElementWithoutException(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@class, 'child')][$ind]//span[contains(., 'Окончание приема заявок')]/following-sibling::span"))?.text?.trim { it <= ' ' }
                    ?: ""
        }
        val endDate = endDateT.getDateFromString(formatterGpn)
        if (endDate == Date(0L)) {
            logger("can not find date in tender", hrefL, endDateT)
            return
        }
        var status = el.findElementWithoutException(By.xpath(".//td[7]"))?.text?.trim { it <= ' ' }
                ?: ""
        if (status == "") {
            status = el.findElementWithoutException(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@class, 'child')][$ind]//span[contains(., 'Статус лота')]/following-sibling::span"))?.text?.trim { it <= ' ' }
                    ?: ""
        }
        var placingWayName = el.findElementWithoutException(By.xpath(".//td[8]"))?.text?.trim { it <= ' ' }
                ?: ""
        if (placingWayName == "") {
            placingWayName = el.findElementWithoutException(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@class, 'child')][$ind]//span[contains(., 'Способ проведения закупки')]/following-sibling::span"))?.text?.trim { it <= ' ' }
                    ?: ""
        }
        var nameCus = el.findElementWithoutException(By.xpath(".//td[6]"))?.text?.trim { it <= ' ' }
                ?: ""
        if (nameCus == "") {
            nameCus = el.findElementWithoutException(By.xpath("//table[@aria-describedby = 'grid_TenderGridViewModel_info']/tbody/tr[contains(@class, 'child')][$ind]//span[contains(., 'Заказчик')]/following-sibling::span"))?.getAttribute("href")?.trim { it <= ' ' }
                    ?: ""
        }
        val tn = Lsr(purNum, hrefT, hrefL, purName, pubDate, endDate, status, placingWayName, nameCus)
        val t = TenderLsr(tn)
        ParserTender(t)
    }
}