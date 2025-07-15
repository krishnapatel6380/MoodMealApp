package com.krishna.moodmeal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.krishna.moodmeal.utils.NetworkUtils

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force Light Mode globally in all activities that extend BaseActivity
       // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
    }
    fun isNetworkAvailable(): Boolean = NetworkUtils.isNetworkAvailable(this)

    fun showNoNetworkToast() {
        Toast.makeText(this, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
    }
}
