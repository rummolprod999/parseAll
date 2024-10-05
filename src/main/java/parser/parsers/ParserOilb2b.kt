package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.AttachOilb2b
import parser.tenderClasses.Oilb2b
import parser.tenderClasses.Oilb2bProduct
import parser.tenders.TenderOilb2b
import parser.tools.formatterGpn
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserOilb2b :
    ParserAbstract(),
    IParser {
    private val tendersS = mutableListOf<TenderOilb2b>()

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
        options.addArguments("headless")
        options.addArguments("disable-gpu")
        options.addArguments("no-sandbox")
        val driver = ChromeDriver(options)
        try {
            driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
            driver.manage().deleteAllCookies()
            driver.get(BaseUrl)
            driver.switchTo().defaultContent()
            // driver.manage().window().maximize()
            val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[. = 'Опубликована']"),
                ),
            )
            sleep(7000)

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
                    // println(it)
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

    private fun parserPageN(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[contains(@href, 'javascript:loadPage')]"),
                ),
            )
        } catch (e: Exception) {
            logger("next page not found")
            return false
        }
        val js = driver as JavascriptExecutor
        js.executeScript(
            "document.querySelectorAll('a[href=\"javascript:loadPage($i)\"]')[0].click()",
        )
        i++
        return getListTenders(driver, wait)
    }

    private fun getListTenders(
        driver: ChromeDriver,
        wait: WebDriverWait,
    ): Boolean {
        sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ul[@id = 'planClaimData']/li[1]"),
                ),
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        val tenders = driver.findElements(By.xpath("//ul[@id = 'planClaimData']/li"))
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
        val purNum =
            el
                .findElementWithoutException(By.xpath(".//span[contains(., 'Заявка')]"))
                ?.text
                ?.replace("Заявка №", "")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("purNum not found")
                    return
                }
        val purName =
            el
                .findElementWithoutException(
                    By.xpath(".//a[@class = 'name' and contains(@href, '/admin/')]"),
                )?.text
                ?.trim { it <= ' ' }
                ?: run {
                    logger("purName not found")
                    return
                }
        val href =
            el
                .findElementWithoutException(
                    By.xpath(".//a[@class = 'name' and contains(@href, '/admin/')]"),
                )?.getAttribute("href")
                ?.trim { it <= ' ' }
                ?: run {
                    logger("href not found")
                    return
                }
        val status =
            el
                .findElementWithoutException(By.xpath(".//span[contains(@class, 'status')]"))
                ?.text
                ?.trim { it <= ' ' } ?: ""
        val cusName = ""
        val pubDateT =
            el
                .findElementWithoutException(
                    By.xpath(".//div[. = 'Сформирована:']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val datePub = pubDateT.getDateFromString(formatterGpn)
        val endDateT =
            el
                .findElementWithoutException(
                    By.xpath(".//div[contains(., 'Окончание приема предложений ')]/p"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val dateEnd = endDateT.getDateFromString(formatterGpn)
        val tenderDate =
            el
                .findElementWithoutException(
                    By.xpath(".//div[. = 'Срок закупки:']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val endTenderDate =
            el
                .findElementWithoutException(
                    By.xpath(".//div[. = 'Срок исполнения договора:']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val attachments = mutableListOf<AttachOilb2b>()
        el.findElements(By.xpath(".//div/a[contains(@href, '/PlanClaimFiles')]")).forEach { at ->
            val hrefAtt = at.getAttribute("href") ?: return@forEach
            val nameAtt = at.text?.trim { it <= ' ' } ?: return@forEach
            attachments.add(
                AttachOilb2b(
                    hrefAtt,
                    if (nameAtt == "") {
                        "Документация"
                    } else {
                        nameAtt
                    },
                ),
            )
        }
        val purObgs = mutableListOf<Oilb2bProduct>()
        el
            .findElements(
                By.xpath(".//h4[. = 'Спецификация заявки']/following-sibling::table/tbody/tr"),
            ).forEach { prod ->
                val prodName =
                    prod.findElementWithoutException(By.xpath("./td[2]"))?.text?.trim { it <= ' ' }
                        ?: return@forEach
                val quant =
                    prod.findElementWithoutException(By.xpath("./td[3]"))?.text?.trim { it <= ' ' }
                        ?: ""
                val okei =
                    prod.findElementWithoutException(By.xpath("./td[4]"))?.text?.trim { it <= ' ' }
                        ?: ""
                val extDef =
                    prod.findElementWithoutException(By.xpath("./td[5]"))?.text?.trim { it <= ' ' }
                        ?: ""
                if (prodName != "") purObgs.add(Oilb2bProduct(prodName, quant, okei, extDef))
            }
        val tt =
            Oilb2b(
                purNum,
                href,
                purName,
                status,
                cusName,
                datePub,
                dateEnd,
                tenderDate,
                endTenderDate,
                purObgs,
                attachments,
            )
        val t = TenderOilb2b(tt, driver)
        tendersS.add(t)
    }

    companion object WebCl {
        const val BaseUrl = "https://oilb2bcs.ru/?pageTo=RegAgent&params=%5bType=1"
        const val timeoutB = 30L
        const val CountPage = 10
        var i = 2
    }
}
