package parser.logger

import java.io.FileWriter
import java.io.IOException
import java.util.*

fun <T : Any> logger(vararg parametrs: T) {
    val s = StringBuilder()
    s.append(Date())
    for (p in parametrs) {
        if (p is Array<*>) {
            for (n in p) {
                s.append(" $n\n")
            }
        } else {
            s.append(" $p\n")
        }
    }
    try {
        FileWriter(parser.builderApp.BuilderApp.LogFile, true).use { writer ->
            writer.write(s.toString())
        }
    } catch (ex: IOException) {
        println(ex.message)
    }
}
