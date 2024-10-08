package parser.parsers

import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.ZakazRf
import parser.tenders.TenderZakazRfEx
import parser.tools.formatterEtpRfN
import parser.tools.formatterOnlyDate
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserZakazRfEx :
    ParserAbstract(),
    IParser {
    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog",
        )
        java.util.logging.Logger
            .getLogger("org.openqa.selenium")
            .level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    private val tendersS = mutableListOf<TenderZakazRfEx>()

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

        // options.addArguments("user-agent=${RandomUserAgent.randomUserAgent}")
        options.setCapability(
            CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR,
            UnexpectedAlertBehaviour.IGNORE,
        )
        val driver = ChromeDriver(options)
        driver.manage().window().size = Dimension(1280, 1024)
        driver.manage().window().fullscreen()
        try {
            driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
            driver.manage().deleteAllCookies()

            driver.get(BaseUrl)
            try {
                val alert: Alert = driver.switchTo().alert()
                val alertText: String = alert.getText()
                println("Alert data: $alertText")
                alert.accept()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            driver.get(BaseUrl)
            Thread.sleep(5000)
            driver.switchTo().defaultContent()
            val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
            // driver.manage().window().maximize()
            try {
                wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//table[@objecttype]/tbody/tr[position() > 1]"),
                    ),
                )
            } catch (e: Exception) {
                throw e
            }
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

    private fun parserPageN(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[contains(@class, 'pager-button-next')]"),
                ),
            )
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.querySelectorAll('a.pager-button-next')[0].click()")
        return getListTenders(driver, wait)
    }

    private fun getListTenders(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//table[@objecttype]/tbody/trr[position() > 1][1]"),
                ),
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders = driver.findElements(By.xpath("//table[@objecttype]/tbody/tr[position() > 1]"))
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
        val href =
            el.findElementWithoutException(By.xpath("./td[2]/a"))?.getAttribute("href")?.trim {
                it <= ' '
            }
                ?: run {
                    logger("href not found")
                    return
                }
        val purName =
            el.findElementWithoutException(By.xpath("./td[5]"))?.text?.trim { it <= ' ' }
                ?: el.findElementWithoutException(By.xpath("./td[4]"))?.text?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found $href")
                    return
                }
        val purNum =
            el.findElementWithoutException(By.xpath("./td[2]/a"))?.text?.trim { it <= ' ' } ?: ""
        val status =
            el.findElementWithoutException(By.xpath("./td[3]"))?.text?.trim { it <= ' ' } ?: ""
        val okei = ""
        val price =
            el
                .findElementWithoutException(By.xpath("./td[6]"))
                ?.text
                ?.trim { it <= ' ' }
                ?.replace(",", ".")
                ?.deleteAllWhiteSpace() ?: ""
        val orgName =
            el.findElementWithoutException(By.xpath("./td[7]"))?.text?.trim { it <= ' ' } ?: ""
        val pubDateT =
            el.findElementWithoutException(By.xpath("./td[10]"))?.text?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found $href")
                    return
                }
        val datePub = pubDateT.getDateFromString(formatterOnlyDate)
        val endDateT =
            el.findElementWithoutException(By.xpath("./td[12]"))?.text?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found $href")
                    return
                }
        val dateEnd = endDateT.getDateFromString(formatterEtpRfN)
        val tt =
            ZakazRf(
                purNum,
                href,
                purName,
                datePub,
                dateEnd,
                okei,
                price,
                "",
                "",
                orgName,
                "",
                "",
                status,
            )
        val t = TenderZakazRfEx(tt)
        tendersS.add(t)
    }

    companion object WebCl {
        const val BaseUrl = "http://zakazrf.ru/NotificationEx"
        const val timeoutB = 30L
        const val CountPage = 80
    }
}
