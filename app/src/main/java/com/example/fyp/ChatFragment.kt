package com.example.fyp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var chatButton: ImageButton
    private lateinit var viewType: String // "user" or "merchant"
    private var whichTab: String = "asCustomer" // "user" or "merchant"

    private lateinit var chatListContainer: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        whichTab = arguments?.getString("whichTab").toString()
        viewType = arguments?.getString("viewType")?: "user"
        viewType = viewType.toString()
        merchantName = arguments?.getString("merchantName").toString()
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
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        chatListContainer = view.findViewById(R.id.chatListContainer)

        // Get a reference to the ImageButton
        chatButton = view.findViewById(R.id.chatButton)
        updateProfileButtonAppearance()

        // navigate to other fragment at toolbar buttons
        val profileButton = view.findViewById<ImageButton>(R.id.profileButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)

        profileButton.setOnClickListener {
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        cartButton.setOnClickListener {
            val fragment = CartFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        homeButton.setOnClickListener {
            val fragment = HomeFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        val asCustomerTab: RelativeLayout = view.findViewById(R.id.asCustomerTab)
        val asMerchantTab: RelativeLayout = view.findViewById(R.id.asMerchantTab)
        val notificationTab: RelativeLayout = view.findViewById(R.id.notificationTab)


        if(whichTab == "asCustomer") {
            asCustomerTab.setBackgroundResource(R.color.white)
            asMerchantTab.setBackgroundResource(R.color.chat_grey)
            notificationTab.setBackgroundResource(R.color.chat_grey)
            whichTab = "asCustomer"
            if (viewType != "user") {
                chatListContainer.removeAllViews()
                viewType = "user"
                fetchChatList()
            }
        } else if (whichTab == "asMerchant"){
            asCustomerTab.setBackgroundResource(R.color.chat_grey)
            asMerchantTab.setBackgroundResource(R.color.white)
            notificationTab.setBackgroundResource(R.color.chat_grey)
            whichTab = "asMerchant"
            if (viewType != "merchant"){
                chatListContainer.removeAllViews()
                viewType = "merchant"
                fetchChatList()
            }
        } else if (whichTab == "notification"){
            asCustomerTab.setBackgroundResource(R.color.chat_grey)
            asMerchantTab.setBackgroundResource(R.color.chat_grey)
            notificationTab.setBackgroundResource(R.color.white)
            chatListContainer.removeAllViews()
            whichTab = "notification"
        } else{
            asCustomerTab.setBackgroundResource(R.color.white)
            asMerchantTab.setBackgroundResource(R.color.chat_grey)
            notificationTab.setBackgroundResource(R.color.chat_grey)
            whichTab = "asCustomer"
            viewType = "user"
            chatListContainer.removeAllViews()
            fetchChatList()
        }

        asCustomerTab.setOnClickListener(){
            asCustomerTab.setBackgroundResource(R.color.white)
            asMerchantTab.setBackgroundResource(R.color.chat_grey)
            notificationTab.setBackgroundResource(R.color.chat_grey)
            whichTab = "asCustomer"
            if (viewType != "user"){
                chatListContainer.removeAllViews()
                viewType = "user"
                fetchChatList()
            }
        }
        asMerchantTab.setOnClickListener(){
            asCustomerTab.setBackgroundResource(R.color.chat_grey)
            asMerchantTab.setBackgroundResource(R.color.white)
            notificationTab.setBackgroundResource(R.color.chat_grey)
            whichTab = "asMerchant"
            if (viewType != "merchant"){
                chatListContainer.removeAllViews()
                viewType = "merchant"
                fetchChatList()
            }
        }
        notificationTab.setOnClickListener(){
            asCustomerTab.setBackgroundResource(R.color.chat_grey)
            asMerchantTab.setBackgroundResource(R.color.chat_grey)
            notificationTab.setBackgroundResource(R.color.white)
            chatListContainer.removeAllViews()
            whichTab = "notification"
        }

        return view
    }

    private fun updateProfileButtonAppearance() {
        chatButton?.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.chat_icon_white)
        }
    }

    private fun resetProfileButtonAppearance() {
        chatButton?.apply {
            setBackgroundResource(android.R.color.transparent)
            setImageResource(R.drawable.chat_icon)
        }
    }

    private fun fetchChatList() {
        val query = when (viewType) {
            "user" -> db.collection("chats").whereEqualTo("user", username)
            "merchant" -> db.collection("chats").whereEqualTo("merchantName", merchantName)
            else -> {
                Log.e("ChatFragment", "Invalid viewType: $viewType. Defaulting to 'user' view.")
                db.collection("chats").whereEqualTo("user", username)
            }
        }

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ChatFragment", "Listen failed.", e)
                Toast.makeText(context, "Error fetching chats: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                chatListContainer.removeAllViews()

                snapshot.documents.forEach { doc ->
                    val chatId = doc.id
                    val otherUser = if (viewType == "user") {
                        doc.getString("merchantName")
                    } else {
                        doc.getString("user")
                    }

                    if (otherUser != null) {
                        // Check if there are any messages in this chat
                        db.collection("chats").document(chatId).collection("messages").limit(1).get()
                            .addOnSuccessListener { messagesSnapshot ->
                                if (!messagesSnapshot.isEmpty) {
                                    addChatItemToView(chatId, otherUser)
                                }
                            }
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        // Reset the profile icon when leaving the fragment
        resetProfileButtonAppearance()

        // Remove all listeners to prevent memory leaks
        for (i in 0 until chatListContainer.childCount) {
            val chatItemView = chatListContainer.getChildAt(i)
            (chatItemView.tag as? ListenerRegistration)?.remove()
        }
    }

    private fun addChatItemToView(chatId: String, otherUser: String) {
        val chatItemView = layoutInflater.inflate(R.layout.item_chat_head, chatListContainer, false)
        val chatNameTextView: TextView = chatItemView.findViewById(R.id.senderTextView)
        val messageTextView: TextView = chatItemView.findViewById(R.id.messageTextView)
        val senderImage: ImageView = chatItemView.findViewById(R.id.senderImage)

        chatNameTextView.text = otherUser

        // Fetch and display the last message
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val lastMessage = snapshot.documents[0]
                    val messageType = lastMessage.getString("type") ?: "text"
                    val messageContent = if (messageType == "image") "[Image]" else lastMessage.getString("content") ?: ""
                    messageTextView.text = messageContent
                } else {
                    messageTextView.text = "No messages yet"
                }
            }


        val unreadIndicator: View = chatItemView.findViewById(R.id.unreadIndicator)

        val unreadListener = db.collection("chats").document(chatId).collection("messages")
            .whereEqualTo("read", false)
            .whereNotEqualTo("sender", if (viewType == "user") username else merchantName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    unreadIndicator.visibility = View.VISIBLE
                } else {
                    unreadIndicator.visibility = View.GONE
                }
            }
        // Store the listener to remove it later
        chatItemView.tag = unreadListener


        chatItemView.setOnClickListener {
            if (chatId.isNotEmpty() && otherUser.isNotEmpty()) {
                openChatDetail(chatId, otherUser)
                Log.d("ChatFragment", "Chat item clicked. chatId: $chatId, otherUser: $otherUser")
            } else {
                Log.e("ChatFragment", "Invalid chatId or otherUser")
            }
        }

        // Load profile image for both users and merchants
        val profileImageQuery = if (viewType == "user") {
            db.collection("merchants").document(otherUser)
        } else {
            db.collection("users").document(otherUser)
        }

        profileImageQuery.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Glide.with(this)
                        .load(document.getString("profileImage"))
                        .circleCrop()
                        .error(R.drawable.account_icon_lightgrey)
                        .into(senderImage)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        chatListContainer.addView(chatItemView)
    }

    private fun openChatDetail(chatId: String, otherUser: String) {
        val fragment = ChatRoomFragment().apply {
            arguments = Bundle().apply {
                putString("username", username)
                putString("merchantName", merchantName)
                putString("chatId", chatId)
                putString("otherUser", otherUser)
                putString("viewType", viewType)
                putString("whichTab", whichTab)
            }
        }
//        (activity as? MainActivity)?.replaceFragment(fragment, "ChatRoomFragment")
        replaceFragment(fragment)
    }
}


