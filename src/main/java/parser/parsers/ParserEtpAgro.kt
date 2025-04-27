package parser.parsers

import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.EtpAgro
import parser.tenders.TenderEtpAgro
import parser.tools.formatterGpn
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserEtpAgro :
    ParserAbstract(),
    IParser {
    private val tendersS = mutableListOf<TenderEtpAgro>()

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

    override fun parser() = parse { parserEtpAgro() }

    private fun parserEtpAgro() {
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
            val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
            driver.get("https://zakupka.etpagro.ru/user/login")
            Thread.sleep(5000)
            driver.switchTo().defaultContent()
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@name = 'login']")),
            )
            driver.findElement(By.xpath("//input[@name = 'login']")).sendKeys("enter-it_1@mail.ru")
            driver.findElement(By.xpath("//input[@ppassword]")).sendKeys("mcfbJrBy73FYpN6)")
            // driver.findElement(By.xpath("//button[@pbutton]")).click()
            val js = driver as JavascriptExecutor
            js.executeScript("document.querySelectorAll('button[pbutton]')[1].click()")
            Thread.sleep(5000)
            driver.get(BaseUrl)
            driver.switchTo().defaultContent()
            // driver.manage().window().maximize()
            try {
                wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                        By.xpath(
                            "//div[contains(@class, 'p-grid p-nogutter')]/ef-widget-container[contains(@element_path, 'table')]",
                        ),
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
                    By.xpath("//button[contains(@class, 'p-paginator-next')]"),
                ),
            )
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript("document.querySelectorAll('button.p-paginator-next')[0].click()")
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
                    By.xpath(
                        "//div[contains(@class, 'p-grid p-nogutter')]/ef-widget-container[contains(@element_path, 'table')][1]",
                    ),
                ),
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders =
            driver.findElements(
                By.xpath(
                    "//div[contains(@class, 'p-grid p-nogutter')]/ef-widget-container[contains(@element_path, 'table')]",
                ),
            )
        for (it in tenders) {
            try {
                parserTender(it, driver)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
        return true
    }

    private fun parserTender(
        el: WebElement,
        driver: ChromeDriver,
    ) {
        val href =
            el
                .findElementWithoutException(By.xpath(".//a[@name = 'typeAndNumber']"))
                ?.getAttribute("href")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("href not found")
                    return
                }
        val purNumT =
            el.findElementWithoutException(By.xpath(".//a[@name = 'typeAndNumber']"))?.text?.trim {
                it <= ' '
            }
                ?: run {
                    logger("purNumT not found $href")
                    return
                }
        val purNum = purNumT.getDataFromRegexp("№(\\d+)")
        val purName =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//ef-widget-info[contains(@element_path, 'nameBlock.name')]//div[@efclasselement = 'value']/span",
                    ),
                )?.text
                ?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found $href")
                    return
                }
        val status =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//ef-widget-html[contains(@element_path, 'statusBlock.status')]//div[@efclasselement = 'value']/span",
                    ),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val endDateT =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//ef-widget-html[contains(@element_path, 'infoBlock.applicationDeadline')]//div[@efclasselement = 'value']/span",
                    ),
                )?.text
                ?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found $href")
                    return
                }
        val endDateR = endDateT.getDataFromRegexp("""(\d{2}\.\d{2}\.\d{4}\s\d{2}:\d{2})""")
        val dateEnd = endDateR.getDateFromString(formatterGpn)
        val tt = EtpAgro(purNum, href, purName, dateEnd, status)
        val t = TenderEtpAgro(tt, driver)
        tendersS.add(t)
    }

    companion object WebCl {
        const val BaseUrl = "https://zakupka.etpagro.ru/cabinet/procedure/all"
        const val timeoutB = 30L
        const val CountPage = 10
    }
}
