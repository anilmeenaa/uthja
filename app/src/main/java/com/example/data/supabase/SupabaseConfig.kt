package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    val url: String = try {
        BuildConfig.SUPABASE_URL.trim()
    } catch (e: Throwable) {
        ""
    }

    val anonKey: String = try {
        BuildConfig.SUPABASE_ANON_KEY.trim()
    } catch (e: Throwable) {
        ""
    }

    val isConfigured: Boolean
        get() = url.isNotBlank() &&
                !url.contains("placeholder") &&
                anonKey.isNotBlank() &&
                !anonKey.contains("placeholder")
}
