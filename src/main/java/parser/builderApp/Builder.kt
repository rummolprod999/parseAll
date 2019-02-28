package parser.builderApp

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import parser.Arguments
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*


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

const val arguments = "salavat, umz, lsr, zmokursk, zmo45, zmokurgan, zmochel, transast, alrosa, ageat, rzn, brn, ivan, orel, nov, komi, kalin, nen, yalta, dag, stav, chuv, cheb, hant, neft, omsk, omskobl, ppp, magnit, surgut"

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
            else -> run { println("Неверно указаны аргументы, используйте $arguments, выходим из программы"); System.exit(0) }
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