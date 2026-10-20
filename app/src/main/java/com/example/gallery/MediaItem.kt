package com.example.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CinemaMediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long
) {
    val formattedDuration: String
        get() {
            if (!isVideo) return "STILL"
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format(Locale.US, "%02d:%02d", min, sec)
        }

    val formattedDate: String
        get() {
            return try {
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
                sdf.format(Date(dateAdded * 1000L))
            } catch (_: Exception) {
                ""
            }
        }
}

object MediaStoreHelper {
    fun queryAppMedia(context: Context): List<CinemaMediaItem> {
        val list = mutableListOf<CinemaMediaItem>()

        // 1. Query Videos
        try {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

            val videoSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            }

            val videoArgs = arrayOf("%KeedCinemaCamera%", "KEED_%")

            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                videoArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            videoCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "KEED_VIDEO.mp4"
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)

                    list.add(
                        CinemaMediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            isVideo = true,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = date
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Query Images
        try {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED
            )

            val imageSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            }

            val imageArgs = arrayOf("%KeedCinemaCamera%", "KEED_%")

            val imageCursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                imageSelection,
                imageArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            imageCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "KEED_STILL.jpg"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)

                    list.add(
                        CinemaMediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            isVideo = false,
                            durationMs = 0L,
                            sizeBytes = size,
                            dateAdded = date
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return list.sortedByDescending { it.dateAdded }
    }

    fun deleteMedia(context: Context, item: CinemaMediaItem): Boolean {
        return try {
            val rows = context.contentResolver.delete(item.uri, null, null)
            rows > 0
        } catch (_: Exception) {
            false
        }
    }
}
