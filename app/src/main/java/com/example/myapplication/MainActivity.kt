package com.example.myapplication
import androidx.compose.foundation.BorderStroke
import android.os.Bundle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.initialize
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Firebase.initialize(this)
        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 35.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                ProgressTrackerApp()
            }
        }
    }







    enum class Screen {
        WELCOME,
        PROJECT_NAME,
        TASK_LIST
    }

    data class Task(
        var id: Int = 0,
        var name: String = "",
        var progressStatus: ProgressStatus = ProgressStatus.NOT_STARTED,
        var dateAdded: String = SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(Date())
    ) {
        // Required for Firebase
        @Exclude
        fun toMap(): Map<String, Any?> {
            return mapOf(
                "id" to id,
                "name" to name,
                "progressStatus" to progressStatus.name,
                "dateAdded" to dateAdded
            )
        }
    }

    data class Project(
        val id: Int = 0,
        val name: String = "",
        val tasks: List<Task> = listOf(),
        val dateCreated: String = SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(Date())
    ) {

        @Exclude
        fun toMap(): Map<String, Any?> {
            return mapOf(
                "id" to id,
                "name" to name,
                "tasks" to tasks.map { it.toMap() },
                "dateCreated" to dateCreated
            )
        }
    }

    enum class ProgressStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED;

        companion object {
            fun fromString(value: String): ProgressStatus {
                return try {
                    valueOf(value)
                } catch (e: IllegalArgumentException) {
                    NOT_STARTED
                }
            }
        }
    }

    class FirebaseRepository {
        private val database = FirebaseDatabase.getInstance()
        private val projectsRef = database.getReference("projects")

        fun observeProjects(onProjects: (List<Project>) -> Unit) {
            projectsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val projects = snapshot.children.mapNotNull { snapShot ->
                        try {
                            val map = snapShot.value as? Map<*, *> ?: return@mapNotNull null

                            val tasks = (map["tasks"] as? List<*>)?.mapNotNull { taskMap ->
                                (taskMap as? Map<*, *>)?.let { task ->
                                    Task(
                                        id = (task["id"] as? Long)?.toInt() ?: 0,
                                        name = task["name"] as? String ?: "",
                                        progressStatus = ProgressStatus.fromString(
                                            task["progressStatus"] as? String ?: ""
                                        ),
                                        dateAdded = task["dateAdded"] as? String ?: ""
                                    )
                                }
                            } ?: listOf()

                            Project(
                                id = (map["id"] as? Long)?.toInt() ?: 0,
                                name = map["name"] as? String ?: "",
                                tasks = tasks,
                                dateCreated = map["dateCreated"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            println("Error parsing project: ${e.message}")
                            null
                        }
                    }
                    onProjects(projects)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error: ${error.message}")
                }
            })
        }

        suspend fun addProject(project: Project) {
            projectsRef.child(project.id.toString()).setValue(project.toMap()).await()
        }

        suspend fun updateProject(project: Project) {
            projectsRef.child(project.id.toString()).setValue(project.toMap()).await()
        }

        suspend fun deleteProject(projectId: Int) {
            projectsRef.child(projectId.toString()).removeValue().await()
        }
    }

    class ProjectViewModel {
        private val repository = FirebaseRepository()
        var projects by mutableStateOf<List<Project>>(emptyList())
            private set

        init {
            repository.observeProjects { projectsList ->
                projects = projectsList
            }
        }

        suspend fun addProject(name: String) {
            val newProject = Project(
                id = (projects.maxOfOrNull { it.id } ?: 0) + 1,
                name = name
            )
            repository.addProject(newProject)
        }

        suspend fun updateProject(project: Project) {
            repository.updateProject(project)
        }

        suspend fun deleteProject(projectId: Int) {
            repository.deleteProject(projectId)
        }
    }

    @Composable
    fun ProgressTrackerApp() {
        val scope = rememberCoroutineScope()
        val viewModel = remember { ProjectViewModel() }
        var currentScreen by remember { mutableStateOf(Screen.WELCOME) }
        var showDialog by remember { mutableStateOf(false) }
        var taskName by remember { mutableStateOf("") }
        var projectName by remember { mutableStateOf("") }
        var selectedProjectId by remember { mutableStateOf<Int?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.WELCOME -> {
                    WelcomeScreen(
                        onNext = { currentScreen = Screen.PROJECT_NAME }
                    )
                }

                Screen.PROJECT_NAME -> {
                    ProjectNameScreen(
                        projectName = projectName,
                        projects = viewModel.projects,
                        onProjectSelect = { project ->
                            projectName = project.name
                            selectedProjectId = project.id
                            currentScreen = Screen.TASK_LIST
                        },
                        onProjectNameSet = { name ->
                            scope.launch {
                                viewModel.addProject(name)
                                projectName = name
                                selectedProjectId = viewModel.projects.maxByOrNull { it.id }?.id
                                currentScreen = Screen.TASK_LIST
                            }
                        },
                        onBack = { currentScreen = Screen.WELCOME },
                        onDeleteProject = { projectId ->
                            scope.launch {
                                viewModel.deleteProject(projectId)
                            }
                        }
                    )
                }

                Screen.TASK_LIST -> {
                    val currentProject = viewModel.projects.find { it.id == selectedProjectId }
                    if (currentProject != null) {
                        TaskListScreen(
                            project = currentProject,
                            onProjectUpdate = { updatedProject ->
                                scope.launch {
                                    viewModel.updateProject(updatedProject)
                                }
                            },
                            showDialog = showDialog,
                            onShowDialogChange = { showDialog = it },
                            taskName = taskName,
                            onTaskNameChange = { taskName = it },
                            onBack = {
                                currentScreen = Screen.PROJECT_NAME
                                projectName = ""
                                selectedProjectId = null
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun WelcomeScreen(onNext: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(title = "Progress Tracker")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Track your project tasks easily",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = onNext,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Get Started")
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ProjectNameScreen(
        projectName: String,
        projects: List<Project>,
        onProjectSelect: (Project) -> Unit,
        onProjectNameSet: (String) -> Unit,
        onBack: () -> Unit,
        onDeleteProject: (Int) -> Unit  // New parameter
    ) {
        var tempProjectName by remember { mutableStateOf(projectName) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var projectToDelete by remember { mutableStateOf<Project?>(null) }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                title = "Enter Project Name",
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tempProjectName,
                    onValueChange = { tempProjectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                )

                Button(
                    onClick = {
                        if (tempProjectName.isNotBlank()) {
                            onProjectNameSet(tempProjectName)
                            tempProjectName = ""
                        }
                    },
                    enabled = tempProjectName.isNotBlank()
                ) {
                    Text("Create Project")
                }
            }

            if (projects.isNotEmpty()) {
                Text(
                    text = "Previous Projects",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(projects) { project ->
                        ProjectHistoryItem(
                            project = project,
                            onClick = { onProjectSelect(project) },
                            onDeleteClick = {
                                projectToDelete = project
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (showDeleteConfirmation && projectToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Project") },
                text = { Text("Are you sure you want to delete '${projectToDelete?.name}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            projectToDelete?.id?.let { onDeleteProject(it) }
                            showDeleteConfirmation = false
                            projectToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    fun TopBar(
        title: String,
        onBack: (() -> Unit)? = null
    ) {
        TopBarContent(
            title = {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onBack = onBack
        )
    }

    @Composable
    fun TopBarWithContent(
        title: @Composable () -> Unit,
        onBack: (() -> Unit)? = null
    ) {
        TopBarContent(title = title, onBack = onBack)
    }

    @Composable
    private fun TopBarContent(
        title: @Composable () -> Unit,
        onBack: (() -> Unit)? = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                title()
            }

            Spacer(modifier = Modifier.width(48.dp))
        }
    }

    @Composable
    fun TaskListScreen(
        project: Project,
        onProjectUpdate: (Project) -> Unit,
        showDialog: Boolean,
        onShowDialogChange: (Boolean) -> Unit,
        taskName: String,
        onTaskNameChange: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var isEditingName by remember { mutableStateOf(false) }
        var editedName by remember { mutableStateOf(project.name) }

        val completedTasks = project.tasks.count { it.progressStatus == ProgressStatus.COMPLETED }
        val progressPercentage = if (project.tasks.isNotEmpty()) {
            (completedTasks.toFloat() / project.tasks.size) * 100
        } else 0f

        Column(modifier = Modifier.fillMaxSize()) {
            TopBarWithContent(
                title = {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            singleLine = true,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth(0.8f),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (editedName.isNotBlank()) {
                                            onProjectUpdate(project.copy(name = editedName))
                                            isEditingName = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = project.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isEditingName = true }
                                .padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                onBack = onBack
            )

            Text(
                text = "Progress: ${progressPercentage.toInt()}%",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LinearProgressIndicator(
                progress = progressPercentage / 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Button(
                onClick = { onShowDialogChange(true) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Add Task")
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(project.tasks) { task ->
                    TaskListItem(
                        task = task,
                        onProgressUpdateClick = { newStatus ->
                            val updatedTasks = project.tasks.map {
                                if (it.id == task.id) it.copy(progressStatus = newStatus) else it
                            }
                            onProjectUpdate(project.copy(tasks = updatedTasks))
                        },
                        onDeleteClick = {
                            val updatedTasks = project.tasks - task
                            onProjectUpdate(project.copy(tasks = updatedTasks))
                        }
                    )
                }
            }
        }

        if (showDialog) {
            AddTaskDialog(
                taskName = taskName,
                onTaskNameChange = onTaskNameChange,
                projectName = project.name,
                onDismiss = { onShowDialogChange(false) },
                onConfirm = {
                    if (taskName.isNotBlank()) {
                        val newTask = Task(
                            id = (project.tasks.maxOfOrNull { it.id } ?: 0) + 1,
                            name = taskName
                        )
                        onProjectUpdate(project.copy(tasks = project.tasks + newTask))
                        onTaskNameChange("")
                        onShowDialogChange(false)
                    }
                }
            )
        }
    }

    @Composable
    fun ProjectHistoryItem(
        project: Project,
        onClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                ) {
                    Text(
                        text = project.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Created on: ${project.dateCreated}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Tasks: ${project.tasks.size}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Project",
                        tint = Color.Red
                    )
                }
            }
        }
        Divider(modifier = Modifier.padding(horizontal = 8.dp))
    }


    @Composable
    fun TaskListItem(
        task: Task,
        onProgressUpdateClick: (ProgressStatus) -> Unit,
        onDeleteClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    border = BorderStroke(2.dp, Color.Cyan),
                    shape = RoundedCornerShape(20)
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )

                Text(
                    text = "Added on: ${task.dateAdded}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Text(
                    text = "Status: ${task.progressStatus.name.replace("_", " ")}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = { onProgressUpdateClick(ProgressStatus.NOT_STARTED) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Not Started")
                }
                IconButton(onClick = { onProgressUpdateClick(ProgressStatus.IN_PROGRESS) }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "In Progress")
                }
                IconButton(onClick = { onProgressUpdateClick(ProgressStatus.COMPLETED) }) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Completed")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    @Composable
    fun AddTaskDialog(
        taskName: String,
        onTaskNameChange: (String) -> Unit,
        projectName: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onConfirm,
                        enabled = taskName.isNotBlank()
                    ) {
                        Text(text = "Add")
                    }
                    Button(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                }
            },
            title = { Text(text = "Add Task to $projectName") },
            text = {
                Column {
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = onTaskNameChange,
                        label = { Text("Task Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                    if (taskName.isBlank()) {
                        Text(
                            text = "Task name cannot be empty",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        )
    }
}