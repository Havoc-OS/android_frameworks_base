/*
 * Copyright (c) 2015, Sergii Pylypenko
 *           (c) 2018, Joe Maples
 *           (c) 2018, Adin Kwok
 *           (c) 2018, CarbonROM
 *           (C) 2023 the RisingOS Android Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of screen-dimmer-pixel-filter nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.systemui.smartpixels

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.android.systemui.R
import android.content.res.Resources
import android.graphics.Point
import android.os.UserHandle

class SmartPixelsService : Service() {
    companion object {
        const val LOG = "SmartPixelsService"
        var running = false
    }

    private lateinit var windowManager: WindowManager
    private var view: ImageView? = null
    private lateinit var bmp: Bitmap

    private var destroyed = false

    private var startCounter = 0
    private lateinit var mContext: Context

    // Pixel Filter Settings
    private var mPattern = 3
    private var mShiftTimeout = 4

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        mContext = this
        updateSettings()
        Log.d(LOG, "Service started")
        startFilter()
    }

    private fun startFilter() {
        if (view != null) {
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        view = ImageView(this)
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        bmp = Bitmap.createBitmap(Grids.GridSideSize, Grids.GridSideSize, Bitmap.Config.ARGB_4444)

        updatePattern()
        val draw = BitmapDrawable(bmp).apply {
            setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            setFilterBitmap(false)
            setAntiAlias(false)
            setTargetDensity(metrics.densityDpi)
        }

        view!!.background = draw

        val params = getLayoutParams()
        params.privateFlags = params.privateFlags or WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            running = false
            view = null
            return
        }

        startCounter++
        val handlerStartCounter = startCounter
        val handler = Handler()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (view == null || destroyed || handlerStartCounter != startCounter) {
                    return
                } else if (pm.isInteractive) {
                    updatePattern()
                    view!!.invalidate()
                }
                if (!destroyed) {
                    handler.postDelayed(this, Grids.ShiftTimeouts[mShiftTimeout].toLong())
                }
            }
        }, Grids.ShiftTimeouts[mShiftTimeout].toLong())
    }

    private fun stopFilter() {
        if (view == null) {
            return
        }

        startCounter++

        windowManager.removeView(view)
        view = null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyed = true
        stopFilter()
        Log.d(LOG, "Service stopped")
        running = false
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(LOG, "Screen orientation changed, updating window layout")
        val params = getLayoutParams()
        windowManager.updateViewLayout(view, params)
    }

    private fun getLayoutParams(): WindowManager.LayoutParams {
        val displaySize = Point()
        windowManager.defaultDisplay.getRealSize(displaySize)
        val windowSize = Point()
        windowManager.defaultDisplay.getRealSize(windowSize)
        val res: Resources = resources
        val mStatusBarHeight = res.getDimensionPixelOffset(R.dimen.status_bar_height)
        displaySize.x += displaySize.x - windowSize.x + (mStatusBarHeight * 2)
        displaySize.y += displaySize.y - windowSize.y + (mStatusBarHeight * 2)

        return WindowManager.LayoutParams(
                displaySize.x,
                displaySize.y,
                0,
                0,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSPARENT
        ).apply {
            // Use the rounded corners overlay to hide it from screenshots. See 132c9f514.
            privateFlags = privateFlags or WindowManager.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY
            dimAmount = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
    }

    private fun getShift(): Int {
        val shift = (System.currentTimeMillis() / Grids.ShiftTimeouts[mShiftTimeout]).toInt() % Grids.GridSize
        return Grids.GridShift[shift].toInt()
    }

    private fun updatePattern() {
        val shift = getShift().toInt()
        val shiftX = shift % Grids.GridSideSize
        val shiftY = shift / Grids.GridSideSize
        for (i in 0 until Grids.GridSize) {
            val x = (i + shiftX) % Grids.GridSideSize
            val y = ((i / Grids.GridSideSize) + shiftY) % Grids.GridSideSize
            val color = if (Grids.Patterns[mPattern][i] == 0.toByte()) Color.TRANSPARENT else Color.BLACK
            bmp.setPixel(x, y, color)
        }
    }

    private fun updateSettings() {
        mPattern = Settings.System.getIntForUser(
                mContext.contentResolver, Settings.System.SMART_PIXELS_PATTERN,
                5, UserHandle.USER_CURRENT)
        mShiftTimeout = Settings.System.getIntForUser(
                mContext.contentResolver, Settings.System.SMART_PIXELS_SHIFT_TIMEOUT,
                4, UserHandle.USER_CURRENT)
    }
}
