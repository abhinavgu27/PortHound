package com.example.a1st.di

import com.example.a1st.data.camerax.CameraManager
import com.example.a1st.data.camerax.CameraManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraManager(
        cameraManagerImpl: CameraManagerImpl
    ): CameraManager
}
