package parser.tenders

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.ZakazRf
import parser.tools.formatterEtpRf
import parser.tools.formatterEtpRfN
import parser.tools.formatterOnlyDate

class TenderZakazRfUdmurt(val tn: ZakazRf) : TenderAbstract(), ITender {
    init {
        etpName = "Zakaz RF Удмуртия"
        etpUrl = "http://udmurtia.zakazrf.ru/DeliveryRequest"
    }

    companion object TypeFz {
        const val typeFz = 376
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con.prepareStatement(
                            "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ?"
                        )
                    stmt0.setString(1, tn.purNum)
                    stmt0.setTimestamp(2, Timestamp(tn.pubDate.time))
                    stmt0.setInt(3, typeFz)
                    stmt0.setString(4, tn.status)
                    val r = stmt0.executeQuery()
                    if (r.next()) {
                        r.close()
                        stmt0.close()
                        return
                    }
                    r.close()
                    stmt0.close()
                    val stPage = downloadFromUrl(tn.href, i = 1)
                    if (stPage == "") {
                        logger("Gets empty string TenderEtpRf", tn.href)
                        return
                    }
                    val html = Jsoup.parse(stPage)
                    val eis =
                        html
                            .selectFirst("td:containsOwn(Состояние извещения) ~ td")
                            ?.ownText()
                            ?.trim()
                            ?: ""
                    if (eis == "Опубликовано в ЕИС") {
                        logger("Опубликовано в ЕИС")
                        // return
                    }
                    var cancelstatus = 0
                    var update = false
                    val stmt =
                        con.prepareStatement(
                            "SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?"
                        )
                    stmt.setString(1, tn.purNum)
                    stmt.setInt(2, typeFz)
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        update = true
                        val idT = rs.getInt(1)
                        val dateB: Timestamp = rs.getTimestamp(2)
                        if (tn.pubDate.after(dateB) || dateB == Timestamp(tn.pubDate.time)) {
                            val preparedStatement =
                                con.prepareStatement(
                                    "UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?"
                                )
                            preparedStatement.setInt(1, idT)
                            preparedStatement.execute()
                            preparedStatement.close()
                        } else {
                            cancelstatus = 1
                        }
                    }
                    rs.close()
                    stmt.close()
                    var idOrganizer = 0
                    if (tn.orgName != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?"
                            )
                        stmto.setString(1, tn.orgName)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            idOrganizer = rso.getInt(1)
                            rso.close()
                            stmto.close()
                        } else {
                            rso.close()
                            stmto.close()
                            val postalAdr =
                                html
                                    .selectFirst("td:contains(Почтовый адрес) + td")
                                    ?.ownText()
                                    ?.trim { it <= ' ' }
                                    ?: ""
                            val factAdr = ""
                            val inn = ""
                            val kpp = ""
                            val email =
                                html
                                    .selectFirst("td:contains(Адрес электронной почты) + td a")
                                    ?.ownText()
                                    ?.trim { it <= ' ' }
                                    ?: ""
                            val phone =
                                html
                                    .selectFirst("td:contains(Номер контактного телефона) + td")
                                    ?.ownText()
                                    ?.trim { it <= ' ' }
                                    ?: ""
                            val contactPerson = ""
                            val stmtins =
                                con.prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                        Statement.RETURN_GENERATED_KEYS
                                    )
                                    .apply {
                                        setString(1, tn.orgName)
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
                    var IdEtp = 0
                    try {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_etp FROM ${BuilderApp.Prefix}etp WHERE name = ? AND url = ? LIMIT 1"
                            )
                        stmto.setString(1, etpName)
                        stmto.setString(2, etpUrl)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            IdEtp = rso.getInt(1)
                            rso.close()
                            stmto.close()
                        } else {
                            rso.close()
                            stmto.close()
                            val stmtins =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}etp SET name = ?, url = ?, conf=0",
                                    Statement.RETURN_GENERATED_KEYS
                                )
                            stmtins.setString(1, etpName)
                            stmtins.setString(2, etpUrl)
                            stmtins.executeUpdate()
                            val rsoi = stmtins.generatedKeys
                            if (rsoi.next()) {
                                IdEtp = rsoi.getInt(1)
                            }
                            rsoi.close()
                            stmtins.close()
                        }
                    } catch (ignored: Exception) {}

                    var IdPlacingWay = 0
                    val placingWay =
                        html
                            .selectFirst("td:containsOwn(Тип закупочной процедуры) ~ td")
                            ?.ownText()
                            ?.trim()
                            ?: ""
                    if (placingWay != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_placing_way FROM ${BuilderApp.Prefix}placing_way WHERE name = ? LIMIT 1"
                            )
                        stmto.setString(1, placingWay)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            IdPlacingWay = rso.getInt(1)
                            rso.close()
                            stmto.close()
                        } else {
                            rso.close()
                            stmto.close()
                            val conf = getConformity(placingWay)
                            val stmtins =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}placing_way SET name = ?, conformity = ?",
                                    Statement.RETURN_GENERATED_KEYS
                                )
                            stmtins.setString(1, placingWay)
                            stmtins.setInt(2, conf)
                            stmtins.executeUpdate()
                            val rsoi = stmtins.generatedKeys
                            if (rsoi.next()) {
                                IdPlacingWay = rsoi.getInt(1)
                            }
                            rsoi.close()
                            stmtins.close()
                        }
                    }
                    val printForm = tn.href.replace("/id/", "/Print/id/")
                    var idTender = 0
                    var scoringDT =
                        html
                            .selectFirst("td:containsOwn(Дата и время рассмотрения заявок) ~ td")
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    var scoringDate = scoringDT.getDateFromString(formatterEtpRfN)
                    if (scoringDate == Date(0L)) {
                        scoringDT =
                            html
                                .selectFirst(
                                    "td:containsOwn(Дата и время рассмотрения заявок) ~ td div"
                                )
                                ?.ownText()
                                ?.trim { it <= ' ' }
                                ?: ""
                        scoringDate = scoringDT.getDateFromString(formatterEtpRfN)
                    }
                    if (scoringDate == Date(0L)) {
                        scoringDT =
                            html
                                .selectFirst(
                                    "td:containsOwn(Дата рассмотрения первых частей заявок) ~ td"
                                )
                                ?.ownText()
                                ?.trim { it <= ' ' }
                                ?: ""
                        scoringDate = scoringDT.getDateFromString(formatterOnlyDate)
                    }
                    val dateBiddingT =
                        html
                            .selectFirst("td:containsOwn(Дата и время проведения торгов) ~ td")
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    val dateBidding = dateBiddingT.getDateFromString(formatterEtpRf)
                    val extendScoringDate =
                        html
                            .selectFirst("td:containsOwn(Окончание определения лучшей цены) ~ td")
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""

                    val extendBiddingDate =
                        html
                            .selectFirst(
                                "td:containsOwn(Окончание определения лучшего предложения) ~ td"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    val idRegion = getIdRegion(con, "Удмурт")

                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, scoring_date = ?, bidding_date = ?, extend_scoring_date = ?, extend_bidding_date  = ?, id_region = ?",
                            Statement.RETURN_GENERATED_KEYS
                        )
                    insertTender.setString(1, tn.purNum)
                    insertTender.setString(2, tn.purNum)
                    insertTender.setTimestamp(3, Timestamp(tn.pubDate.time))
                    insertTender.setString(4, tn.href)
                    insertTender.setString(5, tn.purName)
                    insertTender.setInt(6, typeFz)
                    insertTender.setInt(7, idOrganizer)
                    insertTender.setInt(8, IdPlacingWay)
                    insertTender.setInt(9, IdEtp)
                    insertTender.setTimestamp(10, Timestamp(tn.endDate.time))
                    insertTender.setInt(11, cancelstatus)
                    insertTender.setTimestamp(12, Timestamp(tn.pubDate.time))
                    insertTender.setInt(13, 1)
                    insertTender.setString(14, tn.status)
                    insertTender.setString(15, tn.href)
                    insertTender.setString(16, printForm)
                    insertTender.setTimestamp(17, Timestamp(scoringDate.time))
                    insertTender.setTimestamp(18, Timestamp(dateBidding.time))
                    insertTender.setString(19, extendScoringDate)
                    insertTender.setString(20, extendBiddingDate)
                    insertTender.setInt(21, idRegion)
                    insertTender.executeUpdate()
                    val rt = insertTender.generatedKeys
                    if (rt.next()) {
                        idTender = rt.getInt(1)
                    }
                    rt.close()
                    insertTender.close()
                    if (update) {
                        UpdateTender++
                    } else {
                        AddTender++
                    }
                    val documents: Elements =
                        html.select("table[data-orm-table-id = DocumentMetas] tbody tr[style]")
                    documents.addAll(
                        html.select("table[data-orm-table-id = OtherFiles] tbody tr[style]")
                    )
                    documents.addAll(
                        html.select(
                            "table[data-orm-table-id = HistoryFilesFiltered] tbody tr[style]"
                        )
                    )

                    if (documents.count() > 0) {
                        documents.forEach { doc ->
                            val href =
                                doc.select("td:eq(0) > a[href]")?.attr("href")?.trim { it <= ' ' }
                                    ?: ""
                            val descDoc = doc.select("td:eq(1)").text().trim { it <= ' ' }
                            val nameDoc =
                                doc.select("td:eq(0) > a[href]")?.text()?.trim { it <= ' ' } ?: ""
                            if (href != "") {
                                val insertDoc =
                                    con.prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?, description = ?"
                                    )
                                insertDoc.setInt(1, idTender)
                                insertDoc.setString(2, nameDoc)
                                insertDoc.setString(3, href)
                                insertDoc.setString(4, descDoc)
                                insertDoc.executeUpdate()
                                insertDoc.close()
                            }
                        }
                    }
                    var idLot = 0
                    val LotNumber = 1
                    val priceLot = returnPriceEtpRf(tn.price)
                    val currency =
                        html.selectFirst("td:containsOwn(Валюта) ~ td")?.ownText()?.trim {
                            it <= ' '
                        }
                            ?: ""
                    val insertLot =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                            Statement.RETURN_GENERATED_KEYS
                        )
                    insertLot.setInt(1, idTender)
                    insertLot.setInt(2, LotNumber)
                    insertLot.setString(3, currency)
                    insertLot.setString(4, priceLot)
                    insertLot.executeUpdate()
                    val rl = insertLot.generatedKeys
                    if (rl.next()) {
                        idLot = rl.getInt(1)
                    }
                    rl.close()
                    insertLot.close()
                    var idCustomer = 0
                    if (tn.orgName != "") {
                        val stmtoc =
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1"
                            )
                        stmtoc.setString(1, tn.orgName)
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
                            stmtins.setString(1, tn.orgName)
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
                    val purObj: Elements =
                        html.select("table[data-orm-table-id = LotItems] tbody tr[style]")
                    if (purObj.count() > 0) {
                        purObj.forEach { po ->
                            val okpd2Code = po.select("td:eq(1)")?.text()?.trim { it <= ' ' } ?: ""
                            val okpd2Name = po.select("td:eq(2)")?.text()?.trim { it <= ' ' } ?: ""
                            val (okpd2GroupCode, okpd2GroupLevel1Code) = getOkpd(okpd2Code)
                            val okei = po.select("td:eq(7)")?.text()?.trim { it <= ' ' } ?: ""
                            val quantityValue =
                                po.select("td:eq(8)")?.text()?.trim { it <= ' ' } ?: ""
                            var namePO =
                                html
                                    .selectFirst(
                                        "td:containsOwn(Полное наименование (предмет договора)) ~ td"
                                    )
                                    ?.ownText()
                                    ?.trim { it <= ' ' }
                                    ?: ""
                            if (namePO == "") {
                                namePO =
                                    html
                                        .selectFirst("td:containsOwn(Предмет договора) ~ td")
                                        ?.ownText()
                                        ?.trim { it <= ' ' }
                                        ?: ""
                            }
                            if (namePO == "") {
                                namePO = okpd2Name
                            }
                            val insertPurObj =
                                con.prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?, okpd2_code = ?, okpd2_group_code = ?, okpd2_group_level1_code = ?"
                                )
                            insertPurObj.setInt(1, idLot)
                            insertPurObj.setInt(2, idCustomer)
                            insertPurObj.setString(3, namePO)
                            insertPurObj.setString(4, quantityValue)
                            insertPurObj.setString(5, okei)
                            insertPurObj.setString(6, quantityValue)
                            insertPurObj.setString(7, okpd2Code)
                            insertPurObj.setInt(8, okpd2GroupCode)
                            insertPurObj.setString(9, okpd2GroupLevel1Code)
                            insertPurObj.executeUpdate()
                            insertPurObj.close()
                        }
                    } else {
                        con.prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?"
                            )
                            .apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, tn.purName)
                                setString(4, tn.okei)
                                setString(5, tn.quantity)
                                setString(6, tn.quantity)
                                setString(7, tn.price)
                                setString(8, tn.sum)
                                setString(9, "")
                                setString(10, "")
                                executeUpdate()
                                close()
                            }
                    }
                    var delivPlace = tn.delivPlace
                    if (delivPlace == "") {
                        val delivPlace1 =
                            html
                                .selectFirst(
                                    "td:containsOwn(Место поставки товаров, выполнения работ, оказания услуг) ~ td"
                                )
                                ?.ownText()
                                ?.trim { it <= ' ' }
                                ?: ""
                        delivPlace =
                            "Регион: " +
                                tn.delivPlace +
                                " " +
                                "Место поставки товаров, выполнения работ, оказания услуг: " +
                                delivPlace1
                    }
                    val delivTerm =
                        html
                            .selectFirst("td:containsOwn(Дополнительные комментарии) ~ td")
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    var applGAmount =
                        html
                            .selectFirst(
                                "td:containsOwn(Размер обеспечения(резервирования оплаты) заявки на участие, в рублях) ~ td"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    applGAmount = applGAmount.replace("руб.", "")
                    val applGuaranteeAmount = returnPriceEtpRf(applGAmount)
                    if (delivPlace != "") {
                        val insertCusRec =
                            con.prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?, application_guarantee_amount = ?, max_price = ?"
                            )
                        insertCusRec.setInt(1, idLot)
                        insertCusRec.setInt(2, idCustomer)
                        insertCusRec.setString(3, delivTerm)
                        insertCusRec.setString(4, delivPlace)
                        insertCusRec.setString(5, applGuaranteeAmount)
                        insertCusRec.setString(6, priceLot)
                        insertCusRec.executeUpdate()
                        insertCusRec.close()
                    }
                    val restr =
                        html
                            .selectFirst(
                                "td:containsOwn(Требование к отсутствию участника в реестре недобросовестных поставщиков) ~ td"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    if (restr == "Да") {
                        val restInfo =
                            "Требование к отсутствию участника в реестре недобросовестных поставщиков"
                        val insertRestr =
                            con.prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}restricts SET id_lot = ?, info = ?"
                            )
                        insertRestr.setInt(1, idLot)
                        insertRestr.setString(2, restInfo)
                        insertRestr.executeUpdate()
                        insertRestr.close()
                    }
                    val msp =
                        html
                            .selectFirst(
                                "td:containsOwn(Торги для субъектов малого и среднего предпринимательства) ~ td"
                            )
                            ?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    if (msp == "Да") {
                        val recContent = "Торги для субъектов малого и среднего предпринимательства"
                        val insertRec =
                            con.prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}requirement SET id_lot = ?, content = ?"
                            )
                        insertRec.setInt(1, idLot)
                        insertRec.setString(2, recContent)
                        insertRec.executeUpdate()
                        insertRec.close()
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
                }
            )
    }

    fun returnPriceEtpRf(s: String): String {
        var t = ""
        val tt = s.replace(',', '.')
        val pattern: Pattern = Pattern.compile("\\s+")
        val matcher: Matcher = pattern.matcher(tt)
        t = matcher.replaceAll("")
        return t
    }
}
