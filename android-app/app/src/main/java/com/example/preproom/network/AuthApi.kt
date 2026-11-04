package com.example.preproom.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: UserDto)

@Serializable
data class RegisterUserRequest(val username: String, val email: String, val password: String, val role: String = "USER")

@Serializable
data class UserDto(val id: Long, val username: String, val email: String, val role: String)

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterUserRequest): UserDto
}
