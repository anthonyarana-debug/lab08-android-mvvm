package com.example.lab08

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        ).fallbackToDestructiveMigration().build()

        viewModel = TaskViewModel(db.taskDao())

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                TaskScreen(viewModel)
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredTasks = remember(tasks, filter, searchQuery, sortAscending, selectedCategory) {
        viewModel.getFilteredTasks()
    }

    var newTaskDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(3) }
    var selectedNewCategory by remember { mutableStateOf("General") }
    var isRecurring by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editDescription by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf(3) }
    var editCategory by remember { mutableStateOf("General") }
    var editRecurring by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tarea...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleSort() }) {
                    Icon(
                        imageVector = if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ordenar",
                        tint = Color(0xFFDB4035)
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF424242),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            IconButton(onClick = {
                                viewModel.toggleTaskCompletion(task)
                                if (!task.isCompleted) {
                                    NotificationHelper.showTaskCompletedNotification(context, task.description)
                                }
                            }) {
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
                                    RadioButton(
                                        selected = task.isCompleted,
                                        onClick = {
                                            viewModel.toggleTaskCompletion(task)
                                            if (!task.isCompleted) {
                                                NotificationHelper.showTaskCompletedNotification(context, task.description)
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = priorityColor[task.priority]!!,
                                            unselectedColor = priorityColor[task.priority]!!
                                        )
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = task.description,
                                        fontSize = 15.sp,
                                        color = if (task.isCompleted) Color.Gray else Color(0xFF212121),
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (task.isRecurring) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Recurrente",
                                            tint = Color(0xFF757575),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = priorityLabel[task.priority]!!,
                                        fontSize = 11.sp,
                                        color = priorityColor[task.priority]!!
                                    )
                                    Text(
                                        text = "· ${task.category}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            IconButton(onClick = {
                                taskToEdit = task
                                editDescription = task.description
                                editPriority = task.priority
                                editCategory = task.category
                                editRecurring = task.isRecurring
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                            }

                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

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
                    Text("Categoría:", fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("General", "Trabajo", "Estudio", "Personal", "Salud").forEach { cat ->
                            FilterChip(
                                selected = selectedNewCategory == cat,
                                onClick = { selectedNewCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF424242),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDB4035))
                        )
                        Text("Tarea recurrente")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskDescription.isNotEmpty()) {
                            viewModel.addTask(newTaskDescription, selectedPriority, selectedNewCategory, isRecurring)
                            NotificationHelper.showTaskAddedNotification(context, newTaskDescription)
                            newTaskDescription = ""
                            selectedPriority = 3
                            selectedNewCategory = "General"
                            isRecurring = false
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4035))
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }

    taskToEdit?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            title = { Text("Editar tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Prioridad:", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Alta", 2 to "Media", 3 to "Baja").forEach { (value, label) ->
                            FilterChip(
                                selected = editPriority == value,
                                onClick = { editPriority = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priorityColor[value]!!,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Text("Categoría:", fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("General", "Trabajo", "Estudio", "Personal", "Salud").forEach { cat ->
                            FilterChip(
                                selected = editCategory == cat,
                                onClick = { editCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF424242),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editRecurring,
                            onCheckedChange = { editRecurring = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDB4035))
                        )
                        Text("Tarea recurrente")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDescription.isNotEmpty()) {
                            viewModel.editTask(task, editDescription, editPriority, editCategory, editRecurring)
                            taskToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4035))
                ) {
                    Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) { Text("Cancelar") }
            }
        )
    }
}