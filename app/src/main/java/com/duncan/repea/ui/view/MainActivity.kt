package com.duncan.repea.ui.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.duncan.repea.data.model.Song
import com.duncan.repea.data.model.SongDao
import com.duncan.repea.databinding.ModelBinding
import com.duncan.repea.ui.viewmodel.SongsViewModel
import com.duncan.repea.utilities.constants.Constants
import com.duncan.repea.utilities.constants.Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.duncan.repea.R
import info.camposha.pollux.PolluxAdapter
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.content_home.playBtn
import kotlinx.android.synthetic.main.model.*
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var hasFinished = false
    private var currentSong: Song? = null
    private var currentSongId: String? = null
    var mMediaPlayer: MediaPlayer? = null
    var mHandler = Handler()
    var adapter: PolluxAdapter<Song>? = null
    private var sv: SongsViewModel? = null

    var SONG_POSITION = 0
    var SONGS_CACHE = ArrayList<Song>()
    var SEEK_FORWARD = 5000
    var SEEK_BACKWARD = 5000
    var playbackParams = PlaybackParams()

    private fun initializeViews() {
        songsRV!!.layoutManager = LinearLayoutManager(this)
        progressSPD.isEnabled = false
    }

    // Get song position in cache
    private fun getPosition(s: Song?): Int {
        val pos = 0
        for (song in SONGS_CACHE) {
            if (s!!.id.equals(song.id, ignoreCase = true)) {
                return SONGS_CACHE.indexOf(s)
            }
        }
        return pos
    }

    private val player: MediaPlayer?
        private get() {
            if (mMediaPlayer == null) {
                if (currentSong == null) {
                    currentSong = if (SONGS_CACHE.size > 0) {
                        SONGS_CACHE[0]
                    } else {
                        return null
                    }
                }
                mMediaPlayer = MediaPlayer.create(this, Uri.parse(currentSong!!.data))
            }
            return mMediaPlayer
        }

    // Releases mediaPlayer resources from memory
    private fun cleanUpMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    private fun fetchAllSongs() {
        sv!!.loadAllSongs(this).observe(this, Observer { requestCall: SongDao ->
            val linkedHashSet = LinkedHashSet(requestCall.songs)
            SONGS_CACHE.clear()
            SONGS_CACHE.addAll(linkedHashSet)
            SONGS_CACHE.sortWith(compareBy({ it.title }))
            if (currentSong == null && SONGS_CACHE.size > 0) {
                currentSong = SONGS_CACHE.get(0)
            }
        })
    }

    private fun checkPermissionsThenLoadSongs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
            show("Please allow external storage access")
        } else {
            fetchAllSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantedResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantedResults.size > 0
                    && grantedResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    fetchAllSongs()
                } else {
                    show("Please allow external storage access")
                }
                return
            }
        }
    }

    private fun playOrPause(song: Song) {
        currentSong = song
        if (player == null) {
            show("Please add songs")
            return
        }
        hasFinished = false
   try {
            songsRV.smoothScrollToPosition(getPosition(song))
        } catch (e: Exception) {

        }
        if (player!!.isPlaying) {
            sv!!.pause(player, this, song).observe(this, Observer { requestCall: SongDao ->
                if (requestCall.status == Constants.STOPPED) {
                    progressSPD.isEnabled = false
                    song.isPlaying = false
                    currentSongId = null
                    refreshRecyclerView(song.isPlaying)
                    playBtn!!.setImageResource(R.drawable.ic_play)
                    waveLineView.stopAnim()
                    adapter!!.notifyDataSetChanged()
                }
            })
        } else {
            sv!!.play(player, this, song).observe(this, Observer { requestCall: SongDao ->
                if (requestCall.status == Constants.PLAYING) {
                    progressSPD.isEnabled = true
                    song.isPlaying = true
                    currentSongId = song.id
                    SONG_POSITION = getPosition(song)
                    refreshRecyclerView(song.isPlaying)
                    playBtn!!.setImageResource(R.drawable.ic_pause)
                    waveLineView.startAnim()
                    updateSongProgress()
                    adapter!!.notifyDataSetChanged()
                }
            })
        }
        refreshRecyclerView(true)
    }

    private fun nextSong(){
        refreshRecyclerView(false)
        SONG_POSITION = getPosition(currentSong) + 1
        if (SONG_POSITION >= SONGS_CACHE.size) {
            SONG_POSITION = 0
        }
        cleanUpMediaPlayer()

        // Keeps speedSeek at 100 when skipping
        findViewById<TextView>(R.id.speedPercentage).text = "100"
        progressSPD.progress = 100

        val nextSong: Song = SONGS_CACHE.get(SONG_POSITION)
        playOrPause(nextSong)
    }

    private fun  prevSong(){
        refreshRecyclerView(false)
        SONG_POSITION--
        if (SONG_POSITION < 0) {
            if (SONGS_CACHE.size > 0) {
                SONG_POSITION = SONGS_CACHE.size - 1
            } else {
                SONG_POSITION = 0
            }
        }
        // Keeps speedSeek at 100 when skipping
        findViewById<TextView>(R.id.speedPercentage).text = "100"
        progressSPD.progress = 100

        val prevSong: Song = SONGS_CACHE[SONG_POSITION]
        cleanUpMediaPlayer()
        playOrPause(prevSong)
    }

    private fun fastForward(){
        refreshRecyclerView(true)
        SONG_POSITION = mMediaPlayer!!.currentPosition

        if (SONG_POSITION + SEEK_FORWARD <= mMediaPlayer!!.duration) {
            mMediaPlayer!!.seekTo(SONG_POSITION + SEEK_FORWARD)
        } else {
            mMediaPlayer!!.seekTo(mMediaPlayer!!.duration)
        }

    }

    private fun rewind(){
        refreshRecyclerView(true)
        SONG_POSITION = mMediaPlayer!!.currentPosition

        if (SONG_POSITION - SEEK_BACKWARD <= mMediaPlayer!!.duration) {
            mMediaPlayer!!.seekTo(SONG_POSITION - SEEK_BACKWARD)
        } else {
            mMediaPlayer!!.seekTo(0)
        }
    }

    private fun handleEvents() {
        playBtn!!.setOnClickListener {
            playOrPause(currentSong!!)
        }

        nextBtn!!.setOnClickListener {
            nextSong()
        }

        prevBtn!!.setOnClickListener {
            prevSong()
        }

        fastForwardBtn!!.setOnClickListener {
            fastForward()
        }

        rewindBtn!!.setOnClickListener {
            rewind()
        }

        progressSB!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (player == null) {
                        show("Please add songs")
                        return
                    }
                    mMediaPlayer!!.seekTo(
                        (sv!!.getTimeFromProgress(
                            seekBar.progress,
                            player!!.duration
                        ))
                    )
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        progressSPD!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    var speedPercentage = findViewById<TextView>(R.id.speedPercentage)
                    var progString = progress.toString()
                    speedPercentage.text = progString + "%"
                    var speed = (progress.toFloat()) / 100
                    mMediaPlayer!!.playbackParams = mMediaPlayer!!.playbackParams!!.setSpeed(speed)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun refreshRecyclerView(playing: Boolean) {
        if (currentSong != null && SONGS_CACHE.size > 0) {
            currentSong!!.isPlaying = playing
            val s = SONGS_CACHE.find { it.id == currentSong!!.id }
            if (s != null) {
                SONGS_CACHE[getPosition(s)] = currentSong!!
            }
            adapter!!.notifyDataSetChanged()
            currentSongTV.text = currentSong!!.title
            currentSongTV.marqueeRepeatLimit = -1
        } else {
            currentSongTV.text = "Song"
        }
    }

    // Toast
    private fun show(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupRecycler(songs: ArrayList<Song>) {
        adapter =
            PolluxAdapter.with<Song, ModelBinding>(R.layout.model) { adapterPosition, song, mb ->
                mb.titleTV.text = song.title
                if (song.id == currentSongId) {
                    mb.playBtn.setImageResource(R.drawable.ic_pause)
                    mb.titleTV.setTextColor(Color.rgb(203, 167, 255))
                    mb.titleTV.setTypeface(null, Typeface.BOLD_ITALIC)
                } else {
                    mb.playBtn.setImageResource(R.drawable.ic_play)
                    mb.titleTV.setTextColor(Color.GRAY)
                    mb.titleTV.setTypeface(null, Typeface.NORMAL)
                    if (hasFinished) {
                        mb.playBtn.setImageResource(R.drawable.ic_play)
                        mb.titleTV.setTextColor(Color.GRAY)
                        mb.titleTV.setTypeface(null, Typeface.NORMAL)
                    } else {
                        if (currentSong != null && currentSong!!.id === song.id) {
                            mb.playBtn.setImageResource(R.drawable.ic_play)
                            mb.titleTV.setTextColor(Color.rgb(203, 167, 255))
                            mb.titleTV.setTypeface(null, Typeface.NORMAL)
                        }
                    }
                }

                mb.llCard.setOnClickListener { view: View? ->
                    if (!song.isPlaying) {
                        if (SONG_POSITION != getPosition(song)) {
                            if (mMediaPlayer != null) {
                                mMediaPlayer!!.release()
                                mMediaPlayer = null
                            }
                        }
                        SONG_POSITION = getPosition(song)
                        if (currentSong != null) {
                            currentSong!!.isPlaying = false
                            adapter!!.notifyDataSetChanged()
                        }
                        currentSong = SONGS_CACHE.get(SONG_POSITION)
                        playOrPause(song)
                        show("Now Playing: " + song.title)
                        mb.playBtn.setImageResource(android.R.drawable.ic_media_pause)
                        mb.titleTV.setTextColor(Color.GREEN)
                        mb.titleTV.setTypeface(null, Typeface.ITALIC)
                        // Keeps speedSeek at 100 when changing songs
                        findViewById<TextView>(R.id.speedPercentage).text = "100"
                        progressSPD.progress = 100
                    } else {
                        if (SONG_POSITION != getPosition(song)) {
                            if (mMediaPlayer != null) {
                                mMediaPlayer!!.release()
                                mMediaPlayer = null
                            }
                        }
                        SONG_POSITION = getPosition(song)
                        show("Stopped: " + song.title)
                        playOrPause(song)
                        mb.playBtn.setImageResource(android.R.drawable.ic_media_play)
                        mb.titleTV.setTextColor(Color.RED)
                        mb.titleTV.setTypeface(null, Typeface.ITALIC)
                    }
                }
            }
        songsRV.layoutManager = GridLayoutManager(this@MainActivity, 2)
        adapter!!.addAll(songs)
        songsRV!!.adapter = adapter
    }

    private fun updateSongProgress() {
        mHandler.postDelayed(runnable, 1000)
    }

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            if (!hasFinished) {
                if (mMediaPlayer == null) {
                    return
                }
                val currentDuration = mMediaPlayer!!.currentPosition
                val totalDuration = mMediaPlayer!!.duration
                currentPosTV!!.text = sv!!.convertToTimerMode(currentDuration.toString())
                progressSB!!.progress = sv!!.getSongProgress(totalDuration, currentDuration)
                totalDurationTV!!.text = sv!!.convertToTimerMode(totalDuration.toString())
                if (progressSB!!.progress >= 99 && !mMediaPlayer!!.isPlaying) {
                    playBtn!!.setImageResource(android.R.drawable.ic_media_play)
                    hasFinished = true
                    adapter!!.notifyDataSetChanged()
                    nextBtn!!.performClick()
                } else {
                    hasFinished = false
                }
                mHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keeps screen from locking
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sv = ViewModelProvider(this).get(SongsViewModel::class.java)
        initializeViews()
        handleEvents()
    }

    override fun onResume() {
        super.onResume()
        if (SONGS_CACHE.isEmpty()) {
            checkPermissionsThenLoadSongs()
        }
        setupRecycler(SONGS_CACHE)
    }

    override fun onPause() {
        super.onPause()
        if (currentSong != null && currentSong!!.isPlaying) {
            currentSong!!.isPlaying = false
            playOrPause(currentSong!!)
        }
        cleanUpMediaPlayer()
    }

    override fun onStop() {
        super.onStop()
        SONGS_CACHE.clear()
        cleanUpMediaPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        waveLineView.release()
    }
}