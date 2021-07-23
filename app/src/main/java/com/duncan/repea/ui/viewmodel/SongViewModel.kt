package com.duncan.repea.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.duncan.repea.data.model.Song
import com.duncan.repea.data.model.SongDao
import com.duncan.repea.data.repository.SongRepository

class SongsViewModel(application: Application) : AndroidViewModel(application) {
    private val sr = SongRepository()

    fun loadAllSongs(a: AppCompatActivity?): MutableLiveData<SongDao> {
        return sr.fetchAllSongs(a!!)
    }

    fun play(mMediaPlayer: MediaPlayer?, c: Context?, s: Song?): MutableLiveData<SongDao> {
        return sr.play(mMediaPlayer, c, s!!)
    }

    fun pause(mMediaPlayer: MediaPlayer?, c: Context?, s: Song?): MutableLiveData<SongDao> {
        return sr.pause(mMediaPlayer, c, s!!)
    }

    fun getTimeFromProgress(progress: Int, duration: Int): Int {
        return sr.getTimeFromProgress(progress, duration)
    }

    fun getSongProgress(totalDuration: Int, currentDuration: Int): Int {
        return sr.getSongProgress(totalDuration, currentDuration)
    }

    fun convertToTimerMode(songDuration: String?): String {
        return sr.convertToTimerMode(songDuration!!)
    }
}