package com.safespot.scanner.scan

/** Plain-language explanation and category for the permissions that matter for spying. */
object Permissions {
    data class Info(val cat: PermCategory?, val explain: String, val weight: Int)
    val map: Map<String, Info> = mapOf(
        "android.permission.ACCESS_FINE_LOCATION" to Info(PermCategory.LOCATION, "Exact GPS location", 6),
        "android.permission.ACCESS_COARSE_LOCATION" to Info(PermCategory.LOCATION, "Approximate location", 3),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to Info(PermCategory.LOCATION, "Location even when the app is closed", 15),
        "android.permission.CAMERA" to Info(PermCategory.CAMERA, "Take photos and video", 5),
        "android.permission.RECORD_AUDIO" to Info(PermCategory.MIC, "Record audio with the microphone", 10),
        "android.permission.READ_SMS" to Info(PermCategory.SMS, "Read your text messages", 15),
        "android.permission.RECEIVE_SMS" to Info(PermCategory.SMS, "Receive / intercept SMS (incl. 2FA codes)", 10),
        "android.permission.SEND_SMS" to Info(PermCategory.SMS, "Send SMS (can cost money)", 10),
        "android.permission.READ_CALL_LOG" to Info(PermCategory.CALLS, "Read who you called and when", 15),
        "android.permission.PROCESS_OUTGOING_CALLS" to Info(PermCategory.CALLS, "See outgoing calls", 8),
        "android.permission.READ_PHONE_STATE" to Info(PermCategory.CALLS, "Phone number and call state", 3),
        "android.permission.ANSWER_PHONE_CALLS" to Info(PermCategory.CALLS, "Answer calls", 5),
        "android.permission.READ_CONTACTS" to Info(PermCategory.CONTACTS, "Read your contacts", 5),
        "android.permission.READ_EXTERNAL_STORAGE" to Info(PermCategory.STORAGE, "Read files and photos", 3),
        "android.permission.READ_MEDIA_IMAGES" to Info(PermCategory.STORAGE, "Read photos", 3),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to Info(PermCategory.STORAGE, "Full access to all files", 8),
        "android.permission.SYSTEM_ALERT_WINDOW" to Info(PermCategory.OVERLAY, "Draw over other apps (can fake screens)", 10),
        "android.permission.REQUEST_INSTALL_PACKAGES" to Info(PermCategory.INSTALL, "Install other apps", 8),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to Info(PermCategory.ACCESSIBILITY, "Offers an accessibility service (can read the screen)", 5),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to Info(PermCategory.NOTIFICATIONS, "Offers a notification reader", 3),
        "android.permission.BIND_DEVICE_ADMIN" to Info(PermCategory.ADMIN, "Offers device admin", 3),
        "android.permission.RECEIVE_BOOT_COMPLETED" to Info(null, "Starts automatically at boot", 0),
    )
}
