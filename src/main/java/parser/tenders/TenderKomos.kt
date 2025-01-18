package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import parser.builderApp.BuilderApp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.networkTools.downloadFromUrlNoSslNew
import parser.tools.formatterGpn
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class TenderKomos(val Url: String, val ContactPerson: String, val NumberT: String, var PurchaseObj: String, var TypeT: String, val DateSt: Date, var DateEn: Date): TenderAbstract(),
    ITender  {

    val typeFz by lazy { 11 }
    override fun parsing() {
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM  ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ?")
            stmt0.setString(1, NumberT)
            stmt0.setTimestamp(2, Timestamp(DateSt.time))
            stmt0.setInt(3, typeFz)
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            val stPage = downloadFromUrlNoSslNew(Url)
            if (stPage == "") {
                logger("Gets empty string urlPageAll", Url)
                return
            }
            val html = Jsoup.parse(stPage)
            var cancelstatus = 0
            var updated = false
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, NumberT)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                updated = true
                val idT = rs.getInt(1)
                val dateB = rs.getTimestamp(2)
                if (DateSt.after(dateB) || dateB == Timestamp(DateSt.time)) {
                    val preparedStatement = con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?")
                    preparedStatement.setInt(1, idT)
                    preparedStatement.execute()
                    preparedStatement.close()
                } else {
                    cancelstatus = 1
                }

            }
            rs.close()
            stmt.close()
            TypeT = html.selectFirst("p:containsOwn(Тип лота:) > span ")?.ownText()?.trim() ?: ""
            PurchaseObj = html.selectFirst("h5")?.ownText()?.trim() ?: ""
            var NoticeVersion = html.selectFirst("p:has(b:containsOwn(Примечание:))")?.ownText()?.trim() ?: ""
            var dateEndT = html.selectFirst("p:has(b:containsOwn(Запланированная дата окончания сбора предложений:))")?.ownText()?.trim() ?: ""
            if (dateEndT != "") {
                val dte = dateEndT.replace("Запланированная дата окончания сбора предложений:", "").trim { it <= ' ' }
                DateEn = dte.getDateFromString(formatterGpn)
            }
            var IdOrganizer = 0
            val fullnameOrg = html.selectFirst("div.card-title")?.ownText()?.trim() ?: ""
            if (fullnameOrg != "") {
                val stmto = con.prepareStatement("SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?")
                stmto.setString(1, fullnameOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdOrganizer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            var IdPlacingWay = 0
            if (TypeT != "") {
                IdPlacingWay = getPlacingWay(con, TypeT)
            }
            var IdEtp = 0
            val etpName = "КОМОС ГРУПП"
            val etpUrl = "http://zakupkikomos.ru"
            try {
                val stmto = con.prepareStatement("SELECT id_etp FROM ${BuilderApp.Prefix}etp WHERE name = ? AND url = ? LIMIT 1")
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
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}etp SET name = ?, url = ?, conf=0", Statement.RETURN_GENERATED_KEYS)
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
            } catch (ignored: Exception) {

            }
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}tender SET id_region = 0, id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, NumberT)
            insertTender.setString(2, NumberT)
            insertTender.setTimestamp(3, Timestamp(DateSt.time))
            insertTender.setString(4, Url)
            insertTender.setString(5, PurchaseObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, IdPlacingWay)
            insertTender.setInt(9, IdEtp)
            insertTender.setTimestamp(10, Timestamp(DateEn.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(DateSt.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, NoticeVersion)
            insertTender.setString(15, Url)
            insertTender.setString(16, Url)
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
            val LotNumber = 1
            val currency = html.selectFirst("p:has(b:containsOwn(Валюта:))")?.ownText()?.trim() ?: ""
            val mPr = html.selectFirst("b:containsOwn(Сумма лота:)")?.text()?.trim() ?: ""
            val maxPrice = extractNum(mPr)
            val insertLot = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS)
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.setString(3, currency)
            insertLot.setString(4, maxPrice)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            if (fullnameOrg != "") {
                val stmto = con.prepareStatement("SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1")
                stmto.setString(1, fullnameOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    idCustomer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer SET full_name = ?, is223=1, reg_num = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.setString(2, java.util.UUID.randomUUID().toString())
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idCustomer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val purObj: Elements = html.getElementsByAttributeValueEnding("itemtype", "https://schema.org/OfferCatalog")
            purObj.forEach { po ->
                val p = po.select("div")
                p.forEach { ppp ->
                    var name = ppp.selectFirst("meta[itemprop='name']")?.attr("content")?.trim() ?: ""
                    val addChar = ppp.selectFirst("meta[itemprop='description']")?.attr("content")?.trim() ?: ""
                    if (addChar != "") {
                        name = "$name ($addChar)"
                    }
                    val okei = po.select("div > meta[itemprop = 'priceCurrency']")?.text()?.trim() ?: ""
                    val quantity_value =  ""
                    val price = ppp.selectFirst("meta[itemprop='price']")?.attr("content")?.trim() ?: ""
                    if (name != "") {
                        val insertPurObj =
                            con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, price = ?, okei = ?, customer_quantity_value = ?")
                        insertPurObj.setInt(1, idLot)
                        insertPurObj.setInt(2, idCustomer)
                        insertPurObj.setString(3, name)
                        insertPurObj.setString(4, quantity_value)
                        insertPurObj.setString(5, extractNum(price))
                        insertPurObj.setString(6, okei)
                        insertPurObj.setString(7, quantity_value)
                        insertPurObj.executeUpdate()
                        insertPurObj.close()
                    }
                }

            }
            val delivDate = html.selectFirst("p:has(b:containsOwn(Срок поставки:))")?.ownText()?.trim { it <= ' ' }
                ?: ""
            val delivPay = html.selectFirst("p:has(b:containsOwn(Условия оплаты:))")?.ownText()?.trim { it <= ' ' }
                ?: ""
            val delivPlace = html.selectFirst("p:has(b:containsOwn(Условия поставки:))")?.ownText()?.trim { it <= ' ' }
                ?: ""
            var dTerm = ""
            if (delivDate != "") {
                dTerm = "Срок поставки: $delivDate\n"
            }
            if (delivPay != "") {
                dTerm = "${dTerm}Условия оплаты: $delivPay\n"
            }
            dTerm = dTerm.trim { it <= ' ' }
            if (dTerm != "") {
                val insertCusRec = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?")
                insertCusRec.setInt(1, idLot)
                insertCusRec.setInt(2, idCustomer)
                insertCusRec.setString(3, dTerm)
                insertCusRec.setString(4, delivPlace)
                insertCusRec.executeUpdate()
                insertCusRec.close()
            }
            try {
                tenderKwords(idTender, con)
            } catch (e: Exception) {
                logger("Ошибка добавления ключевых слов", e.stackTrace, e)
            }


            try {
                addVNum(con, NumberT, typeFz)
            } catch (e: Exception) {
                logger("Ошибка добавления версий", e.stackTrace, e)
            }

        })
    }

    fun extractNum(s: String): String {
        var nm = ""
        try {
            val pattern: Pattern = Pattern.compile("\\s+")
            val matcher: Matcher = pattern.matcher(s)
            val s = matcher.replaceAll("")
            val p = Pattern.compile("""(\d+\.*\d*)""")
            val m = p.matcher(s)
            if (m.find()) {
                nm = m.group()
            }
        } catch (e: Exception) {
        }
        return nm
    }
}