package com.azure.android.communication.acschatuser

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.azure.android.communication.chat.ChatAsyncClient
import com.azure.android.communication.chat.ChatClientBuilder
import com.azure.android.communication.chat.ChatThreadAsyncClient
import com.azure.android.communication.chat.ChatThreadClientBuilder
import com.azure.android.communication.chat.models.*
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationUserIdentifier
import com.azure.android.core.http.policy.UserAgentPolicy
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var chatThreadAsyncClient: ChatThreadAsyncClient
    private lateinit var chatUI: ConstraintLayout
    private lateinit var setupUI: ConstraintLayout
    private lateinit var chatAsyncClient: ChatAsyncClient
    private lateinit var id: String
    private lateinit var token: String
    private lateinit var threadId: String
    private lateinit var name: String
    private lateinit var url: String
    private val applicationID = "ACS Chat Join"
    private val sdkName = "azure-communication-com.azure.android.communication.chat"
    private val sdkVersion = "1.0.0"
    private lateinit var messagesList: MessagesList
    private lateinit var messageInput: MessageInput
    private lateinit var adapter: MessagesListAdapter<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI = findViewById(R.id.userSetupUI)
        chatUI = findViewById(R.id.chatUI)
        chatUI.visibility = GONE

        findViewById<Button>(R.id.joinChat).setOnClickListener {
            id = findViewById<EditText>(R.id.acsID).text.toString()
            token = findViewById<EditText>(R.id.acsToken).text.toString()
            threadId = findViewById<EditText>(R.id.chatThreadID).text.toString()
            name = findViewById<EditText>(R.id.userName).text.toString()
            url = findViewById<EditText>(R.id.resourceURL).text.toString()

            if (id.isBlank() || token.isBlank() || threadId.isBlank() || name.isBlank() || url.isBlank()) {
                Toast.makeText(this, "Enter required information!", Toast.LENGTH_LONG).show()
            }
            setupChat()
            createChatClient()
        }
    }

    private fun setupChat() {
        adapter = MessagesListAdapter<Message>(id, null)
        messagesList = findViewById(R.id.messagesList)
        messageInput = findViewById(R.id.input)
        messagesList.setAdapter(adapter)
        messageInput.setInputListener {


            val chatMessageOptions = SendChatMessageOptions()
                .setType(ChatMessageType.TEXT)
                .setContent(it.toString())
                .setSenderDisplayName(name)
            val id = chatThreadAsyncClient.sendMessage(chatMessageOptions).get().id

            true
        }

        joinChatThread()
        setupUI.visibility = GONE
        chatUI.visibility = VISIBLE
    }

    private fun createChatClient() {
        chatAsyncClient = ChatClientBuilder()
            .endpoint(url)
            .credential(CommunicationTokenCredential(token))
            .addPolicy(
                UserAgentPolicy(
                    applicationID,
                    sdkName, sdkVersion
                )
            )
            .buildAsyncClient()
        subscribeToMessageReceived()
    }

    private fun subscribeToMessageReceived() {
        chatAsyncClient.startRealtimeNotifications(token, applicationContext)
        chatAsyncClient.addEventHandler(
            ChatEventType.CHAT_MESSAGE_RECEIVED
        ) { payload: ChatEvent? ->
            val chatMessageReceivedEvent = payload as ChatMessageReceivedEvent?
            if (chatMessageReceivedEvent != null) {
                val authorID =
                    (chatMessageReceivedEvent.sender as CommunicationUserIdentifier).id.toString()

                this@MainActivity.runOnUiThread {
                    adapter.addToStart(
                        Message(
                            chatMessageReceivedEvent.id,
                            chatMessageReceivedEvent.content.toString(),
                            Date(chatMessageReceivedEvent.createdOn.toEpochSecond()),
                            Author(
                                authorID,
                                chatMessageReceivedEvent.senderDisplayName,
                                chatMessageReceivedEvent.senderDisplayName
                            )
                        ), true
                    )
                }
            }
        }
    }

    private fun joinChatThread() {

        chatThreadAsyncClient = ChatThreadClientBuilder()
            .endpoint(url)
            .credential(CommunicationTokenCredential(token))
            .addPolicy(
                UserAgentPolicy(
                    applicationID,
                    sdkName, sdkVersion
                )
            )
            .chatThreadId(threadId)
            .buildAsyncClient()
    }
}