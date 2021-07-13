package com.duncan.repea.data.model

import com.duncan.repea.utilities.constants.Constants
import java.util.ArrayList

// Song data access object

class SongDao {
    var status = Constants.STOPPED
    var songs = ArrayList<Song>()
}