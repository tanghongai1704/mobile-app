package com.example.onespace.core.network.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.onespace.core.network.AccessTokenProvider
import com.example.onespace.core.network.AuthHeaderInterceptor
import com.example.onespace.core.network.api.AuthApi
import com.example.onespace.core.network.api.CatalogApi
import com.example.onespace.core.network.api.MeApi
import com.example.onespace.core.network.api.NewsApi
import com.example.onespace.core.network.fake.FakeApiInterceptor
import com.example.onespace.core.network.fake.FakeRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Lets the "Mô phỏng mất mạng" setting cut the network for demos, on top of real connectivity. */
fun interface OfflineSimulation {
    fun isSimulatingOffline(): Boolean
}

/** Base URL of the designed backend; the fake interceptor answers it until the server exists. */
const val API_BASE_URL = "https://api.onespace.app/v1/"

@Module
@InstallIn(SingletonComponent::class)
abstract class FakeRouteBindings {
    /** Data modules contribute their slice of the fake backend with `@IntoSet`. */
    @Multibinds abstract fun fakeRoutes(): Set<FakeRoute>
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Provides @Singleton
    fun okHttp(
        @ApplicationContext context: Context,
        routes: Set<@JvmSuppressWildcards FakeRoute>,
        tokens: AccessTokenProvider,
        simulation: OfflineSimulation
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(AuthHeaderInterceptor(tokens))
        // Must stay last: it terminates the chain instead of opening a socket.
        .addInterceptor(FakeApiInterceptor(routes, isOnline = { !simulation.isSimulatingOffline() && context.isOnline() }))
        .build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton fun catalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)
    @Provides @Singleton fun newsApi(retrofit: Retrofit): NewsApi = retrofit.create(NewsApi::class.java)
    @Provides @Singleton fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides @Singleton fun meApi(retrofit: Retrofit): MeApi = retrofit.create(MeApi::class.java)
}

private fun Context.isOnline(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
