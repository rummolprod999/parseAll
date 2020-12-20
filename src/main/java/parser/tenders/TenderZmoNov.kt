package parser.tenders

import org.jsoup.Jsoup
import parser.builderApp.BuilderApp
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.getDataFromRegexp
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.ZmoKursk
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderZmoNov(val tn: ZmoKursk) : TenderAbstract(), ITender {
    init {
        etpName = "Электронный магазин Великого Новгорода"
        etpUrl = "https://market-nov.rts-tender.ru/"
    }

    override fun parsing() {
        val (status, purNum, purObj, nmck, pubDate, endDate, url) = tn
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(fun(con: Connection) {
                val stmt0 =
                    con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?")
                        .apply {
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
                val stmt =
                    con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
                        .apply {
                            setString(1, purNum)
                            setInt(2, typeFz)
                        }
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    updated = true
                    val idT = rs.getInt(1)
                    val dateB: Timestamp = rs.getTimestamp(2)
                    if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                        con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?")
                            .apply {
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
                val pageTen = downloadFromUrl(url)
                if (pageTen == "") {
                    logger("Gets empty string ${this::class.simpleName}", url)
                    return
                }
                val htmlTen = Jsoup.parse(pageTen)
                var idOrganizer = 0
                var inn = ""
                val fullnameOrg =
                    htmlTen.selectFirst("td:contains(Полное наименование) + td > a")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                if (fullnameOrg != "") {
                    val stmto =
                        con.prepareStatement("SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?")
                    stmto.setString(1, fullnameOrg)
                    val rso = stmto.executeQuery()
                    if (rso.next()) {
                        idOrganizer = rso.getInt(1)
                        rso.close()
                        stmto.close()
                    } else {
                        rso.close()
                        stmto.close()
                        val postalAdr = htmlTen.selectFirst("td:contains(Адрес места нахождения) + td")?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                        val factAdr = ""
                        inn = htmlTen.selectFirst("td:contains(ИНН) + td")?.ownText()?.trim { it <= ' ' }
                            ?: ""
                        val kpp = ""
                        val email = ""
                        val phone = ""
                        val contactPerson = ""
                        val stmtins = con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                            Statement.RETURN_GENERATED_KEYS
                        ).apply {
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
                val idPlacingWay = 0
                var idTender = 0
                val idRegion = getIdRegion(con, "новгор")
                val insertTender = con.prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?",
                    Statement.RETURN_GENERATED_KEYS
                )
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
                if (updated) {
                    TenderAbstract.UpdateTender++
                } else {
                    TenderAbstract.AddTender++
                }
                //val documents: Elements = htmlTen.select("h1:containsOwn(Документы закупки) + div ")
                try {
                    getAttachmentsZmo(idTender, con, purNum)
                } catch (e: Exception) {
                    logger("Ошибка добавления документации", e.stackTrace, e)
                }
                var idLot = 0
                val lotNumber = 1
                val currency = "руб."
                val insertLot = con.prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                    Statement.RETURN_GENERATED_KEYS
                ).apply {
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
                    val stmtoc =
                        con.prepareStatement("SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1")
                    stmtoc.setString(1, fullnameOrg)
                    val rsoc = stmtoc.executeQuery()
                    if (rsoc.next()) {
                        idCustomer = rsoc.getInt(1)
                        rsoc.close()
                        stmtoc.close()
                    } else {
                        rsoc.close()
                        stmtoc.close()
                        val stmtins = con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?",
                            Statement.RETURN_GENERATED_KEYS
                        )
                        stmtins.setString(1, fullnameOrg)
                        stmtins.setString(2, java.util.UUID.randomUUID().toString())
                        stmtins.setString(3, inn)
                        stmtins.executeUpdate()
                        val rsoi = stmtins.generatedKeys
                        if (rsoi.next()) {
                            idCustomer = rsoi.getInt(1)
                        }
                        rsoi.close()
                        stmtins.close()
                    }
                }
                val delivPlace = htmlTen.selectFirst("td:contains(Место поставки) + td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val delivTerm = htmlTen.selectFirst("td:contains(Сроки поставки) + td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                if (delivPlace != "" || delivTerm != "") {
                    val insertCusRec =
                        con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?")
                            .apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, delivPlace)
                                setString(4, delivTerm)
                                executeUpdate()
                                close()
                            }
                }
                val purobj1 = htmlTen.select("table:contains(Код классификатора) + div table tbody tr")
                purobj1.forEach { element ->
                    val name = element.selectFirst("td:eq(1)")?.text()?.trim { it <= ' ' }
                        ?: ""
                    val s = element.selectFirst("td:eq(1)")
                    val p = element.selectFirst("td:eq(0)")
                    val okei = element.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                    val quantity = element.selectFirst("td:eq(4)")?.ownText()?.replace(',', '.')?.trim { it <= ' ' }
                        ?: ""
                    val price =
                        element.selectFirst("td:eq(5) > p")?.ownText()?.replace("&nbsp;", "")?.deleteAllWhiteSpace()
                            ?.replace(',', '.')?.trim { it <= ' ' }
                            ?: ""
                    val sum =
                        element.selectFirst("td:eq(6) > p")?.ownText()?.replace("&nbsp;", "")?.deleteAllWhiteSpace()
                            ?.replace(',', '.')?.trim { it <= ' ' }
                            ?: ""
                    val fullOkpd = element.selectFirst("td:eq(2)")?.ownText()?.replace(',', '.')?.trim { it <= ' ' }
                        ?: ""
                    val okpd2 = fullOkpd.getDataFromRegexp("^(.+)\\s+/")
                    val okpdName = fullOkpd.getDataFromRegexp("/\\s*(.*)\$")
                    con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?")
                        .apply {
                            setInt(1, idLot)
                            setInt(2, idCustomer)
                            setString(3, name)
                            setString(4, okei)
                            setString(5, quantity)
                            setString(6, quantity)
                            setString(7, price)
                            setString(8, sum)
                            setString(9, okpd2)
                            setString(10, okpdName)
                            executeUpdate()
                            close()
                        }
                }
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
            })

    }

    companion object TypeFz {
        const val typeFz = 150
    }
}