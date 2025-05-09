package com.dasoni.musicai

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var button : Button = findViewById(R.id.button)

        button.setOnClickListener{
            setStartingPage()

        }
    }
    fun setStartingPage() {
        setContentView(R.layout.second_activity)
        var avatar : ImageView = findViewById(R.id.avatar)
    }
}