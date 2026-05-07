package com.mckv.attendance.utils

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceIdentifier {

    // Standard Widevine UUID — identifies the hardware DRM chip
    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

    /**
     * Returns a single stable fingerprint string for this device.
     *
     * Layer 1 — Widevine DRM ID:
     *   Tied to the hardware security chip. Survives factory resets.
     *   Most reliable source. Requires NO permissions.
     *
     * Layer 2 — Android ID:
     *   Unique per (device, user account, app signing key).
     *   Resets on factory reset but good secondary source.
     *
     * Layer 3 — Build properties:
     *   Manufacturer, model, board, device name, hardware name.
     *   Never changes unless the ROM itself is replaced.
     *
     * All three are combined and SHA-256 hashed into one string.
     */
    fun getDeviceFingerprint(context: Context): String {
        val widevine = getWidevineId() ?: ""
        val androidId = getAndroidId(context)
        val buildStr  = getStableBuildString()

        val combined = "$widevine|$androidId|$buildStr"
        return sha256(combined)
    }

    fun getDeviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"
    fun getOsVersion(): String   = "Android ${Build.VERSION.RELEASE}"
    fun getSdkVersion(): Int     = Build.VERSION.SDK_INT

    // ── Private ──────────────────────────────────────────────────────────────

    private fun getWidevineId(): String? = try {
        val drm = MediaDrm(WIDEVINE_UUID)
        val id  = drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
        drm.close()
        id?.toHex()
    } catch (e: Exception) { null }   // Gracefully falls back to layers 2 & 3

    private fun getAndroidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    private fun getStableBuildString(): String =
        "${Build.BOARD}|${Build.BRAND}|${Build.DEVICE}|" +
                "${Build.HARDWARE}|${Build.MANUFACTURER}|${Build.MODEL}|${Build.PRODUCT}"

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}