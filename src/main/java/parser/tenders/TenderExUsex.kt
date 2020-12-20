package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.extensions.extractNum
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Uzex
import parser.tools.formatterOnlyDate
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderExUzex(val tn: Uzex) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "АО \"Узбекская Республиканская товарно-сырьевая биржа\" для корпоративных заказчиков"
        etpUrl = "https://exarid.uzex.uz/ru"
    }

    val typeFz by lazy {
        222
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(fun(con: Connection) {
                val stmt0 =
                    con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ?")
                        .apply {
                            setString(1, tn.purNum)
                            setInt(2, typeFz)
                            setTimestamp(3, Timestamp(tn.endDate.time))
                        }
                val r = stmt0.executeQuery()
                if (r.next()) {
                    r.close()
                    stmt0.close()
                    return
                }
                r.close()
                stmt0.close()

                val pageTen = downloadFromUrl(tn.href)
                if (pageTen == "") {
                    logger("Gets empty string ${this::class.simpleName}", tn.href)
                    return
                }
                val htmlTen = Jsoup.parse(pageTen)
                val (cancelstatus, updated) = updateVersion(con, dateVer)
                var idOrganizer = 0
                var fullnameOrg = htmlTen.selectFirst("div.left_element:contains(Наименование заказчика) + div")?.text()
                    ?.trim { it <= ' ' }
                    ?: ""
                if (fullnameOrg == "") {
                    fullnameOrg = htmlTen.selectFirst("div.left_element:contains(Название организации:) + div")?.text()
                        ?.trim { it <= ' ' }
                        ?: ""
                }
                var inn = ""
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
                        val postalAdr = htmlTen.selectFirst("div.left_element:contains(Адрес заказчика:) + div")?.text()
                            ?.trim { it <= ' ' }
                            ?: ""
                        inn = htmlTen.selectFirst("div.left_element:contains(ИНН:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                        val kpp = ""
                        val email = ""
                        val phone =
                            htmlTen.selectFirst("div.left_element:contains(Телефон:) + div")?.text()?.trim { it <= ' ' }
                                ?: ""
                        val contactPerson =
                            htmlTen.selectFirst("div.left_element:contains(Имя и должность ответственного лица заказчика) + div")
                                ?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val stmtins = con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                            Statement.RETURN_GENERATED_KEYS
                        ).apply {
                            setString(1, fullnameOrg)
                            setString(2, postalAdr)
                            setString(3, email)
                            setString(4, phone)
                            setString(5, postalAdr)
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
                val datePubTmp =
                    htmlTen.selectFirst("div.left_element:contains(Дата начала:) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
                val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
                if (datePub != Date(0L)) {
                    tn.pubDate = datePub
                }
                val idEtp = getEtp(con)
                val idPlacingWay = 0
                var idTender = 0
                val idRegion = 0
                val insertTender = con.prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?",
                    Statement.RETURN_GENERATED_KEYS
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
                var idLot = 0
                val lotNumber = 1
                val insertLot = con.prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                    Statement.RETURN_GENERATED_KEYS
                ).apply {
                    setInt(1, idTender)
                    setInt(2, lotNumber)
                    setString(3, tn.currency)
                    setString(4, tn.nmck)
                    executeUpdate()
                }
                val rl = insertLot.generatedKeys
                if (rl.next()) {
                    idLot = rl.getInt(1)
                }
                rl.close()
                insertLot.close()
                try {
                    insertDocs(htmlTen, con, idTender)
                } catch (e: Exception) {
                    logger(e, e.stackTrace)
                }
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
                val delivPlace =
                    htmlTen.selectFirst("div.left_element:contains(Место поставки:) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
                val delivTerm1 =
                    htmlTen.selectFirst("div.left_element:contains(Срок поставки) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
                val delivTerm2 =
                    htmlTen.selectFirst("div.left_element:contains(Условия поставки) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
                val delivTerm = "$delivTerm1 $delivTerm2".trim { it <= ' ' }
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
                val requirements = htmlTen.select("ul.conditionsList li")
                insertRequirements(requirements, con, idLot)
                val purchaseObjects = htmlTen.select("div.full_block.content")
                insertPurObjs(purchaseObjects, con, idLot, idCustomer)
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
            })
    }

    private fun insertPurObjs(purchaseObjects: Elements, con: Connection, idLot: Int, idCustomer: Int) {
        for (po in purchaseObjects) {
            val name = po.selectFirst("p:eq(0)")?.ownText()?.trim { it <= ' ' } ?: ""
            val quantity = po.selectFirst("tbody tr td:eq(0)")?.ownText()?.trim { it <= ' ' } ?: ""
            val okei = po.selectFirst("tbody tr td:eq(1)")?.ownText()?.trim { it <= ' ' } ?: ""
            val priceT = po.selectFirst("tbody tr td:eq(2)")?.ownText()?.trim { it <= ' ' } ?: ""
            val price = priceT.extractNum()
            if (name != "") {
                con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?")
                    .apply {
                        setInt(1, idLot)
                        setInt(2, idCustomer)
                        setString(3, name)
                        setString(4, okei)
                        setString(5, quantity)
                        setString(6, quantity)
                        setString(7, price)
                        setString(8, "")
                        setString(9, "")
                        setString(10, "")
                        executeUpdate()
                        close()
                    }
            }

        }
    }

    private fun insertRequirements(requirements: Elements, con: Connection, idLot: Int) {
        for (rec in requirements) {
            val recName = rec?.text()?.trim { it <= ' ' } ?: ""
            if (recName != "") {
                val insertRec =
                    con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}requirement SET id_lot = ?, name = ?").apply {
                        setInt(1, idLot)
                        setString(2, recName)
                        executeUpdate()
                        close()
                    }
            }
        }
    }

    private fun insertDocs(htmlTen: Document, con: Connection, idTender: Int) {
        val documents = htmlTen.select("a.product_photo")
        documents.addAll(htmlTen.select("a.product_file"))
        for (doc in documents) {
            val urlT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
            val url = "https://exarid.uzex.uz$urlT"
            var docName = doc?.text()?.trim { it <= ' ' } ?: ""
            if (docName == "") {
                docName = urlT
            }
            val insertDoc =
                con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
            insertDoc.setInt(1, idTender)
            insertDoc.setString(2, docName)
            insertDoc.setString(3, url)
            insertDoc.executeUpdate()
            insertDoc.close()
        }
    }

    private fun updateVersion(con: Connection, dateVer: Date): Result {
        var updated1 = false
        var cancelstatus1 = 0
        val stmt =
            con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
                .apply {
                    setString(1, tn.purNum)
                    setInt(2, typeFz)
                }
        val rs = stmt.executeQuery()
        while (rs.next()) {
            updated1 = true
            val idT = rs.getInt(1)
            val dateB: Timestamp = rs.getTimestamp(2)
            if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?").apply {
                    setInt(1, idT)
                    execute()
                    close()
                }
            } else {
                cancelstatus1 = 1
            }
        }
        rs.close()
        stmt.close()
        return Result(cancelstatus1, updated1)
    }
}