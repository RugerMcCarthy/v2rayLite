package com.thoughtcrime.v2raylite.di

import com.thoughtcrime.v2raylite.network.CardPlatformRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Provides
    @Singleton
    fun provideDeeplRepo(okHttpClient: OkHttpClient): CardPlatformRepo {
        return CardPlatformRepo(okHttpClient)
    }
}