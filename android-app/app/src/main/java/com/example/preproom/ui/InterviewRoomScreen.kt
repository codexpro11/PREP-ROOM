package com.example.preproom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.preproom.audio.WavRecorder
import com.example.preproom.network.Answer
import com.example.preproom.network.Detail
import com.example.preproom.network.NetworkModule
import com.example.preproom.network.Turn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewRoomScreen(
    interviewId: String,
    onBack: () -> Unit
) {
    var detail by remember { mutableStateOf<Detail?>(null) }
    var textAnswer by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val wavRecorder = remember { WavRecorder() }

    LaunchedEffect(interviewId) {
        detail = NetworkModule.interviewApi.get(UUID.fromString(interviewId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.jobRole ?: "Loading...") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            detail = NetworkModule.interviewApi.finish(UUID.fromString(interviewId))
                        }
                    }) {
                        Text("Finish", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (detail?.state == "IN_PROGRESS") {
                BottomAppBar(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = { textAnswer = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type answer...") }
                        )
                        IconButton(onClick = {
                            if (textAnswer.isNotBlank()) {
                                scope.launch {
                                    detail = NetworkModule.interviewApi.answer(
                                        UUID.fromString(interviewId),
                                        Answer(UUID.randomUUID(), textAnswer)
                                    )
                                    textAnswer = ""
                                }
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send Text")
                        }
                        
                        IconButton(onClick = {
                            if (isRecording) {
                                scope.launch {
                                    val bytes = wavRecorder.stopRecording()
                                    isRecording = false
                                    
                                    if (bytes.isNotEmpty()) {
                                        val reqFile = bytes.toRequestBody("audio/wav".toMediaTypeOrNull())
                                        val body = MultipartBody.Part.createFormData("file", "audio.wav", reqFile)
                                        val reqId = UUID.randomUUID().toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                        
                                        detail = NetworkModule.interviewApi.audio(UUID.fromString(interviewId), body, reqId)
                                    }
                                }
                            } else {
                                wavRecorder.startRecording()
                                isRecording = true
                            }
                        }) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Record Audio",
                                tint = if (isRecording) Color.Red else LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            detail?.let { d ->
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(d.transcript) { turn ->
                        ChatBubble(turn)
                    }
                }
                
                if (d.feedback != null) {
                    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Feedback", style = MaterialTheme.typography.titleMedium)
                            Text("Overall Score: ${d.feedback!!.overallScore}/10")
                            Text("Plan: ${d.feedback!!.practicePlan}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(turn: Turn) {
    val isSystem = turn.speaker == "SYSTEM"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSystem) Arrangement.Start else Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSystem) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = turn.text, color = if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer)
                if (turn.observation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "[Observation: ${turn.observation}]", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
