package com.duncan.repea.data.repository

import android.content.Context
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.duncan.repea.data.model.Song
import com.duncan.repea.data.model.SongDao
import com.duncan.repea.utilities.constants.Constants
import com.duncan.repea.utilities.constants.Constants.PLAYING
import com.duncan.repea.utilities.constants.Constants.STOPPED
import java.util.ArrayList

class SongRepository {
    private fun convertToSong(cursor: Cursor): Song {
        val song = Song()
        song.id = cursor.getString(0)
        song.artist = cursor.getString(1)
        song.title = cursor.getString(2)
        song.data = cursor.getString(3)
        song.displayName = cursor.getString(4)
        song.duration = cursor.getString(5)
        song.isPlaying = false
        return song
    }

    fun fetchAllSongs(c: AppCompatActivity): MutableLiveData<SongDao> {
        val r = SongDao()
        r.status = Constants.LOADING
        val mLiveData = MutableLiveData<SongDao>()
        val songs = ArrayList<Song>()
        r.songs = songs
        mLiveData.setValue(r)
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
        )
        val cursor = c.managedQuery(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val s = convertToSong(cursor)
            if (!songs.contains(s)) {
                songs.add(s)
            }
        }
        r.status = STOPPED
        r.songs = songs
        mLiveData.postValue(r)
        return mLiveData
    }

    fun play(mMediaPlayer: MediaPlayer?, c: Context?, song: Song): MutableLiveData<SongDao> {
        var mMediaPlayer = mMediaPlayer
        val r = SongDao()
        r.status = STOPPED
        r.songs = ArrayList<Song>()
        val mLiveData = MutableLiveData<SongDao>()
        mLiveData.setValue(r)
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(c, Uri.parse(song.data))
        }
        mMediaPlayer!!.start()
        r.status = PLAYING
        mLiveData.postValue(r)
        return mLiveData
    }

    fun pause(mMediaPlayer: MediaPlayer?, c: Context?, song: Song): MutableLiveData<SongDao> {
        var mMediaPlayer = mMediaPlayer
        val r = SongDao()
        r.songs = ArrayList<Song>()
        val mLiveData = MutableLiveData<SongDao>()
        mLiveData.setValue(r)
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(c, Uri.parse(song.data))
        } else {
            mMediaPlayer.pause()
            r.status = STOPPED
            mLiveData.postValue(r)
        }
        return mLiveData
    }

    fun getTimeFromProgress(progress: Int, duration: Int): Int {
        return duration * progress / 100
    }

    fun getSongProgress(totalDuration: Int, currentDuration: Int): Int {
        return currentDuration * 100 / totalDuration
    }

    fun convertToTimerMode(songDuration: String): String {
        val duration = songDuration.toInt()
        val minute = duration % (1000 * 60 * 60) / (1000 * 60)
        val seconds = duration % (1000 * 60 * 60) % (1000 * 60) / 1000
        var finalString = ""
        if (minute < 10) finalString += "0"
        finalString += "$minute:"
        if (seconds < 10) finalString += "0"
        finalString += seconds
        return finalString
    }
}