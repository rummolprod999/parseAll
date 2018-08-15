package parser.executor

import parser.Arguments
import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.parsers.IParser
import parser.parsers.ParserSalavat
import parser.parsers.ParserUmz

class Executor {
    lateinit var p: IParser
    init {
        when (BuilderApp.arg) {
            Arguments.SALAVAT -> run { p = ParserSalavat(); executeParser(p) { parser() } }
            Arguments.UMZ -> run { p = ParserUmz(); executeParser(p) { parser() } }
        }
    }

    fun executeParser(d: IParser, fn: IParser.() -> Unit) {
        try {
            d.fn()
        } catch (e: Exception) {
            logger("error in executor fun", e.stackTrace, e)
        }

    }
}