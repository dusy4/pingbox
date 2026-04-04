package com.dusy4.pingbox.data.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TriggerApiService

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(TriggerApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // ===== REGISTER TESTS =====

    @Test
    fun `register returns valid AuthResponse on success`() = runTest {
        val successResponse = """
            {
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test",
                "token_type": "bearer",
                "user": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "email": "test@example.com"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successResponse)
        )

        val response = apiService.register(RegisterRequest("test@example.com", "password123"))

        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", response.body()?.accessToken)
        assertEquals("bearer", response.body()?.tokenType)
        assertNotNull(response.body()?.user)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.body()?.user?.id)
        assertEquals("test@example.com", response.body()?.user?.email)
    }

    @Test
    fun `register returns 400 when email already registered`() = runTest {
        val errorResponse = """{"detail": "Email already registered"}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorResponse)
        )

        val response = apiService.register(RegisterRequest("existing@example.com", "password123"))

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }

    @Test
    fun `register returns 400 for invalid email format`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"detail": "Invalid email format"}""")
        )

        val response = apiService.register(RegisterRequest("not-an-email", "password123"))

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }

    @Test
    fun `register returns 400 for weak password`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"detail": "Password too short"}""")
        )

        val response = apiService.register(RegisterRequest("test@example.com", "123"))

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }

    @Test
    fun `register sends correct request body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "test-token",
                        "token_type": "bearer",
                        "user": {
                            "id": "uuid-1",
                            "email": "user@test.com"
                        }
                    }
                """.trimIndent())
        )

        apiService.register(RegisterRequest("user@test.com", "securepass"))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/v1/auth/register", recordedRequest.path)
        assertTrue(recordedRequest.body?.readUtf8()?.contains("user@test.com") == true)
    }

    @Test
    fun `register handles null user gracefully`() = runTest {
        val responseWithNullUser = """
            {
                "access_token": "test-token",
                "token_type": "bearer",
                "user": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseWithNullUser)
        )

        val response = apiService.register(RegisterRequest("test@example.com", "password123"))

        assertTrue(response.isSuccessful)
        assertEquals("test-token", response.body()?.accessToken)
        assertNull(response.body()?.user)
    }

    // ===== LOGIN TESTS =====

    @Test
    fun `login returns valid AuthResponse on success`() = runTest {
        val successResponse = """
            {
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.login",
                "token_type": "bearer",
                "user": {
                    "id": "660e8400-e29b-41d4-a716-446655440001",
                    "email": "user@example.com"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successResponse)
        )

        val response = apiService.login(LoginRequest("user@example.com", "correctpassword"))

        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.login", response.body()?.accessToken)
        assertEquals("bearer", response.body()?.tokenType)
        assertNotNull(response.body()?.user)
        assertEquals("660e8400-e29b-41d4-a716-446655440001", response.body()?.user?.id)
        assertEquals("user@example.com", response.body()?.user?.email)
    }

    @Test
    fun `login returns 401 for invalid credentials`() = runTest {
        val errorResponse = """{"detail": "Invalid credentials"}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(errorResponse)
        )

        val response = apiService.login(LoginRequest("user@example.com", "wrongpassword"))

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `login returns 401 for non-existent user`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"detail": "Invalid credentials"}""")
        )

        val response = apiService.login(LoginRequest("nouser@example.com", "password123"))

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `login sends correct request body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "login-token",
                        "token_type": "bearer",
                        "user": {
                            "id": "uuid-2",
                            "email": "login@test.com"
                        }
                    }
                """.trimIndent())
        )

        apiService.login(LoginRequest("login@test.com", "mypassword"))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/v1/auth/login", recordedRequest.path)
        assertTrue(recordedRequest.body?.readUtf8()?.contains("login@test.com") == true)
    }

    @Test
    fun `login handles null user gracefully`() = runTest {
        val responseWithNullUser = """
            {
                "access_token": "login-token",
                "token_type": "bearer",
                "user": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseWithNullUser)
        )

        val response = apiService.login(LoginRequest("user@example.com", "password123"))

        assertTrue(response.isSuccessful)
        assertEquals("login-token", response.body()?.accessToken)
        assertNull(response.body()?.user)
    }

    // ===== DATA MODEL TESTS =====

    @Test
    fun `RegisterRequest serializes to correct JSON`() {
        val request = RegisterRequest("test@example.com", "password123")
        val json = gson.toJson(request)

        assertTrue(json.contains("\"email\":\"test@example.com\""))
        assertTrue(json.contains("\"password\":\"password123\""))
    }

    @Test
    fun `LoginRequest serializes to correct JSON`() {
        val request = LoginRequest("user@example.com", "secret")
        val json = gson.toJson(request)

        assertTrue(json.contains("\"email\":\"user@example.com\""))
        assertTrue(json.contains("\"password\":\"secret\""))
    }

    @Test
    fun `AuthResponse deserializes from snake_case JSON`() {
        val json = """
            {
                "access_token": "token123",
                "token_type": "bearer",
                "user": {
                    "id": "uuid-3",
                    "email": "user@test.com"
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, AuthResponse::class.java)

        assertEquals("token123", response.accessToken)
        assertEquals("bearer", response.tokenType)
        assertEquals("uuid-3", response.user?.id)
        assertEquals("user@test.com", response.user?.email)
    }

    @Test
    fun `AuthResponse deserializes with null user`() {
        val json = """
            {
                "access_token": "token456",
                "token_type": "bearer",
                "user": null
            }
        """.trimIndent()

        val response = gson.fromJson(json, AuthResponse::class.java)

        assertEquals("token456", response.accessToken)
        assertNull(response.user)
    }

    @Test
    fun `UserResponse deserializes with null email`() {
        val json = """
            {
                "id": "uuid-4",
                "email": null
            }
        """.trimIndent()

        val user = gson.fromJson(json, UserResponse::class.java)

        assertEquals("uuid-4", user.id)
        assertNull(user.email)
    }
}
