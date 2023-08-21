package com.example.primierleaguematches


sealed class ResultWorker<out T> {
    object Loading : ResultWorker<Nothing>()
    data class Success<out T>(val data: T) : ResultWorker<T>()
    data class Error(val e: String) : ResultWorker<Nothing>()
}
