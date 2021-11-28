package parser.parsers

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.Alrosa
import parser.tenderClasses.AlrosaProduct
import parser.tenders.TenderAlrosa
import parser.tools.formatterOnlyDate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserAlrosa : IParser, ParserAbstract() {
    private val tendersList = mutableListOf<TenderAlrosa>()

    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl =
            "https://zakupki.alrosa.ru/info_pur_proc?sap-return-url=%2fsap%2fbc%2fnwbc#"
        const val timeoutB = 30L
        const val CountPage = 10
    }

    override fun parser() = parse { parserAlrosa() }
    private fun parserAlrosa() {
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
            val wait = WebDriverWait(driver, timeoutB)
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//tbody[@id = 'WD0120-contentTBody']/tr[@rr and @sst and @rt]")
                )
            )
            try {
                val js = driver as JavascriptExecutor
                js.executeScript("document.querySelector('#WD24').click()")
                Thread.sleep(10000)
                js.executeScript(
                    "document.querySelector('div[title = \"Дата начала приема предложений\"]').click()"
                )
                Thread.sleep(5000)
                js.executeScript(
                    "document.querySelector('div[title = \"Дата начала приема предложений\"]').click()"
                )
                Thread.sleep(5000)
            } catch (e: Exception) {
                logger(e)
            }
            driver.switchTo().defaultContent()
            getListTenders(driver, wait)
            tendersList.forEach {
                try {
                    // println(it)
                    ParserTender(it)
                } catch (e: Exception) {
                    logger("error in TenderZmo.parsing()", e.stackTrace, e)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun getListTenders(driver: ChromeDriver, wait: WebDriverWait): Boolean {
        Thread.sleep(5000)
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//tbody[@id = 'WD0120-contentTBody']/tr[@rr and @sst and @rt]")
                )
            )
        } catch (e: Exception) {
            logger("Error in wait tender table function")
            return false
        }
        var st = 2
        loop@ while (true) {
            driver.switchTo().defaultContent()
            val tenders =
                driver.findElements(
                    By.xpath("//tbody[@id = 'WD0120-contentTBody']/tr[@rr and @sst and @rt]")
                )
            for (it in tenders) {
                try {
                    parserTender(it, driver)
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

    private fun parserTender(el: WebElement, driver: ChromeDriver) {
        driver.switchTo().defaultContent()
        val purNum =
            el.findElementWithoutException(By.xpath("./td[3]/a"))?.text?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            throw Exception("cannot purNum in tender")
        }
        val purObj =
            el.findElementWithoutException(By.xpath("./td[5]/a"))?.text?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath("./td[10]/span/span"))?.text?.trim()?.trim {
                it <= ' '
            }
                ?: ""
        val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
        if (datePub == Date(0L)) {
            logger("cannot find pubDate on page", datePubTmp, purNum)
            return
        }
        val dateEndTmp =
            el.findElementWithoutException(By.xpath("./td[11]/span/span"))?.text?.trim()?.trim {
                it <= ' '
            }
                ?: ""
        val dateEnd = dateEndTmp.getDateFromString(formatterOnlyDate)
        if (dateEnd == Date(0L)) {
            logger("cannot find dateEnd on page", dateEndTmp, purNum)
            return
        }
        val status =
            el.findElementWithoutException(By.xpath("./td[8]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val pwName =
            el.findElementWithoutException(By.xpath("./td[7]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val nmckT =
            el.findElementWithoutException(By.xpath("./td[12]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val nmck = nmckT.replace(".", "").replace(",", ".").deleteAllWhiteSpace()
        val currency =
            el.findElementWithoutException(By.xpath("./td[13]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val contactPerson =
            el.findElementWithoutException(By.xpath("./td[14]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val phone =
            el.findElementWithoutException(By.xpath("./td[15]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val email =
            el.findElementWithoutException(By.xpath("./td[16]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val orgName =
            el.findElementWithoutException(By.xpath("./td[17]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val cusName =
            el.findElementWithoutException(By.xpath("./td[20]/span/span"))?.text?.trim { it <= ' ' }
                ?: ""
        val prod =
            try {
                getProducts(el, driver)
            } catch (e: Exception) {
                mutableListOf<AlrosaProduct>()
            }
        val tt =
            Alrosa(
                purNum,
                BaseUrl,
                purObj,
                datePub,
                dateEnd,
                status,
                pwName,
                cusName,
                nmck,
                currency,
                orgName,
                contactPerson,
                phone,
                email,
                prod
            )
        val tender = TenderAlrosa(tt)
        tendersList.add(tender)
        Thread.sleep(1000)
    }

    private fun getProducts(el: WebElement, driver: ChromeDriver): MutableList<AlrosaProduct> {
        val listProd: MutableList<AlrosaProduct> = mutableListOf()
        val p = el.findElement(By.xpath("./td[3]/a"))
        p.click()
        Thread.sleep(3000)
        driver.switchTo().defaultContent()
        driver.switchTo().frame(0)
        Thread.sleep(3000)
        val products =
            driver.findElements(By.xpath("//td[@class = 'urSTSStd']/table/tbody/tr[@rr and @sst]"))
        for (prod in products) {
            val pName =
                prod.findElementWithoutException(By.xpath("./td[3]/span/span"))?.text?.trim {
                    it <= ' '
                }
                    ?: ""
            if (pName == "") continue
            val quantT =
                prod.findElementWithoutException(By.xpath("./td[4]/span/span"))?.text?.trim {
                    it <= ' '
                }
                    ?: ""
            val quant = quantT.replace(".", "").replace(",", ".").deleteAllWhiteSpace()
            val okei =
                prod.findElementWithoutException(By.xpath("./td[5]/span/span"))?.text?.trim {
                    it <= ' '
                }
                    ?: ""
            val pr = AlrosaProduct(pName, quant, okei)
            listProd.add(pr)
        }
        //        val button = driver.findElement(By.xpath("//a[@action = 'close']//tbody/tr[@rr and
        // @sst]"))
        //        button.click()
        try {
            val js = driver as JavascriptExecutor
            js.executeScript("document.querySelector('a[action = \"close\"]').click()")
            js.executeScript(
                "var d = document.querySelectorAll('a[action = \"close\"]'); if(d.length>0){d[0].click()};"
            )
        } catch (e: Exception) {
            logger(e)
        }
        return listProd
    }
}
