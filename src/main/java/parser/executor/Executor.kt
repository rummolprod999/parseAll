package parser.executor

import ParserProtek
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
            Arguments.SURGUT -> run { p = UnParserZmo(161, "ЗАКУПКИ МАЛОГО ОБЪЁМА СУРГУТСКОГО РАЙОНА", "https://admsr-zmo.rts-tender.ru/", "ханты"); executeParser(p) { parser() } }
            Arguments.MAGNIT -> run { p = UnParserZmo(162, "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА МАГНИТОГОРСКА", "https://magnitogorskmarket.rts-tender.ru/", "челяб"); executeParser(p) { parser() } }
            Arguments.PPP -> run { p = UnParserZmo(163, "ЭЛЕКТРОННЫЙ МАГАЗИН ФГУП «ППП»", "https://pppmarket.rts-tender.ru/", ""); executeParser(p) { parser() } }
            Arguments.OMSK -> run { p = UnParserZmo(164, "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА ОМСКА", "https://zmo-omsk.rts-tender.ru/", "омск"); executeParser(p) { parser() } }
            Arguments.OMSKOBL -> run { p = UnParserZmo(165, "ЭЛЕКТРОННЫЙ МАГАЗИН ОМСКОЙ ОБЛАСТИ", "https://zmo-omskobl.rts-tender.ru/", "омск"); executeParser(p) { parser() } }
            Arguments.IRKOBL -> run { p = UnParserZmo(166, "ЭЛЕКТРОННЫЙ МАГАЗИН ИРКУТСКОЙ ОБЛАСТИ ДЛЯ ЗАКУПОК МАЛОГО ОБЪЕМА (РТС-МАРКЕТ)", "https://irkoblmarket.rts-tender.ru/", "иркут"); executeParser(p) { parser() } }
            Arguments.ALTAY -> run { p = UnParserZmo(167, "ЭЛЕКТРОННЫЙ МАГАЗИН МИНИСТЕРСТВА ЭКОНОМИЧЕСКОГО РАЗВИТИЯ И ТУРИЗМА РЕСПУБЛИКИ АЛТАЙ", "https://zmo04.rts-tender.ru/", "алтай"); executeParser(p) { parser() } }
            Arguments.HAKAS -> run { p = UnParserZmo(168, "ЭЛЕКТРОННЫЙ МАРКЕТ ГОСУДАРСТВЕННОГО КОМИТЕТА ПО РЕГУЛИРОВАНИЮ КОНТРАКТНОЙ СИСТЕМЫ В СФЕРЕ ЗАКУПОК РЕСПУБЛИКИ ХАКАСИЯ", "https://zmo19.rts-tender.ru/", "хакас"); executeParser(p) { parser() } }
            Arguments.ZABAY -> run { p = UnParserZmo(169, "ЭЛЕКТРОННЫЙ МАГАЗИН ЗАБАЙКАЛЬСКОГО КРАЯ", "https://zmo-zab.rts-tender.ru/", "забайк"); executeParser(p) { parser() } }
            Arguments.NOVOSIB -> run { p = UnParserZmo(170, "ЭЛЕКТРОННЫЙ МАГАЗИН НОВОСИБИРСКОЙ ОБЛАСТИ", "https://novobl-zmo.rts-tender.ru/", "новосиб"); executeParser(p) { parser() } }
            Arguments.TPU -> run { p = UnParserZmo(171, "ЗАКУПКИ У ЕДИНСТВЕННОГО ПОСТАВЩИКА (ПОДРЯДЧИКА , ИСПОЛНИТЕЛЯ). ЭЛЕКТРОННЫЙ МАГАЗИН ТПУ", "https://tpu.rts-tender.ru/", "томск"); executeParser(p) { parser() } }
            Arguments.GORTOMSK -> run { p = UnParserZmo(172, "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА ТОМСКА", "https://tomsk.rts-tender.ru/", "томск"); executeParser(p) { parser() } }
            Arguments.TSU -> run { p = UnParserZmo(173, "ЗАКУПКИ МАЛОГО ОБЪЕМА ТГУ", "https://tsu.rts-tender.ru/", "томск"); executeParser(p) { parser() } }
            Arguments.TUSUR -> run { p = UnParserZmo(174, "ЗАКУПКИ МАЛОГО ОБЪЕМА ТУСУР", "https://tusur.rts-tender.ru/", "томск"); executeParser(p) { parser() } }
            Arguments.TGASU -> run { p = UnParserZmo(175, "ЗАКУПКИ МАЛОГО ОБЪЕМА ТГАСУ", "https://tgasu.rts-tender.ru/", "томск"); executeParser(p) { parser() } }
            Arguments.TUVA -> run { p = UnParserZmo(176, "ЗАКУПКИ МАЛОГО ОБЪЕМА РЕСПУБЛИКИ ТЫВА", "https://tuva-zmo.rts-tender.ru/", "тыва"); executeParser(p) { parser() } }
            Arguments.GZALT -> run { p = UnParserZmo(177, "ПОРТАЛ ПОСТАВЩИКОВ АЛТАЙСКОГО КРАЯ", "https://gzalt.rts-tender.ru/", "алтайск"); executeParser(p) { parser() } }
            Arguments.AMUROBL -> run { p = UnParserZmo(178, "ЭЛЕКТРОННЫЙ МАГАЗИН АМУРСКОЙ ОБЛАСТИ", "https://zmo-amurobl.rts-tender.ru/", "амурск"); executeParser(p) { parser() } }
            Arguments.DVRT -> run {
                p = UnParserZmo(179, "ЗАКУПКИ МАЛОГО ОБЪЕМА МАКРОРЕГИОНАЛЬНОГО ФИЛИАЛА \"ДАЛЬНИЙ ВОСТОК\" ПАО \"РОСТЕЛЕКОМ\"", "https://zmodvrt.rts-tender.ru/", ""); executeParser(p) { parser() }
            }
            Arguments.AFKAST -> run { p = ParserAfkAst(); executeParser(p) { parser() } }
            Arguments.TMK -> run { p = ParserTmk(); executeParser(p) { parser() } }
            Arguments.EVRAZ -> run { p = ParserEvraz(); executeParser(p) { parser() } }
            Arguments.ROSLES -> run {
                p = UnParserZmo(192, "МАГАЗИН ЗАКУПОК МАЛОГО ОБЪЕМА РОСЛЕСИНФОРГ", "https://roslesinforg-market.rts-tender.ru/", ""); executeParser(p) { parser() }
            }
            Arguments.RUSNANO -> run { p = ParserRusNano(); executeParser(p) { parser() } }
            Arguments.UZEX -> run { p = ParserUzex(); executeParser(p) { parser() } }
            Arguments.ACHI -> run { p = ParserAchi(); executeParser(p) { parser() } }
            Arguments.VIPAST -> run { p = ParserVipAst(); executeParser(p) { parser() } }
            Arguments.RETAILAST -> run { p = ParserRetailAst(); executeParser(p) { parser() } }
            Arguments.NEFTAST -> run { p = ParserNeftAst(); executeParser(p) { parser() } }
            Arguments.EXUSEX -> run { p = ParserExUzex(); executeParser(p) { parser() } }
            Arguments.POSTAST -> run { p = ParserRussianPostAst(); executeParser(p) { parser() } }
            Arguments.CBRFAST -> run { p = ParserCbrfAst(); executeParser(p) { parser() } }
            Arguments.PROTEK -> run { p = ParserProtek(); executeParser(p) { parser() } }
            Arguments.DMTU -> run { p = ParserDmtu(); executeParser(p) { parser() } }
            Arguments.RENCREDIT -> run { p = ParserRenCredit(); executeParser(p) { parser() } }
            Arguments.ORPNZ -> run { p = ParserOrPnz(); executeParser(p) { parser() } }
            Arguments.BEREL -> run { p = ParserBerel(); executeParser(p) { parser() } }
            Arguments.DELLIN -> run { p = ParserDellin(); executeParser(p) { parser() } }
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