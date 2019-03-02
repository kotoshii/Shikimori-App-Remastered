package com.gnoemes.shikimori.presentation.view.splash

import android.content.Intent
import android.os.Bundle
import com.gnoemes.shikimori.presentation.view.base.activity.MvpActivity
import com.gnoemes.shikimori.presentation.view.main.MainActivity

class SplashActivity : MvpActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Todo check user status

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}