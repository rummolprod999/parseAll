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
            Arguments.ZMOCHEL -> run { p = ParserZmoChel(); executeParser(p) { parser() } }
            Arguments.TRANSAST -> run { p = ParserTransAst(); executeParser(p) { parser() } }
            Arguments.ALROSA -> run { p = ParserAlrosa(); executeParser(p) { parser() } }
            Arguments.AGEAT -> run { p = ParserAgEat(); executeParser(p) { parser() } }
            Arguments.RZN -> run { p = ParserRzn(); executeParser(p) { parser() } }
            Arguments.BRN -> run { p = ParserBrn(); executeParser(p) { parser() } }
            Arguments.IVAN -> run { p = ParserZmoIvan(); executeParser(p) { parser() } }
            Arguments.OREL -> run { p = ParserZmoOrel(); executeParser(p) { parser() } }
            Arguments.NOV -> run { p = ParserZmoNov(); executeParser(p) { parser() } }
            Arguments.KOMI -> run { p = ParserZmoKomi(); executeParser(p) { parser() } }
            Arguments.KALIN -> run { p = ParserZmoKalin(); executeParser(p) { parser() } }
            Arguments.NEN -> run { p = ParserZmoNen(); executeParser(p) { parser() } }
            Arguments.YALTA -> run { p = ParserZmoYalta(); executeParser(p) { parser() } }
            Arguments.DAG -> run { p = ParserZmoDag(); executeParser(p) { parser() } }
            Arguments.STAV -> run { p = UnParserZmo(156, "Закупки малого объема города Ставрополя", "https://stavzmo.rts-tender.ru/", "ставроп"); executeParser(p) { parser() } }
            Arguments.CHUV -> run { p = UnParserZmo(157, "Закупки малого объема Чувашской Республики", "https://zmo21.rts-tender.ru/", "чуваш"); executeParser(p) { parser() } }
            Arguments.CHEB -> run { p = UnParserZmo(158, "Электронный магазин города Чебоксары", "https://chebzmo.rts-tender.ru/", "чуваш"); executeParser(p) { parser() } }
            Arguments.HANT -> run { p = UnParserZmo(159, "Электронный магазин Ханты-мансийского автономного округа", "https://ozhmao-zmo.rts-tender.ru/", "ханты"); executeParser(p) { parser() } }
            Arguments.NEFT -> run { p = UnParserZmo(160, "Закупки малого объема администрации города Нефтеюганска", "https://uganskzmo.rts-tender.ru/", "ханты"); executeParser(p) { parser() } }
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