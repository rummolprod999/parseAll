package parser.tools

import java.text.Format
import java.text.SimpleDateFormat

var formatter: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss")
var formatterGpn: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy kk:mm")
var formatterOnlyDate: Format = SimpleDateFormat("dd.MM.yyyy")
var formatterZakupkiDate: Format = SimpleDateFormat("yyyy-MM-dd")
var formatterZakupkiDateTime: Format = SimpleDateFormat("yyyy-MM-dd kk:mm:ss")
var formatterEtpRf: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss (XXX)")
var formatterEtpRfN: Format = SimpleDateFormat("dd.MM.yyyy kk:mm (XXX)")
var formatterAchi: SimpleDateFormat = SimpleDateFormat("d MM yyyy, k:mm")
var formatterBorets: SimpleDateFormat = SimpleDateFormat("d MM yyyy - kk:mm")
var formatterGns: SimpleDateFormat = SimpleDateFormat("d.MM.yyyy")
var formatterRs: Format = SimpleDateFormat("dd.MM.yy")
