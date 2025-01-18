package com.gta.reversed

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class REGTA : GTASA() {

    private external fun initreGTA()

    override fun onCreate(savedInstanceState: Bundle?) {

        activity = this

        try {
            initreGTA()
        } catch (e: UnsatisfiedLinkError) {
            e.message?.let { Log.e("Error", it) }
        }

        super.onCreate(savedInstanceState)
    }

    companion object {
        @JvmStatic
        lateinit var activity: AppCompatActivity
    }
}