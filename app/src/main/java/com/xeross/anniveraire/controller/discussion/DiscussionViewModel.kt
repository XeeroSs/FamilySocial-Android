package com.xeross.anniveraire.controller.discussion

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.xeross.anniveraire.model.Discussion
import java.util.concurrent.Executor

class DiscussionViewModel(private val executor: Executor) : ViewModel() {

    companion object {
        const val DISCUSSION_COLLECTION = "discussions"
        const val USERS_COLLECTION = "users"
        const val MESSAGES_COLLECTION = "messages"
    }

    private val databaseInstanceDiscussion =
            FirebaseFirestore.getInstance().collection(DISCUSSION_COLLECTION)
    private val databaseInstanceUsers =
            FirebaseFirestore.getInstance().collection(USERS_COLLECTION)
    private val databaseInstanceMessages =
            FirebaseFirestore.getInstance().collection(MESSAGES_COLLECTION)

    fun getDiscussion(discussionId: String) = databaseInstanceDiscussion.document(discussionId).get()
    fun getUser(userId: String) = databaseInstanceUsers.document(userId).get()

    /*fun getDiscussions(): LiveData<List<Discussion>>? {
        databaseInstance.get().addOnCompleteListener { task ->
            task.result?.let { querySnapshot ->
                querySnapshot.documents.forEach { document ->
                    document.toObject(Discussion::class.java)?.let { discussion -> }
                }
            }
        }
        return null
    }*/

    private fun updateCountDiscussionsUser(id: String, discussionsId: ArrayList<String>?) {
        databaseInstanceUsers.document(id).update("discussionsId", discussionsId)
    }

    fun deleteDiscussion(discussionId: String) {
        databaseInstanceDiscussion.document(discussionId).delete()
        // TODO("Delete collection")
    }

    private fun updateCountDiscussionsId(id: String, discussionsId: ArrayList<String>) {
        databaseInstanceDiscussion.document(id).update("userId", discussionsId)
    }

    fun removeDiscussionAndUser(discussion: Discussion, userId: String, discussionsId: ArrayList<String>?) = executor.execute {
        discussion.usersId.remove(userId)
        discussionsId?.remove(discussion.id)
        discussionsId?.let { updateCountDiscussionsUser(userId, it) }
        updateCountDiscussionsId(discussion.id, discussion.usersId)
    }

    fun createDiscussion(discussion: Discussion, userId: String, discussionsId: ArrayList<String>?) = executor.execute {
        discussion.usersId.add(userId)
        discussionsId?.add(discussion.id)
        updateCountDiscussionsUser(userId, discussionsId)
        databaseInstanceDiscussion.document(discussion.id).set(discussion).addOnCompleteListener {
        }
    }

    fun updateDiscussionName(name:String, discussionId: String) {
        databaseInstanceDiscussion.document(discussionId).update("name", name)
    }
}