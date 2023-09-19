package com.ncs.nextbus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ncs.nextbus.databinding.ActivityFrontScreenBinding
import com.ncs.nextbus.databinding.NavBarBinding

class FrontScreen : AppCompatActivity() {
    private lateinit var binding : ActivityFrontScreenBinding
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
}