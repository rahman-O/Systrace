package com.gis.systrace.core.common.collectors

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.UserManager
data class MdmSecurityData(
    val deviceOwnerStatus: String,
    val isDeviceOwner: Boolean = false,
    val isProfileOwner: Boolean = false,
    val deviceOwnerComponent: String? = null,
    val organizationName: String? = null,
    val isEncrypted: Boolean? = null,
    val encryptionStatus: String? = null,
    val screenLockType: String? = null,
    val isOemUnlocked: Boolean? = null,
    val googlePlayServicesVersion: String? = null,
    val isWorkProfile: Boolean = false,
)

object MdmSecurityCollector {

    fun collect(context: Context): MdmSecurityData {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        val isProfileOwner = dpm?.isProfileOwnerApp(context.packageName) == true

        val deviceOwnerComponent = dpm?.let { readDeviceOwnerComponent(it) }

        val deviceOwnerStatus = when {
            isDeviceOwner -> "DEVICE_OWNER"
            isProfileOwner -> "PROFILE_OWNER"
            deviceOwnerComponent != null -> "MANAGED_OTHER"
            else -> "NONE"
        }

        val encryptionStatus = dpm?.storageEncryptionStatus
        val encryptionStatusName = encryptionStatus?.let { status ->
            when (status) {
                DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "UNSUPPORTED"
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "INACTIVE"
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "ACTIVATING"
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "ACTIVE"
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "ACTIVE_DEFAULT_KEY"
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "ACTIVE_PER_USER"
                else -> "UNKNOWN_$status"
            }
        }

        val isEncrypted = encryptionStatus?.let {
            it == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                it == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ||
                it == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
        }

        val screenLockType = when {
            keyguard == null -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keyguard.isDeviceSecure -> "SECURE"
            keyguard.isKeyguardSecure -> "KEYGUARD_SECURE"
            else -> "NONE"
        }

        val verifiedBoot = SystemPropertiesHelper.get("ro.boot.verifiedbootstate")
        val flashLocked = SystemPropertiesHelper.get("ro.boot.flash.locked")
        val isOemUnlocked = when {
            flashLocked == "0" -> true
            flashLocked == "1" -> false
            verifiedBoot == "orange" -> true
            verifiedBoot == "green" -> false
            else -> null
        }

        val gmsVersion = try {
            val pm = context.packageManager
            val pkg = pm.getPackageInfo("com.google.android.gms", 0)
            pkg.versionName
        } catch (t: Throwable) {
            null
        }

        val isWorkProfile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            userManager?.isManagedProfile == true
        } else {
            false
        }

        return MdmSecurityData(
            deviceOwnerStatus = deviceOwnerStatus,
            isDeviceOwner = isDeviceOwner,
            isProfileOwner = isProfileOwner,
            deviceOwnerComponent = deviceOwnerComponent,
            organizationName = null,
            isEncrypted = isEncrypted,
            encryptionStatus = encryptionStatusName,
            screenLockType = screenLockType,
            isOemUnlocked = isOemUnlocked,
            googlePlayServicesVersion = gmsVersion,
            isWorkProfile = isWorkProfile,
        )
    }

    private fun readDeviceOwnerComponent(dpm: DevicePolicyManager): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return try {
            val method = dpm.javaClass.getMethod("getDeviceOwnerComponentOnAnyUser")
            val component = method.invoke(dpm) as? android.content.ComponentName
            component?.flattenToString()
        } catch (t: Throwable) {
            null
        }
    }
}
