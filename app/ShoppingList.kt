package com.example.myshoppinglistapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.text.SimpleDateFormat
import java.util.Date

data class Task(
    var id: Int,
    var name: String,
    var progressStatus: ProgressStatus = ProgressStatus.NOT_STARTED,
    var dateAdded: String = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date()) // New date field
)



enum class ProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Composable
fun ProgressTrackerApp() {

    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var showDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var isProjectNameSet by remember { mutableStateOf(false) }


    val completedTasks = tasks.count { it.progressStatus == ProgressStatus.COMPLETED }
    val progressPercentage = if (tasks.isNotEmpty()) (completedTasks.toFloat() / tasks.size) * 100 else 0f

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isProjectNameSet) {

            ProjectNameInput(
                projectName = projectName,
                onProjectNameSet = { name ->
                    projectName = name
                    isProjectNameSet = true
                }
            )
        } else {
            // Display project name above the progress bar
            Text(
                text = projectName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )

            Text(text = "Progress: ${progressPercentage.toInt()}%", modifier = Modifier.padding(16.dp))
            LinearProgressIndicator(progress = progressPercentage / 100, modifier = Modifier.fillMaxWidth().padding(16.dp))


            Button(onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = "Add Task")
            }

            // Task list (using LazyColumn for scrollable list)
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(tasks) { task ->
                    TaskListItem(
                        task = task,
                        onProgressUpdateClick = { newStatus ->
                            tasks = tasks.map {
                                if (it.id == task.id) it.copy(progressStatus = newStatus) else it
                            }
                        },
                        onDeleteClick = {
                            tasks = tasks - task
                        }
                    )
                }
            }
        }
    }

    // Dialog for adding a new task
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        if (taskName.isNotBlank()) {
                            val newTask = Task(id = tasks.size + 1, name = taskName)
                            tasks = tasks + newTask
                            taskName = ""
                            showDialog = false
                        }
                    }) {
                        Text(text = "Add")
                    }
                    Button(onClick = { showDialog = false }) {
                        Text(text = "Cancel")
                    }
                }
            },
            title = { Text(text = "Add Task to $projectName") },
            text = {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        )
    }
}

// Composable to take Project Name input
@Composable
fun ProjectNameInput(
    projectName: String,
    onProjectNameSet: (String) -> Unit
) {
    var tempProjectName by remember { mutableStateOf(projectName) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Enter Project Name", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = tempProjectName,
            onValueChange = { tempProjectName = it },
            label = { Text("Project Name") },
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        )

        Button(onClick = {
            if (tempProjectName.isNotBlank()) {
                onProjectNameSet(tempProjectName)
            }
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "Confirm")
        }
    }
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
        // Task details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
            // Display the date the task was added
            Text(
                text = "Added on: ${task.dateAdded}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
            // Display current task progress status
            Text(
                text = "Status: ${task.progressStatus.name.replace("_", " ")}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Buttons to change progress status and delete task
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
