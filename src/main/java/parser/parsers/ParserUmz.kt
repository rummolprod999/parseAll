package parser.parsers

import com.google.gson.Gson
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.Umz
import parser.tenders.TenderUmz
import parser.tools.formatterOnlyDate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ParserUmz : IParser, ParserAbstract() {
    init {
        System.setProperty(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.NoOpLog"
        )
        java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.OFF
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    }

    companion object WebCl {
        const val BaseUrl = "http://umz-vrn.etc.ru/FKS/Home/PublicPurchaseList/PublishedRequest"
        const val timeoutB = 30L
        const val CountPage = 15
    }

    class UrlTen {
        val url: String? = null
        val title: String? = null
    }

    override fun parser() = parse { parserUmz() }

    fun parserUmz() {
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
        val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
        driver.manage().timeouts().pageLoadTimeout(timeoutB, TimeUnit.SECONDS)
        driver.manage().deleteAllCookies()
        driver.manage().window().maximize()
        try {
            driver.get(BaseUrl)
            try {
                wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[@class = 'paging']/i[contains(., 'Всего записей:')]")
                    )
                )
                driver.findElement(By.cssSelector("body")).sendKeys(Keys.END)
                driver.findElement(By.cssSelector("body")).sendKeys(Keys.RIGHT)
                Thread.sleep(5000)
                driver.switchTo().defaultContent()
                try {
                    val js = driver as JavascriptExecutor
                    js.executeScript(
                        """document.querySelectorAll('div.dataPager div span[onclick="\$.ETC.EventContainer.trigger(this,\'Grid.SetRowPerPage\',\'100\')"]')[0].click()"""
                    )
                } catch (e: Exception) {
                    logger(e)
                }
                driver.switchTo().defaultContent()
                parserPageN(driver, wait)
            } catch (e: TimeoutException) {
                logger("next page not found")
                return
            } catch (e: org.openqa.selenium.NoSuchElementException) {
                logger("next page not exist")
                return
            } catch (e: Exception) {
                if (e.message!!.contains("no such element")) {
                    logger("next page not exist")
                    return
                }
                logger("Error in parserPageN function", e.stackTrace, e)
            }
            (1..CountPage).forEach { pp ->
                try {
                    parserPageN(driver, wait, pp)
                } catch (e: TimeoutException) {
                    logger("next page not found")
                    return
                } catch (e: org.openqa.selenium.NoSuchElementException) {
                    logger("next page not exist")
                    return
                } catch (e: Exception) {
                    if (e.message!!.contains("no such element")) {
                        logger("next page not exist")
                        return
                    }
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            driver.quit()
        }
    }

    private fun parserPageN(driver: ChromeDriver, wait: WebDriverWait, np: Int = 0) {
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class = 'paging']/i[contains(., 'Всего записей:')]")
            )
        )
        driver.findElement(By.cssSelector("body")).sendKeys(Keys.END)
        Thread.sleep(5000)
        driver.switchTo().defaultContent()
        if (np == 0) {
            /*try {
                val js = driver as JavascriptExecutor
                js.executeScript("var d = document.querySelectorAll('div.rowPerPage span'); d[2].click()")
                //driver.findElementByXPath("//div[@class = 'rowPerPage']/span[3]").click()
            } catch (e: Exception) {
                println(e)
            }
            Thread.sleep(5000)*/
        }
        if (np != 0) {
            var c = 0
            while (true) {
                try {
                    try {
                        /* val js = driver as JavascriptExecutor
                        js.executeScript("document.querySelectorAll('div.dataPager div.paging span')[${np + 1}].click()")*/
                        // println("//div[@class = 'dataPager']/div/span[. = '${np + 1}']")
                        // driver.findElementByXPath("//div[@class = 'dataPager']/div/span[. = '${np
                        // + 1}']").click()
                        val js = driver as JavascriptExecutor
                        js.executeScript(
                            """document.querySelectorAll('div.dataPager div span[onclick="${'$'}.ETC.EventContainer.trigger(this,\'Grid.SetPage\',${np + 1})"]')[0].click()"""
                        )
                        break
                    } catch (e: JavascriptException) {
                        c++
                        driver
                            .findElement(
                                By.xpath("//div[@class = 'dataPager']/div/span[. = '${np + 1}']")
                            )
                            .click()
                        break
                    }
                } catch (e: Exception) {
                    if (c > 5) {
                        throw e
                    }
                    c++
                }
            }
        }

        Thread.sleep(5000)
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//table[contains(@class, 'datagrid')]/tbody")
            )
        )
        driver.switchTo().defaultContent()
        val tenders =
            driver.findElements(By.xpath("//table[contains(@class, 'datagrid')]/tbody/tr"))
        tenders.forEach {
            try {
                parserTender(it)
            } catch (e: Exception) {
                logger("error in parserTender", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: WebElement) {
        val purNum =
            el.findElementWithoutException(By.xpath("./td[4]"))?.text?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender")
            return
        }
        val urlT =
            el.findElementWithoutException(By.xpath("."))?.getAttribute("data-load")?.trim {
                it <= ' '
            } ?: ""
        if (urlT == "") {
            logger("cannot urlT in tender", purNum)
            return
        }
        val gson = Gson()
        val doc = gson.fromJson<UrlTen>(urlT, UrlTen::class.java)
        when {
            doc.url == null || doc.url == "" -> {
                logger("cannot doc.url in tender", purNum)
                return
            }
        }
        val urlTender = "http://umz-vrn.etc.ru${doc.url}"
        val purObj =
            el.findElementWithoutException(By.xpath("./td[5]"))?.text?.trim { it <= ' ' } ?: ""
        val pwName =
            el.findElementWithoutException(By.xpath("./td[6]"))?.text?.trim { it <= ' ' } ?: ""
        val datePubTmp =
            el.findElementWithoutException(By.xpath("./td[2]"))
                ?.text
                ?.trim()
                ?.replace("в ", "")
                ?.trim { it <= ' ' } ?: ""
        val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
        if (datePub == Date(0L)) {
            logger("cannot find datePub on page", urlTender, purNum)
            return
        }
        val cusName =
            el.findElementWithoutException(By.xpath("./td[8]"))?.text?.trim { it <= ' ' } ?: ""
        val status =
            el.findElementWithoutException(By.xpath("./td[9]"))?.text?.trim { it <= ' ' } ?: ""
        val nmckT =
            el.findElementWithoutException(By.xpath("./td[7]"))?.text?.trim { it <= ' ' } ?: ""
        val nmck = nmckT.replace("&nbsp;", "").deleteAllWhiteSpace()
        val tt = Umz(purNum, urlTender, purObj, datePub, pwName, cusName, nmck, status)
        val t = TenderUmz(tt)
        ParserTender(t)
    }
}
