package com.ncs.nextbus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class SpScreen : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sp_layout)


//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN
//        )
        val logo  = findViewById<ImageView>(R.id.ivLogo)
        val tagline = findViewById<ImageView>(R.id.ivTagline)
////        val spanimation = AnimationUtils.loadAnimation(this,R.anim.sp_animation)
        logo.alpha=0f
        tagline.alpha=0f
//        logo.animate().setDuration(2000).alpha(1f).withEndAction{
//            val intent = Intent(this,MainActivity::class.java)
//            startActivity(intent)
//            (android.R.anim.slide_in_left,android.R.anim.slid)
//            finish()
//        }
        logo.animate().setDuration(1500).alpha(1f).withEndAction{
            val i= Intent(this,FrontScreen::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
        tagline.animate().setDuration(1500).alpha(1f).withEndAction{
            val i= Intent(this,FrontScreen::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }


//        logo.startAnimation(spanimation)

//        Handler().postDelayed({
//            val intent = Intent(this,MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        },3000)


}}