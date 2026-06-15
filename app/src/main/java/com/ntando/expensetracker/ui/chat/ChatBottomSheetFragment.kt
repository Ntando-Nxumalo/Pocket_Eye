package com.ntando.expensetracker.ui.chat

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.expensetracker.R
import com.ntando.expensetracker.data.database.DatabaseProvider
import com.ntando.expensetracker.data.repository.ExpenseRepository
import com.ntando.expensetracker.viewmodel.ChatViewModel

class ChatBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_chat_bot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("PocketEyePrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        val db = DatabaseProvider.getDatabase(requireContext())
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    return ChatViewModel(repository, db.goalDao(), currentUserId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        val rvChat = view.findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = view.findViewById<EditText>(R.id.etChatMessage)
        val btnSend = view.findViewById<FloatingActionButton>(R.id.btnSendMessage)

        adapter = ChatAdapter(emptyList())
        rvChat.layoutManager = LinearLayoutManager(context)
        rvChat.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.updateMessages(messages)
            if (messages.isNotEmpty()) {
                rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etMessage.text.clear()
            }
        }

        viewModel.sendWelcomeMessage()
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog
}
