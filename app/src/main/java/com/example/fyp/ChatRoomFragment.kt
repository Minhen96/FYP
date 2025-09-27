package com.example.fyp

import android.app.Activity
import android.app.Dialog
import android.app.Notification
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firestore.v1.DocumentChange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomFragment : Fragment() {

    private lateinit var username: String
    private lateinit var whichTab: String
    private var merchantName: String = ""
    private lateinit var chatButton: ImageButton
    private lateinit var viewType: String // "user" or "merchant"

    private lateinit var chatId: String
    private lateinit var otherUser: String
    private lateinit var chatContainer: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var scrollView: ScrollView
    private val db = FirebaseFirestore.getInstance()

    private lateinit var imagePreview: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var imagePreviewCloseButton: ImageView
    private var draftMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        whichTab = arguments?.getString("whichTab").toString()
        merchantName = arguments?.getString("merchantName").toString()
        otherUser = arguments?.getString("otherUser").toString()
        chatId = arguments?.getString("chatId") ?: ""
        viewType = arguments?.getString("viewType").toString()
    }

    fun replaceFragment(fragment: Fragment){
        if(fragment != null){
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainer, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat_room, container, false)

        // Get a reference to the ImageButton
        chatButton = view.findViewById(R.id.chatButton)
        updateProfileButtonAppearance()

        // navigate to other fragment at toolbar buttons
        val profileButton = view.findViewById<ImageButton>(R.id.profileButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)

        profileButton.setOnClickListener {
//            (activity as? MainActivity)?.popBackStack()
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        cartButton.setOnClickListener {
//            (activity as? MainActivity)?.popBackStack()
            val fragment = CartFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        homeButton.setOnClickListener {
//            (activity as? MainActivity)?.popBackStack()
            val fragment = HomeFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        chatContainer = view.findViewById(R.id.chatContainer)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        scrollView = view.findViewById(R.id.scrollView)


        loadDraft()
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveDraft(s.toString())
            }
        })

        val titleTextView: TextView = view.findViewById(R.id.sellerName)
        val fullText = "Chat with $otherUser"
        titleTextView.text = truncateText(fullText, 25)

        val sellerNotExist: TextView = view.findViewById(R.id.sellerNotExist)
        val textPlace: LinearLayout = view.findViewById(R.id.textPlace)

        val senderImage: ImageView = view.findViewById(R.id.sellerImage)
        if(viewType == "user") {
            db.collection("merchants").document(otherUser).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        sellerNotExist.visibility = View.GONE
                        textPlace.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(document.getString("profileImage"))
                            .circleCrop()
                            .error(R.drawable.account_icon_lightgrey)
                            .into(senderImage)
                    } else {
                        Toast.makeText(requireContext(), "Merchant not exist", Toast.LENGTH_SHORT)
                            .show()
                        sellerNotExist.visibility = View.VISIBLE
                        textPlace.visibility = View.GONE
                        //TODO tell user and remove this chat
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else{
            db.collection("users").document(otherUser).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        sellerNotExist.visibility = View.GONE
                        textPlace.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(document.getString("profileImage"))
                            .circleCrop()
                            .error(R.drawable.account_icon_lightgrey)
                            .into(senderImage)
                    } else {
                        Toast.makeText(requireContext(), "User not exist", Toast.LENGTH_SHORT).show()
                        //TODO tell user and remove this chat
                        sellerNotExist.visibility = View.VISIBLE
                        textPlace.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }

        val back = view.findViewById<ImageView>(R.id.backButton)
        back.setOnClickListener {
            val fragment = ChatFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            bundle.putString("whichTab", whichTab)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        imagePreview = view.findViewById(R.id.imagePreview)
        imagePreviewCloseButton = view.findViewById(R.id.imagePreviewCloseButton)

        imagePreviewCloseButton.setOnClickListener {
            removePickedImage()
        }

        val imageButton: ImageView = view.findViewById(R.id.imageButton)
        imageButton.setOnClickListener {
            openImagePicker()
        }

        sendButton.setOnClickListener {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri!!)
                imagePreview.visibility = View.GONE
                messageInput.hint = "Type a message"
            } else {
                sendMessage()
            }
        }

        listenForMessages()

        return view
    }

    fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 3) + "..."
        } else {
            text
        }
    }

    override fun onPause() {
        super.onPause()
        draftMessage = messageInput.text.toString() // Save the draft when the fragment is paused
    }

    override fun onResume() {
        super.onResume()
        markMessagesAsRead()
//        scrollToBottom()
    }

    private fun markMessagesAsRead() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("read", false)
            .whereNotEqualTo("sender", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    document.reference.update("read", true)
                }
            }
    }

    private fun listenForMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatRoomFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                var newMessagesAdded = false

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val message = change.document.toObject(Message::class.java)
                        addMessageToView(message)

                        // Mark message as read if it's not from the current user
                        if (message.sender != username) {
                            change.document.reference.update("read", true)
                        }
                    }
                    scrollToBottom()
                }
            }
    }

    private fun scrollToBottom() {
        scrollView.post {
            chatContainer.requestLayout()
            if (chatContainer.childCount > 0) {
                val lastChild = chatContainer.getChildAt(chatContainer.childCount - 1)
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, lastChild.bottom)
                }, 300)
            }
        }
    }

    private val PICK_IMAGE_REQUEST = 1

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun removePickedImage() {
        selectedImageUri = null
        imagePreview.setImageURI(null)
        imagePreview.visibility = View.GONE
        imagePreviewCloseButton.visibility = View.GONE
        messageInput.isEnabled = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imagePreview.setImageURI(selectedImageUri)
            imagePreview.visibility = View.VISIBLE
            imagePreviewCloseButton.visibility = View.VISIBLE
            messageInput.isEnabled = false // Disable the input field
            messageInput.hint = "Send the image"
        }
    }

    private fun uploadImage(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("chat_images/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    sendImageMessage(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendImageMessage(imageUrl: String) {
        val message = hashMapOf(
            "sender" to username,
            "content" to imageUrl,
            "timestamp" to Date(),
            "type" to "image",
            "read" to false
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                selectedImageUri = null
                scrollToBottom()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to send image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessage() {
        if (selectedImageUri != null) {
            uploadImage(selectedImageUri!!)
            imagePreview.visibility = View.GONE
            messageInput.hint = "Type a message"
            selectedImageUri = null
            messageInput.isEnabled = true // Re-enable the input field
        } else {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val message = hashMapOf(
                    "sender" to username,
                    "content" to messageText,
                    "timestamp" to Date(),
                    "type" to "text",
                    "read" to false
                )

                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        messageInput.text.clear()
                        scrollToBottom()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Failed to send message: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun addMessageToView(message: Message) {
        val messageView = layoutInflater.inflate(
            if (message.sender == username) R.layout.item_message_sent else R.layout.item_message_received,
            chatContainer,
            false
        )

        val messageTextView: TextView = messageView.findViewById(R.id.messageTextView)
        val timeTextView: TextView = messageView.findViewById(R.id.timeTextView)
        val messageImageView: ImageView = messageView.findViewById(R.id.messageImageView)

        timeTextView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)

        if (message.type == "image") {
            messageTextView.visibility = View.GONE
            messageImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(message.content)
                .into(messageImageView)

            messageImageView.setOnClickListener {
                openFullScreenImage(message.content)
            }
        } else {
            messageTextView.visibility = View.VISIBLE
            messageImageView.visibility = View.GONE
            messageTextView.text = message.content
        }

        chatContainer.addView(messageView)
    }

    private fun openFullScreenImage(imageUrl: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(requireContext())
        Glide.with(this).load(imageUrl).into(imageView)
        dialog.setContentView(imageView)
        dialog.show()

        imageView.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun loadDraft() {
        db.collection("chatDrafts").document(chatId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val draft = document.getString("draft") ?: ""
                    messageInput.setText(draft)
                }
            }
            .addOnFailureListener { e ->
                Log.w("ChatRoomFragment", "Error loading draft", e)
            }
    }

    private fun saveDraft(draft: String) {
        db.collection("chatDrafts").document(chatId)
            .set(hashMapOf("draft" to draft))
            .addOnFailureListener { e ->
                Log.w("ChatRoomFragment", "Error saving draft", e)
            }
    }

    private fun updateProfileButtonAppearance() {
        chatButton?.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.chat_icon_white)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveDraft(messageInput.text.toString())
        // Reset the profile icon when leaving the fragment
        resetProfileButtonAppearance()
    }

    private fun resetProfileButtonAppearance() {
        chatButton?.apply {
            setBackgroundResource(android.R.color.transparent)
            setImageResource(R.drawable.chat_icon)
        }
    }

}
