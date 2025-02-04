package com.frank.androidmedia.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import java.io.FileOutputStream
import java.lang.Exception

/**
 * Using MediaProjectionManager to screenshot,
 * and using MediaProjection recording screen.
 *
 * @author frank
 * @date 2022/3/25
 */
open class MediaProjectionController(type: Int) {

    companion object {
        const val TYPE_SCREEN_SHOT   = 0
        const val TYPE_SCREEN_RECORD = 1
        const val TYPE_SCREEN_LIVING = 2
    }

    private var type = TYPE_SCREEN_SHOT
    private val requestCode = 123456
    private var virtualDisplay: VirtualDisplay? = null
    private var displayMetrics: DisplayMetrics? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    init {
        this.type = type
    }

    fun startScreenRecord(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        (context as Activity).startActivityForResult(intent, requestCode)
    }

    fun createVirtualDisplay(surface: Surface) {
        virtualDisplay = mediaProjection?.createVirtualDisplay("hello", displayMetrics!!.widthPixels,
                displayMetrics!!.heightPixels, displayMetrics!!.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null)
    }

    private fun saveBitmap(bitmap: Bitmap?, path: String) {
        if (path.isEmpty() || bitmap == null)
            return
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(path)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        } catch (e: Exception) {

        } finally {
            outputStream?.close()
        }
    }

    private fun getBitmap() {
        val imageReader = ImageReader.newInstance(displayMetrics!!.widthPixels,
                displayMetrics!!.heightPixels, PixelFormat.RGBA_8888, 3)
        createVirtualDisplay(imageReader.surface)
        imageReader.setOnImageAvailableListener ({ reader: ImageReader ->

            val image = reader.acquireNextImage()
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride,
                    image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            val filePath = Environment.getExternalStorageDirectory().path + "/hello.jpg"
            saveBitmap(bitmap, filePath)
            image.close()
            imageReader.close()
        }, null)
    }

    fun onActivityResult(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        if (type == TYPE_SCREEN_SHOT) {
            getBitmap()
        }
    }

    fun getRequestCode(): Int {
        return requestCode
    }


    fun stopScreenRecord() {
        mediaProjection?.stop()
        virtualDisplay?.release()
    }

}