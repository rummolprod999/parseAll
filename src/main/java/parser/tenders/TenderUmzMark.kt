package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.UmzMark
import parser.tools.formatterGpn
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

/**
 * Tender umz mark
 *
 * @property tn
 * @constructor Create empty Tender umz mark
 */
class TenderUmzMark(val tn: UmzMark) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 79
    }

    init {
        etpName = "Управление муниципальных закупок администрации городского округа город Воронеж"
        etpUrl = "http://umz-vrn.etc.ru"
    }

    override fun parsing() {
        val pageTen = downloadFromUrl(tn.href)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.href)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val datePubT =
            htmlTen
                .selectFirst(
                    "th:contains(Дата и время создания маркетингового исследования) + td > span"
                )
                ?.ownText()
                ?.trim { it <= ' ' }
                ?: ""
        val datePub = datePubT.getDateFromString(formatterGpn)
        if (datePub == Date(0L)) {
            logger("cannot find datePub on page", tn.href, tn.purName)
            return
        }
        val purNum =
            htmlTen
                .selectFirst("th:contains(Номер маркетингового исследования) + td > span")
                ?.ownText()
                ?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("cannot find purNum on page", tn.href, tn.purName)
            return
        }
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con.prepareStatement(
                            "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?"
                        )
                            .apply {
                                setString(1, purNum)
                                setTimestamp(2, Timestamp(datePub.time))
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
                    val stmt =
                        con.prepareStatement(
                            "SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?"
                        )
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
                            val preparedStatement =
                                con.prepareStatement(
                                    "UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?"
                                )
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
                    var IdOrganizer = 0
                    if (etpName != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?"
                            )
                        stmto.setString(1, etpName)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            IdOrganizer = rso.getInt(1)
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
                            val stmtins =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                    Statement.RETURN_GENERATED_KEYS
                                )
                                    .apply {
                                        setString(1, etpName)
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
                    val idPlacingWay = getPlacingWay(con, "запрос цен")
                    var idTender = 0
                    val idRegion = getIdRegion(con, "ворон")
                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, end_date = ?",
                            Statement.RETURN_GENERATED_KEYS
                        )
                    insertTender.setString(1, purNum)
                    insertTender.setString(2, purNum)
                    insertTender.setTimestamp(3, Timestamp(datePub.time))
                    insertTender.setString(4, tn.href)
                    insertTender.setString(5, tn.purName)
                    insertTender.setInt(6, typeFz)
                    insertTender.setInt(7, IdOrganizer)
                    insertTender.setInt(8, idPlacingWay)
                    insertTender.setInt(9, idEtp)
                    insertTender.setInt(10, cancelstatus)
                    insertTender.setTimestamp(11, Timestamp(dateVer.time))
                    insertTender.setInt(12, 1)
                    insertTender.setString(13, tn.status)
                    insertTender.setString(14, tn.href)
                    insertTender.setString(15, tn.href)
                    insertTender.setInt(16, idRegion)
                    insertTender.setTimestamp(17, Timestamp(tn.endDate.time))
                    insertTender.executeUpdate()
                    val rt = insertTender.generatedKeys
                    if (rt.next()) {
                        idTender = rt.getInt(1)
                    }
                    rt.close()
                    insertTender.close()
                    if (updated) {
                        UpdateTender++
                    } else {
                        AddTender++
                    }
                    val documents: Elements = htmlTen.select("div.DSFileContentView a")
                    documents.forEach { doc ->
                        val hrefT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
                        val href = "$etpUrl$hrefT"
                        val nameDoc = doc?.text()?.trim { it <= ' ' } ?: ""
                        if (href != "") {
                            val insertDoc =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?"
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
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                            Statement.RETURN_GENERATED_KEYS
                        )
                            .apply {
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
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1"
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
                                    Statement.RETURN_GENERATED_KEYS
                                )
                            stmtins.setString(1, tn.nameCus)
                            stmtins.setString(2, UUID.randomUUID().toString())
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
                    con.prepareStatement(
                        "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?"
                    )
                        .apply {
                            setInt(1, idLot)
                            setInt(2, idCustomer)
                            setString(3, tn.purName)
                            setString(4, tn.okei)
                            setString(5, tn.quant)
                            setString(6, tn.quant)
                            setString(7, "")
                            setString(8, tn.nmck)
                            setString(9, tn.okpd)
                            setString(10, "")
                            executeUpdate()
                            close()
                        }
                    var delivTerm1 =
                        htmlTen
                            .selectFirst(
                                "th:contains(Дата начала срока поставки товаров) + td > span"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    var delivTerm2 =
                        htmlTen
                            .selectFirst(
                                "th:contains(Дата окончания срока поставки товаров) + td > span"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    var delivTerm3 =
                        htmlTen
                            .selectFirst(
                                "th:contains(Требования к порядку поставки товаров) + td > span"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    var delivTerm4 =
                        htmlTen
                            .selectFirst("th:contains(Порядок оплаты) + td > span")
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    val delivTerm =
                        "Дата начала срока поставки товаров: $delivTerm1\nДата окончания срока поставки товаров: $delivTerm2\nТребования к порядку поставки товаров: $delivTerm3\nПорядок оплаты): $delivTerm4"
                    val contrGuarantAmount =
                        htmlTen
                            .selectFirst(
                                "th:contains(Размер обеспечения исполнения контракта) + td > span"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    if (delivTerm != "") {
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?, contract_guarantee_amount = ?"
                        )
                            .apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, "")
                                setString(4, delivTerm)
                                setString(5, contrGuarantAmount)
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
                }
            )
    }
}
