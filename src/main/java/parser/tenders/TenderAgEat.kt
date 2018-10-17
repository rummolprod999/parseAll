package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.parsers.ParserAgEat
import parser.tenderClasses.AgEat
import parser.tools.formatterGpn
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderAgEat(val tn: AgEat, val driver: ChromeDriver) : TenderAbstract(), ITender {

    init {
        etpName = "ЕАТ"
        etpUrl = "https://agregatoreat.ru/"
    }

    override fun parsing() {
        val (purNum, url, purObj, status) = tn
        driver.get(url)
        driver.switchTo().defaultContent()
        val wait = WebDriverWait(driver, ParserAgEat.timeoutB)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(., 'Дата и время начала процедуры дисконтирования')]")))
        val datePubTmp = driver.findElementWithoutException(By.xpath("//div[contains(., 'Дата и время начала процедуры дисконтирования')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                ?: ""
        val pubDate = datePubTmp.getDateFromString(formatterGpn)
        if (pubDate == Date(0L)) {
            logger("can not find pubDate on page", datePubTmp, url)
            return
        }
        val dateEndTmp = driver.findElementWithoutException(By.xpath("//div[contains(., 'Дата и время окончания процедуры дисконтирования')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                ?: ""
        val endDate = dateEndTmp.getDateFromString(formatterGpn)
        if (endDate == Date(0L)) {
            logger("can not find dateEnd on page", dateEndTmp, url)
            return
        }
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?").apply {
                setString(1, purNum)
                setTimestamp(2, Timestamp(pubDate.time))
                setInt(3, typeFz)
                setTimestamp(4, Timestamp(endDate.time))
                setString(5, status)
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
            var orgName = driver.findElementWithoutException(By.xpath("//div[contains(., 'Наименование / ФИО')]/following-sibling::div"))?.text?.trim { it <= ' ' }
                    ?: ""
            val fullnameOrg = orgName.getDataFromRegexp("(.+)/")
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
                    val factAdr = driver.findElementWithoutException(By.xpath("//div[contains(., 'Адрес')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val inn = ""
                    val kpp = ""
                    val email = driver.findElementWithoutException(By.xpath("//div[contains(., 'Адрес электронной почты')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val phone = driver.findElementWithoutException(By.xpath("//div[contains(., 'Номер контактного телефона')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
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
            val placingWayName = driver.findElementWithoutException(By.xpath("//div[contains(., 'Тип закупки')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            if (placingWayName != "") {
                idPlacingWay = getPlacingWay(con, placingWayName)
            }
            val regionName = driver.findElementWithoutException(By.xpath("//div[contains(., 'Регион поставки')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            val idRegion = getIdRegion(con, regionName)
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
            insertTender.setString(14, status)
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
            val documents = driver.findElements(By.xpath("//div[contains(., 'Регион поставки')]/following-sibling::div/a"))
            getDocs(documents, con, idTender)
            val nmck = driver.findElementWithoutException(By.xpath("//div[contains(., 'Стартовая цена, руб.')]/following-sibling::div"))?.text?.deleteAllWhiteSpace()?.replace(",", ".")?.trim()?.trim { it <= ' ' }
                    ?: ""
            var idLot = 0
            val lotNumber = 1
            val currency = "руб."
            val insertLot = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS).apply {
                setInt(1, idTender)
                setInt(2, lotNumber)
                setString(3, currency)
                setString(4, nmck)
                executeUpdate()
            }
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            if (fullnameOrg != "") {
                val stmtoc = con.prepareStatement("SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1")
                stmtoc.setString(1, fullnameOrg)
                val rsoc = stmtoc.executeQuery()
                if (rsoc.next()) {
                    idCustomer = rsoc.getInt(1)
                    rsoc.close()
                    stmtoc.close()
                } else {
                    rsoc.close()
                    stmtoc.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.setString(2, java.util.UUID.randomUUID().toString())
                    stmtins.setString(3, "")
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idCustomer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val delivPlace = driver.findElementWithoutException(By.xpath("//div[contains(., 'Место поставки')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            val delivTerm = driver.findElementWithoutException(By.xpath("//div[contains(., 'График поставки')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            if (delivPlace != "" || delivTerm != "") {
                con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?").apply {
                    setInt(1, idLot)
                    setInt(2, idCustomer)
                    setString(3, delivPlace)
                    setString(4, delivTerm)
                    executeUpdate()
                    close()
                }
            }
            val purName = driver.findElementWithoutException(By.xpath("//div[contains(., 'Наименование ТРУ')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            var okei = driver.findElementWithoutException(By.xpath("//div[contains(., 'Единица измерения:')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            okei = okei.let { if (it.contains("Скачать")) "" else it }
            var okpd2 = driver.findElementWithoutException(By.xpath("//div[contains(., 'Код по ОКПД2')]/following-sibling::div"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            okpd2 = okpd2.let { if (it.contains("Скачать")) "" else it }
            val price = driver.findElementWithoutException(By.xpath("//div[contains(., 'Цена за единицу, руб.')]/following-sibling::div"))?.text?.deleteAllWhiteSpace()?.replace(",", ".")?.trim()?.trim { it <= ' ' }
                    ?: ""
            con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, price = ?, sum = ?, okpd2_code = ?").apply {
                setInt(1, idLot)
                setInt(2, idCustomer)
                setString(3, purName)
                setString(4, okei)
                setString(5, price)
                setString(6, nmck)
                setString(7, okpd2)
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

    private fun addCounts(updated: Boolean) {
        if (updated) {
            UpdateTender++
        } else {
            AddTender++
        }
    }

    companion object TypeFz {
        const val typeFz = 112
    }
}