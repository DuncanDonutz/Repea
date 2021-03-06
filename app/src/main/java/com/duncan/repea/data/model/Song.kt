package com.duncan.repea.data.model

// Song data object

class Song {
    var id: String? = null
    var artist: String? = null
    var title: String? = null
    var data: String? = null
    var displayName: String? = null
    var duration: String? = null
    var isPlaying = false
    override fun equals(obj: Any?): Boolean {
        if (obj !is Song) {
            return false
        }
        // Return eliminates duplicate songs
        return id === obj.id && duration == obj.duration || (duration == obj.duration && title == obj.title && artist == obj.artist)
    }
}