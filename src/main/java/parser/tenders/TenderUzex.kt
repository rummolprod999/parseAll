package parser.tenders

import org.jsoup.Jsoup
import parser.builderApp.BuilderApp
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

class TenderUzex(val tn: Uzex) : TenderAbstract(), ITender {
    data class Result(val cancelstatus: Int, val updated: Boolean)

    init {
        etpName = "АО \"Узбекская Республиканская товарно-сырьевая биржа\""
        etpUrl = "https://dxarid.uzex.uz/ru"
    }

    val typeFz by lazy {
        205
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager.getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ?").apply {
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
            var fullnameOrg = htmlTen.selectFirst("div.left_element:contains(Наименование заказчика) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (fullnameOrg == "") {
                fullnameOrg = htmlTen.selectFirst("div.left_element:contains(Название организации:) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
            }
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
                    val postalAdr = htmlTen.selectFirst("div.left_element:contains(Адрес заказчика:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    inn = htmlTen.selectFirst("div.left_element:contains(ИНН:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val kpp = ""
                    val email = ""
                    val phone = htmlTen.selectFirst("div.left_element:contains(Телефон:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val contactPerson = htmlTen.selectFirst("div.left_element:contains(Имя и должность ответственного лица заказчика) + div")?.ownText()?.trim { it <= ' ' }
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
            val datePubTmp = htmlTen.selectFirst("div.left_element:contains(Дата начала:) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            val datePub = datePubTmp.getDateFromString(formatterOnlyDate)
            if (datePub != Date(0L)) {
                tn.pubDate = datePub
            }
            val idEtp = getEtp(con)
            val idPlacingWay = 0
            var idTender = 0
            val idRegion = 0
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
                TenderAbstract.UpdateTender++
            } else {
                TenderAbstract.AddTender++
            }
            

        })
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