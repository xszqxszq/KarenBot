package xyz.xszq.bot.util

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * 信任所有证书的证书管理器，用于转发时绕过证书检查
 */
class AllTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}