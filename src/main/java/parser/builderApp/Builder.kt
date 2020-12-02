package parser.builderApp

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import parser.Arguments
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


object BuilderApp {
    lateinit var arg: Arguments
    lateinit var Database: String
    lateinit var Prefix: String
    lateinit var UserDb: String
    lateinit var PassDb: String
    lateinit var Server: String
    var Port: Int = 3306
    lateinit var TempPath: String
    lateinit var LogPath: String
    lateinit var LogFile: String
    lateinit var UrlConnect: String
}

const val arguments = "salavat, umz, lsr, zmokursk, zmo45, zmokurgan, zmochel, transast, alrosa, ageat, rzn, brn, ivan, orel, nov, komi, kalin, nen, yalta, dag, stav, chuv, cheb, hant, neft, omsk, omskobl, ppp, magnit, surgut, irkobl, altay, hakas, zabay, novosib, tpu, gortomsk, tsu, tusur, tgasu, tuva, gzalt, amurobl, dvrt, afkast, tmk, evraz, rosles, rusnano, uzex, achi, vipast, retailast, neftast, exusex, postast, cbrfast, protek, dmtu, rencredit, berel, orpnz, dellin, vgtrk, aorti, kurgankhim, oilb2b, domrfast, enplusast, kamaz, rb2b"

class Builder(args: Array<String>) {
    lateinit var arg: Arguments
    lateinit var Database: String
    lateinit var Prefix: String
    lateinit var UserDb: String
    lateinit var PassDb: String
    lateinit var Server: String
    var Port: Int = 3306
    val executePath: String = File(Class.forName("parser.AppKt").protectionDomain.codeSource.location.path).parentFile.toString()
    lateinit var TempPath: String
    lateinit var LogPath: String
    lateinit var LogFile: String

    init {
        if (args.isEmpty()) {
            println("Недостаточно агрументов для запуска, используйте $arguments для запуска")
            System.exit(0)
        }
        when (args[0]) {
            "salavat" -> arg = Arguments.SALAVAT
            "umz" -> arg = Arguments.UMZ
            "lsr" -> arg = Arguments.LSR
            "zmokursk" -> arg = Arguments.ZMOKURSK
            "zmo45" -> arg = Arguments.ZMO45
            "zmokurgan" -> arg = Arguments.ZMOKURGAN
            "zmochel" -> arg = Arguments.ZMOCHEL
            "transast" -> arg = Arguments.TRANSAST
            "alrosa" -> arg = Arguments.ALROSA
            "ageat" -> arg = Arguments.AGEAT
            "rzn" -> arg = Arguments.RZN
            "brn" -> arg = Arguments.BRN
            "ivan" -> arg = Arguments.IVAN
            "orel" -> arg = Arguments.OREL
            "nov" -> arg = Arguments.NOV
            "komi" -> arg = Arguments.KOMI
            "kalin" -> arg = Arguments.KALIN
            "nen" -> arg = Arguments.NEN
            "yalta" -> arg = Arguments.YALTA
            "dag" -> arg = Arguments.DAG
            "stav" -> arg = Arguments.STAV
            "chuv" -> arg = Arguments.CHUV
            "cheb" -> arg = Arguments.CHEB
            "hant" -> arg = Arguments.HANT
            "neft" -> arg = Arguments.NEFT
            "omsk" -> arg = Arguments.OMSK
            "omskobl" -> arg = Arguments.OMSKOBL
            "ppp" -> arg = Arguments.PPP
            "magnit" -> arg = Arguments.MAGNIT
            "surgut" -> arg = Arguments.SURGUT
            "irkobl" -> arg = Arguments.IRKOBL
            "altay" -> arg = Arguments.ALTAY
            "hakas" -> arg = Arguments.HAKAS
            "zabay" -> arg = Arguments.ZABAY
            "novosib" -> arg = Arguments.NOVOSIB
            "tpu" -> arg = Arguments.TPU
            "gortomsk" -> arg = Arguments.GORTOMSK
            "tsu" -> arg = Arguments.TSU
            "tusur" -> arg = Arguments.TUSUR
            "tgasu" -> arg = Arguments.TGASU
            "tuva" -> arg = Arguments.TUVA
            "gzalt" -> arg = Arguments.GZALT
            "amurobl" -> arg = Arguments.AMUROBL
            "dvrt" -> arg = Arguments.DVRT
            "afkast" -> arg = Arguments.AFKAST
            "tmk" -> arg = Arguments.TMK
            "evraz" -> arg = Arguments.EVRAZ
            "rosles" -> arg = Arguments.ROSLES
            "rusnano" -> arg = Arguments.RUSNANO
            "uzex" -> arg = Arguments.UZEX
            "achi" -> arg = Arguments.ACHI
            "vipast" -> arg = Arguments.VIPAST
            "retailast" -> arg = Arguments.RETAILAST
            "neftast" -> arg = Arguments.NEFTAST
            "exusex" -> arg = Arguments.EXUSEX
            "postast" -> arg = Arguments.POSTAST
            "cbrfast" -> arg = Arguments.CBRFAST
            "protek" -> arg = Arguments.PROTEK
            "dmtu" -> arg = Arguments.DMTU
            "rencredit" -> arg = Arguments.RENCREDIT
            "orpnz" -> arg = Arguments.ORPNZ
            "berel" -> arg = Arguments.BEREL
            "dellin" -> arg = Arguments.DELLIN
            "vgtrk" -> arg = Arguments.VGTRK
            "aorti" -> arg = Arguments.AORTI
            "kurgankhim" -> arg = Arguments.KURGANKHIM
            "oilb2b" -> arg = Arguments.OILB2B
            "domrfast" -> arg = Arguments.DOMRFAST
            "enplusast" -> arg = Arguments.ENPLUSAST
            "kamaz" -> arg = Arguments.KAMAZ
            "rb2b" -> arg = Arguments.RB2B
            else -> run { println("Неверно указаны аргументы, используйте $arguments, выходим из программы"); exitProcess(0) }
        }
        setSettings()
        createDirs()
        createObj()
    }

    private fun setSettings() {
        val filename = executePath + File.separator + "settings.json"
        val gson = Gson()
        val reader = JsonReader(FileReader(filename))
        val doc = gson.fromJson<Settings>(reader, Settings::class.java)
        Database = doc.database ?: throw IllegalArgumentException("bad database")
        Prefix = doc.prefix ?: ""
        UserDb = doc.userdb ?: throw IllegalArgumentException("bad userdb")
        PassDb = doc.passdb ?: throw IllegalArgumentException("bad passdb")
        Server = doc.server ?: throw IllegalArgumentException("bad server")
        Port = doc.port ?: 3306
        TempPath = "$executePath${File.separator}tempdir_tenders_${arg.name.toLowerCase()}"
        LogPath = "$executePath${File.separator}logdir_tenders_${arg.name.toLowerCase()}"
    }

    private fun createDirs() {
        val tmp = File(TempPath)
        if (tmp.exists()) {
            tmp.delete()
            tmp.mkdir()
        } else {
            tmp.mkdir()
        }
        val log = File(LogPath)
        if (!log.exists()) {
            log.mkdir()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        LogFile = "$LogPath${File.separator}log_parsing_${arg}_${dateFormat.format(Date())}.log"
    }

    private fun createObj() {
        BuilderApp.arg = arg
        BuilderApp.Database = Database
        BuilderApp.PassDb = PassDb
        BuilderApp.UserDb = UserDb
        BuilderApp.Port = Port
        BuilderApp.Prefix = Prefix
        BuilderApp.Server = Server
        BuilderApp.LogPath = LogPath
        BuilderApp.TempPath = TempPath
        BuilderApp.LogFile = LogFile
        BuilderApp.UrlConnect = "jdbc:mysql://$Server:$Port/$Database?jdbcCompliantTruncation=false&useUnicode=true&characterEncoding=utf-8&useLegacyDatetimeCode=false&serverTimezone=Europe/Moscow&connectTimeout=30000&socketTimeout=30000"
    }
}

class Settings {
    var database: String? = null
    var prefix: String? = null
    var userdb: String? = null
    var passdb: String? = null
    var server: String? = null
    var port: Int? = null
}