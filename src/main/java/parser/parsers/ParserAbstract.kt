package parser.parsers

import parser.logger.logger
import parser.tenders.ITender
import parser.tenders.TenderAbstract

abstract class ParserAbstract {
    fun parse(fn: () -> Unit) {
        logger("Начало парсинга")
        fn()
        logger("Добавили тендеров ${TenderAbstract.AddTender}")
        logger("Обновили тендеров ${TenderAbstract.UpdateTender}")
        logger("Конец парсинга")
    }

    open fun ParserTender(t: ITender) {
        try {
            t.parsing()
        } catch (e: Exception) {
            logger("error in ${t::class.simpleName}.parsing()", e.stackTrace, e)
        }
    }
}
