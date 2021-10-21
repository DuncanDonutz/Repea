package com.duncan.repea.ui.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.view.ViewCompat.animate
import com.airbnb.lottie.LottieAnimationView
import com.duncan.repea.R
import kotlinx.android.synthetic.main.activity_splash_screen.*


class SplashScreenActivity : AppCompatActivity() {
    companion object {
        const val ANIMATION_TIME: Long = 6000 //Change time according to your animation.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        // Handler to delay the start of next Activity
        Handler(this.mainLooper).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, ANIMATION_TIME)
    }
}