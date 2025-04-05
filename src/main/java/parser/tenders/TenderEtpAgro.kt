package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.findElementWithoutException
import parser.extensions.getDataFromRegexp
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tenderClasses.EtpAgro
import parser.tools.formatterOnlyDate
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderEtpAgro(
    val tn: EtpAgro,
    val driver: ChromeDriver,
) : TenderAbstract(),
    ITender {
    init {
        etpName = "ЭТП-АГРО"
        etpUrl = "https://zakupka.etpagro.ru/"
    }

    companion object TypeFz {
        const val typeFz = 377
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
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? AND end_date = ? AND notice_version = ?",
                            ).apply {
                                setString(1, tn.purNum)
                                setInt(2, typeFz)
                                setTimestamp(3, Timestamp(tn.endDate.time))
                                setString(4, tn.status)
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
                                setInt(2, TenderAkbars.typeFz)
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
                    driver.get(tn.href)
                    driver.switchTo().defaultContent()
                    val wait = WebDriverWait(driver, java.time.Duration.ofSeconds(30L))
                    wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//span[. = 'Сведения о процедуре']"),
                        ),
                    )
                    val datePubTmp =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//ef-widget-label//span[. = 'Дата публикации процедуры']/../../following-sibling::div//span",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    val pubDate = datePubTmp.getDateFromString(formatterOnlyDate)
                    if (pubDate == Date(0L)) {
                        logger("cannot find pubDate on page", datePubTmp, tn.href)
                        return
                    }
                    var idOrganizer = 0
                    val fullnameOrg =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//ef-widget-label//span[. = 'Наименование организации']/../../following-sibling::div//span",
                                ),
                            )?.text
                            ?.trim { it <= ' ' } ?: ""
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
                            val factAdr =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//ef-widget-label//span[. = 'Юридический адрес']/../../following-sibling::div//span",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val kpp =
                                driver
                                    .findElementWithoutException(
                                        By.xpath("//div[@id = 'customerKpp']"),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val email =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//ef-widget-label//span[. = 'Адрес электронной почты']/../../following-sibling::div//span",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val phone =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//ef-widget-label//span[. = 'Контактный телефон']/../../following-sibling::div//span",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val contactPerson =
                                driver
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//ef-widget-label//span[. = 'Ответственный исполнитель']/../../following-sibling::div//span",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
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
                                        setString(7, "")
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
                    val placingWayName =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//ef-widget-label//span[. = 'Форма проведения торгов']/../../following-sibling::div//span",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    if (placingWayName != "") {
                        idPlacingWay = getPlacingWay(con, placingWayName)
                    }
                    val regionName =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//ef-widget-label//span[. = 'Регион']/../../following-sibling::div//span",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    val idRegion = getIdRegion(con, regionName)
                    var idTender = 0
                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?",
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
                    if (updated) {
                        UpdateTender++
                    } else {
                        AddTender++
                    }
                    driver.findElement(By.xpath("//a[contains(., 'Список лотов')]")).click()
                    // Thread.sleep(3000)
                    wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//span[contains(., ' Наименование ')]"),
                        ),
                    )
                    driver.findElement(By.xpath("//p-treetabletoggler/button")).click()
                    Thread.sleep(3000)
                    driver.switchTo().defaultContent()
                    var idLot = 0
                    val lotNumber = 1
                    val currency = "Российский рубль (RUB)"
                    val lotName =
                        driver
                            .findElementWithoutException(
                                By.xpath("//tr[contains(@class, 'treetable__node_root')]/td[2]"),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    val insertLot =
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?, lot_name = ?",
                                Statement.RETURN_GENERATED_KEYS,
                            ).apply {
                                setInt(1, idTender)
                                setInt(2, lotNumber)
                                setString(3, currency)
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
                    val cusName =
                        driver
                            .findElementWithoutException(
                                By.xpath(
                                    "//tr[contains(@class, 'treetable__node_root')]/td[3]//div[@efcfgelement]/span",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
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
                    driver.switchTo().defaultContent()
                    val purObjts =
                        driver.findElements(
                            By.xpath("//tr[contains(@class, 'treetable__node_child')]"),
                        )
                    purObjts.forEach { element ->
                        val purName =
                            element
                                .findElementWithoutException(
                                    By.xpath("./td[2]//div[@efcfgelement]/span"),
                                )?.text
                                ?.trim()
                                ?.trim { it <= ' ' } ?: ""
                        val okeiT =
                            element
                                .findElementWithoutException(
                                    By.xpath("./td[4]//div[@efcfgelement]/span"),
                                )?.text
                                ?.trim()
                                ?.trim { it <= ' ' } ?: ""
                        val okei = okeiT.getDataFromRegexp("""\d+\s*(.+)""")
                        val quant = okeiT.getDataFromRegexp("""(\d+)""")
                        val price = ""
                        val sum = ""
                        con
                            .prepareStatement(
                                "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, price = ?, sum = ?",
                            ).apply {
                                setInt(1, idLot)
                                setInt(2, idCustomer)
                                setString(3, purName)
                                setString(4, okei)
                                setString(5, quant)
                                setString(6, quant)
                                setString(7, price)
                                setString(8, sum)
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
}
