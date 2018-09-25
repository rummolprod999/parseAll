package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp.PassDb
import parser.builderApp.BuilderApp.Prefix
import parser.builderApp.BuilderApp.UrlConnect
import parser.builderApp.BuilderApp.UserDb
import parser.extensions.extractNum
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.Lsr
import parser.tools.formatterGpn
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderLsr(val tn: Lsr) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 34
    }

    init {
        etpName = "«Группа ЛСР»"
        etpUrl = "http://zakupki.lsr.ru"
    }

    override fun parsing() {
        val pageLot = downloadFromUrl(tn.hrefL)
        if (pageLot == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.hrefL)
            return
        }
        val htmlLot = Jsoup.parse(pageLot)
        val pageTen = downloadFromUrl(tn.hrefT)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.hrefT)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val dateVer = Date()
        if (tn.status == "") {
            tn.status = htmlLot.selectFirst("label:containsOwn(Статус) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
        }
        if (tn.placingWayName == "") {
            tn.placingWayName = htmlLot.selectFirst("div:containsOwn(Способ проведения) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
        }
        if (tn.nameCus == "") {
            tn.nameCus = htmlLot.selectFirst("label:containsOwn(Заказчик) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
        }
        val datePubT = htmlTen.selectFirst("label:containsOwn(Дата начала подачи заявок) + div > div")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val pubDate = datePubT.getDateFromString(formatterGpn)
        if (pubDate != Date(0L)) {
            tn.pubDate = pubDate
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?").apply {
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
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?").apply {
                setString(1, tn.purNum)
                setInt(2, typeFz)
            }
            val rs = stmt.executeQuery()
            while (rs.next()) {
                updated = true
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                    val preparedStatement = con.prepareStatement("UPDATE ${Prefix}tender SET cancel=1 WHERE id_tender = ?").apply {
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
            var inn = ""
            var fullnameOrg = ""
            var urlOrgT = htmlTen.selectFirst("label:containsOwn(Организатор) + div > div a")?.attr("href")?.trim { it <= ' ' }
                    ?: ""
            if (urlOrgT != "") {
                urlOrgT = "http://zakupki.lsr.ru$urlOrgT"
                val pageOrg = downloadFromUrl(urlOrgT)
                if (pageOrg == "") {
                    logger("Gets empty string ${this::class.simpleName}", urlOrgT)
                    return
                }
                val htmlOrg = Jsoup.parse(pageOrg)
                fullnameOrg = htmlOrg.selectFirst("label:containsOwn(Полное наименование) + div > div")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                if (fullnameOrg != "") {
                    val stmto = con.prepareStatement("SELECT id_organizer FROM ${Prefix}organizer WHERE full_name = ?")
                    stmto.setString(1, fullnameOrg)
                    val rso = stmto.executeQuery()
                    if (rso.next()) {
                        IdOrganizer = rso.getInt(1)
                        rso.close()
                        stmto.close()
                    } else {
                        rso.close()
                        stmto.close()
                        val postalAdr = htmlOrg.selectFirst("label:containsOwn(Почтовый адрес) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val factAdr = htmlOrg.selectFirst("label:containsOwn(Юридический адрес) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        inn = htmlOrg.selectFirst("label:containsOwn(ИНН) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val kpp = htmlOrg.selectFirst("label:containsOwn(КПП) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val email = htmlOrg.selectFirst("label:containsOwn(Эл. почта) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val phone = htmlOrg.selectFirst("label:containsOwn(Телефоны) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val contactPerson = htmlOrg.selectFirst("label:containsOwn(Руководитель) + div > div")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val stmtins = con.prepareStatement("INSERT INTO ${Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?", Statement.RETURN_GENERATED_KEYS).apply {
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
                            IdOrganizer = rsoi.getInt(1)
                        }
                        rsoi.close()
                        stmtins.close()
                    }
                }
            }
            val idEtp = getEtp(con)
            var idPlacingWay = 0
            var idTender = 0
            if (tn.placingWayName != "") {
                idPlacingWay = getPlacingWay(con, tn.placingWayName)
            }
            var biddingDateT = htmlLot.selectFirst("label:containsOwn(Дата и время начала торгов) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            if (biddingDateT == "") {
                biddingDateT = htmlLot.selectFirst("div:containsOwn(Дата и время начала торгов) + div")?.ownText()?.trim { it <= ' ' }
                        ?: ""
            }
            val biddingDate = biddingDateT.getDateFromString(formatterGpn)
            val idRegion = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, bidding_date = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, tn.purNum)
            insertTender.setString(2, tn.purNum)
            insertTender.setTimestamp(3, Timestamp(tn.pubDate.time))
            insertTender.setString(4, tn.hrefT)
            insertTender.setString(5, tn.purName)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(tn.endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, tn.status)
            insertTender.setString(15, tn.hrefT)
            insertTender.setString(16, tn.hrefT)
            insertTender.setInt(17, idRegion)
            insertTender.setTimestamp(18, Timestamp(biddingDate.time))
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
            val documents: Elements = htmlTen.select("div[id^=documentsContentBlock] a.file-download-link")
            documents.forEach { doc ->
                val hrefT = doc?.attr("href")?.trim { it <= ' ' } ?: ""
                val href = "$etpUrl$hrefT"
                val nameDoc = doc?.text()?.trim { it <= ' ' } ?: ""
                if (href != "") {
                    val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                    insertDoc.setInt(1, idTender)
                    insertDoc.setString(2, nameDoc)
                    insertDoc.setString(3, href)
                    insertDoc.executeUpdate()
                    insertDoc.close()
                }
            }
            var idLot = 0
            val LotNumber = 1
            val currency = htmlLot.selectFirst("label:containsOwn(Валюта) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var maxPriceT = htmlLot.selectFirst("div:containsOwn(Начальная цена) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            if (maxPriceT == "") {
                maxPriceT = htmlLot.selectFirst("label:containsOwn(Начальная цена) + div > div")?.ownText()?.trim { it <= ' ' }
                        ?: ""
            }
            var maxPrice = maxPriceT.replace("&nbsp;", "").replace(",", ".").replace(Regex("\\s+"), "")
            maxPrice = maxPrice.extractNum()
            val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS).apply {
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
                val stmtoc = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
                stmtoc.setString(1, tn.nameCus)
                val rsoc = stmtoc.executeQuery()
                if (rsoc.next()) {
                    idCustomer = rsoc.getInt(1)
                    rsoc.close()
                    stmtoc.close()
                } else {
                    rsoc.close()
                    stmtoc.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, tn.nameCus)
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
            val delivPlace = htmlLot.selectFirst("label:containsOwn(Место поставки) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            val delivTerm = htmlLot.selectFirst("label:containsOwn(Дополнительная информация) + div > div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            if (delivPlace != "" || delivTerm != "") {
                val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?").apply {
                    setInt(1, idLot)
                    setInt(2, idCustomer)
                    setString(3, delivPlace)
                    setString(4, delivTerm)
                    executeUpdate()
                    close()
                }
            }
            val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, sum = ?").apply {
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
        })
    }

}