package parser.executor

import parser.Arguments
import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.parsers.*

class Executor {
    lateinit var p: IParser

    init {
        when (BuilderApp.arg) {
            Arguments.SALAVAT -> run { p = ParserSalavat(); executeParser(p) { parser() } }
            Arguments.UMZ -> run { p = ParserUmz(); executeParser(p) { parser() } }
            Arguments.LSR -> run { p = ParserLsr(); executeParser(p) { parser() } }
            Arguments.ZMOKURSK -> run { p = ParserZmoKursk(); executeParser(p) { parser() } }
            Arguments.ZMO45 -> run { p = ParserZmo45(); executeParser(p) { parser() } }
            Arguments.ZMOKURGAN -> run { p = ParserZmoKurgan(); executeParser(p) { parser() } }
        }
    }

    private fun executeParser(d: IParser, fn: IParser.() -> Unit) {
        try {
            d.fn()
        } catch (e: Exception) {
            logger("error in executor fun", e.stackTrace, e)
        }

    }
}