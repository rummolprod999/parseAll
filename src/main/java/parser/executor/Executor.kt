package parser.executor

import parser.Arguments
import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.parsers.IParser
import parser.parsers.ParserSalavat

class Executor {
    init {
        when (BuilderApp.arg) {
            Arguments.SALAVAT -> run { val d = ParserSalavat(); executeParser(d) { parser() } }
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