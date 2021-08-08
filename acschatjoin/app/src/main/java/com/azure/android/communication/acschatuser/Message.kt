package com.azure.android.communication.acschatuser

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*


class Message(
    private val id: String,
    private val text: String,
    private val createdAt: Date,
    private val author: IUser?
) : IMessage {
    override fun getId(): String {
        return id
    }

    override fun getText(): String {
        return text
    }

    override fun getUser(): IUser? {
        return author
    }

    override fun getCreatedAt(): Date {
        return createdAt
    }
}