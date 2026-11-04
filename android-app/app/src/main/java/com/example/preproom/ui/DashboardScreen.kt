package com.example.preproom.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.preproom.network.NetworkModule
import com.example.preproom.network.Setup
import com.example.preproom.network.Summary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInterview: (String) -> Unit,
    onLogout: () -> Unit
) {
    var sessions by remember { mutableStateOf<List<Summary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewInterviewDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            sessions = NetworkModule.interviewApi.list()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Sessions") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewInterviewDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Interview")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No sessions yet. Create one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onNavigateToInterview(session.id.toString()) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(session.jobRole, style = MaterialTheme.typography.titleMedium)
                            Text("Mode: ${session.mode}", style = MaterialTheme.typography.bodyMedium)
                            Text("State: ${session.state}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showNewInterviewDialog) {
            var jobRole by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showNewInterviewDialog = false },
                title = { Text("New Interview") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = jobRole,
                            onValueChange = { jobRole = it },
                            label = { Text("Job Role") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            try {
                                val setup = Setup(
                                    jobRole = jobRole,
                                    mode = "INTERVIEW",
                                    style = "FRIENDLY",
                                    level = "INTERMEDIATE",
                                    questionCount = 3
                                )
                                val detail = NetworkModule.interviewApi.create(setup)
                                showNewInterviewDialog = false
                                onNavigateToInterview(detail.id.toString())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewInterviewDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
