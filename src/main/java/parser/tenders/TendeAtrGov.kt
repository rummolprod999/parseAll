package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.AtrGov
import parser.tools.formatterGpn
import parser.tools.formatterOnlyDate
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TendeAtrGov(
    val tn: AtrGov,
    val driver: ChromeDriver,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "Агенство по технологическому развитию"
        etpUrl = "https://208.atr.gov.ru/"
    }

    val typeFz by lazy { 409 }

    override fun parsing() {
        val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
        driver.get(tn.href)
        driver.switchTo().defaultContent()
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(@class, 'js-store-prod-name')]"),
            ),
        )
        if (tn.purName == "") {
            tn.purName = driver
                .findElementWithoutException(
                    By.xpath(
                        "//h1[contains(@class, 'js-store-prod-name')]",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        }
        var datePubTmp =
            driver
                .findElementWithoutException(
                    By.xpath(
                        "//p[contains(., 'Начало приема заявок:')]",
                    ),
                )?.text
                ?.replace("Начало приема заявок:", "")
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val pubDate =
            if (datePubTmp.length < 11) {
                datePubTmp.getDateFromString(formatterOnlyDate)
            } else {
                datePubTmp.getDateFromString(formatterGpn)
            }
        var dateEndTmp =
            driver
                .findElementWithoutException(
                    By.xpath(
                        "//p[contains(., 'Окончание приема заявок:')]",
                    ),
                )?.text
                ?.replace("Окончание приема заявок:", "")
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val endDate =
            if (dateEndTmp.length < 11) {
                dateEndTmp.getDateFromString(formatterOnlyDate)
            } else {
                dateEndTmp.getDateFromString(formatterGpn)
            }
        if (pubDate == Date(0L) && endDate == Date(0L)) {
            logger("cannot find pubDate and dateEnd on page", tn.href)
            return
        }
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
                                setTimestamp(3, Timestamp(endDate.time))
                            }
                    val r = stmt0.executeQuery()
                    if (r.next()) {
                        r.close()
                        stmt0.close()
                        return
                    }
                    r.close()
                    stmt0.close()
                    val (cancelstatus, updated) = updateVersion(con, dateVer, typeFz, tn.purNum)
                    var idOrganizer = 0
                    if (etpName != "") {
                        val stmto =
                            con.prepareStatement(
                                "SELECT id_organizer FROM ${BuilderApp.Prefix}organizer WHERE full_name = ?",
                            )
                        stmto.setString(1, etpName)
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
                            val phone =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//p[contains(., 'Телефон:')]",
                                        ),
                                    )?.text
                                    ?.replace("Телефон:", "")
                                    ?.trim { it <= ' ' } ?: ""
                            val contactPerson =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//p[contains(., 'Менеджер:')]",
                                        ),
                                    )?.text
                                    ?.replace("Менеджер:", "")
                                    ?.trim { it <= ' ' } ?: ""
                            val stmtins =
                                con
                                    .prepareStatement(
                                        "INSERT INTO ${BuilderApp.Prefix}organizer SET full_name = ?, post_address = ?, contact_email = ?, contact_phone = ?, fact_address = ?, contact_person = ?, inn = ?, kpp = ?",
                                        Statement.RETURN_GENERATED_KEYS,
                                    ).apply {
                                        setString(1, etpName)
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
                    var idPlacingWay = 0
                    var idTender = 0
                    val idRegion = 0
                    val dateScoringTmp =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//p[contains(., 'Дата подведения итогов:')]",
                                ),
                            )?.text
                            ?.replace("Дата подведения итогов:", "")
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    val dateScoring = dateScoringTmp.getDateFromString(formatterOnlyDate)
                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, scoring_date = ?",
                            Statement.RETURN_GENERATED_KEYS,
                        )
                    insertTender.setString(1, tn.purNum)
                    insertTender.setString(2, tn.purNum)
                    insertTender.setTimestamp(3, Timestamp(pubDate.time))
                    insertTender.setString(4, tn.href)
                    insertTender.setString(5, tn.purName)
                    insertTender.setInt(6, typeFz)
                    insertTender.setInt(7, idOrganizer)
                    insertTender.setInt(8, idPlacingWay)
                    insertTender.setInt(9, idEtp)
                    insertTender.setTimestamp(10, Timestamp(endDate.time))
                    insertTender.setInt(11, cancelstatus)
                    insertTender.setTimestamp(12, Timestamp(dateVer.time))
                    insertTender.setInt(13, 1)
                    insertTender.setString(14, "")
                    insertTender.setString(15, tn.href)
                    insertTender.setString(16, tn.href)
                    insertTender.setInt(17, idRegion)
                    insertTender.setTimestamp(18, Timestamp(dateScoring.time))
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
                                setString(5, tn.purName)
                                executeUpdate()
                            }
                    val rl = insertLot.generatedKeys
                    if (rl.next()) {
                        idLot = rl.getInt(1)
                    }
                    rl.close()
                    insertLot.close()
                    var idCustomer = 0
                    if (etpName != "") {
                        val stmtoc =
                            con.prepareStatement(
                                "SELECT id_customer FROM ${BuilderApp.Prefix}customer WHERE full_name = ? LIMIT 1",
                            )
                        stmtoc.setString(1, etpName)
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
                            stmtins.setString(1, etpName)
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
                        val documents = driver.findElements(By.xpath("//div[@class = 'js-store-prod-all-text']/a"))
                        documents.forEach {
                            val href =
                                it
                                    ?.getAttribute("href")
                                    ?.trim { it <= ' ' } ?: ""

                            val nameDoc =
                                it.text?.trim {
                                    it <= ' '
                                } ?: ""
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
                    } catch (e: Exception) {
                        logger(e, e.stackTrace)
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
}
