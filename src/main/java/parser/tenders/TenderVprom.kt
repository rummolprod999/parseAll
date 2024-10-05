package parser.tenders

import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.tenderClasses.Vprom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderVprom(
    val tn: Vprom,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "АО «Волтайр-Пром»"
        etpUrl = "https://voltyre-prom.ru/"
    }

    val typeFz by lazy { 319 }

    override fun parsing() {
        val dateVer = Date()
        DriverManager
            .getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con
                            .prepareStatement(
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ?",
                            ).apply {
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
                    val (cancelstatus, updated) = updateVersion(con, dateVer)
                    var idOrganizer = 0
                    val fullnameOrg = etpName
                    if (fullnameOrg != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?",
                            )
                        stmto.setString(1, fullnameOrg)
                        val rso = stmto.executeQuery()
                        if (rso.next()) {
                            idOrganizer = rso.getInt(1)
                            rso.close()
                            stmto.close()
                        } else {
                            rso.close()
                            stmto.close()
                            val postalAdr =
                                "Россия, 404103, Волгоградская область, г. Волжский, 7-я Автодорога, 25 А"
                            val inn = ""
                            val kpp = ""
                            val email = "voltyre@titancis.ru"
                            val phone = "+7-800-770-71-45"
                            val contactPerson = ""
                            val stmtins =
                                con
                                    .prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                        Statement.RETURN_GENERATED_KEYS,
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
                    val idEtp = getEtp(con)
                    val idPlacingWay = 0
                    var idTender = 0
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
                    var idLot = 0
                    val lotNumber = 1
                    val insertLot =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                                Statement.RETURN_GENERATED_KEYS,
                            ).apply {
                                setInt(1, idTender)
                                setInt(2, lotNumber)
                                setString(3, "")
                                setString(4, "")
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
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1",
                            )
                        stmtoc.setString(1, fullnameOrg)
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
                            stmtins.setString(1, fullnameOrg)
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
                    con
                        .prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?",
                        ).apply {
                            setInt(1, idLot)
                            setInt(2, idCustomer)
                            setString(3, tn.purName)
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

    private fun updateVersion(
        con: Connection,
        dateVer: Date,
    ): TenderUzex.Result {
        var updated1 = false
        var cancelstatus1 = 0
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
            updated1 = true
            val idT = rs.getInt(1)
            val dateB: Timestamp = rs.getTimestamp(2)
            if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
                con
                    .prepareStatement(
                        "UPDATE ${BuilderApp.Prefix}tender SET cancel=1 WHERE id_tender = ?",
                    ).apply {
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
        return TenderUzex.Result(cancelstatus1, updated1)
    }
}
