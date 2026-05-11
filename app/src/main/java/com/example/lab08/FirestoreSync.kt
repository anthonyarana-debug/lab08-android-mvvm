package com.example.lab08

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirestoreSync {

    private val db = Firebase.firestore
    private val tasksCollection = db.collection("tasks")

    fun uploadTask(task: Task) {
        val taskMap = hashMapOf(
            "id" to task.id,
            "description" to task.description,
            "isCompleted" to task.isCompleted,
            "priority" to task.priority,
            "category" to task.category,
            "isRecurring" to task.isRecurring
        )
        tasksCollection.document(task.id.toString()).set(taskMap)
    }

    fun deleteTask(task: Task) {
        tasksCollection.document(task.id.toString()).delete()
    }

    fun syncAllTasks(tasks: List<Task>) {
        tasks.forEach { uploadTask(it) }
    }
}