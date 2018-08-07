package parser.parsers

class ParserSalavat : IParser, ParserAbstract() {
    private val hello = "hello"
    override fun parser() = Parse { parserSalavat() }

    fun parserSalavat(){

    }
}