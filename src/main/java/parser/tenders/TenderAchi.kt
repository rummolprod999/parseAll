package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import parser.builderApp.BuilderApp
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.extensions.replaceDateAchi
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Achi
import parser.tools.formatterAchi
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderAchi(val tn: Achi) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "Achizitii.md"
        etpUrl = "https://achizitii.md/"
    }

    val typeFz by lazy {
        207
    }

    override fun parsing() {
        val pageTen = downloadFromUrl(tn.href)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.href)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val status = htmlTen.selectFirst("div.tender__item__status > span")?.text()?.trim { it <= ' ' }
                ?: ""
        val dates = htmlTen.selectFirst("div.tender__item__client__info:contains(Подача предложений:) span")?.text()?.trim { it <= ' ' }
                ?: run { logger("dates was not found", tn.href); throw Exception("dates was not found") }
        val pubDateT = dates.getDataFromRegexp("(.+)-").replaceDateAchi()
        val datePub = pubDateT.getDateFromString(formatterAchi)
        if (datePub == Date(0L)) {
            run { logger("datePub was not found", tn.href); throw Exception("datePub was not found") }
        }
        val endDateT = dates.getDataFromRegexp("""-\s+(.+)""").replaceDateAchi()
        val dateEnd = endDateT.getDateFromString(formatterAchi)
        if (dateEnd == Date(0L)) {
            run { logger("dateEnd was not found", tn.href); throw Exception("dateEnd was not found") }
        }
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ? AND doc_publish_date = ? AND notice_version = ?").apply {
                setString(1, tn.purNum)
                setInt(2, typeFz)
                setTimestamp(3, Timestamp(dateEnd.time))
                setTimestamp(4, Timestamp(datePub.time))
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
            val dateVer = Date()
            val (cancelstatus, updated) = updateVersion(con, dateVer)
            var idOrganizer = 0
            var fullnameOrg = htmlTen.selectFirst("span:contains(Наименование) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            var inn = ""
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
                    val postalAdr = htmlTen.selectFirst("span:contains(Адрес) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    inn = htmlTen.selectFirst("span:contains(Фискальный код/IDNO) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val kpp = ""
                    val email = htmlTen.selectFirst("span:contains(Эл. адрес) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val phone = htmlTen.selectFirst("span:contains(Контактный номер) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val contactPerson = htmlTen.selectFirst("Ф.И.О")?.ownText()?.trim { it <= ' ' }
                            ?: ""
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?", Statement.RETURN_GENERATED_KEYS).apply {
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
            val idEtp = getEtp(con)
            val pwName = htmlTen.selectFirst("span:contains(Тип процедуры) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            var idPlacingWay = 0
            if (pwName != "") {
                idPlacingWay = getPlacingWay(con, pwName)
            }
            var idTender = 0
            val idRegion = 0
            val insertTender = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, tn.purNum)
            insertTender.setString(2, tn.purNum)
            insertTender.setTimestamp(3, Timestamp(datePub.time))
            insertTender.setString(4, tn.href)
            insertTender.setString(5, tn.purName)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, idOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(dateEnd.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, status)
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
            try {
                insertDocs(htmlTen, con, idTender)
            } catch (e: Exception) {
                logger(e, e.stackTrace)
            }
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
            val lots = htmlTen.select("div.tender__item__list div.tender__item__info__title.lot-list ~ div.tender__item__list__item")
            for ((ind, l) in lots.iterator().withIndex()) {
                val nmckT = l.selectFirst("a + div:contains(Бюджет:)")?.text()?.replace("Бюджет:", "")?.trim { it <= ' ' }
                        ?: ""
                val currency = nmckT.getDataFromRegexp("""(\w+)${'$'}""")
                val nmck = nmckT.getDataFromRegexp("""([\d.]+)""")
                val purObj = l.selectFirst("a strong")?.text()?.trim { it <= ' ' }
                        ?: ""
                var idLot = 0
                val lotNumber = ind + 1
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
                if (purObj != "") {
                    con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?").apply {
                        setInt(1, idLot)
                        setInt(2, idCustomer)
                        setString(3, purObj)
                        setString(4, "")
                        setString(5, "")
                        setString(6, "")
                        setString(7, "")
                        setString(8, "")
                        setString(9, "")
                        setString(10, "")
                        executeUpdate()
                        close()
                    }
                }
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
        })

    }

    private fun insertDocs(htmlTen: Document, con: Connection, idTender: Int) {
        val documents = htmlTen.select("div.tender__item__list.docs-list div.tender__item__list__item a")
        for (doc in documents) {
            val urlT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
            val url = urlT
            var docName = doc?.text()?.trim { it <= ' ' } ?: ""
            if (docName == "") {
                docName = urlT
            }
            val insertDoc = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
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
        val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?").apply {
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