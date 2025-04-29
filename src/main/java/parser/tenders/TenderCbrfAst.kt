package parser.tenders

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import parser.builderApp.BuilderApp
import parser.extensions.deleteAllWhiteSpace
import parser.extensions.findElementWithoutException
import parser.extensions.getDateFromString
import parser.logger.logger
import parser.tools.formatterGpn
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderCbrfAst(
    val drv: ChromeDriver,
) : TenderAbstract(),
    ITender {
    var idTender = 0

    init {
        etpName =
            "«Сбербанк - Автоматизированная система торгов» - \"Закупки Центрального банка Российской Федерации\""
        etpUrl = "http://utp.sberbank-ast.ru/CBRF/List/PurchaseList"
    }

    override fun parsing() {
        // drv.switchTo().defaultContent()
        val wait = WebDriverWait(drv, java.time.Duration.ofSeconds(30L))
        val href =
            try {
                drv.currentUrl
            } catch (e: Exception) {
                // logger(e)
                return
            }
        try {
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[p[contains(., 'Номер процедуры')]]/following-sibling::div/p"),
                ),
            )
        } catch (e: Exception) {
            logger("cannot wait appier purNum", href)
            return
        }
        val purNum =
            drv
                .findElementWithoutException(
                    By.xpath("//div[p[contains(., 'Номер процедуры')]]/following-sibling::div/p"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("cannot purNum in tender", href)
            return
        }
        val status =
            drv
                .findElementWithoutException(
                    By.xpath("//div[p[contains(., 'Статус процедуры')]]/following-sibling::div/p"),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        val purName =
            drv
                .findElementWithoutException(
                    By.xpath(
                        "//div[p[contains(., 'Наименование процедуры')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim { it <= ' ' } ?: ""
        if (purName == "") {
            logger("cannot purName in tender", href)
            return
        }
        var datePubTmp =
            drv
                .findElementWithoutException(
                    By.xpath(
                        "//div[p[contains(., 'Дата и время начала срока подачи предложений')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        if (datePubTmp == "") {
            datePubTmp =
                drv
                    .findElementWithoutException(
                        By.xpath(
                            "//div[p[contains(., 'Дата и время начала подачи заявок на участие')]]/following-sibling::div/p",
                        ),
                    )?.text
                    ?.trim()
                    ?.trim { it <= ' ' } ?: ""
        }
        var dateEndTmp =
            drv
                .findElementWithoutException(
                    By.xpath(
                        "//div[p[contains(., 'Дата и время окончания подачи заявок на участие')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        if (dateEndTmp == "") {
            dateEndTmp =
                drv
                    .findElementWithoutException(
                        By.xpath(
                            "//div[p[contains(., 'Дата и время окончания срока подачи заявок на участие')]]/following-sibling::div/p",
                        ),
                    )?.text
                    ?.trim()
                    ?.trim { it <= ' ' } ?: ""
        }
        val pubDate = datePubTmp.getDateFromString(formatterGpn)
        val endDate = dateEndTmp.getDateFromString(formatterGpn)
        if (pubDate == Date(0L) || endDate == Date(0L)) {
            logger("cannot find pubDate or dateEnd on page", href, purNum)
            return
        }
        val dateScoringTmp =
            drv
                .findElementWithoutException(
                    By.xpath(
                        "//div[p[contains(., 'Окончание рассмотрения заявок на участие')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val dateScoring = dateScoringTmp.getDateFromString(formatterGpn)
        val dateBiddingTmp =
            drv
                .findElementWithoutException(
                    By.xpath(
                        "//div[p[contains(., 'Дата и время начала торгов')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val dateBidding = dateBiddingTmp.getDateFromString(formatterGpn)
        val dateVer = Date()
        DriverManager
            .getConnection(BuilderApp.UrlConnect, BuilderApp.UserDb, BuilderApp.PassDb)
            .use(
                fun(con: Connection) {
                    val stmt0 =
                        con
                            .prepareStatement(
                                "SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ? AND notice_version = ? AND scoring_date = ? AND bidding_date = ?",
                            ).apply {
                                setString(1, purNum)
                                setTimestamp(2, Timestamp(pubDate.time))
                                setInt(3, typeFz)
                                setTimestamp(4, Timestamp(endDate.time))
                                setString(5, status)
                                setTimestamp(6, Timestamp(dateScoring.time))
                                setTimestamp(7, Timestamp(dateBidding.time))
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
                                setString(1, purNum)
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
                    val fullnameOrg =
                        drv
                            .findElementWithoutException(
                                By.xpath(
                                    "//div[p[contains(., 'Наименование организатора')]]/following-sibling::div/p",
                                ),
                            )?.text
                            ?.trim()
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
                            val postalAdr =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'Фактический адрес (почтовый)')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val factAdr =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'Место нахождения (юридический адрес)')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val inn =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'ИНН организатора')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val kpp =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'КПП организатора')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val email =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'Адрес электронной почты')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val phone =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'Номер контактного телефона')]]/following-sibling::div/p",
                                        ),
                                    )?.text
                                    ?.trim()
                                    ?.trim { it <= ' ' } ?: ""
                            val contactPerson =
                                drv
                                    .findElementWithoutException(
                                        By.xpath(
                                            "//div[p[contains(., 'Контактное лицо')]]/following-sibling::div/p",
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
                    val placingWayName =
                        drv
                            .findElementWithoutException(
                                By.xpath(
                                    "//div[p[contains(., 'Тип процедуры')]]/following-sibling::div/p",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    var idPlacingWay = 0
                    if (placingWayName != "") {
                        idPlacingWay = getPlacingWay(con, placingWayName)
                    }
                    val factAdrOrg =
                        drv
                            .findElementWithoutException(
                                By.xpath(
                                    "//div[p[contains(., 'Место нахождения')]]/following-sibling::div/p",
                                ),
                            )?.text
                            ?.trim()
                            ?.trim { it <= ' ' } ?: ""
                    val idRegion = getIdRegion(con, factAdrOrg)

                    val insertTender =
                        con.prepareStatement(
                            "INSERT INTO ${BuilderApp.Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, bidding_date = ?, scoring_date = ?",
                            Statement.RETURN_GENERATED_KEYS,
                        )
                    insertTender.setString(1, purNum)
                    insertTender.setString(2, purNum)
                    insertTender.setTimestamp(3, Timestamp(pubDate.time))
                    insertTender.setString(4, href)
                    insertTender.setString(5, purName)
                    insertTender.setInt(6, typeFz)
                    insertTender.setInt(7, idOrganizer)
                    insertTender.setInt(8, idPlacingWay)
                    insertTender.setInt(9, idEtp)
                    insertTender.setTimestamp(10, Timestamp(endDate.time))
                    insertTender.setInt(11, cancelstatus)
                    insertTender.setTimestamp(12, Timestamp(dateVer.time))
                    insertTender.setInt(13, 1)
                    insertTender.setString(14, status)
                    insertTender.setString(15, href)
                    insertTender.setString(16, href)
                    insertTender.setInt(17, idRegion)
                    insertTender.setTimestamp(18, Timestamp(dateBidding.time))
                    insertTender.setTimestamp(19, Timestamp(dateScoring.time))
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
                    parserLots(drv, con, href)
                    try {
                        getDocsAst(drv, con, "CBRF", idTender)
                    } catch (e: Exception) {
                        logger(e, e.stackTrace)
                    }
                    try {
                        tenderKwords(idTender, con)
                    } catch (e: Exception) {
                        logger("Ошибка добавления ключевых слов", e.stackTrace, e)
                    }

                    try {
                        addVNum(con, purNum, typeFz)
                    } catch (e: Exception) {
                        logger("Ошибка добавления версий", e.stackTrace, e)
                    }
                },
            )
        return
    }

    override fun getDocsAst(
        drv: ChromeDriver,
        con: Connection,
        section: String,
        idTender: Int,
    ) {
        val docs = drv.findElements(By.xpath("//div[@class = 'file-wrapper']"))
        docs.forEach {
            it.click()
            Thread.sleep(1000)
            val name =
                drv.findElementWithoutException(By.xpath("//p[a[@class='file-modal']]"))?.text?.trim {
                    it <= ' '
                } ?: ""
            val href =
                drv.findElementWithoutException(By.xpath("//p[a[@class='file-modal']]/a"))?.getAttribute("href")?.trim {
                    it <= ' '
                } ?: ""
            if (name != "" && href != "") {
                val insertDoc =
                    con.prepareStatement(
                        "INSERT INTO ${BuilderApp.Prefix}attachment SET id_tender = ?, file_name = ?, url = ?",
                    )
                insertDoc.setInt(1, idTender)
                insertDoc.setString(2, name)
                insertDoc.setString(3, href)
                insertDoc.executeUpdate()
                insertDoc.close()
            }
            drv.findElement(By.xpath("//p[.='Скачивание файла']/following-sibling::div[@class='modal-close']")).click()
            Thread.sleep(500)

        }
    }

    private fun parserLot(
        el: WebElement,
        con: Connection,
        href: String,
        lotNum: Int,
    ) {
        val nmck =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//div[p[contains(., 'Начальная (максимальная) цена')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.replace(',', '.')
                ?.deleteAllWhiteSpace()
                ?.trim { it <= ' ' } ?: ""
        val lotName =
            el
                .findElementWithoutException(
                    By.xpath(".//div[p[contains(., 'Наименование лота')]]/following-sibling::div/p"),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val currency =
            el
                .findElementWithoutException(
                    By.xpath(".//div[p[contains(., 'Валюта')]]/following-sibling::div/p"),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        var idLot = 0
        val insertLot =
            con
                .prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                    Statement.RETURN_GENERATED_KEYS,
                ).apply {
                    setInt(1, idTender)
                    setInt(2, lotNum)
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
        var idCustomer = 0
        val customers =
            el.findElements(
                By.xpath("//div[p[contains(., 'Заказчики')]]/following-sibling::div//tbody/tr"),
            )
        if (!customers.isEmpty()) {
            val cusName =
                customers[0]
                    .findElementWithoutException(By.xpath("./td[2]/span"))
                    ?.text
                    ?.trim()
                    ?.trim { it <= ' ' } ?: ""
            val cusInn =
                customers[0]
                    .findElementWithoutException(By.xpath("./td[3]/span"))
                    ?.text
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
                    stmtins.setString(3, cusInn)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idCustomer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
        }
        val delivPlace =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//div[p[contains(., 'Место поставки товара, выполнения работ, оказания услуг')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        val delivTerm =
            el
                .findElementWithoutException(
                    By.xpath(
                        ".//div[p[contains(., 'Сроки поставки товара, выполнения работ, оказания услуг')]]/following-sibling::div/p",
                    ),
                )?.text
                ?.trim()
                ?.trim { it <= ' ' } ?: ""
        if (delivPlace != "" || delivTerm != "") {
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
        val purObjects =
            el.findElements(
                By.xpath(".//div[p[contains(., 'Позиции лота')]]/following-sibling::div//tbody/tr"),
            )
        if (purObjects.isNotEmpty()) {
            for (po in purObjects) {
                val name =
                    po.findElementWithoutException(By.xpath("./td[2]//p"))?.text?.trim()?.trim {
                        it <= ' '
                    } ?: ""
                val quantity =
                    po
                        .findElementWithoutException(By.xpath("./td[3]//p"))
                        ?.text
                        ?.trim()
                        ?.deleteAllWhiteSpace()
                        ?.trim { it <= ' ' } ?: ""
                val okei =
                    po
                        .findElementWithoutException(By.xpath("./td[5]//p"))
                        ?.text
                        ?.trim()
                        ?.deleteAllWhiteSpace()
                        ?.trim { it <= ' ' } ?: ""
                val pr =
                    po
                        .findElementWithoutException(By.xpath("./td[4]//p"))
                        ?.text
                        ?.trim()
                        ?.replace(',', '.')
                        ?.deleteAllWhiteSpace()
                        ?.trim { it <= ' ' } ?: ""
                val okpd2 = ""
                con
                    .prepareStatement(
                        "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, okei = ?, quantity_value = ?, customer_quantity_value = ?, okpd2_code = ?, sum = ?",
                    ).apply {
                        setInt(1, idLot)
                        setInt(2, idCustomer)
                        setString(3, name)
                        setString(4, okei)
                        setString(5, quantity)
                        setString(6, quantity)
                        setString(7, okpd2)
                        setString(8, pr)
                        executeUpdate()
                        close()
                    }
            }
        } else {
            con
                .prepareStatement(
                    "INSERT INTO ${BuilderApp.Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, sum = ?",
                ).apply {
                    setInt(1, idLot)
                    setInt(2, idCustomer)
                    setString(3, lotName)
                    setString(4, nmck)
                    executeUpdate()
                    close()
                }
        }
    }

    private fun parserLots(
        drv: ChromeDriver,
        con: Connection,
        href: String,
    ) {
        val lots =
            drv.findElements(
                By.xpath("//div[@class = 'div-hidden__header' and div[p[ contains(.,'Лоты')]]]/following-sibling::div"),
            )
        if (lots.isEmpty()) {
            logger("cannot find lots in tender", href)
            return
        }
        for ((ind, el) in lots.withIndex()) {
            try {
                parserLot(el, con, href, ind + 1)
            } catch (e: Exception) {
                logger("Error in parserLot", href, e, e.stackTrace)
            }
        }
    }

    companion object TypeFz {
        val typeFz = 226
    }
}
