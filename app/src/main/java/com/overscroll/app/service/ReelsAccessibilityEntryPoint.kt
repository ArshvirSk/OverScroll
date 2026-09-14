package com.overscroll.app.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for the AccessibilityService.
 *
 * AccessibilityService is system-managed and cannot use @AndroidEntryPoint.
 * Instead, the service accesses Hilt dependencies through this EntryPoint
 * via EntryPointAccessors.fromApplication().
 *
 * See: https://dagger.dev/hilt/entry-points
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReelsAccessibilityEntryPoint {
    fun scrollCountRepository(): ScrollCountRepository
    fun appSettingsDataStore(): com.overscroll.app.data.settings.AppSettingsDataStore
}
