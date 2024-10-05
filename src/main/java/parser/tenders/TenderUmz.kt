package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Umz
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.time.ZoneId
import java.util.*

class TenderUmz(
    val tn: Umz,
) : TenderAbstract(),
    ITender {
    companion object TypeFz {
        val typeFz = 79
    }

    init {
        etpName = "Управление муниципальных закупок администрации городского округа город Воронеж"
        etpUrl = "http://umz-vrn.etc.ru"
    }

    override fun parsing() {
        val dateVer = tn.pubDate
        DriverManager
            .getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con
                            .prepareStatement(
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND doc_publish_date = ? AND notice_version = ?",
                            ).apply {
                                setString(1, tn.purNum)
                                setInt(2, typeFz)
                                setTimestamp(3, Timestamp(tn.pubDate.time))
                                setString(4, tn.status)
                            }
                    val r = stmt0.executeQuery()
                    if (r.next()) {
                        r.close()
                        stmt0.close()
                        return
                    }
                    r.close()
                    stmt0.close()
                    val pageTen = downloadFromUrl(tn.hrefT)
                    if (pageTen == "") {
                        logger("Gets empty string ${this::class.simpleName}", tn.hrefT)
                        return
                    }
                    val htmlTen = Jsoup.parse(pageTen)
                    var cancelstatus = 0
                    var updated = false
                    val stmt =
                        con
                            .prepareStatement(
                                "SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?",
                            ).apply {
                                setString(1, tn.purNum)
                                setInt(2, typeFz)
                            }
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        updated = true
                        val idT = rs.getInt(1)
                        val dateB: Timestamp = rs.getTimestamp(2)
                        if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                            val preparedStatement =
                                con
                                    .prepareStatement(
                                        "UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?",
                                    ).apply {
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
                    var IdOrganizer = 0
                    if (tn.nameCus != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?",
                            )
                        stmto.setString(1, tn.nameCus)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            IdOrganizer = rso.getInt(1)
                            rso.close()
                            stmto.close()
                        } else {
                            rso.close()
                            stmto.close()
                            val postalAdr =
                                htmlTen
                                    .selectFirst("td:contains(Адрес места нахождения) + td")
                                    ?.ownText()
                                    ?.trim { it <= ' ' } ?: ""
                            val factAdr = ""
                            val inn = ""
                            val kpp = ""
                            val email = ""
                            val phone = ""
                            val contactPerson = ""
                            val stmtins =
                                con
                                    .prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                        Statement.RETURN_GENERATED_KEYS,
                                    ).apply {
                                        setString(1, tn.nameCus)
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
                                IdOrganizer = rsoi.getInt(1)
                            }
                            rsoi.close()
                            stmtins.close()
                        }
                    }
                    val idEtp = getEtp(con)
                    var idPlacingWay = 0
                    var idTender = 0
                    val idRegion = getIdRegion(con, "ворон")
                    if (tn.placingWayName != "") {
                        idPlacingWay = getPlacingWay(con, tn.placingWayName)
                    }
                    val dateEnd =
                        Date.from(
                            tn.pubDate
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .plusDays(2)
                                .toInstant(),
                        )
                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, end_date = ?",
                            Statement.RETURN_GENERATED_KEYS,
                        )
                    insertTender.setString(1, tn.purNum)
                    insertTender.setString(2, tn.purNum)
                    insertTender.setTimestamp(3, Timestamp(tn.pubDate.time))
                    insertTender.setString(4, tn.hrefT)
                    insertTender.setString(5, tn.purName)
                    insertTender.setInt(6, typeFz)
                    insertTender.setInt(7, IdOrganizer)
                    insertTender.setInt(8, idPlacingWay)
                    insertTender.setInt(9, idEtp)
                    insertTender.setInt(10, cancelstatus)
                    insertTender.setTimestamp(11, Timestamp(dateVer.time))
                    insertTender.setInt(12, 1)
                    insertTender.setString(13, tn.status)
                    insertTender.setString(14, tn.hrefT)
                    insertTender.setString(15, tn.hrefT)
                    insertTender.setInt(16, idRegion)
                    insertTender.setTimestamp(17, Timestamp(dateEnd.time))
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
                    val documents: Elements = htmlTen.select("div.DSFileContentView a")
                    documents.forEach { doc ->
                        val hrefT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
                        val href = "$etpUrl$hrefT"
                        val nameDoc = doc?.text()?.trim { it <= ' ' } ?: ""
                        if (href != "") {
                            val insertDoc =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?",
                                )
                            insertDoc.setInt(1, idTender)
                            insertDoc.setString(2, nameDoc)
                            insertDoc.setString(3, href)
                            insertDoc.executeUpdate()
                            insertDoc.close()
                        }
                    }
                    var idLot = 0
                    val LotNumber = 1
                    val currency = ""
                    val maxPrice = tn.nmck
                    val insertLot =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                                Statement.RETURN_GENERATED_KEYS,
                            ).apply {
                                setInt(1, idTender)
                                setInt(2, LotNumber)
                                setString(3, currency)
                                setString(4, maxPrice)
                                executeUpdate()
                            }
                    val rl = insertLot.generatedKeys
                    if (rl.next()) {
                        idLot = rl.getInt(1)
                    }
                    rl.close()
                    insertLot.close()
                    var idCustomer = 0
                    if (tn.nameCus != "") {
                        val stmtoc =
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1",
                            )
                        stmtoc.setString(1, tn.nameCus)
                        val rsoc = stmtoc.executeQuery()
                        if (rsoc.next()) {
                            idCustomer = rsoc.getInt(1)
                            rsoc.close()
                            stmtoc.close()
                        } else {
                            rsoc.close()
                            stmtoc.close()
                            val stmtins =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?",
                                    Statement.RETURN_GENERATED_KEYS,
                                )
                            stmtins.setString(1, tn.nameCus)
                            stmtins.setString(
                                2,
                                java.util.UUID
                                    .randomUUID()
                                    .toString(),
                            )
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
                    val insertPurObj =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, sum = ?",
                            ).apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, tn.purName)
                                setString(4, maxPrice)
                                executeUpdate()
                                close()
                            }
                    try {
                        tenderKwords(idTender, con)
                    } catch (e: Exception) {
                        logger("Ошибка добавления ключевых слов", e.stackTrace, e)
                    }
                    try {
                        addVNum(con, tn.purNum, typeFz)
                    } catch (e: Exception) {
                        logger("Ошибка добавления версий", e.stackTrace, e)
                    }
                },
            )
    }
}
