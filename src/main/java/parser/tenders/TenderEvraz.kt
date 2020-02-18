package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.parsers.ParserEvraz
import parser.tenderClasses.Evraz
import parser.tools.formatter
import parser.tools.formatterOnlyDate
import java.lang.Thread.sleep
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderEvraz(val tn: Evraz, val driver: ChromeDriver) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 189
    }

    init {
        etpName = "ЕВРАЗ"
        etpUrl = "http://supply.evraz.com/"
    }

    override fun parsing() {
        val (purNum, url, purObj) = tn
        driver.get(url)
        driver.switchTo().defaultContent()
        val wait = WebDriverWait(driver, ParserEvraz.timeoutB)
        sleep(2000)
        driver.switchTo().defaultContent()
        val datePubTmp = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата публикации:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
                ?: ""
        var pubDate = datePubTmp.getDateFromString(formatterOnlyDate)
        if (pubDate == Date(0L)) {
            pubDate = Date()
        }
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[contains(., 'Дата принятия Технической части:')]")))
        } catch (e: Exception) {
            logger("date pub not found", tn.href)
            return
        }
        var dateEndTmp = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата продления Технической части:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
                ?: ""
        if (dateEndTmp == "") {
            dateEndTmp = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата принятия Технической части:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
        }
        if (dateEndTmp == "") {
            dateEndTmp = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата принятия Коммерческой части:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
        }
        var endDate = dateEndTmp.getDateFromString(formatter)
        if (endDate == Date(0L)) {
            endDate = dateEndTmp.getDateFromString(formatterOnlyDate)
        }
        if (endDate == Date(0L)) {
            logger("can not find dateEnd on page", dateEndTmp, url)
            return
        }
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ?").apply {
                setString(1, purNum)
                setInt(2, typeFz)
                setTimestamp(3, Timestamp(endDate.time))
            }
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            var cancelstatus = 0
            var updated = false
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?").apply {
                setString(1, purNum)
                setInt(2, typeFz)
            }
            val rs = stmt.executeQuery()
            while (rs.next()) {
                updated = true
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                    con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?").apply {
                        setInt(1, idT)
                        execute()
                        close()
                    }
                } else {
                    cancelstatus = 1
                }
            }
            rs.close()
            stmt.close()
            var idOrganizer = 0
            val fullnameOrg = "ЕВРАЗ"
            if (fullnameOrg != "") {
                val stmto = con.prepareStatement("SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?")
                stmto.setString(1, fullnameOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    idOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val postalAdr = ""
                    val factAdr = ""
                    val inn = ""
                    val kpp = ""
                    val email = ""
                    val phone = ""
                    val contactPerson = ""
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?", Statement.RETURN_GENERATED_KEYS).apply {
                        setString(1, fullnameOrg)
                        setString(2, postalAdr)
                        setString(3, email)
                        setString(4, phone)
                        setString(5, factAdr)
                        setString(6, contactPerson)
                        setString(7, inn)
                        setString(8, kpp)
                        executeUpdate()
                    }
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idOrganizer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val idEtp = getEtp(con)
            var idPlacingWay = 0
            val noticeVer = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дополнительная информация:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            val idRegion = 0
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(pubDate.time))
            insertTender.setString(4, url)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, idOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, noticeVer)
            insertTender.setString(15, url)
            insertTender.setString(16, url)
            insertTender.setInt(17, idRegion)
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            addCounts(updated)
            val documents = driver.findElements(By.xpath("//p[contains(., 'Техническая документация к лоту:')]/a"))
            getDocs(documents, con, idTender)
            var idLot = 0
            val lotNumber = 1
            val currency = ""
            val insertLot = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?", Statement.RETURN_GENERATED_KEYS).apply {
                setInt(1, idTender)
                setInt(2, lotNumber)
                setString(3, currency)
                executeUpdate()
            }
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            val delivTerm1 = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата начала работ:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
            val delivTerm2 = driver.findElementWithoutException(By.xpath("//p[contains(., 'Дата окончания работ:')]/strong"))?.text?.trim()?.trim { it <= ' ' }
            val delivTerm = "Дата начала работ: $delivTerm1 Дата окончания работ: $delivTerm2".trim()
            if (delivTerm != "") {
                con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?").apply {
                    setInt(1, idLot)
                    setInt(2, idCustomer)
                    setString(3, delivTerm)
                    executeUpdate()
                    close()
                }
            }
            con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?").apply {
                setInt(1, idLot)
                setInt(2, idCustomer)
                setString(3, purObj)
                executeUpdate()
                close()
            }
            afterParsing(idTender, con, purNum)
        })
    }

    private fun afterParsing(idTender: Int, con: Connection, purNum: String) {
        try {
            tenderKwords(idTender, con)
        } catch (e: Exception) {
            logger("Ошибка добавления ключевых слов", e.stackTrace, e)
        }

        try {
            addVNum(con, purNum, typeFz)
        } catch (e: Exception) {
            logger("Ошибка добавления версий", e.stackTrace, e)
        }
    }

    private fun addCounts(updated: Boolean) {
        if (updated) {
            UpdateTender++
        } else {
            AddTender++
        }
    }

    private fun getDocs(documents: MutableList<WebElement>, con: Connection, idTender: Int) {
        documents.forEach {
            val href = it.getAttribute("href")
            val nameDoc = it.text.trim().trim { rr -> rr <= ' ' }
            if (href != "") {
                val insertDoc = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                insertDoc.setInt(1, idTender)
                insertDoc.setString(2, nameDoc)
                insertDoc.setString(3, href)
                insertDoc.executeUpdate()
                insertDoc.close()
            }
        }
    }
}