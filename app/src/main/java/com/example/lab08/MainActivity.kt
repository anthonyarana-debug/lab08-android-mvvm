package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                ).fallbackToDestructiveMigration().build()

                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)
                TaskScreen(viewModel)
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTasks = viewModel.getFilteredTasks()

    var newTaskDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(3) }
    var showDialog by remember { mutableStateOf(false) }

    val priorityColor = mapOf(
        1 to Color(0xFFD32F2F),
        2 to Color(0xFFFF6F00),
        3 to Color(0xFF1565C0)
    )
    val priorityLabel = mapOf(1 to "Alta", 2 to "Media", 3 to "Baja")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFDB4035)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
            }
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mis Tareas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tarea...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtros
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "Pendientes", "Completadas").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { viewModel.setFilter(label) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDB4035),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de tareas
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredTasks) { task ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox circular con color de prioridad
                            IconButton(onClick = { viewModel.toggleTaskCompletion(task) }) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (task.isCompleted) priorityColor[task.priority]!!
                                            else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(Color.Transparent, CircleShape)
                                    ) {
                                        RadioButton(
                                            selected = task.isCompleted,
                                            onClick = { viewModel.toggleTaskCompletion(task) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = priorityColor[task.priority]!!,
                                                unselectedColor = priorityColor[task.priority]!!
                                            )
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.description,
                                    fontSize = 15.sp,
                                    color = if (task.isCompleted) Color.Gray else Color(0xFF212121),
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = "Prioridad: ${priorityLabel[task.priority]}",
                                    fontSize = 11.sp,
                                    color = priorityColor[task.priority]!!
                                )
                            }

                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog para agregar tarea
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTaskDescription,
                        onValueChange = { newTaskDescription = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Prioridad:", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Alta", 2 to "Media", 3 to "Baja").forEach { (value, label) ->
                            FilterChip(
                                selected = selectedPriority == value,
                                onClick = { selectedPriority = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priorityColor[value]!!,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskDescription.isNotEmpty()) {
                            viewModel.addTask(newTaskDescription, selectedPriority)
                            newTaskDescription = ""
                            selectedPriority = 3
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4035))
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}