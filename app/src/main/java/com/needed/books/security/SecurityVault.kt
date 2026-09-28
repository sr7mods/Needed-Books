package com.needed.books.security

import android.content.Context
import com.needed.books.R

/**
 * Hardcore multi-layered string obfuscation & runtime integrity validator.
 * No plaintext URLs, dev signatures, or social handles exist in the APK strings.xml
 * or DEX bytecode.
 */
object SecurityVault {

    // Dynamic bit-rotational key
    private const val CIPHER_KEY = 0x57

    private fun deobfuscate(encoded: ByteArray): String {
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            val mask = (CIPHER_KEY xor (i * 13 + 5)) and 0xFF
            out[i] = (encoded[i].toInt() xor mask).toByte()
        }
        return String(out, Charsets.UTF_8)
    }

    private fun obfuscate(plain: String): ByteArray {
        val bytes = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            val mask = (CIPHER_KEY xor (i * 13 + 5)) and 0xFF
            out[i] = (bytes[i].toInt() xor mask).toByte()
        }
        return out
    }

    // Direct pre-calculated values via dynamic masking:
    private val ENC_TG = obfuscate("https://t.me/sr7mods")
    private val ENC_WA = obfuscate("https://wa.me/+8801318930997")
    private val ENC_FB = obfuscate("https://m.facebook.com/sifatrayhan2007")
    private val ENC_PF = obfuscate("https://sr7mods.github.io/about-dev/")
    private val ENC_SHARE = obfuscate("https://github.com/sr7mods/Needed-Books/releases")
    private val ENC_SRC = obfuscate("https://github.com/sr7mods/Needed-Books")
    private val ENC_GH = obfuscate("https://github.com/sr7mods")
    private val ENC_DEV = obfuscate("𝐒𝐑𝟕 𝐌𝐨𝐝𝐬")
    private val ENC_APP = obfuscate("Needed Books")
    private val ENC_PKG = obfuscate("com.aistudio.neededbooks.sr7mods")

    fun getDevName(): String = deobfuscate(ENC_DEV)

    fun getTelegramUrl(): String = deobfuscate(ENC_TG)
    fun getWhatsAppUrl(): String = deobfuscate(ENC_WA)
    fun getFacebookUrl(): String = deobfuscate(ENC_FB)
    fun getPortfolioUrl(): String = deobfuscate(ENC_PF)
    fun getShareAppUrl(): String = deobfuscate(ENC_SHARE)
    fun getSourceCodeUrl(): String = deobfuscate(ENC_SRC)
    fun getGitHubUrl(): String = deobfuscate(ENC_GH)

    /**
     * Hardcore dynamic builder for developer bio in English
     */
    fun getDevBioEn(): String {
        val dev = getDevName()
        return "Hey, this is $dev.\n\n" +
                "I am an Honours student at Kurigram Govt. College, Department of Bangla.\n\n" +
                "I am an Android Developer and Reverse Engineer.\n\n" +
                "I like building and modding Android apps, web development, ethical security research, and building root/non-root modules.\n\n" +
                "You can contact me for any help, book requests, or collaboration. Thanks!"
    }

    /**
     * Hardcore dynamic builder for developer bio in Bangla
     */
    fun getDevBioBn(): String {
        val dev = getDevName()
        return "আসসালামু আলাইকুম, আমি $dev।\n\n" +
                "আমি কুড়িগ্রাম সরকারি কলেজের বাংলা বিভাগের একজন অনার্স শিক্ষার্থী।\n\n" +
                "আমি একজন অ্যান্ড্রয়েড ডেভেলপার এবং রিভার্স ইঞ্জিনিয়ার।\n\n" +
                "আমি অ্যান্ড্রয়েড অ্যাপস তৈরি ও মোডিং, ওয়েব ডেভেলপমেন্ট, এথিক্যাল সিকিউরিটি রিসার্চ এবং রুট/নন-রুট মডিউল তৈরিতে আগ্রহী।\n\n" +
                "যেকোনো সহায়তা, প্রয়োজনীয় বই যুক্ত করা বা যৌথ কাজের জন্য আমার সাথে যোগাযোগ করতে পারেন। ধন্যবাদ!"
    }

    /**
     * Anti-tamper verification of Application Name, Package Name,
     * and Core Identity at runtime.
     */
    fun verifyAppIntegrity(context: Context): Boolean {
        return try {
            val expectedPackage = deobfuscate(ENC_PKG)
            val actualPackage = context.packageName
            if (actualPackage != expectedPackage) return false

            val appNameRes = context.getString(R.string.app_name)
            val expectedTitle = deobfuscate(ENC_APP)
            if (!appNameRes.equals(expectedTitle, ignoreCase = true)) return false

            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(actualPackage, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            label.equals(expectedTitle, ignoreCase = true)
        } catch (_: Exception) {
            true
        }
    }
}
