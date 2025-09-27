package com.example.fyp

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import android.widget.ProgressBar
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var username: String
    private lateinit var currentEditingPetName: String
    private var merchantName: String = ""
    private lateinit var ivPetImage: ImageView
    private lateinit var profileButton: ImageButton
    private lateinit var petSection: LinearLayout
    private lateinit var petImagesContainer: LinearLayout
    private lateinit var fileContainer: LinearLayout
    private var selectedImageUri: Uri? = null
    private val selectedFiles = MutableLiveData<List<Uri>>()
    private lateinit var currentPetFiles: MutableList<String>
    private var petProfileImage: String? = null

    private val newFiles = mutableListOf<Uri>()

    private lateinit var dialogViewAddPet: View
    private lateinit var dialogEditPet: View
    private lateinit var dialogPetDetails: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
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
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileButton = view.findViewById(R.id.profileButton)
        updateProfileButtonAppearance()

        val userImageView: ImageView = view.findViewById(R.id.user_image)
        userImageView.setCircularImage()

        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val chatButton = view.findViewById<ImageButton>(R.id.chatButton)

        homeButton.setOnClickListener {
            val fragment = HomeFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        cartButton.setOnClickListener {
            val fragment = CartFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }
        chatButton.setOnClickListener {
            val fragment = ChatFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val db = FirebaseFirestore.getInstance()

        // Assuming "username" variable holds the document name
        val docRef = db.collection("users").document(username)
        docRef.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        // Username is the document ID
                        val username = document.id
                        tvUsername.setText(username)
                        merchantName = document.getString("MerchantName").toString()
                    } else {
                        // Handle missing document with Toast
                        Toast.makeText(view.context, "User document not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle errors with Toast
                    val error = task.exception
                    val message = error?.message ?: "Error getting document"
                    Toast.makeText(view.context, message, Toast.LENGTH_SHORT).show()
                }
            }

        val ivProfileBackground = view.findViewById<ImageView>(R.id.ivProfileBackground)
        val userDocument = db.collection("users").document(username)
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val profileImageUrl = documentSnapshot.getString("profileImage")
                val backgroundImageUrl = documentSnapshot.getString("backgroundImage")
                profileImageUrl?.let { Glide.with(this).load(it).circleCrop().into(userImageView) }
                backgroundImageUrl?.let { Glide.with(this).load(it).into(ivProfileBackground) }
            } else {
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                requireContext(),
                "Failed to fetch user data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        val merchantCentreButton : TextView = view.findViewById(R.id.merchant_centre)
        merchantCentreButton.setOnClickListener {
            checkMerchantStatus()
        }

        val manageProfileButton : TextView = view.findViewById(R.id.manage_profile)
        manageProfileButton.setOnClickListener {
            val fragment = ManageProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "ManageProfileFragment")
        }

        val trackOrderButton : TextView = view.findViewById(R.id.track_order)
        trackOrderButton.setOnClickListener {
            val fragment = TrackOrderFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "TrackOrderFragment")
        }

        val appointmentButton : TextView = view.findViewById(R.id.appointment_booked)
        appointmentButton.setOnClickListener {
            val fragment = TrackAppointmentFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "TrackAppointmentFragment")
        }

        val logoutButton: ImageButton = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            val message = "Are you sure you want to log out?"
            showConfirmationDialog(requireContext(), message) {
                // Code to execute when Yes is clicked
                replaceFragment(LoginFragment())
            }
        }

        petSection = view.findViewById(R.id.pet_section_layout)
        petImagesContainer = view.findViewById(R.id.pet_images_container)
        val addPetButton: ImageButton = view.findViewById(R.id.add_pet_button)

        addPetButton.setOnClickListener {
            newFiles.clear()
            selectedFiles.value?.toMutableList()?.clear() // Clear previously selected files
            showAddPetDialog()
        }

        // Load previously added pets
        loadPets()

        return view
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

    private fun showAddPetDialog() {
        dialogViewAddPet = layoutInflater.inflate(R.layout.dialog_add_pet, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogViewAddPet)
            .create()


        ivPetImage = dialogViewAddPet.findViewById(R.id.ivPetImage)
        val btnAddPet = dialogViewAddPet.findViewById<Button>(R.id.btnAddPet)
        val closeButton = dialogViewAddPet.findViewById<ImageButton>(R.id.closeButton)

        ivPetImage.setOnClickListener {
            selectImage()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        val uploadFileButton = dialogViewAddPet.findViewById<RelativeLayout>(R.id.uploadFileButton)
        val itemsContainer = dialogViewAddPet.findViewById<LinearLayout>(R.id.itemsContainer)
        itemsContainer.removeAllViews() // Clear any existing views

        uploadFileButton.setOnClickListener {
            showFileChooser()
        }

        // Add this observer to update the file list when files are selected
        selectedFiles.observe(viewLifecycleOwner, Observer { files ->
            updateFileList(itemsContainer, mutableListOf(), mutableListOf(), files.toMutableList())
        })

        btnAddPet.setOnClickListener {
            val petNameBox = dialogViewAddPet.findViewById<EditText>(R.id.etPetName)
            val petName = petNameBox.text.toString().trim()
            val petBreed = dialogViewAddPet.findViewById<EditText>(R.id.etPetBreed).text.toString()
            val petGender = dialogViewAddPet.findViewById<EditText>(R.id.etPetGender).text.toString()
            val petAge = dialogViewAddPet.findViewById<EditText>(R.id.etPetAge).text.toString()
            val petColor = dialogViewAddPet.findViewById<EditText>(R.id.etPetColor).text.toString()
            val petDOB = dialogViewAddPet.findViewById<EditText>(R.id.etPetDOB).text.toString()
            val petSpecies = dialogViewAddPet.findViewById<EditText>(R.id.etPetSpecies).text.toString()
            val petWeight = dialogViewAddPet.findViewById<EditText>(R.id.etPetWeight).text.toString()

            isPetNameUnique(petName) { isUnique ->
                if (isUnique) {
                    uploadPetWithFiles(petName, petBreed, petGender, petAge, petColor, petDOB, petSpecies, petWeight, selectedFiles.value ?: emptyList())
                    dialog.dismiss()
                } else {
                    petNameBox.error = "Pet name already exists"
                }
            }
        }

        dialog.show()
    }

    private fun uploadPetWithFiles(petName: String, petBreed: String, petGender: String, petAge: String, petColor: String,
                                   petDOB: String, petSpecies: String, petWeight: String, files: List<Uri>) {
        val storageRef = FirebaseStorage.getInstance().reference
        val db = FirebaseFirestore.getInstance()
        val petDocRef = db.collection("users").document(username).collection("pets").document(petName)
        val mutableFiles = files.toMutableList()


        // Upload pet image first
        val imageUploadTask = selectedImageUri?.let { uri ->
            val imageRef = storageRef.child("pet_images/$petName/${UUID.randomUUID()}")
            imageRef.putFile(uri).continueWithTask { it.result.storage.downloadUrl }
        }

        // Then upload pet data and files
        (imageUploadTask ?: Tasks.forResult(null)).continueWithTask { imageUrlTask ->
            val imageUrl = imageUrlTask.result?.toString()

            val petData = hashMapOf(
                "petName" to petName,
                "petBreed" to petBreed,
                "petGender" to petGender,
                "petAge" to petAge,
                "petColor" to petColor,
                "petDOB" to petDOB,
                "petImageUrl" to imageUrl,
                "petSpecies" to petSpecies,
                "petWeight" to petWeight
            )

            petDocRef.set(petData)
        }.continueWithTask {
            // Upload files
            val fileUploadTasks = mutableFiles.map { uri ->
                val fileRef = storageRef.child("pet_files/$petName/${UUID.randomUUID()}_${getFileName(uri)}")
                fileRef.putFile(uri).continueWithTask { it.result.storage.downloadUrl }
            }

            Tasks.whenAllSuccess<Uri>(fileUploadTasks)
        }.addOnSuccessListener { fileUrls ->
            // Add file references to Firestore
            val filesCollection = petDocRef.collection("files")
            val fileBatch = db.batch()

            fileUrls.forEachIndexed { index, uri ->
                val fileData = hashMapOf(
                    "url" to uri.toString(),
                    "name" to getFileName(files[index])
                )
                fileBatch.set(filesCollection.document(), fileData)
            }

            fileBatch.commit()
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Pet and files added successfully", Toast.LENGTH_SHORT).show()
            loadPets()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun showFileChooser() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "image/*",
                    "text/plain",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
            }
            startActivityForResult(intent, PICK_FILE)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening file chooser: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (resultCode == RESULT_OK && data != null) {
                when (requestCode) {
                    PICK_IMAGE_REQUEST -> {
                        selectedImageUri = data.data!!
                        ivPetImage.setImageURI(selectedImageUri)
                        ivPetImage.setBackgroundResource(R.drawable.circular_border)
                        ivPetImage.setCircularImage()
                        Glide.with(requireContext()).load(selectedImageUri).into(ivPetImage)
                    }
                    PICK_FILE -> {
                        if (data.clipData != null) {
                            for (i in 0 until data.clipData!!.itemCount) {
                                newFiles.add(data.clipData!!.getItemAt(i).uri)
                            }
                        } else {
                            data.data?.let { newFiles.add(it) }
                        }
                        selectedFiles.value = newFiles
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error processing selected files: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isPetNameUnique(newPetName: String, currentPetName: String = "", callback: (Boolean) -> Unit) {
        FirebaseFirestore.getInstance().collection("users").document(username).collection("pets")
            .get()
            .addOnSuccessListener { documents ->
                var isUnique = true
                for (document in documents) {
                    val petName = document.id
                    if (petName == newPetName && petName != currentPetName) {
                        isUnique = false
                        break
                    }
                }
                callback(isUnique)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check pet names", Toast.LENGTH_SHORT).show()
                callback(false)  // Assume it's not unique in case of failure
            }
    }

    private fun addNewPet(petName: String, petBreed: String, petGender: String, petAge: String, petColor: String, petDOB: String, imageUrl: String?, petSpecies: String, petWeight: String, fileUrls: List<String>, addToFirestore: Boolean) {

            val newPetImage = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx()).apply {
                    setMargins(10.dpToPx(), 0, 0, 0)
                }
                if (imageUrl == null) {
                    setImageResource(R.drawable.temp_pet_icon)
                } else {
                    Glide.with(requireContext()).load(imageUrl).into(this)
                }
                setBackgroundResource(R.drawable.circular_border)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setCircularImage()
                setOnClickListener {
                    showPetDetailsDialog(petName, petBreed, petGender, petAge, petColor, petDOB, petSpecies, petWeight, imageUrl)
                }
            }
            petImagesContainer.addView(newPetImage)
    //        petImagesContainer.addView(addButton)

            if (addToFirestore) {
                val petData = hashMapOf(
                    "petProfileImage" to petProfileImage,
                    "petName" to petName,
                    "petBreed" to petBreed,
                    "petGender" to petGender,
                    "petAge" to petAge,
                    "petColor" to petColor,
                    "petDOB" to petDOB,
                    "petImageUrl" to imageUrl,
                    "petSpecies" to petSpecies,
                    "petWeight" to petWeight,
                    "petFileUrls" to fileUrls
                )

                FirebaseFirestore.getInstance().collection("users").document(username).collection("pets").document(petName).set(petData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Pet added successfully", Toast.LENGTH_SHORT).show()
                        selectedImageUri = null
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to add pet", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun showPetDetailsDialog(petName: String, petBreed: String, petGender: String, petAge: String, petColor: String, petDOB: String, petSpecies: String, petWeight: String, imageUrl: String?) {
        dialogPetDetails = layoutInflater.inflate(R.layout.dialog_pet_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogPetDetails)
            .setNegativeButton("Close", null)
            .create()

        dialogPetDetails.findViewById<TextView>(R.id.petTitle).text = petName
        dialogPetDetails.findViewById<TextView>(R.id.tvPetBreed).text = "Pet breed: $petBreed"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetGender).text = "Pet gender: $petGender"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetAge).text = "Pet age: $petAge"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetColor).text = "Color: $petColor"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetDOB).text = "Date Of Birth: $petDOB"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetSpecies).text = "Pet Species: $petSpecies"
        dialogPetDetails.findViewById<TextView>(R.id.tvPetWeight).text = "Pet Weight: $petWeight"

        val petImageView = dialogPetDetails.findViewById<ImageView>(R.id.ivPetImage)
        petImageView.setBackgroundResource(R.drawable.circular_border)
        petImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        petImageView.setCircularImage()
        imageUrl?.let { Glide.with(this).load(it).into(petImageView) }

        // Display uploaded files
        val itemsContainer = dialogPetDetails.findViewById<LinearLayout>(R.id.itemsContainer)
        itemsContainer.removeAllViews()

        FirebaseFirestore.getInstance().collection("users").document(username).collection("pets").document(petName)
            .collection("files").get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot) {
                        val fileUrl = document.getString("url") ?: continue
                        val fileName = document.getString("name") ?: continue

                        val fileItemView = layoutInflater.inflate(R.layout.item_file, itemsContainer, false)
                        val tvFileName = fileItemView.findViewById<TextView>(R.id.tvFileName)
                        val btnRemoveFile = fileItemView.findViewById<ImageButton>(R.id.btnRemoveFile)

                        tvFileName.text = fileName
                        btnRemoveFile.visibility = View.GONE

                        fileItemView.setOnClickListener {
                            showFilePreviewDialog(fileUrl)
                        }

                        itemsContainer.addView(fileItemView)
                    }
                } else {
                    val tvNoFiles = dialogPetDetails.findViewById<TextView>(R.id.tvNoFiles)
                    tvNoFiles.visibility = View.VISIBLE
                    itemsContainer.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        dialog.show()

        val editPetButton = dialogPetDetails.findViewById<TextView>(R.id.petTitle) // Make sure you have an appropriate ID in your XML
        editPetButton.setOnClickListener {
            newFiles.clear()
            selectedFiles.value?.toMutableList()?.clear() // Clear previously selected files
            showEditPetDialog(petName, petBreed, petGender, petAge, petColor, petDOB, petSpecies, petWeight, imageUrl)
            dialog.dismiss()
        }
    }

//    private fun showFilePreviewDialog(fileUrl: String) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_file_preview, null)
//        val dialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .create()
//
//        val webView = dialogView.findViewById<WebView>(R.id.webViewFilePreview)
//        val btnDownload = dialogView.findViewById<Button>(R.id.btnDownload)
//
//        webView.settings.javaScriptEnabled = true
//        webView.loadUrl(fileUrl)
//
//        btnDownload.setOnClickListener {
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
//            startActivity(intent)
//        }
//
//        dialog.show()
//    }

    private fun showEditPetDialog(petName: String, petBreed: String, petGender: String, petAge: String, petColor: String, petDOB: String, petSpecies: String, petWeight: String, imageUrl: String?) {
        dialogEditPet = layoutInflater.inflate(R.layout.dialog_edit_pet, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogEditPet).create()
        currentEditingPetName = petName

        dialogEditPet.findViewById<EditText>(R.id.etPetName).setText(petName)
        dialogEditPet.findViewById<EditText>(R.id.etPetBreed).setText(petBreed)
        dialogEditPet.findViewById<EditText>(R.id.etPetGender).setText(petGender)
        dialogEditPet.findViewById<EditText>(R.id.etPetAge).setText(petAge)
        dialogEditPet.findViewById<EditText>(R.id.etPetColor).setText(petColor)
        dialogEditPet.findViewById<EditText>(R.id.etPetDOB).setText(petDOB)
        dialogEditPet.findViewById<EditText>(R.id.etPetSpecies).setText(petSpecies)
        dialogEditPet.findViewById<EditText>(R.id.etPetWeight).setText(petWeight)

        val editPetImageView = dialogEditPet.findViewById<ImageView>(R.id.ivPetImage)
        editPetImageView.setBackgroundResource(R.drawable.circular_border)
        editPetImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        editPetImageView.setCircularImage()
        imageUrl?.let { Glide.with(this).load(it).into(editPetImageView) }
        dialog.show()

        //for file section
        val itemsContainer = dialogEditPet.findViewById<LinearLayout>(R.id.itemsContainer)
        var currentFiles = mutableListOf<FileItem>()
        val filesToRemove = mutableListOf<String>()
        val newFiles = mutableListOf<Uri>()

        // Load existing files
        loadExistingFiles(petName, itemsContainer) { currentFiles2 ->
            updateFileList(itemsContainer, currentFiles2, filesToRemove, newFiles)
            currentFiles = currentFiles2.toMutableList()
        }

        val uploadFileButton = dialogEditPet.findViewById<RelativeLayout>(R.id.uploadFileButton)
        uploadFileButton.setOnClickListener {
            showFileChooser()
        }

        selectedFiles.observe(viewLifecycleOwner) { files ->
            newFiles.clear()
            newFiles.addAll(files)
            updateFileList(itemsContainer, currentFiles, filesToRemove, newFiles)
        }


        var newPetNameString : String = ""
        val confirmBtn : Button = dialogEditPet.findViewById(R.id.btnEditPet)
        confirmBtn.setOnClickListener {
            // Handle save action
            val newPetName = dialogEditPet.findViewById<EditText>(R.id.etPetName)
            newPetNameString = newPetName.text.toString()
            val newPetBreed = dialogEditPet.findViewById<EditText>(R.id.etPetBreed).text.toString()
            val newPetGender = dialogEditPet.findViewById<EditText>(R.id.etPetGender).text.toString()
            val newPetAge = dialogEditPet.findViewById<EditText>(R.id.etPetAge).text.toString()
            val newPetColor = dialogEditPet.findViewById<EditText>(R.id.etPetColor).text.toString()
            val newPetDOB = dialogEditPet.findViewById<EditText>(R.id.etPetDOB).text.toString()
            val newPetSpecies = dialogEditPet.findViewById<EditText>(R.id.etPetSpecies).text.toString()
            val newPetWeight = dialogEditPet.findViewById<EditText>(R.id.etPetWeight).text.toString()


            isPetNameUnique(newPetNameString, petName) { isUnique ->
                if (isUnique) {
                    // Update pet details in the database and UI
                    updatePetDetailsWithFiles(
                        petName,
                        newPetNameString,
                        newPetBreed,
                        newPetGender,
                        newPetAge,
                        newPetColor,
                        newPetDOB,
                        imageUrl,
                        newPetSpecies,
                        newPetWeight,
                        filesToRemove,
                        newFiles
                    )
                    dialog.dismiss()
                } else {
                    newPetName.error = "Pet name already exists"
                }
            }
        }

        val deleteBtn : Button = dialogEditPet.findViewById(R.id.btnDeletePet)
        deleteBtn.setOnClickListener {
            // Handle delete action
            if(newPetNameString == ""){
                deletePet(petName, imageUrl)
            } else{
                deletePet(newPetNameString, imageUrl)
            }
            dialog.dismiss()
        }

        val closeButton = dialogEditPet.findViewById<ImageButton>(R.id.closeButton)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun loadExistingFiles(petName: String, itemsContainer: LinearLayout, callback: (List<FileItem>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(username).collection("pets").document(petName).collection("files")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val currentFiles = mutableListOf<FileItem>()
                for (document in querySnapshot) {
                    val fileUrl = document.getString("url") ?: continue
                    val fileName = document.getString("name") ?: continue
                    currentFiles.add(FileItem(fileUrl, fileName))
                }
                callback(currentFiles)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading existing files: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePetDetailsWithFiles(oldPetName: String, newPetName: String, newPetBreed: String, newPetGender: String, newPetAge: String, newPetColor: String, newPetDOB: String, imageUrl: String?, newPetSpecies: String, newPetWeight: String, filesToRemove: List<String>, newFilesHere: List<Uri>) {
        val db = FirebaseFirestore.getInstance()
        val storageRef = FirebaseStorage.getInstance().reference
        val petDocRef = db.collection("users").document(username).collection("pets").document(oldPetName)

        db.runTransaction { transaction ->
            // Update pet details
            val petData = hashMapOf(
                "petName" to newPetName,
                "petBreed" to newPetBreed,
                "petGender" to newPetGender,
                "petAge" to newPetAge,
                "petColor" to newPetColor,
                "petDOB" to newPetDOB,
                "petImageUrl" to imageUrl,
                "petSpecies" to newPetSpecies,
                "petWeight" to newPetWeight
            )
            transaction.update(petDocRef, petData as Map<String, Any>)

            // Remove files
            val filesCollection = petDocRef.collection("files")
            filesToRemove.forEach { fileUrl ->
                filesCollection.whereEqualTo("url", fileUrl).get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        document.reference.delete()
                        // Also delete from storage
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                        storageRef.delete()
                    }
                }
            }

            // Add new files
            newFilesHere.forEach { uri ->
                val fileRef = storageRef.child("pet_files/$newPetName/${UUID.randomUUID()}_${getFileName(uri)}")
                fileRef.putFile(uri).continueWithTask { it.result.storage.downloadUrl }
                    .addOnSuccessListener { downloadUri ->
                        val fileData = hashMapOf(
                            "url" to downloadUri.toString(),
                            "name" to getFileName(uri)
                        )
                        filesCollection.add(fileData)
                    }
            }
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Pet details and files updated", Toast.LENGTH_SHORT).show()
            if(oldPetName != newPetName) {
                changePetDocumentId(oldPetName, newPetName)
            }
            loadPets()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to update pet details and files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFileList(itemsContainer: LinearLayout, currentFiles: List<FileItem>, filesToRemove: MutableList<String>, newFiles: MutableList<Uri>) {
        itemsContainer.removeAllViews()

        // Add existing files (not marked for removal)
        for (file in currentFiles) {
            if (!filesToRemove.contains(file.url)) {
                addFileItemToContainer(currentFiles, filesToRemove, itemsContainer, file.name, file.url, true) {
                    filesToRemove.add(file.url)
                    updateFileList(itemsContainer, currentFiles, filesToRemove, newFiles)
                }
            }
        }

        // Add new files
        for (fileUri in newFiles.toList()) { // Use toList() to avoid concurrent modification
            addFileItemToContainer(currentFiles, filesToRemove, itemsContainer, getFileName(fileUri), fileUri.toString(), true) {
                newFiles.remove(fileUri)
                updateFileList(itemsContainer, currentFiles, filesToRemove, newFiles)
            }
        }
    }

    private fun addFileItemToContainer(currentFiles: List<FileItem>, filesToRemove: MutableList<String>, itemsContainer: LinearLayout, fileName: String, fileUrl: String, isRemovable: Boolean = false, onRemove: () -> Unit = {}) {
        val fileItemView = layoutInflater.inflate(R.layout.item_file, itemsContainer, false)
        val tvFileName = fileItemView.findViewById<TextView>(R.id.tvFileName)
        val btnRemoveFile = fileItemView.findViewById<ImageButton>(R.id.btnRemoveFile)

        tvFileName.text = fileName

        if (isRemovable) {
            btnRemoveFile.visibility = View.VISIBLE
            btnRemoveFile.setOnClickListener {
                removeFile(Uri.parse(fileUrl), newFiles, filesToRemove, currentFiles)
                onRemove()
            }
        } else {
            btnRemoveFile.visibility = View.GONE
        }

        tvFileName.setOnClickListener {
            showFilePreviewDialog(fileUrl)
        }

        itemsContainer.addView(fileItemView)
    }

    private fun showFilePreviewDialog(fileUrl: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_preview, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewPreview)
        val webView = dialogView.findViewById<WebView>(R.id.webViewFilePreview)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        webView.visibility = View.GONE

        when {
            fileUrl.contains(".jpg", ignoreCase = true) ||
                    fileUrl.contains(".jpeg", ignoreCase = true) ||
                    fileUrl.contains(".png", ignoreCase = true) ||
                    fileUrl.contains(".gif", ignoreCase = true) -> {
                Toast.makeText(requireContext(), "fileUrl: ${fileUrl}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                // Handle image files
                Glide.with(requireContext())
                    .load(fileUrl)
                    .error(R.drawable.temp_pet_icon)
                    .into(imageView)
            }
            fileUrl.contains(".pdf", ignoreCase = true) -> {
                // Handle PDF files
                webView.visibility = View.VISIBLE
                webView.settings.javaScriptEnabled = true
                webView.loadUrl("https://docs.google.com/gview?embedded=true&url=$fileUrl")
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        progressBar.visibility = View.GONE
                    }
                }
            }
            else -> {
                // Handle other file types or provide a download option
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Unsupported file type. You can download the file instead.", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }

    private fun removeFile(fileUri: Uri, newFiles: MutableList<Uri>, filesToRemove: MutableList<String>, currentFiles: List<FileItem>) {
        if (fileUri.toString().startsWith("http")) {
            // This is an existing file
            filesToRemove.add(fileUri.toString())
        } else {
            // This is a new file
            newFiles.remove(fileUri)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown file"
    }

    private fun removeFileFromPet(fileUrl: String) {
        val petDocRef = FirebaseFirestore.getInstance().collection("users").document(username)
            .collection("pets").document(currentEditingPetName)

        petDocRef.get().addOnSuccessListener { document ->
            val fileUrls = document.get("petFileUrls") as? MutableList<String> ?: mutableListOf()
            fileUrls.remove(fileUrl)

            petDocRef.update("petFileUrls", fileUrls)
                .addOnSuccessListener {
                    Toast.makeText(context, "File removed successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error removing file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updatePetDetails(oldPetName: String, newPetName: String, newPetBreed: String, newPetGender: String, newPetAge: String, newPetColor: String, newPetDOB: String, imageUrl: String?, newPetSpecies: String, newPetWeight: String) {
        // Update the pet details in Firestore and the local UI
        val petData = hashMapOf(
            "petName" to newPetName,
            "petBreed" to newPetBreed,
            "petGender" to newPetGender,
            "petAge" to newPetAge,
            "petColor" to newPetColor,
            "petDOB" to newPetDOB,
            "petImageUrl" to imageUrl,
            "newPetSpecies" to newPetSpecies,
            "newPetWeight" to newPetWeight
        )


        val db = FirebaseFirestore.getInstance()
        val petDocument = db.collection("users").document(username).collection("pets").document(oldPetName)
        petDocument.update(petData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pet details updated", Toast.LENGTH_SHORT).show()
                // Update the UI as necessary
                if(oldPetName != newPetName){
                    changePetDocumentId(oldPetName, newPetName)
                }

                loadPets() // Reload the pets to reflect the changes
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update pet details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePet(petName: String, imageUrl: String?) {
        try {
            Toast.makeText(requireContext(), "Deleting pet...", Toast.LENGTH_SHORT).show()

            val db = FirebaseFirestore.getInstance()
            val userDocument = db.collection("users").document(username)
            val petsCollection = userDocument.collection("pets")

            // Find the document with the given pet name (this assumes pet names are unique)
            petsCollection.whereEqualTo("petName", petName).get().addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "Pet not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val petDocument = querySnapshot.documents[0]

                // Delete the pet document
                petDocument.reference.delete().addOnSuccessListener {

                    // Delete the pet image from Firebase Storage if it exists
                    imageUrl?.let { url ->
                        try {
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                            storageReference.delete().addOnSuccessListener {
                                Toast.makeText(requireContext(), "Pet deleted from Firestore", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Failed to delete pet image: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error processing image URL: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }

                    // Reload pets after successful deletion
                    loadPets()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to delete pet: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error finding pet document: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun changePetDocumentId(oldDocumentId: String, newDocumentId: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocument = db.collection("users").document(username)
        val petsCollection = userDocument.collection("pets")

        // Get the data from the old document
        petsCollection.document(oldDocumentId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val petData = documentSnapshot.data

                // Create a new document with the new ID and set the data
                petData?.let {
                    petsCollection.document(newDocumentId).set(it).addOnSuccessListener {
                        // Delete the old document
                        petsCollection.document(oldDocumentId).delete().addOnSuccessListener {
                            Toast.makeText(requireContext(), "Document ID changed successfully", Toast.LENGTH_SHORT).show()
                            loadPets() // Reload pets to reflect changes
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to delete old document: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun loadPets() {
        val db = FirebaseFirestore.getInstance()
        petImagesContainer.removeAllViews() // Clear existing views to avoid duplication
        db.collection("users").document(username).collection("pets").get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val petName = document.getString("petName") ?: ""
                val petBreed = document.getString("petBreed") ?: ""
                val petGender = document.getString("petGender") ?: ""
                val petAge = document.getString("petAge") ?: ""
                val petColor = document.getString("petColor") ?: ""
                val petDOB = document.getString("petDOB") ?: ""
                val imageUrl = document.getString("petImageUrl") ?: ""
                val petSpecies = document.getString("petSpecies") ?: ""
                val petWeight = document.getString("petWeight") ?: ""

                // Retrieve the list of file URLs
                val fileUrlStrings = document.get("petFileUrls") as? List<String> ?: listOf()

                // Convert the list of URL strings to a MutableList<Uri>
                val fileUrlList = fileUrlStrings.mapTo(mutableListOf()) { (it) }

                addNewPet(petName, petBreed, petGender, petAge, petColor, petDOB, imageUrl, petSpecies, petWeight, fileUrlList, addToFirestore = false)
            }
        }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(requireContext(), "Failed to load pets", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkMerchantStatus() {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(username)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Retrieve isMerchant as a string
                    val isMerchantString = document.getString("isMerchant") ?: "false"
                    // Convert the string to a boolean
                    val isMerchant = isMerchantString.toBoolean()

                    if (isMerchant) {
                        val fragment = MerchantFragment()
                        val bundle = Bundle()
                        bundle.putString("username", username)
                        bundle.putString("merchantName", merchantName)
                        fragment.arguments = bundle
                        replaceFragment(fragment)
                    } else {
                        val fragment = MerchantRequestFragment()
                        val bundle = Bundle()
                        bundle.putString("username", username)
                        bundle.putString("merchantName", merchantName)
                        fragment.arguments = bundle
                        replaceFragment(fragment)
                    }
                } else {
                    // Handle case where document does not exist
                    Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Handle any errors
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfileButtonAppearance() {
        profileButton.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.account_icon_white)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    companion object {
        internal const val PICK_IMAGE_REQUEST = 1 // Request code for image selection
        private val PICK_FILE = 2
    }
}
