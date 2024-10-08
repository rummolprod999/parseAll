package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.AttachOilb2b
import parser.tenderClasses.Oilb2b
import parser.tenderClasses.Oilb2bProduct
import parser.tools.formatter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderOilb2b(
    val tn: Oilb2b,
    val driver: ChromeDriver,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "«НЕФТЬ-B2B»"
        etpUrl = "https://oilb2bcs.ru/"
    }

    override fun parsing() {
        val dateVer = Date()
        val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
        driver.get(tn.href)
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(., 'Сведения о закупке')]"),
            ),
        )
        val pubDateT =
            driver
                .findElementWithoutException(
                    By.xpath("//div[. = 'дата публикации заявки']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' }
                ?: run {
                    logger("pubDateT not found")
                    return
                }
        tn.pubDate = pubDateT.getDateFromString(formatter)
        val endDateT =
            driver
                .findElementWithoutException(
                    By.xpath("//div[. = 'окончание приема предложений']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' }
                ?: run {
                    logger("endDateT not found")
                    return
                }
        tn.endDate = endDateT.getDateFromString(formatter)
        tn.cusName =
            driver
                .findElementWithoutException(
                    By.xpath("//div[. = 'заказчик']/following-sibling::div"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        driver.findElements(By.xpath("//div/a[contains(@href, '/PlanClaimFiles')]")).forEach { at ->
            val hrefAtt = at.getAttribute("href") ?: return@forEach
            val nameAtt = at.text?.trim { it <= ' ' } ?: return@forEach
            tn.attachments.add(
                AttachOilb2b(
                    hrefAtt,
                    if (nameAtt == "") {
                        "Документация"
                    } else {
                        nameAtt
                    },
                ),
            )
        }
        driver.findElements(By.xpath("//ul[@id = 'specData']/li[position() > 1]")).forEach { prod ->
            val prodName =
                prod.findElementWithoutException(By.xpath("./div[2]"))?.text?.trim { it <= ' ' }
                    ?: return@forEach
            val quant =
                prod.findElementWithoutException(By.xpath("./div[3]"))?.text?.trim { it <= ' ' }
                    ?: ""
            val okei =
                prod.findElementWithoutException(By.xpath("./div[4]"))?.text?.trim { it <= ' ' }
                    ?: ""
            val extDef =
                prod.findElementWithoutException(By.xpath("./div[5]"))?.text?.trim { it <= ' ' }
                    ?: ""
            if (prodName != "") tn.products.add(Oilb2bProduct(prodName, quant, okei, extDef))
        }
        DriverManager
            .getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con
                            .prepareStatement(
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ?",
                            ).apply {
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
                    val fullnameOrg = tn.cusName
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
                            val postalAdr = ""
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
                                idOrganizer = rsoi.getInt(1)
                            }
                            rsoi.close()
                            stmtins.close()
                        }
                    }
                    val idEtp = getEtp(con)
                    var idPlacingWay = 0
                    val idRegion = 0
                    var idTender = 0
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
                    insertTender.setString(14, tn.status)
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
                    addCounts(updated)
                    var idLot = 0
                    val insertLot =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                                Statement.RETURN_GENERATED_KEYS,
                            ).apply {
                                setInt(1, idTender)
                                setInt(2, 1)
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
                    val cusName = fullnameOrg
                    if (cusName != "") {
                        val stmtoc =
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1",
                            )
                        stmtoc.setString(1, cusName)
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
                            stmtins.setString(1, cusName)
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
                    if (tn.tenderDate != "" || tn.endTenderDate != "") {
                        val delivTerm =
                            "Срок закупки: ${tn.tenderDate}\nСрок исполнения договора (поставки): ${tn.endTenderDate}"
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?",
                            ).apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, "")
                                setString(4, delivTerm)
                                executeUpdate()
                                close()
                            }
                    }
                    tn.attachments.forEach { (Url, Name) ->
                        val insertDoc =
                            con
                                .prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?",
                                ).also {
                                    it.setInt(1, idTender)
                                    it.setString(2, Name)
                                    it.setString(3, Url)
                                    it.executeUpdate()
                                    it.close()
                                }
                    }
                    tn.products.forEach {
                        val insertPurObj =
                            con
                                .prepareStatement(
                                    "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, sum = ?, okpd_name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?",
                                ).apply {
                                    setInt(1, idLot)
                                    setInt(2, idCustomer)
                                    setString(3, "${it.prodName} ${it.extDef}")
                                    setString(4, "")
                                    setString(5, "")
                                    setString(6, it.okei)
                                    setString(7, it.quant)
                                    setString(8, it.quant)
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
                        addVNum(con, tn.purNum, typeFz)
                    } catch (e: Exception) {
                        logger("Ошибка добавления версий", e.stackTrace, e)
                    }
                },
            )
    }

    private fun addCounts(updated: Boolean) {
        if (updated) {
            UpdateTender++
        } else {
            AddTender++
        }
    }

    companion object TypeFz {
        const val typeFz = 266
    }
}
