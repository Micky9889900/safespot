package com.safespot.scanner.scan

enum class Risk(val label: String) { SAFE("Safe"), LOW("Low"), MEDIUM("Medium"), HIGH("High"), CRITICAL("Threat") }

enum class Source(val label: String) { PLAY("Google Play"), STORE("Other app store"), SIDELOAD("Sideloaded / file"), UNKNOWN("Unknown"), SYSTEM("Pre-installed") }

data class Finding(val title: String, val detail: String, val points: Int)

data class AppReport(
    val pkg: String,
    val label: String,
    val versionName: String?,
    val system: Boolean,
    val installer: String?,
    val source: Source,
    val hasLauncher: Boolean,
    val firstInstall: Long,
    val lastUpdate: Long,
    val certSha256: List<String>,
    val certSha1: List<String>,
    val apkSha256: String?,
    val grantedPerms: List<String>,
    val requestedPerms: List<String>,
    val categories: Set<PermCategory>,
    val accessibilityOn: Boolean,
    val deviceAdmin: Boolean,
    val notifListener: Boolean,
    val iocMatch: String?,      // stalkerware family name
    val malwareMatch: String?,  // malware DB family / signature
    val findings: List<Finding>,
    val score: Int,
    val risk: Risk,
)

enum class PermCategory(val label: String) {
    LOCATION("Location"), CAMERA("Camera"), MIC("Microphone"), SMS("SMS"), CALLS("Calls / call log"),
    CONTACTS("Contacts"), STORAGE("Files / media"), ACCESSIBILITY("Accessibility"), NOTIFICATIONS("Notification access"),
    OVERLAY("Draw over apps"), ADMIN("Device admin"), INSTALL("Install other apps")
}

data class ScanResult(
    val time: Long,
    val deep: Boolean,
    val apps: List<AppReport>,
    val device: DeviceHealth,
    val score: Int,
    val iocVersion: String,
    val malwareDbVersion: String,
) {
    val threats get() = apps.filter { it.risk == Risk.CRITICAL }
    val risky get() = apps.filter { it.risk >= Risk.MEDIUM }
    val permsExposed get() = apps.sumOf { it.categories.size }
}
