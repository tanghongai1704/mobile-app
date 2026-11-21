package com.example.onespace.core.network.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Public content, paginated (Section 4, API contract: Nội dung). */
interface CatalogApi {
    @GET("catalog/videos")
    suspend fun videos(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): PageDto<VideoDto>

    @GET("catalog/tracks")
    suspend fun tracks(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): PageDto<TrackDto>
}

interface NewsApi {
    @GET("news/articles")
    suspend fun articles(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): PageDto<ArticleDto>

    @GET("news/articles/{id}")
    suspend fun article(@Path("id") id: String): ArticleDto
}

/** Authentication (Section 4, API contract: Xác thực). */
interface AuthApi {
    @POST("auth/otp")
    suspend fun requestOtp(@Body body: OtpRequest)

    @POST("auth/verify")
    suspend fun verify(@Body body: VerifyRequest): TokenDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenDto

    @POST("auth/logout")
    suspend fun logout()
}

/** Personal data; every call carries `Authorization: Bearer` (added by [com.example.onespace.core.network.AuthHeaderInterceptor]). */
interface MeApi {
    @GET("me/playlists")
    suspend fun playlists(): List<PlaylistDto>

    @POST("me/playlists")
    suspend fun createPlaylist(@Body body: PlaylistDto): PlaylistDto

    @PUT("me/playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: String, @Body body: PlaylistDto): PlaylistDto

    @DELETE("me/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String)

    @POST("me/events")
    suspend fun postEvent(@Body body: EventDto): PointsDto

    @GET("me/points")
    suspend fun points(): PointsDto
}
