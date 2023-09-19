package com.ncs.nextbus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.ncs.nextbus.databinding.ActivityFrontScreenBinding
import com.ncs.nextbus.databinding.NavBarBinding

class FrontScreen : AppCompatActivity() {
    private lateinit var binding : ActivityFrontScreenBinding
    private var backPressedCount by mutableStateOf(0L)
    private var backPressedToast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFrontScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment())
        binding.bottonnav.setOnItemSelectedListener() {
            when(it.itemId){
                R.id.home -> replaceFragment(HomeFragment())
                R.id.Nearyou-> this.startActivity(Intent(this,MainActivity::class.java))
                R.id.Profile->replaceFragment(ProfileFragment())
        }
            true
        }
    }
    private fun replaceFragment(fragment : Fragment)
    {
        val fragmentManager =supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment,fragment)
        fragmentTransaction.commit()

    }
    override fun onBackPressed() {
        if (backPressedCount == 1L) {
            backPressedToast?.cancel()
            finishAffinity()

        } else {
            backPressedCount++
            backPressedToast?.cancel()
            backPressedToast = Toast.makeText(
                this,
                "Press back again to exit",
                Toast.LENGTH_SHORT
            )
            backPressedToast?.show()
        }
    }
}
