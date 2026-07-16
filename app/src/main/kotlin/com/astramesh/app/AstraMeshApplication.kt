package com.astramesh.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AstraMesh application entry point.
 *
 * The mobile app is fully self-contained: it never depends on a remote server or the
 * optional desktop companion for core phone-to-phone messaging.
 */
@HiltAndroidApp
class AstraMeshApplication : Application()
