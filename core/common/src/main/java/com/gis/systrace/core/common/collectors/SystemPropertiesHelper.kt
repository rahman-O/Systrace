package com.gis.systrace.core.common.collectors

object SystemPropertiesHelper {

    private val mdmPropertyKeys = listOf(
        "ro.serialno",
        "ro.boot.serialno",
        "ro.product.model",
        "ro.product.manufacturer",
        "ro.product.brand",
        "ro.product.device",
        "ro.product.name",
        "ro.build.display.id",
        "ro.build.version.release",
        "ro.build.version.security_patch",
        "ro.build.version.codename",
        "ro.build.description",
        "ro.build.type",
        "ro.build.tags",
        "ro.build.fingerprint",
        "ro.vendor.build.fingerprint",
        "ro.boot.verifiedbootstate",
        "ro.boot.flash.locked",
        "ro.boot.vbmeta.device_state",
        "ro.debuggable",
        "ro.secure",
        "ro.hardware",
        "ro.board.platform",
        "ro.product.cpu.abilist",
        "ro.bootloader",
        "ro.boot.mode",
        "gsm.version.baseband",
    )

    fun get(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            val value = method.invoke(null, key, "") as? String
            value?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        }
    }

    fun collectMdmProperties(): Map<String, String> {
        val props = linkedMapOf<String, String>()
        for (key in mdmPropertyKeys) {
            get(key)?.let { props[key] = it }
        }
        return props
    }
}
