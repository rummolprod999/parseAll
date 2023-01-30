@file:Suppress("ConvertLambdaToReference")

package parser.executor

import ParserProtek
import parser.Arguments
import parser.builderApp.BuilderApp
import parser.logger.logger
import parser.parsers.*

@Suppress("ConvertLambdaToReference")
class Executor {
    lateinit var p: IParser

    init {
        when (BuilderApp.arg) {
            Arguments.SALAVAT ->
                run {
                    p = ParserSalavat()
                    executeParser(p, IParser::parser)
                }
            Arguments.UMZ -> {
                run {
                    p = ParserUmz()
                    executeParser(p, IParser::parser)
                }
                run {
                    p = ParserUmzMark()
                    executeParser(p, IParser::parser)
                }
            }
            Arguments.LSR ->
                run {
                    p = ParserLsr()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZMOKURSK ->
                run {
                    p = ParserZmoKursk()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZMO45 ->
                run {
                    p = ParserZmo45()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZMOKURGAN ->
                run {
                    p = ParserZmoKurgan()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZMOCHEL ->
                run {
                    p = ParserZmoChel()
                    executeParser(p, IParser::parser)
                }
            Arguments.TRANSAST ->
                run {
                    p = ParserTransAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.ALROSA ->
                run {
                    p = ParserAlrosa()
                    executeParser(p, IParser::parser)
                }
            Arguments.AGEAT ->
                run {
                    p = ParserAgEat()
                    executeParser(p, IParser::parser)
                }
            Arguments.RZN ->
                run {
                    p = ParserRzn()
                    executeParser(p, IParser::parser)
                }
            Arguments.BRN ->
                run {
                    p = ParserBrn()
                    executeParser(p, IParser::parser)
                }
            Arguments.IVAN ->
                run {
                    p = ParserZmoIvan()
                    executeParser(p, IParser::parser)
                }
            Arguments.OREL ->
                run {
                    p = ParserZmoOrel()
                    executeParser(p, IParser::parser)
                }
            Arguments.NOV ->
                run {
                    p = ParserZmoNov()
                    executeParser(p, IParser::parser)
                }
            Arguments.KOMI ->
                run {
                    p = ParserZmoKomi()
                    executeParser(p, IParser::parser)
                }
            Arguments.KALIN ->
                run {
                    p = ParserZmoKalin()
                    executeParser(p, IParser::parser)
                }
            Arguments.NEN ->
                run {
                    p = ParserZmoNen()
                    executeParser(p, IParser::parser)
                }
            Arguments.YALTA ->
                run {
                    p = ParserZmoYalta()
                    executeParser(p, IParser::parser)
                }
            Arguments.DAG ->
                run {
                    p = ParserZmoDag()
                    executeParser(p, IParser::parser)
                }
            Arguments.STAV ->
                run {
                    p =
                        UnParserZmo(
                            156,
                            "Закупки малого объема города Ставрополя",
                            "https://stavzmo.rts-tender.ru/",
                            "ставроп"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.CHUV ->
                run {
                    p =
                        UnParserZmo(
                            157,
                            "Закупки малого объема Чувашской Республики",
                            "https://zmo21.rts-tender.ru/",
                            "чуваш"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.CHEB ->
                run {
                    p =
                        UnParserZmo(
                            158,
                            "Электронный магазин города Чебоксары",
                            "https://chebzmo.rts-tender.ru/",
                            "чуваш"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.HANT ->
                run {
                    p =
                        UnParserZmo(
                            159,
                            "Электронный магазин Ханты-мансийского автономного округа",
                            "https://ozhmao-zmo.rts-tender.ru/",
                            "ханты"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.NEFT ->
                run {
                    p =
                        UnParserZmo(
                            160,
                            "Закупки малого объема администрации города Нефтеюганска",
                            "https://uganskzmo.rts-tender.ru/",
                            "ханты"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.SURGUT ->
                run {
                    p =
                        UnParserZmo(
                            161,
                            "ЗАКУПКИ МАЛОГО ОБЪЁМА СУРГУТСКОГО РАЙОНА",
                            "https://admsr-zmo.rts-tender.ru/",
                            "ханты"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.MAGNIT ->
                run {
                    p =
                        UnParserZmo(
                            162,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА МАГНИТОГОРСКА",
                            "https://magnitogorskmarket.rts-tender.ru/",
                            "челяб"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.PPP ->
                run {
                    p =
                        UnParserZmo(
                            163,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ФГУП «ППП»",
                            "https://pppmarket.rts-tender.ru/",
                            ""
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.OMSK ->
                run {
                    p =
                        UnParserZmo(
                            164,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА ОМСКА",
                            "https://zmo-omsk.rts-tender.ru/",
                            "омск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.OMSKOBL ->
                run {
                    p =
                        UnParserZmo(
                            165,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ОМСКОЙ ОБЛАСТИ",
                            "https://zmo-omskobl.rts-tender.ru/",
                            "омск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.IRKOBL ->
                run {
                    p =
                        UnParserZmo(
                            166,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ИРКУТСКОЙ ОБЛАСТИ ДЛЯ ЗАКУПОК МАЛОГО ОБЪЕМА (РТС-МАРКЕТ)",
                            "https://irkoblmarket.rts-tender.ru/",
                            "иркут"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.ALTAY ->
                run {
                    p =
                        UnParserZmo(
                            167,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН МИНИСТЕРСТВА ЭКОНОМИЧЕСКОГО РАЗВИТИЯ И ТУРИЗМА РЕСПУБЛИКИ АЛТАЙ",
                            "https://zmo04.rts-tender.ru/",
                            "алтай"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.HAKAS ->
                run {
                    p =
                        UnParserZmo(
                            168,
                            "ЭЛЕКТРОННЫЙ МАРКЕТ ГОСУДАРСТВЕННОГО КОМИТЕТА ПО РЕГУЛИРОВАНИЮ КОНТРАКТНОЙ СИСТЕМЫ В СФЕРЕ ЗАКУПОК РЕСПУБЛИКИ ХАКАСИЯ",
                            "https://zmo19.rts-tender.ru/",
                            "хакас"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.ZABAY ->
                run {
                    p =
                        UnParserZmo(
                            169,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ЗАБАЙКАЛЬСКОГО КРАЯ",
                            "https://zmo-zab.rts-tender.ru/",
                            "забайк"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.NOVOSIB ->
                run {
                    p =
                        UnParserZmo(
                            170,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН НОВОСИБИРСКОЙ ОБЛАСТИ",
                            "https://novobl-zmo.rts-tender.ru/",
                            "новосиб"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.TPU ->
                run {
                    p =
                        UnParserZmo(
                            171,
                            "ЗАКУПКИ У ЕДИНСТВЕННОГО ПОСТАВЩИКА (ПОДРЯДЧИКА , ИСПОЛНИТЕЛЯ). ЭЛЕКТРОННЫЙ МАГАЗИН ТПУ",
                            "https://tpu.rts-tender.ru/",
                            "томск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.GORTOMSK ->
                run {
                    p =
                        UnParserZmo(
                            172,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН ГОРОДА ТОМСКА",
                            "https://tomsk.rts-tender.ru/",
                            "томск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.TSU ->
                run {
                    p =
                        UnParserZmo(
                            173,
                            "ЗАКУПКИ МАЛОГО ОБЪЕМА ТГУ",
                            "https://tsu.rts-tender.ru/",
                            "томск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.TUSUR ->
                run {
                    p =
                        UnParserZmo(
                            174,
                            "ЗАКУПКИ МАЛОГО ОБЪЕМА ТУСУР",
                            "https://tusur.rts-tender.ru/",
                            "томск"
                        )
                    executeParser(p) { parser() }
                }
            Arguments.TGASU ->
                run {
                    p =
                        UnParserZmo(
                            175,
                            "ЗАКУПКИ МАЛОГО ОБЪЕМА ТГАСУ",
                            "https://tgasu.rts-tender.ru/",
                            "томск"
                        )
                    executeParser(p) { parser() }
                }
            Arguments.TUVA ->
                run {
                    p =
                        UnParserZmo(
                            176,
                            "ЗАКУПКИ МАЛОГО ОБЪЕМА РЕСПУБЛИКИ ТЫВА",
                            "https://tuva-zmo.rts-tender.ru/",
                            "тыва"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.GZALT ->
                run {
                    p =
                        UnParserZmo(
                            177,
                            "ПОРТАЛ ПОСТАВЩИКОВ АЛТАЙСКОГО КРАЯ",
                            "https://gzalt.rts-tender.ru/",
                            "алтайск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.AMUROBL ->
                run {
                    p =
                        UnParserZmo(
                            178,
                            "ЭЛЕКТРОННЫЙ МАГАЗИН АМУРСКОЙ ОБЛАСТИ",
                            "https://zmo-amurobl.rts-tender.ru/",
                            "амурск"
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.DVRT ->
                run {
                    p =
                        UnParserZmo(
                            179,
                            "ЗАКУПКИ МАЛОГО ОБЪЕМА МАКРОРЕГИОНАЛЬНОГО ФИЛИАЛА \"ДАЛЬНИЙ ВОСТОК\" ПАО \"РОСТЕЛЕКОМ\"",
                            "https://zmodvrt.rts-tender.ru/",
                            ""
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.AFKAST ->
                run {
                    p = ParserAfkAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.TMK ->
                run {
                    p = ParserTmk()
                    executeParser(p, IParser::parser)
                }
            Arguments.EVRAZ ->
                run {
                    p = ParserEvraz()
                    executeParser(p, IParser::parser)
                }
            Arguments.ROSLES ->
                run {
                    p =
                        UnParserZmo(
                            192,
                            "МАГАЗИН ЗАКУПОК МАЛОГО ОБЪЕМА РОСЛЕСИНФОРГ",
                            "https://roslesinforg-market.rts-tender.ru/",
                            ""
                        )
                    executeParser(p, IParser::parser)
                }
            Arguments.RUSNANO ->
                run {
                    p = ParserRusNano()
                    executeParser(p, IParser::parser)
                }
            Arguments.UZEX ->
                run {
                    p = ParserUzex()
                    executeParser(p, IParser::parser)
                }
            Arguments.ACHI ->
                run {
                    p = ParserAchi()
                    executeParser(p, IParser::parser)
                }
            Arguments.VIPAST ->
                run {
                    p = ParserVipAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.RETAILAST ->
                run {
                    p = ParserRetailAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.NEFTAST ->
                run {
                    p = ParserNeftAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.EXUSEX ->
                run {
                    p = ParserExUzex()
                    executeParser(p, IParser::parser)
                }
            Arguments.POSTAST ->
                run {
                    p = ParserRussianPostAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.CBRFAST ->
                run {
                    p = ParserCbrfAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.PROTEK ->
                run {
                    p = ParserProtek()
                    executeParser(p, IParser::parser)
                }
            Arguments.DMTU ->
                run {
                    p = ParserDmtu()
                    executeParser(p, IParser::parser)
                }
            Arguments.RENCREDIT ->
                run {
                    p = ParserRenCredit()
                    executeParser(p, IParser::parser)
                }
            Arguments.ORPNZ ->
                run {
                    p = ParserOrPnz()
                    executeParser(p, IParser::parser)
                }
            Arguments.BEREL ->
                run {
                    p = ParserBerel()
                    executeParser(p, IParser::parser)
                }
            Arguments.DELLIN ->
                run {
                    p = ParserDellin()
                    executeParser(p, IParser::parser)
                }
            Arguments.VGTRK ->
                run {
                    p = ParserVgtrk()
                    executeParser(p, IParser::parser)
                }
            Arguments.AORTI ->
                run {
                    p = ParserAorti()
                    executeParser(p, IParser::parser)
                }
            Arguments.KURGANKHIM ->
                run {
                    p = ParserKurganKhim()
                    executeParser(p, IParser::parser)
                }
            Arguments.OILB2B ->
                run {
                    p = ParserOilb2b()
                    executeParser(p, IParser::parser)
                }
            Arguments.DOMRFAST ->
                run {
                    p = ParserDomRfAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.ENPLUSAST ->
                run {
                    p = ParserEnPlusAst()
                    executeParser(p, IParser::parser)
                }
            Arguments.KAMAZ ->
                run {
                    p = ParserKamaz()
                    executeParser(p, IParser::parser)
                }
            Arguments.RB2B ->
                run {
                    p = ParserRb2B()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZAKAZRF ->
                run {
                    p = ParserZakazRf()
                    executeParser(p, IParser::parser)
                }
            Arguments.BIDBE ->
                run {
                    p = ParserBidBe()
                    executeParser(p, IParser::parser)
                }
            Arguments.SPNOVA ->
                run {
                    p = ParserSpnova()
                    executeParser(p, IParser::parser)
                }
            Arguments.VPROM ->
                run {
                    p = ParserVprom()
                    executeParser(p, IParser::parser)
                }
            Arguments.AOMSZ ->
                run {
                    p = ParserAomsz()
                    executeParser(p, IParser::parser)
                }
            Arguments.FPK ->
                run {
                    p = ParserFpk()
                    executeParser(p, IParser::parser)
                }
            Arguments.BORETS ->
                run {
                    p = ParserBorets()
                    executeParser(p, IParser::parser)
                }
            Arguments.TKNSO ->
                run {
                    p = ParserTknso()
                    executeParser(p, IParser::parser)
                }
            Arguments.GNS ->
                run {
                    p = ParserGns()
                    executeParser(p, IParser::parser)
                }
            Arguments.DSK1 ->
                run {
                    p = ParserDsk1()
                    executeParser(p, IParser::parser)
                }
            Arguments.CDS ->
                run {
                    p = ParserCds()
                    executeParser(p, IParser::parser)
                }
            Arguments.STROYSERV ->
                run {
                    p = ParserStroyServ()
                    executeParser(p, IParser::parser)
                }
            Arguments.MOLSKAZ ->
                run {
                    p = ParserMolskaz()
                    executeParser(p, IParser::parser)
                }
            Arguments.AKBARS ->
                run {
                    p = ParserAkbars()
                    executeParser(p, IParser::parser)
                }
            Arguments.SNM ->
                run {
                    p = ParserSnm()
                    executeParser(p, IParser::parser)
                }
            Arguments.MMKCOAL ->
                run {
                    p = ParserMmkCoal()
                    executeParser(p, IParser::parser)
                }
            Arguments.PRNEFT ->
                run {
                    p = ParserPrNeft()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZAKAZRFEX ->
                run {
                    p = ParserZakazRfEx()
                    executeParser(p, IParser::parser)
                }
            Arguments.ZAKAZRFUDMURT ->
                run {
                    p = ParserZakazRfUdmurt()
                    executeParser(p, IParser::parser)
                }
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
