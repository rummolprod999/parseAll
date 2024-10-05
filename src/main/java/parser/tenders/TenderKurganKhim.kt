package parser.tenders

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.findElementWithoutException
import parser.logger.logger
import parser.networkTools.downloadFromUrl
import parser.tenderClasses.KurganKhim
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderKurganKhim(
    val tn: KurganKhim,
    val driver: ChromeDriver,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "Электронная торговая площадка  ООО «Курганхиммаш»"
        etpUrl = "https://kurgankhimmash-zaproscen.ru/"
    }

    val typeFz by lazy { 265 }

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
                    driver.get(tn.href)
                    driver.switchTo().defaultContent()
                    val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
                    Thread.sleep(2000)
                    driver.switchTo().defaultContent()
                    val (cancelstatus, updated) = updateVersion(con, dateVer)
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
                                        setString(1, tn.orgName)
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
                    val lotName = tn.purName
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
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?, lot_name = ?",
                                Statement.RETURN_GENERATED_KEYS,
                            ).apply {
                                setInt(1, idTender)
                                setInt(2, lotNumber)
                                setString(3, "")
                                setString(4, "")
                                setString(5, lotName)
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
                    val delivTerm0 =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//td[. = 'Требуемые сроки поставки']/following-sibling::td",
                                ),
                            )?.text
                            ?.trim { it <= ' ' } ?: ""
                    val delivTerm1 =
                        driver
                            .findElementWithoutException(
                                By.xpath("//td[. = 'Общие условия оплаты']/following-sibling::td"),
                            )?.text
                            ?.trim { it <= ' ' } ?: ""
                    val delivTerm2 =
                        driver
                            .findElementWithoutException(
                                By.xpath("//td[. = 'Особые требования']/following-sibling::td"),
                            )?.text
                            ?.trim { it <= ' ' } ?: ""
                    val delivTerm =
                        "Требуемые сроки поставки: $delivTerm0\n Общие условия оплаты: $delivTerm1\n Особые требования: $delivTerm2"
                            .trim { it <= ' ' }
                    val delivPlace =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//td[. = 'Адрес доставки ТМЦ или оказания услуг']/following-sibling::td",
                                ),
                            )?.text
                            ?.trim { it <= ' ' } ?: ""
                    if (delivTerm0 != "" || delivTerm1 != "" || delivTerm2 != "") {
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

    private fun getPurObjs(
        it: Element,
        con: Connection,
        idLot: Int,
        idCustomer: Int,
    ) {
        val name = it.selectFirst("td:eq(0) span a")?.ownText()?.trim { it <= ' ' } ?: ""
        if (name != "") {
            con
                .prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?, okpd2_code = ?, okpd_name = ?",
                ).apply {
                    setInt(1, idLot)
                    setInt(2, idCustomer)
                    setString(3, name)
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
        val urlLot =
            it.selectFirst("td:eq(0) span a")?.attr("href")?.trim { it <= ' ' }
                ?: run {
                    logger("urlLot not found on")
                    return
                }
        val href = "https://kurgankhimmash-zaproscen.ru$urlLot"
        val pageTen = downloadFromUrl(href)
        if (pageTen == "") {
            logger("Gets empty string ${this::class.simpleName}", tn.href)
            return
        }
        val htmlTen = Jsoup.parse(pageTen)
        val delivTerm0 =
            htmlTen.selectFirst("td:contains(Требуемые сроки поставки) + td span")?.text()?.trim {
                it <= ' '
            } ?: ""
        val delivTerm1 =
            htmlTen.selectFirst("td:contains(Общие условия оплаты) + td span")?.text()?.trim {
                it <= ' '
            } ?: ""
        val delivTerm2 =
            htmlTen.selectFirst("td:contains(Особые требования) + td span")?.text()?.trim {
                it <= ' '
            } ?: ""
        val delivTerm =
            "Требуемые сроки поставки: $delivTerm0\n Общие условия оплаты: $delivTerm1\n Особые требования: $delivTerm2"
                .trim { it <= ' ' }
        if (delivTerm0 != "" || delivTerm1 != "" || delivTerm2 != "") {
            val insertCusRec =
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
    }

    private fun updateVersion(
        con: Connection,
        dateVer: Date,
    ): Result {
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
        return Result(cancelstatus1, updated1)
    }

    private fun getAttachments(
        idTender: Int,
        con: Connection,
    ) {
        tn.attachments.forEach {
            con
                .prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?",
                ).apply {
                    setInt(1, idTender)
                    setString(2, it.key)
                    setString(3, it.value)
                    executeUpdate()
                    close()
                }
        }
    }
}
