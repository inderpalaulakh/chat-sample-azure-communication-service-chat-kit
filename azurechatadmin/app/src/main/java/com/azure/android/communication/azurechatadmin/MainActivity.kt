package com.azure.android.communication.azurechatadmin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    private lateinit var chatAsyncClient: ChatAsyncClient
    private lateinit var chatThreadAsyncClient: ChatThreadAsyncClient
    private lateinit var chatUI: ConstraintLayout
    private lateinit var setupUI: ConstraintLayout
    private lateinit var userID: String
    private lateinit var token: String
    private lateinit var threadId: String
    private lateinit var name: String
    private lateinit var url: String
    private lateinit var participants: String
    private val applicationID = "ACS Chat"
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
        chatUI.visibility = View.INVISIBLE
        messagesList = findViewById(R.id.messagesList)
        messageInput = findViewById(R.id.input)

        findViewById<Button>(R.id.startChat).setOnClickListener {
            setupUI.visibility = View.INVISIBLE
            chatUI.visibility = View.VISIBLE
            userID = findViewById<EditText>(R.id.acsID).text.toString()
            token = findViewById<EditText>(R.id.acsToken).text.toString()
            name = findViewById<EditText>(R.id.userName).text.toString()
            url = findViewById<EditText>(R.id.resourceURL).text.toString()
            participants = findViewById<EditText>(R.id.usersID).text.toString()

            if (userID.isBlank() || token.isBlank() || name.isBlank() || url.isBlank()) {
                Toast.makeText(this, "Enter required information!", Toast.LENGTH_LONG).show()
            }

            setupUI.visibility = View.GONE
            chatUI.visibility = View.VISIBLE
            adapter = MessagesListAdapter<Message>(userID, null)
            messagesList.setAdapter(adapter)
            createChatClient()
            sendMessageSetup()
        }
    }

    private fun sendMessageSetup() {
        messageInput.setInputListener {
            val chatMessageOptions = SendChatMessageOptions()
                .setType(ChatMessageType.TEXT)
                .setContent(it.toString())
                .setSenderDisplayName(name)
             chatThreadAsyncClient.sendMessage(chatMessageOptions).get().id
            true
        }
    }

    private fun createChatThread() {
        val participantList: MutableList<ChatParticipant> = mutableListOf()
        val localParticipant = ChatParticipant()
        localParticipant.displayName = name
        localParticipant.communicationIdentifier = CommunicationUserIdentifier(userID)
        participantList.add(localParticipant)

        val list = participants.split(",")
        list.forEach {
            val participant = ChatParticipant()
            participant.communicationIdentifier = CommunicationUserIdentifier(it)
            participantList.add(participant)
        }

        val topic = "ACS Chat"
        val createChatThreadOptions = CreateChatThreadOptions()
        createChatThreadOptions.topic = topic
        createChatThreadOptions.participants = participantList

        val createChatThreadResult: CreateChatThreadResult =
            chatAsyncClient.createChatThread(createChatThreadOptions).get()

        val chatThreadProperties: ChatThreadProperties =
            createChatThreadResult.chatThreadProperties

        threadId = chatThreadProperties.id

        Log.d("ChatThreadID - ", threadId)
    }

    private fun createChatThreadClient() {
        chatThreadAsyncClient = ChatThreadClientBuilder()
            .endpoint(url)
            .credential(CommunicationTokenCredential(token))
            .addPolicy(UserAgentPolicy(applicationID, sdkName, sdkVersion))
            .chatThreadId(threadId)
            .buildAsyncClient()
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
        createChatThread()
        createChatThreadClient()
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
}