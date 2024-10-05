package parser.networkTools

import parser.logger.logger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.*

private const val timeoutD = 3000L

var trustAllCerts: Array<TrustManager> =
    arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(
                certs: Array<X509Certificate?>?,
                authType: String?,
            ) {
            }

            override fun checkServerTrusted(
                certs: Array<X509Certificate?>?,
                authType: String?,
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate>? = null
        },
    )

fun downloadFromUrlNoSsl(
    urls: String,
    i: Int = 5,
    wt: Long = 3000,
): String {
    try {
        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
    } catch (e: Exception) {
    }
    var count = 0
    while (true) {
        // val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s
        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(wt)
        }
    }
    return ""
}

fun downloadFromUrlNoSslNew(
    urls: String,
    i: Int = 5,
    wt: Long = 3000,
): String {
    try {
        disableSslVerification()
        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
    } catch (e: Exception) {
    }
    var count = 0
    while (true) {
        // val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s
        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(wt)
        }
    }
    return ""
}

fun downloadFromUrl(
    urls: String,
    i: Int = 5,
    wt: Long = 3000,
): String {
    var count = 0
    while (true) {
        // val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
                if (s == "") {
                    throw Exception("Empty string")
                }
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s
        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(wt)
        }
    }
    return ""
}

fun downloadWait(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val `is`: InputStream = url.openStream()
    val br = BufferedReader(InputStreamReader(`is`))
    var inputLine: String?
    var value = true
    while (value) {
        inputLine = br.readLine()
        if (inputLine == null) {
            value = false
        } else {
            s.append(inputLine)
        }
    }
    br.close()
    `is`.close()
    return s.toString()
}

fun downloadWaitWithRef(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val uc = url.openConnection()
    uc.connectTimeout = 30_000
    uc.readTimeout = 60_000
    uc.addRequestProperty("User-Agent", RandomUserAgent.randomUserAgent)
    uc.connect()
    val `is`: InputStream = uc.getInputStream()
    val br = BufferedReader(InputStreamReader(`is`))
    var inputLine: String?
    var value = true
    while (value) {
        inputLine = br.readLine()
        if (inputLine == null) {
            value = false
        } else {
            s.append(inputLine)
        }
    }
    br.close()
    `is`.close()
    return s.toString()
}

fun downloadFromUrl1251(
    urls: String,
    i: Int = 5,
): String {
    var count = 0
    while (true) {
        // val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef1251(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s
        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(timeoutD)
        }
    }
    return ""
}

fun downloadWaitWithRef1251(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val uc = url.openConnection()
    uc.connectTimeout = 30_000
    uc.readTimeout = 600_000
    uc.addRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.1.6) Gecko/20070802 SeaMonkey/1.1.4",
    )
    uc.connect()
    val `is`: InputStream = uc.getInputStream()
    val br = BufferedReader(InputStreamReader(`is`, "windows-1251"))
    var inputLine: String?
    var value = true
    while (value) {
        inputLine = br.readLine()
        if (inputLine == null) {
            value = false
        } else {
            s.append(inputLine)
        }
    }
    br.close()
    `is`.close()
    return s.toString()
}

private fun disableSslVerification() {
    try {
        val trustAllCerts: Array<TrustManager> =
            arrayOf(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        certs: Array<X509Certificate?>?,
                        authType: String?,
                    ) {
                    }

                    override fun checkServerTrusted(
                        certs: Array<X509Certificate?>?,
                        authType: String?,
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        TODO("Not yet implemented")
                    }
                },
            )
        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        val allHostsValid: HostnameVerifier =
            object : HostnameVerifier {
                override fun verify(
                    hostname: String?,
                    session: SSLSession?,
                ): Boolean = true
            }
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    } catch (e: NoSuchAlgorithmException) {
        logger(e)
    } catch (e: KeyManagementException) {
        logger(e)
    }
}
