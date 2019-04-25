package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.logger.logger
import parser.parsers.ParserTmk
import parser.tenderClasses.Tmk
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderTmk(val tn: Tmk, val driver: ChromeDriver) : TenderAbstract(), ITender {

    init {
        etpName = "ПАО «Трубная Металлургическая Компания»"
        etpUrl = "https://zakupki.tmk-group.com/"
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?").apply {
                setString(1, tn.purNum)
                setTimestamp(2, Timestamp(tn.pubDate.time))
                setInt(3, typeFz)
                setTimestamp(4, Timestamp(tn.endDate.time))
                setString(5, tn.status)
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
            driver.get(tn.href)
            driver.switchTo().defaultContent()
            val wait = WebDriverWait(driver, ParserTmk.timeoutB)
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?").apply {
                setString(1, tn.purNum)
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
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//td[contains(., 'Тип процедуры:')]/following-sibling::td")))
            Thread.sleep(2000)
            driver.switchTo().defaultContent()
            var idOrganizer = 0
            val fullnameOrg = tn.nameOrg
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
                    val postalAdr = driver.findElementWithoutException(By.xpath("//td[contains(., 'Почтовый адрес:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val factAdr = driver.findElementWithoutException(By.xpath("//td[contains(., 'Юридический адрес:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val inn = ""
                    val kpp = ""
                    val email = driver.findElementWithoutException(By.xpath("//td[contains(., 'Адрес электронной почты:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val phone = driver.findElementWithoutException(By.xpath("//td[contains(., 'Контактный телефон:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
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
            val placingWayName = driver.findElementWithoutException(By.xpath("//td[contains(., 'Тип процедуры:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            if (placingWayName != "") {
                idPlacingWay = getPlacingWay(con, placingWayName)
            }
            val regionName = driver.findElementWithoutException(By.xpath("//td[contains(., 'Место поставки:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                    ?: ""
            val idRegion = getIdRegion(con, regionName)
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, tn.purNum)
            insertTender.setString(2, tn.purNum)
            insertTender.setTimestamp(3, Timestamp(tn.pubDate.time))
            insertTender.setString(4, tn.href)
            insertTender.setString(5, tn.purName)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, idOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(tn.endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, tn.status)
            insertTender.setString(15, tn.href)
            insertTender.setString(16, tn.href)
            insertTender.setInt(17, idRegion)
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            addCounts(updated)
            val lots = driver.findElements(By.xpath("//div[contains(@class, 'x-tab-panel-body')]/div[contains(@id, 'ext-comp-') and contains(@class, 'x-panel x-panel-noborder')]/div[@class = 'x-panel-bwrap']"))
            lots.forEachIndexed { index, lot ->
                val lotNum = index + 1
                val nmck = lot.findElementWithoutException(By.xpath(".//td[contains(., 'Начальная цена без учета НДС:')]/following-sibling::td"))?.text?.deleteAllWhiteSpace()?.replace(",", ".")?.trim()?.trim { it <= ' ' }
                        ?: ""
                val currency = lot.findElementWithoutException(By.xpath(".//td[contains(., 'Валюта:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                        ?: ""
                var idLot = 0
                val insertLot = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS).apply {
                    setInt(1, idTender)
                    setInt(2, lotNum)
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
                val cusName = lot.findElementWithoutException(By.xpath(".//td[contains(., 'Наименование заказчика:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                        ?: ""
                if (cusName != "") {

                    val stmtoc = con.prepareStatement("SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1")
                    stmtoc.setString(1, cusName)
                    val rsoc = stmtoc.executeQuery()
                    if (rsoc.next()) {
                        idCustomer = rsoc.getInt(1)
                        rsoc.close()
                        stmtoc.close()
                    } else {
                        rsoc.close()
                        stmtoc.close()
                        val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?", Statement.RETURN_GENERATED_KEYS)
                        stmtins.setString(1, cusName)
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
                val delivPlace = lot.findElementWithoutException(By.xpath(".//td[contains(., 'Место поставки:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                        ?: ""
                val delivTerm = lot.findElementWithoutException(By.xpath(".//td[contains(., 'Условия, сроки поставки и оплаты:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
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
                val purObjects = driver.findElements(By.xpath(".//div[@class = 'x-fieldset-body x-fieldset-body-noheader']"))
                for (po in purObjects) {
                    val poName = po.findElementWithoutException(By.xpath(".//td[contains(., 'Наименование:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    if (poName == "") continue
                    val quant = po.findElementWithoutException(By.xpath(".//td[contains(., 'Количество:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    val okei = po.findElementWithoutException(By.xpath(".//td[contains(., 'Единица измерения:')]/following-sibling::td"))?.text?.trim()?.trim { it <= ' ' }
                            ?: ""
                    con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?").apply {
                        setInt(1, idLot)
                        setInt(2, idCustomer)
                        setString(3, poName)
                        setString(4, okei)
                        setString(5, quant)
                        setString(6, quant)
                        executeUpdate()
                        close()
                    }
                }
            }
            afterParsing(idTender, con, tn.purNum)
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

    companion object TypeFz {
        const val typeFz = 186
    }
}