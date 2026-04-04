package com.dusy4.pingbox.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface TriggerApiService {

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    @POST("v1/push")
    suspend fun sendPush(
        @Header("Authorization") token: String,
        @Body request: PushRequest
    ): Response<PushResponse>

    @POST("v1/devices")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceRegisterRequest
    ): Response<DeviceResponse>

    @GET("v1/devices")
    suspend fun listDevices(
        @Header("Authorization") token: String
    ): Response<DevicesListResponse>

    @DELETE("v1/devices/{deviceId}")
    suspend fun deleteDevice(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: String
    ): Response<Unit>

    @POST("v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("v1/keys")
    suspend fun createApiKey(
        @Header("Authorization") token: String,
        @Query("label") label: String? = null
    ): Response<ApiKeyCreateResponse>

    @GET("v1/keys")
    suspend fun listApiKeys(
        @Header("Authorization") token: String
    ): Response<ApiKeysListResponse>

    @GET("v1/rules")
    suspend fun getRules(
        @Header("Authorization") token: String
    ): Response<RulesListResponse>

    @POST("v1/rules")
    suspend fun createRule(
        @Header("Authorization") token: String,
        @Body request: RuleCreateRequest
    ): Response<RuleResponse>

    @PUT("v1/rules/{ruleId}")
    suspend fun updateRule(
        @Header("Authorization") token: String,
        @Path("ruleId") ruleId: String,
        @Body request: RuleUpdateRequest
    ): Response<RuleResponse>

    @DELETE("v1/rules/{ruleId}")
    suspend fun deleteRule(
        @Header("Authorization") token: String,
        @Path("ruleId") ruleId: String
    ): Response<Unit>

    @GET("v1/history")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<HistoryListResponse>
}

@Serializable
data class PushRequest(
    val title: String,
    val body: String? = null,
    val icon: String? = null,
    val tag: String? = null,
    val priority: String = "high",
    val target: TargetRequest = TargetRequest(),
    val devices: List<String> = listOf("all")
)

@Serializable
data class TargetRequest(
    val type: String = "none",
    val value: String = ""
)

@Serializable
data class PushResponse(
    val id: String,
    val status: String,
    @SerialName("devices_reached") val devicesReached: Int
)

@Serializable
data class DeviceRegisterRequest(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("device_name") val deviceName: String? = null,
    val platform: String = "android"
)

@Serializable
data class DeviceResponse(
    val id: String,
    @SerialName("device_name") val deviceName: String?,
    val platform: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class DevicesListResponse(
    val devices: List<DeviceResponse>
)

@Serializable
data class HealthResponse(
    val status: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String?,
    @SerialName("token_type") val tokenType: String? = "bearer",
    val user: UserResponse?
)

@Serializable
data class UserResponse(
    val id: String?,
    val email: String?
)

@Serializable
data class RuleCreateRequest(
    val name: String,
    @SerialName("match_tag") val matchTag: String? = null,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_value") val targetValue: String,
    val priority: Int = 0,
    val enabled: Boolean = true
)

@Serializable
data class RuleUpdateRequest(
    val name: String? = null,
    @SerialName("match_tag") val matchTag: String? = null,
    @SerialName("target_type") val targetType: String? = null,
    @SerialName("target_value") val targetValue: String? = null,
    val priority: Int? = null,
    val enabled: Boolean? = null
)

@Serializable
data class RuleResponse(
    val id: String,
    val name: String,
    @SerialName("match_tag") val matchTag: String?,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_value") val targetValue: String,
    val priority: Int,
    val enabled: Boolean,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class RulesListResponse(
    val rules: List<RuleResponse>
)

@Serializable
data class HistoryListResponse(
    val notifications: List<HistoryResponse>,
    val total: Int,
    val page: Int,
    val limit: Int
)

@Serializable
data class HistoryResponse(
    val id: String,
    val title: String,
    val body: String?,
    val tag: String?,
    @SerialName("target_type") val targetType: String?,
    @SerialName("target_value") val targetValue: String?,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiKeyCreateResponse(
    val id: String,
    val key: String,
    val label: String?,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiKeysListResponse(
    val keys: List<ApiKeyResponse>
)

@Serializable
data class ApiKeyResponse(
    val id: String,
    val label: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("revoked_at") val revokedAt: String?
)
