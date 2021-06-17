package parser.parsers

import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.ZakazRf
import parser.tenders.TenderZakazRf
import parser.tools.formatterEtpRfN
import java.util.concurrent.TimeUnit
import java.util.logging.Level


class ParserZakazRf : IParser, ParserAbstract() {

    private val tendersS = mutableListOf<TenderZakazRf>()

    init {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    override fun parser() = parse { parserZakazRf() }
    private fun parserZakazRf() {
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
        //options.addArguments("user-agent=${RandomUserAgent.randomUserAgent}")
        //options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE)
        val driver = ChromeDriver(options)
        try {
            driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
            driver.manage().deleteAllCookies()
            try {
                driver.get("http://bp.zakazrf.ru/Logon/Customers")
                driver.switchTo().defaultContent()
            } catch (f: UnhandledAlertException) {
                try {
                    val alert: Alert = driver.switchTo().alert()
                    val alertText: String = alert.getText()
                    println("Alert data: $alertText")
                    alert.accept()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            driver.switchTo().defaultContent()
            val wait = WebDriverWait(driver, timeoutB)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id = 'userName']")))
            driver.findElement(By.xpath("//input[@id = 'userName']")).sendKeys("Morozova_777")
            driver.findElement(By.xpath("//input[@id = 'password']")).sendKeys("Adv66%EgRt")
            driver.findElement(By.xpath("//button[@type = 'submit']")).click()
            Thread.sleep(5000)
            try {
                val alert: Alert = driver.switchTo().alert()
                val alertText: String = alert.getText()
                println("Alert data: $alertText")
                alert.accept()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(5000)
            driver.get(BaseUrl)
            try {
                val alert: Alert = driver.switchTo().alert()
                val alertText: String = alert.getText()
                println("Alert data: $alertText")
                alert.accept()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(5000)
            driver.switchTo().defaultContent()
            //driver.manage().window().maximize()
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@objecttype]/tbody/tr[@id]")))
            Thread.sleep(5000)
            driver.switchTo().defaultContent()
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

    private fun parserPageN(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@class, 'pager-button-next')]")))
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.querySelectorAll('a.pager-button-next')[0].click()")
        return getListTenders(driver, wait)
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@objecttype]/tbody/tr[@id][1]")))
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders = driver.findElements(By.xpath("//table[@objecttype]/tbody/tr[@id]"))
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
        val href = el.findElementWithoutException(By.xpath("./td[2]/a"))?.getAttribute("href")?.trim { it <= ' ' }
            ?: run { logger("href not found"); return }
        val purName = el.findElementWithoutException(By.xpath("./td[5]"))?.text?.trim { it <= ' ' }
            ?: el.findElementWithoutException(By.xpath("./td[4]"))?.text?.trim { it <= ' ' }
            ?: run { logger("purName not found ${href}"); return }
        val purNum = href.getDataFromRegexp("/id/([\\d.]+)")
        val status =
            el.findElementWithoutException(By.xpath("./td[16]"))?.text?.trim { it <= ' ' }
                ?: ""
        val okei =
            el.findElementWithoutException(By.xpath("./td[6]"))?.text?.trim { it <= ' ' }
                ?: ""
        val price =
            el.findElementWithoutException(By.xpath("./td[7]"))?.text?.trim { it <= ' ' }?.replace(",", "")
                ?.deleteAllWhiteSpace()
                ?: ""
        val quantity =
            el.findElementWithoutException(By.xpath("./td[8]"))?.text?.trim { it <= ' ' }?.replace(",", "")
                ?.deleteAllWhiteSpace()
                ?: ""
        val sum =
            el.findElementWithoutException(By.xpath("./td[9]"))?.text?.trim { it <= ' ' }?.replace(",", "")
                ?.deleteAllWhiteSpace()
                ?: ""
        val orgName =
            el.findElementWithoutException(By.xpath("./td[10]"))?.text?.trim { it <= ' ' }
                ?: ""
        val region =
            el.findElementWithoutException(By.xpath("./td[11]"))?.text?.trim { it <= ' ' }
                ?: ""
        val delivPlace =
            el.findElementWithoutException(By.xpath("./td[14]"))?.text?.trim { it <= ' ' }
                ?: ""
        val pubDateT =
            el.findElementWithoutException(By.xpath("./td[12]"))?.text?.trim { it <= ' ' }
                ?: run { logger("pubDateT not found ${href}"); return }
        val datePub = pubDateT.getDateFromString(formatterEtpRfN)
        val endDateT = el.findElementWithoutException(By.xpath("./td[13]"))?.text?.trim { it <= ' ' }
            ?: run { logger("endDateT not found ${href}"); return }
        val dateEnd = endDateT.getDateFromString(formatterEtpRfN)
        val tt = ZakazRf(
            purNum,
            href,
            purName,
            datePub,
            dateEnd,
            okei,
            price,
            quantity,
            sum,
            orgName,
            region,
            delivPlace,
            status
        )
        val t = TenderZakazRf(tt)
        tendersS.add(t)
    }

    companion object WebCl {
        const val BaseUrl = "http://bp.zakazrf.ru/DeliveryRequest"
        const val timeoutB = 30L
        const val CountPage = 20
    }

}