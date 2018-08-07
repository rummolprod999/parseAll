package parser.parsers

import parser.logger.logger

class ParserSalavat : IParser, ParserAbstract() {
    private val hello = "hello"
    override fun parser() = parse { parserSalavat() }

    fun parserSalavat() {
        var tr = 0
        while (true) {
            try {
                parserSelen()
                break
            } catch (e: Exception) {
                tr++
                if (tr > 4) {
                    logger("Количество попыток истекло, выходим из программы")
                    break
                }
                logger("Error in parserUgmk function", e.stackTrace, e)
                e.printStackTrace()
            }
        }
    }

    fun parserSelen() {

    }
}