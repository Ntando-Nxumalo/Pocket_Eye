package com.ntando.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.expensetracker.data.entity.User
import com.ntando.expensetracker.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    val user: StateFlow<User?> = _userId.flatMapLatest { id ->
        repository.getUserById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setUserId(id: Long) {
        _userId.value = id
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            repository.insertUser(User(name = name, email = email, password = password))
        }
    }
}
