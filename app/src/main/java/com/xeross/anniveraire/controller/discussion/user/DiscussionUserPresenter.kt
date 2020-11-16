package com.xeross.anniveraire.controller.discussion.user

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.xeross.anniveraire.R
import com.xeross.anniveraire.listener.UserContract
import com.xeross.anniveraire.model.Discussion
import com.xeross.anniveraire.model.User
import com.xeross.anniveraire.utils.Constants.DISCUSSION_COLLECTION
import com.xeross.anniveraire.utils.Constants.USERS_COLLECTION

class DiscussionUserPresenter(private val contract: UserContract.View) :
        UserContract.Presenter {

    private val databaseDiscussionInstance =
            FirebaseFirestore.getInstance().collection(DISCUSSION_COLLECTION)
    private val databaseUsersInstance =
            FirebaseFirestore.getInstance().collection(USERS_COLLECTION)

    private fun getUsers() = databaseUsersInstance
    private fun getDocumentUser(id: String) = databaseUsersInstance.document(id).get()
    private fun getDocumentDiscussion(id: String) = databaseDiscussionInstance.document(id).get()

    private fun updateDiscussionIdsFromUser(id: String, discussionsId: ArrayList<String>) {
        databaseUsersInstance.document(id).update("discussionsId", discussionsId)
    }

    private fun updateUserIdsFromDiscussion(id: String, usersId: ArrayList<String>) {
        databaseDiscussionInstance.document(id).update("usersId", usersId)
    }

    private fun updateDiscussionsRequestUser(id: String, discussionsRequestId: ArrayList<String>) {
        databaseUsersInstance.document(id).update("discussionsRequestId", discussionsRequestId)
    }

    override fun getObjectsFromUser(id: String) {
        contract.setList()
        val users = ArrayList<User>()
        getDocumentDiscussion(id).addOnCompleteListener { taskDiscussion ->
            taskDiscussion.result?.toObject(Discussion::class.java)?.let { discussion ->
                discussion.usersId.forEach { uId ->
                    getDocumentUser(uId).addOnCompleteListener { taskUser ->
                        taskUser.result?.toObject(User::class.java)?.let { user ->
                            if (!users.contains(user)) {
                                users.add(user)
                                contract.getUsersFromObject(users)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun removeUser(userId: String, id: String) {
        contract.setList()
        getDocumentUser(userId).addOnCompleteListener { taskUser ->
            taskUser.result?.toObject(User::class.java)?.let { user ->
                getDocumentDiscussion(id).addOnCompleteListener { taskDiscussion ->
                    taskDiscussion.result?.toObject(Discussion::class.java)?.let { discussion ->
                        val discussionIds = user.discussionsId
                        val userIds = discussion.usersId
                        userIds.remove(user.id)
                        discussionIds.remove(id)
                        updateDiscussionIdsFromUser(user.id, discussionIds)
                        updateUserIdsFromDiscussion(id, userIds)
                        contract.getUsers()
                    }
                }
            }
        }
    }

    override fun longClick(id: String, userId: String, targetId: String) {
        getDocumentDiscussion(id).addOnCompleteListener { td ->
            td.result?.toObject(Discussion::class.java)?.let { discussion ->
                discussion.ownerId.takeIf { it != "" }?.let { uId ->
                    if (uId == userId) {
                        if (targetId == userId) return@addOnCompleteListener
                        contract.showPopupConfirmSuppress(targetId)
                    }
                }
            }
        }
    }

    override fun sendRequestByEmail(id: String, email: String, alertDialog: BottomSheetDialog) {
        getUsers().whereEqualTo("email", email).get().addOnSuccessListener {
            it.documents.forEach { d ->
                d.toObject(User::class.java)?.let { u ->
                    val discussionsRequestId = u.discussionsRequestId
                    if (discussionsRequestId.contains(id)) {
                        contract.sendToast(R.string.requests_already_sent)
                        return@addOnSuccessListener
                    }
                    discussionsRequestId.add(id)
                    updateDiscussionsRequestUser(u.id, discussionsRequestId)
                    contract.sendToast(R.string.request_sent)
                    alertDialog.dismiss()
                    return@addOnSuccessListener
                }
            }
            contract.sendToast(R.string.error_email_not_found)
        }.addOnFailureListener {
            contract.sendToast(R.string.error_email_not_found)
        }
    }

    override fun isOwnerUser(id: String, userId: String) {
        getDocumentDiscussion(id).addOnCompleteListener { task ->
            task.result?.toObject(Discussion::class.java)?.let { discussion ->
                discussion.ownerId.takeIf { it != "" }?.let { uId ->
                    if (uId == userId) {
                        contract.showPopupAddUser()
                        return@addOnCompleteListener
                    }
                }
            }
            contract.sendToast(R.string.you_cannot_add_anyone)
        }
    }
}
