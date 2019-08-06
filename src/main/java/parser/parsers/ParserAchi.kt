package parser.parsers

import parser.logger.logger

class ParserAchi : IParser, ParserAbstract() {
    companion object WebCl {
        val BaseUrl = arrayOf("https://achizitii.md/ru/public/tender/list?page=")
        const val CountPage = 50
    }

    override fun parser() = parse {
        try {
            parserAchi()
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName} function", e.stackTrace, e)
        }
    }

    private fun parserAchi() {

    }
}