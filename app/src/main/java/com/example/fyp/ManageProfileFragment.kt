package com.example.fyp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ManageProfileFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var profileButton: ImageButton
    private lateinit var tvUsername: EditText
    private lateinit var userImageView: ImageView
    private lateinit var backgroundImageView: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedProfileImageUri: Uri? = null
    private var selectedBackgroundImageUri: Uri? = null
    private var userProfileImage: String? = null
    private var backgroundProfileImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun updateProfileButtonAppearance() {
        profileButton.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.account_icon_white)
        }
    }

    private fun ImageView.setCircularImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = object : ViewOutlineProvider() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_profile, container, false)

        profileButton = view.findViewById(R.id.profileButton)
        updateProfileButtonAppearance()

        userImageView = view.findViewById(R.id.user_image)
        userImageView.setCircularImage()
        userImageView.setOnClickListener {
            selectImage(PICK_PROFILE_IMAGE_REQUEST)
        }

        backgroundImageView = view.findViewById(R.id.ivProfileBackground)
        backgroundImageView.setOnClickListener {
            selectImage(PICK_BACKGROUND_IMAGE_REQUEST)
        }

        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val chatButton = view.findViewById<ImageButton>(R.id.chatButton)
        val profileButton = view.findViewById<ImageButton>(R.id.profileButton)

        homeButton.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
            val fragment = HomeFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        cartButton.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
            val fragment = CartFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        chatButton.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
            val fragment = ChatFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        profileButton.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        val backBtn: TextView = view.findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
        }

        tvUsername = view.findViewById(R.id.tvUsername)
        fetchUserProfile()

        val confirmBtn: TextView = view.findViewById(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
//            Toast.makeText(requireContext(), "Updating data...", Toast.LENGTH_SHORT).show()
            val newUsername = tvUsername.text.toString().trim()
            if (newUsername != username) {
                db.collection("users").document(newUsername).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        tvUsername.error = "Username already exists. Please choose another name."
                    } else {
                        db.collection("merchants").document(newUsername).get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                tvUsername.error =
                                    "Username already exists. Please choose another name."
                            } else {
                                Toast.makeText(requireContext(), "Updating data...", Toast.LENGTH_SHORT).show()
                                changeUserDocumentId(username, newUsername) {
                                    username = newUsername
                                    uploadImagesToFirestore {
                                        navigateToProfileFragment(username)
                                    }
                                }
                            }
                        }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to check username availability: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                uploadImagesToFirestore {
                    navigateToProfileFragment(username)
                }
            }
        }

        val manage_address: TextView = view.findViewById(R.id.manage_address)
        manage_address.setOnClickListener{
            val fragment = AddressFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "AddressFragment")
        }

        val changePasswordBtn: TextView = view.findViewById(R.id.change_password_button)
        changePasswordBtn.setOnClickListener {
            val message = "Are you sure you want to change your password?"
            showConfirmationDialog(requireContext(), message) {
                sendPasswordResetEmail(auth.currentUser?.email.toString())
            }
        }

        return view
    }

    private fun fetchUserProfile() {
        val userDocument = db.collection("users").document(username)
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                tvUsername.setText(username)
                val profileImageUrl = documentSnapshot.getString("profileImage")
                val backgroundImageUrl = documentSnapshot.getString("backgroundImage")
                profileImageUrl?.let { Glide.with(this).load(it)
                    .circleCrop().into(userImageView) }
                backgroundImageUrl?.let { Glide.with(this).load(it).into(backgroundImageView) }
            } else {
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectImage(requestCode: Int) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            when (requestCode) {
                PICK_PROFILE_IMAGE_REQUEST -> {
                    selectedProfileImageUri = data.data!!
                    userImageView.setImageURI(selectedProfileImageUri)
                    userImageView.setCircularImage()
                    Glide.with(requireContext()).load(selectedProfileImageUri)
                        .circleCrop().into(userImageView)
                }
                PICK_BACKGROUND_IMAGE_REQUEST -> {
                    selectedBackgroundImageUri = data.data!!
                    backgroundImageView.setImageURI(selectedBackgroundImageUri)
                    Glide.with(requireContext()).load(selectedBackgroundImageUri).into(backgroundImageView)
                }
            }
        }
    }

    private fun uploadImagesToFirestore(onComplete: () -> Unit) {
        val userDocument = db.collection("users").document(username)
        val updates = mutableMapOf<String, Any>()

        val profileImageUploadTask = selectedProfileImageUri?.let { uri ->
            uploadImageToStorage(uri, "profileImages/$username") { url ->
                if (url != null) {
                    updates["profileImage"] = url
                    userProfileImage = url
                }
            }
        }

        val backgroundImageUploadTask = selectedBackgroundImageUri?.let { uri ->
            uploadImageToStorage(uri, "backgroundImages/$username") { url ->
                if (url != null) {
                    updates["backgroundImage"] = url
                    backgroundProfileImage = url
                }
            }
        }

        profileImageUploadTask?.addOnCompleteListener { profileTask ->
            if (profileTask.isSuccessful) {
                backgroundImageUploadTask?.addOnCompleteListener { backgroundTask ->
                    if (backgroundTask.isSuccessful) {
                        userDocument.update(updates).addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    userDocument.update(updates).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            backgroundImageUploadTask?.addOnCompleteListener { backgroundTask ->
                if (backgroundTask.isSuccessful) {
                    userDocument.update(updates).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                onComplete()
            }
        }
    }

    private fun uploadImageToStorage(uri: Uri, path: String, callback: (String?) -> Unit): Task<Uri> {
        val storageRef = FirebaseStorage.getInstance().reference.child(path)
        val uploadTask = storageRef.putFile(uri)

        return uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(task.result.toString())
            } else {
                Log.w("ManageProfileFragment", "Error uploading image: ${task.exception?.message}")
                callback(null)
            }
        }
    }

    // Function to show confirmation dialog
    fun showConfirmationDialog(context: Context, message: String, onPositiveClick: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirmation")
        builder.setMessage(message)

        // Set positive button (Yes)
        builder.setPositiveButton("Yes") { dialog, which ->
            onPositiveClick() // Call the provided function when Yes is clicked
            dialog.dismiss()
        }

        // Set negative button (No) with optional action
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss() // Dismiss the dialog when No is clicked
        }

        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
            } else {
                Log.w("LoginFragment", "Error sending password reset email: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun changeUserDocumentId(oldDocumentId: String, newDocumentId: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        // List of known subcollections for a user document
        val knownSubcollections = listOf("wishlist", "cart", "pets", "addresses")

        usersCollection.document(oldDocumentId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data

                userData?.let { data ->
                    usersCollection.document(newDocumentId).set(data).addOnSuccessListener {
                        copySubCollections(oldDocumentId, newDocumentId) { success ->
                            if (success) {
                                updateUsernameInCollections(oldDocumentId, newDocumentId) { updateSuccess ->
                                    if (updateSuccess) {
                                        deleteDocumentWithSubcollections(usersCollection.document(oldDocumentId), knownSubcollections) { deleteSuccess ->
                                            if (deleteSuccess) {
                                                Toast.makeText(requireContext(), "Document ID changed successfully", Toast.LENGTH_SHORT).show()
                                                onComplete()
                                            } else {
                                                Toast.makeText(requireContext(), "Failed to delete old document and its subcollections", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to update username in some collections", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to copy sub-collections", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to create new document: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Old document not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to get old document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUsernameInCollections(oldUsername: String, newUsername: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("pendingOrders", "shippingOrders", "completeOrders", "pendingServices", "upcomingServices", "completeServices")

        var updateCount = 0
        var totalUpdateOperations = collections.size + 1 // +1 for the chats collection

        fun checkCompletion() {
            if (updateCount == totalUpdateOperations) {
                callback(true)
            }
        }

        // Update regular collections
        collections.forEach { collectionName ->
            db.collection(collectionName)
                .whereEqualTo("username", oldUsername)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = db.batch()
                    for (document in querySnapshot.documents) {
                        batch.update(document.reference, "username", newUsername)
                    }
                    batch.commit().addOnSuccessListener {
                        updateCount++
                        checkCompletion()
                    }.addOnFailureListener {
                        Log.e("UpdateUsername", "Failed to update $collectionName", it)
                        callback(false)
                    }
                }
                .addOnFailureListener {
                    Log.e("UpdateUsername", "Failed to query $collectionName", it)
                    callback(false)
                }
        }

        // Update chats collection
        db.collection("chats")
            .whereEqualTo("user", oldUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "user", newUsername)

                    // Update sender field in messages subcollection
                    document.reference.collection("messages")
                        .whereEqualTo("sender", oldUsername)
                        .get()
                        .addOnSuccessListener { messagesSnapshot ->
                            for (messageDoc in messagesSnapshot.documents) {
                                batch.update(messageDoc.reference, "sender", newUsername)
                            }

                            batch.commit().addOnSuccessListener {
                                updateCount++
                                checkCompletion()
                            }.addOnFailureListener {
                                Log.e("UpdateUsername", "Failed to update chat messages", it)
                                callback(false)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("UpdateUsername", "Failed to query chat messages", it)
                            callback(false)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("UpdateUsername", "Failed to query chats", it)
                callback(false)
            }
    }

    private fun copySubCollections(oldDocumentId: String, newDocumentId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        val collectionsToCopy = listOf("wishlist", "cart", "pets", "addresses")
        var completedCollections = 0

        fun checkCompletion() {
            completedCollections++
            if (completedCollections == collectionsToCopy.size) {
                callback(true)
            }
        }

        collectionsToCopy.forEach { collectionName ->
            val oldCollection = usersCollection.document(oldDocumentId).collection(collectionName)
            val newCollection = usersCollection.document(newDocumentId).collection(collectionName)

            oldCollection.get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    checkCompletion()
                    return@addOnSuccessListener
                }

                var completedDocuments = 0
                for (document in snapshot.documents) {
                    newCollection.document(document.id).set(document.data!!).addOnSuccessListener {
                        if (collectionName == "pets") {
                            copyPetFiles(oldDocumentId, newDocumentId, document.id) {
                                completedDocuments++
                                if (completedDocuments == snapshot.size()) {
                                    checkCompletion()
                                }
                            }
                        } else {
                            completedDocuments++
                            if (completedDocuments == snapshot.size()) {
                                checkCompletion()
                            }
                        }
                    }.addOnFailureListener {
                        Log.e("CopySubCollections", "Failed to copy document in $collectionName", it)
                        callback(false)
                    }
                }
            }.addOnFailureListener {
                Log.e("CopySubCollections", "Failed to get documents from $collectionName", it)
                callback(false)
            }
        }
    }

    private fun copyPetFiles(oldUserId: String, newUserId: String, petId: String, callback: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val oldFilesCollection = db.collection("users").document(oldUserId).collection("pets").document(petId).collection("files")
        val newFilesCollection = db.collection("users").document(newUserId).collection("pets").document(petId).collection("files")

        oldFilesCollection.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                callback()
                return@addOnSuccessListener
            }

            var completedFiles = 0
            for (fileDocument in snapshot.documents) {
                newFilesCollection.document(fileDocument.id).set(fileDocument.data!!).addOnSuccessListener {
                    completedFiles++
                    if (completedFiles == snapshot.size()) {
                        callback()
                    }
                }.addOnFailureListener {
                    Log.e("CopyPetFiles", "Failed to copy file document", it)
                    callback()
                }
            }
        }.addOnFailureListener {
            Log.e("CopyPetFiles", "Failed to get files", it)
            callback()
        }
    }

    private fun deleteDocumentWithSubcollections(docRef: DocumentReference, knownSubcollections: List<String>, onComplete: (Boolean) -> Unit) {
        fun deleteSubcollections(index: Int) {
            if (index >= knownSubcollections.size) {
                // All subcollections processed, now delete the main document
                docRef.delete().addOnSuccessListener {
                    onComplete(true)
                }.addOnFailureListener {
                    Log.e("DeleteDocument", "Failed to delete main document", it)
                    onComplete(false)
                }
                return
            }

            val collectionRef = docRef.collection(knownSubcollections[index])
            collectionRef.get().addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Move to next subcollection
                    deleteSubcollections(index + 1)
                } else {
                    var deletedDocs = 0
                    for (document in querySnapshot.documents) {
                        if (knownSubcollections[index] == "pets") {
                            // For pets, we know there's a "files" subcollection
                            deleteDocumentWithSubcollections(document.reference, listOf("files")) { success ->
                                if (success) {
                                    deletedDocs++
                                    if (deletedDocs == querySnapshot.size()) {
                                        // All pet documents deleted, move to next main subcollection
                                        deleteSubcollections(index + 1)
                                    }
                                } else {
                                    onComplete(false)
                                }
                            }
                        } else {
                            document.reference.delete().addOnSuccessListener {
                                deletedDocs++
                                if (deletedDocs == querySnapshot.size()) {
                                    // All documents in this subcollection deleted, move to next subcollection
                                    deleteSubcollections(index + 1)
                                }
                            }.addOnFailureListener {
                                Log.e("DeleteDocument", "Failed to delete document in subcollection", it)
                                onComplete(false)
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e("DeleteDocument", "Failed to get documents in subcollection", it)
                onComplete(false)
            }
        }

        deleteSubcollections(0)
    }

    private fun navigateToProfileFragment(username: String) {
        (activity as? MainActivity)?.popBackStack()
        val fragment = ProfileFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        fragment.arguments = bundle
        replaceFragment(fragment)
    }

    companion object {
        internal const val PICK_PROFILE_IMAGE_REQUEST = 1 // Request code for profile image selection
        internal const val PICK_BACKGROUND_IMAGE_REQUEST = 2 // Request code for background image selection
    }
}
