package com.example.ilyphoto

import android.app.Application
import com.example.ilyphoto.data.FirebaseProvider

class ILYPhotoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseProvider.init(this)
    }
}
