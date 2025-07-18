package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.extensions.extractPrice
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.RusNano
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderRusNano(
    val tn: RusNano,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "АО «Роснано»"
        etpUrl = "https://www.b2b-rusnano.ru/market/"
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager
            .getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con
                            .prepareStatement(
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ?",
                            ).apply {
                                setString(1, tn.purNum)
                                setTimestamp(2, Timestamp(tn.pubDate.time))
                                setInt(3, typeFz)
                                setTimestamp(4, Timestamp(tn.endDate.time))
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
                    val pageTen = downloadFromUrl(tn.href)
                    if (pageTen == "") {
                        logger("Gets empty string ${this::class.simpleName}", tn.href)
                        return
                    }
                    val htmlTen = Jsoup.parse(pageTen)
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
                    var idOrganizer = 0
                    if (tn.orgName != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?",
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
                            val postalAdr = ""
                            val factAdr = ""
                            val inn = ""
                            val kpp = ""
                            val email = ""
                            val phone = ""
                            val contactPerson =
                                htmlTen
                                    .selectFirst("td:contains(Ответственное лицо:) + td")
                                    ?.ownText()
                                    ?.trim { it <= ' ' } ?: ""
                            val stmtins =
                                con
                                    .prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                        Statement.RETURN_GENERATED_KEYS,
                                    ).apply {
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
                    val idEtp = getEtp(con)
                    var idPlacingWay = 0
                    var idTender = 0
                    if (tn.pwName != "") {
                        idPlacingWay = getPlacingWay(con, tn.pwName)
                    }
                    val idRegion = 0
                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?",
                            Statement.RETURN_GENERATED_KEYS,
                        )
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
                    insertTender.setString(14, "")
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
                    if (updated) {
                        UpdateTender++
                    } else {
                        AddTender++
                    }
                    val documents: Elements =
                        htmlTen.select("td:contains(Закупочная документация:) + td a")
                    documents.forEach { doc ->
                        val hrefT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
                        val href = hrefT
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
                    val lotNumber = 1
                    val currency =
                        htmlTen.selectFirst("td:contains(Вид валюты:) + td")?.ownText()?.trim {
                            it <= ' '
                        } ?: ""
                    val nmck =
                        (
                                htmlTen
                                    .selectFirst("td:contains(Общая стоимость:) + td")
                                    ?.ownText()
                                    ?.trim { it <= ' ' } ?: ""
                                ).extractPrice()
                    val insertLot =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                                Statement.RETURN_GENERATED_KEYS,
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
                    if (tn.orgName != "") {
                        val stmtoc =
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1",
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
                                    Statement.RETURN_GENERATED_KEYS,
                                )
                            stmtins.setString(1, tn.orgName)
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
                    val delivPlace =
                        htmlTen
                            .selectFirst("td:contains(Адрес места поставки товара) + td")
                            ?.ownText()
                            ?.trim { it <= ' ' } ?: ""
                    val delivTerm =
                        htmlTen.selectFirst("td:contains(Условия поставки) + td")?.ownText()?.trim {
                            it <= ' '
                        } ?: ""
                    if (delivPlace != "" || delivTerm != "") {
                        val insertCusRec =
                            con
                                .prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?",
                                ).apply {
                                    setInt(1, idLot)
                                    setInt(2, idCustomer)
                                    setString(3, delivPlace)
                                    setString(4, delivTerm)
                                    executeUpdate()
                                    close()
                                }
                    }
                    val okpdName =
                        htmlTen.selectFirst("td:contains(Категория ОКПД2) + td")?.text()?.trim {
                            it <= ' '
                        } ?: ""
                    val insertPurObj =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, sum = ?, okpd_name = ?",
                            ).apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, tn.purName)
                                setString(4, nmck)
                                setString(5, okpdName)
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

    companion object TypeFz {
        const val typeFz = 197
    }
}
