package com.example.preproom.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
data class Setup(
    val jobRole: String,
    val mode: String, // INTERVIEW, COMMUNICATION, PRONUNCIATION
    val style: String, // FRIENDLY, TECHNICAL, CHALLENGING
    val level: String, // BEGINNER, INTERMEDIATE, ADVANCED
    val questionCount: Int,
    @Serializable(with = UUIDSerializer::class) val documentId: UUID? = null
)

@Serializable
data class Summary(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val jobRole: String,
    val mode: String,
    val createdAt: String,
    val state: String // IN_PROGRESS, COMPLETED, ANALYZING, ANALYZED, ANALYSIS_FAILED
)

@Serializable
data class Detail(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val jobRole: String,
    val mode: String,
    val style: String,
    val level: String,
    val state: String,
    @Serializable(with = UUIDSerializer::class) val documentId: UUID? = null,
    val transcript: List<Turn>,
    val feedback: Feedback? = null
)

@Serializable
data class Turn(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val speaker: String, // SYSTEM, USER
    val text: String,
    val audioRequested: Boolean = false,
    val audioDurationSeconds: Int? = null,
    val observation: String? = null
)

@Serializable
data class Answer(
    @Serializable(with = UUIDSerializer::class) val requestId: UUID,
    val text: String
)

@Serializable
data class Feedback(
    val relevanceScore: Int,
    val grammarScore: Int,
    val fluencyScore: Int?,
    val starScore: Int?,
    val overallScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val exampleAnswer: String,
    val practicePlan: String
)

interface InterviewApi {
    @GET("api/v1/interviews")
    suspend fun list(): List<Summary>

    @POST("api/v1/interviews")
    suspend fun create(@Body setup: Setup): Detail

    @GET("api/v1/interviews/{id}")
    suspend fun get(@Path("id") id: UUID): Detail

    @POST("api/v1/interviews/{id}/answers")
    suspend fun answer(@Path("id") id: UUID, @Body answer: Answer): Detail

    @Multipart
    @POST("api/v1/interviews/{id}/audio")
    suspend fun audio(
        @Path("id") id: UUID,
        @Part file: MultipartBody.Part,
        @Part("requestId") requestId: RequestBody
    ): Detail

    @POST("api/v1/interviews/{id}/finish")
    suspend fun finish(@Path("id") id: UUID): Detail
}
