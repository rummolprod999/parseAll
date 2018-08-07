package parser.tenders

import parser.builderApp.BuilderApp
import java.sql.*
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class TenderAbstract {
    companion object Counts {
        var AddTender = 0
        var UpdateTender = 0
    }

    var etpName = ""
    var etpUrl = ""

    fun getEtp(con: Connection): Int {
        var IdEtp = 0
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
        return IdEtp
    }

    fun getPlacingWay(con: Connection, placingWay: String): Int {
        var idPlacingWay = 0
        val stmto = con.prepareStatement("SELECT id_placing_way FROM ${BuilderApp.Prefix}placing_way WHERE name = ? LIMIT 1")
        stmto.setString(1, placingWay)
        val rso = stmto.executeQuery()
        if (rso.next()) {
            idPlacingWay = rso.getInt(1)
            rso.close()
            stmto.close()
        } else {
            rso.close()
            stmto.close()
            val conf = getConformity(placingWay)
            val stmtins = con.prepareStatement("INSERT INTO ${BuilderApp.Prefix}placing_way SET name = ?, conformity = ?", Statement.RETURN_GENERATED_KEYS)
            stmtins.setString(1, placingWay)
            stmtins.setInt(2, conf)
            stmtins.executeUpdate()
            val rsoi = stmtins.generatedKeys
            if (rsoi.next()) {
                idPlacingWay = rsoi.getInt(1)
            }
            rsoi.close()
            stmtins.close()

        }
        return idPlacingWay
    }

    fun getIdRegion(con: Connection, reg: String): Int {
        var idReg = 0
        val re = getRegion(reg)
        if (re != "") {
            val stmto = con.prepareStatement("SELECT id FROM region WHERE name LIKE ?")
            stmto.setString(1, "%$re%")
            val rso = stmto.executeQuery()
            if (rso.next()) {
                idReg = rso.getInt(1)
                rso.close()
                stmto.close()
            } else {
                rso.close()
                stmto.close()
            }
        }
        return idReg

    }

    @Throws(SQLException::class, ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    fun addVNum(con: Connection, id: String, typeFz: Int) {
        var verNum = 1
        val p1: PreparedStatement = con.prepareStatement("SELECT id_tender FROM ${BuilderApp.Prefix}tender WHERE purchase_number = ? AND type_fz = ? ORDER BY UNIX_TIMESTAMP(date_version) ASC")
        p1.setString(1, id)
        p1.setInt(2, typeFz)
        val r1: ResultSet = p1.executeQuery()
        while (r1.next()) {
            val IdTender = r1.getInt(1)
            con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET num_version = ? WHERE id_tender = ? AND type_fz = ?").apply {
                setInt(1, verNum)
                setInt(2, IdTender)
                setInt(3, typeFz)
                executeUpdate()
                close()
            }
            verNum++
        }
        r1.close()
        p1.close()

    }

    @Throws(SQLException::class, ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    fun tenderKwords(idTender: Int, con: Connection, addInfo: String = "") {
        val s = StringBuilder()
        if (addInfo != "") with(s) { append(addInfo) }
        val p1: PreparedStatement = con.prepareStatement("SELECT DISTINCT po.name, po.okpd_name FROM ${BuilderApp.Prefix}purchase_object AS po LEFT JOIN ${BuilderApp.Prefix}lot AS l ON l.id_lot = po.id_lot WHERE l.id_tender = ?")
        p1.setInt(1, idTender)
        val r1: ResultSet = p1.executeQuery()
        while (r1.next()) {
            var name: String? = r1.getString(1)
            if (name == null) {
                name = ""
            }
            var okpdName: String? = r1.getString(2)
            if (okpdName == null) {
                okpdName = ""
            }
            with(s) {
                append(" $name")
                append(" $okpdName")
            }
        }
        r1.close()
        p1.close()
        val p2: PreparedStatement = con.prepareStatement("SELECT DISTINCT file_name FROM ${BuilderApp.Prefix}attachment WHERE id_tender = ?")
        p2.setInt(1, idTender)
        val r2: ResultSet = p2.executeQuery()
        while (r2.next()) {
            var attName: String? = r2.getString(1)
            if (attName == null) {
                attName = ""
            }
            s.append(" $attName")
        }
        r2.close()
        p2.close()
        var idOrg = 0
        val p3: PreparedStatement = con.prepareStatement("SELECT purchase_object_info, id_organizer FROM ${BuilderApp.Prefix}tender WHERE id_tender = ?")
        p3.setInt(1, idTender)
        val r3: ResultSet = p3.executeQuery()
        while (r3.next()) {
            idOrg = r3.getInt(2)
            val purOb = r3.getString(1)
            s.append(" $purOb")
        }
        r3.close()
        p3.close()
        if (idOrg != 0) {
            val p4: PreparedStatement = con.prepareStatement("SELECT full_name, inn FROM ${BuilderApp.Prefix}organizer WHERE id_organizer = ?")
            p4.setInt(1, idOrg)
            val r4: ResultSet = p4.executeQuery()
            while (r4.next()) {
                var innOrg: String? = r4.getString(2)
                if (innOrg == null) {
                    innOrg = ""
                }
                var nameOrg: String? = r4.getString(1)
                if (nameOrg == null) {
                    nameOrg = ""
                }
                with(s) {
                    append(" $innOrg")
                    append(" $nameOrg")
                }

            }
            r4.close()
            p4.close()
        }
        val p5: PreparedStatement = con.prepareStatement("SELECT DISTINCT cus.inn, cus.full_name FROM ${BuilderApp.Prefix}customer AS cus LEFT JOIN ${BuilderApp.Prefix}purchase_object AS po ON cus.id_customer = po.id_customer LEFT JOIN ${BuilderApp.Prefix}lot AS l ON l.id_lot = po.id_lot WHERE l.id_tender = ?")
        p5.setInt(1, idOrg)
        val r5: ResultSet = p5.executeQuery()
        while (r5.next()) {
            var fullNameC: String?
            fullNameC = r5.getString(1)
            if (fullNameC == null) {
                fullNameC = ""
            }
            var innC: String?
            innC = r5.getString(2)
            if (innC == null) {
                innC = ""
            }
            with(s) {
                append(" $innC")
                append(" $fullNameC")
            }
        }
        r5.close()
        p5.close()
        val pattern: Pattern = Pattern.compile("\\s+")
        val matcher: Matcher = pattern.matcher(s.toString())
        var ss: String = matcher.replaceAll(" ")
        ss = ss.trim { it <= ' ' }
        val p6 = con.prepareStatement("UPDATE ${BuilderApp.Prefix}tender SET tender_kwords = ? WHERE id_tender = ?")
        p6.setString(1, ss)
        p6.setInt(2, idTender)
        p6.executeUpdate()
        p6.close()

    }

    fun getOkpd(s: String): Pair<Int, String> {
        var okpd2GroupCode = 0
        var okpd2GroupLevel1Code = ""
        if (s.length > 1) {
            val dot = s.indexOf('.')
            if (dot != -1) {
                val okpd2GroupCodeTemp = s.slice(0 until dot)
                try {
                    okpd2GroupCode = Integer.parseInt(okpd2GroupCodeTemp)
                } catch (e: Exception) {
                }
            }
        }
        if (s.length > 3) {
            val dot = s.indexOf('.')
            if (dot != -1) {
                okpd2GroupLevel1Code = s.slice(dot + 1 until dot + 2)
            }

        }
        return Pair(okpd2GroupCode, okpd2GroupLevel1Code)
    }

    fun getRegion(sp: String): String {
        val s = sp.toLowerCase()
        return when {
            s.contains("белгор") -> "белгор"
            s.contains("брянск") -> "брянск"
            s.contains("владимир") -> "владимир"
            s.contains("воронеж") -> "воронеж"
            s.contains("иванов") -> "иванов"
            s.contains("калужск") -> "калужск"
            s.contains("костром") -> "костром"
            s.contains("курск") -> "курск"
            s.contains("липецк") -> "липецк"
            s.contains("москва") -> "москва"
            s.contains("московск") -> "московск"
            s.contains("орлов") -> "орлов"
            s.contains("рязан") -> "рязан"
            s.contains("смолен") -> "смолен"
            s.contains("тамбов") -> "тамбов"
            s.contains("твер") -> "твер"
            s.contains("тульс") -> "тульс"
            s.contains("яросл") -> "яросл"
            s.contains("архан") -> "архан"
            s.contains("вологод") -> "вологод"
            s.contains("калинин") -> "калинин"
            s.contains("карел") -> "карел"
            s.contains("коми") -> "коми"
            s.contains("ленинг") -> "ленинг"
            s.contains("мурм") -> "мурм"
            s.contains("ненец") -> "ненец"
            s.contains("новгор") -> "новгор"
            s.contains("псков") -> "псков"
            s.contains("санкт") -> "санкт"
            s.contains("адыг") -> "адыг"
            s.contains("астрахан") -> "астрахан"
            s.contains("волгог") -> "волгог"
            s.contains("калмык") -> "калмык"
            s.contains("краснод") -> "краснод"
            s.contains("ростов") -> "ростов"
            s.contains("дагест") -> "дагест"
            s.contains("ингуш") -> "ингуш"
            s.contains("кабардин") -> "кабардин"
            s.contains("карача") -> "карача"
            s.contains("осети") -> "осети"
            s.contains("ставроп") -> "ставроп"
            s.contains("чечен") -> "чечен"
            s.contains("башкор") -> "башкор"
            s.contains("киров") -> "киров"
            s.contains("марий") -> "марий"
            s.contains("мордов") -> "мордов"
            s.contains("нижерог") -> "нижерог"
            s.contains("оренбур") -> "оренбур"
            s.contains("пензен") -> "пензен"
            s.contains("пермс") -> "пермс"
            s.contains("самар") -> "самар"
            s.contains("сарат") -> "сарат"
            s.contains("татарс") -> "татарс"
            s.contains("удмурт") -> "удмурт"
            s.contains("ульян") -> "ульян"
            s.contains("чуваш") -> "чуваш"
            s.contains("курган") -> "курган"
            s.contains("свердлов") -> "свердлов"
            s.contains("тюмен") -> "тюмен"
            s.contains("ханты") -> "ханты"
            s.contains("челяб") -> "челяб"
            s.contains("ямало") -> "ямало"
            s.contains("алтайск") -> "алтайск"
            s.contains("алтай") -> "алтай"
            s.contains("бурят") -> "бурят"
            s.contains("забайк") -> "забайк"
            s.contains("иркут") -> "иркут"
            s.contains("кемеров") -> "кемеров"
            s.contains("краснояр") -> "краснояр"
            s.contains("новосиб") -> "новосиб"
            s.contains("томск") -> "томск"
            s.contains("омск") -> "омск"
            s.contains("тыва") -> "тыва"
            s.contains("хакас") -> "хакас"
            s.contains("амурск") -> "амурск"
            s.contains("еврей") -> "еврей"
            s.contains("камчат") -> "камчат"
            s.contains("магад") -> "магад"
            s.contains("примор") -> "примор"
            s.contains("сахалин") -> "сахалин"
            s.contains("якут") -> "якут"
            s.contains("саха") -> "саха"
            s.contains("хабар") -> "хабар"
            s.contains("чукот") -> "чукот"
            s.contains("крым") -> "крым"
            s.contains("севастоп") -> "севастоп"
            s.contains("байкон") -> "байкон"
            else -> ""
        }

    }

    fun getConformity(conf: String): Int {
        val s = conf.toLowerCase()
        return when {
            s.contains("открыт") -> 5
            s.contains("аукцион") -> 1
            s.contains("котиров") -> 2
            s.contains("предложен") -> 3
            s.contains("единств") -> 4
            else -> 6
        }
    }
}