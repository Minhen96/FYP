package com.example.fyp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.security.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class CheckAppointmentFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var profileButton: ImageButton

    private lateinit var itemsContainer: LinearLayout
    private lateinit var status: String

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        db = FirebaseFirestore.getInstance()

        checkUnconfirmedServices()
        checkExpiredServices()
        checkUnpaidConfirmedServices()
        checkDelayedPayments()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_check_appointment, container, false)

        profileButton = view.findViewById(R.id.profileButton)
        updateProfileButtonAppearance()

        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val chatButton = view.findViewById<ImageButton>(R.id.chatButton)

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

        val pendingTab = view.findViewById<RelativeLayout>(R.id.pending)
        val upcomingTab = view.findViewById<RelativeLayout>(R.id.upcoming)
        val completeTab = view.findViewById<RelativeLayout>(R.id.completed)
        val pendingTitle = view.findViewById<TextView>(R.id.pendingTitle)
        val upcomingTitle = view.findViewById<TextView>(R.id.upcomingTitle)
        val completeTitle = view.findViewById<TextView>(R.id.completedTitle)
        val pendingLine = view.findViewById<View>(R.id.pendingline)
        val upcomingLine = view.findViewById<View>(R.id.upcomingline)
        val completeLine = view.findViewById<View>(R.id.completedline)

        val unsuccessTab = view.findViewById<RelativeLayout>(R.id.unsuccess)
        val unsuccessTitle = view.findViewById<TextView>(R.id.unsuccessTitle)
        val unsuccessLine = view.findViewById<View>(R.id.unsuccessline)

        itemsContainer = view.findViewById(R.id.itemsContainer)

        pendingTab.setOnClickListener {
            status = "pending"
            pendingLine.visibility = View.VISIBLE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingServices")
        }
        pendingTitle.setOnClickListener {
            status = "pending"
            pendingLine.visibility = View.VISIBLE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingServices")
        }

        upcomingTab.setOnClickListener {
            status = "upcoming"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.VISIBLE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("upcomingServices")
        }
        upcomingTitle.setOnClickListener {
            status = "upcoming"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.VISIBLE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("upcomingServices")
        }

        completeTab.setOnClickListener {
            status = "complete"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.VISIBLE
            unsuccessLine.visibility = View.GONE
            loadOrders("completeServices")
        }
        completeTitle.setOnClickListener {
            status = "complete"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.VISIBLE
            unsuccessLine.visibility = View.GONE
            loadOrders("completeServices")
        }

        unsuccessTab.setOnClickListener {
            status = "unsuccessful"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.VISIBLE
            loadUnsuccessfulItems()
        }
        unsuccessTitle.setOnClickListener {
            status = "unsuccessful"
            pendingLine.visibility = View.GONE
            upcomingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.VISIBLE
            loadUnsuccessfulItems()
        }

        // Set default to Pending
        status = "pending"
        pendingLine.visibility = View.VISIBLE
        upcomingLine.visibility = View.GONE
        completeLine.visibility = View.GONE
        loadOrders("pendingServices")

        val back = view.findViewById<ImageView>(R.id.backButton)
        back.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
        }

        return view
    }

    private fun loadUnsuccessfulItems() {
        db.collection("unsuccessfulServices")
            .get()
            .addOnSuccessListener { documents ->
                val services = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val serviceData = document.data.toMutableMap()
                    serviceData["serviceId"] = document.id
                    // Check if any item in the service belongs to the current merchant
                    val items = serviceData["items"] as? List<Map<String, Any>> ?: continue
                    if (items.any { it["merchantName"] == merchantName }) {
                        services.add(serviceData)
                    }
                }
                displayItems(services)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading unsuccessful services: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOrders(statusCollection: String) {
        db.collection(statusCollection)
            .get()
            .addOnSuccessListener { documents ->
                val services = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val serviceData = document.data.toMutableMap()
                    serviceData["serviceId"] = document.id
                    // Check if any item in the order belongs to the current merchant
                    val items = serviceData["items"] as? List<Map<String, Any>> ?: continue
                    if (items.any { it["merchantName"] == merchantName }) {
                        services.add(serviceData)
                    }

                    // Check if the service date has passed for upcoming services
//                    if (statusCollection == "upcomingServices") {
//                        val serviceDate = (serviceData["serviceDate"] as? String)?.let {
//                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
//                        }
//                        if (serviceDate != null && serviceDate.before(Date())) {
//                            moveToCompleted(document.id, serviceData)
//                        }
//                    }
                }
                displayItems(services)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading services: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun moveToCompleted(serviceId: String, serviceData: Map<String, Any>) {
        val completedService = serviceData.toMutableMap()
        completedService["status"] = "completed"
        completedService["completedTimestamp"] = com.google.firebase.Timestamp.now()

        db.collection("completeServices").add(completedService)
            .addOnSuccessListener {
                db.collection("upcomingServices").document(serviceId).delete()
            }
            .addOnFailureListener { e ->
                Log.e("CheckAppointmentFragment", "Error moving service to completed: ${e.message}")
            }
    }

    private fun displayItems(orders: List<Map<String, Any>>) {
        itemsContainer.removeAllViews()
        for (order in orders) {
            val items = order["items"] as? List<Map<String, Any>> ?: continue
            for (item in items) {
                if (item["merchantName"] as? String != merchantName) continue

                val itemView = layoutInflater.inflate(R.layout.item_cart, itemsContainer, false)
                val itemNameTextView = itemView.findViewById<TextView>(R.id.itemNameTextView)
                val itemPriceTextView = itemView.findViewById<TextView>(R.id.itemPriceTextView)
                val itemStockTextView = itemView.findViewById<TextView>(R.id.itemStockTextView)
//                itemStockTextView.visibility = GONE
                val imageView = itemView.findViewById<ImageView>(R.id.itemImageView)

                itemView.setBackgroundResource(R.drawable.custom_input_address)

                itemStockTextView.visibility = View.VISIBLE
                val plus_minus = itemView.findViewById<LinearLayout>(R.id.plus_minus)
                plus_minus.visibility = View.GONE
                val itemCheckBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                itemCheckBox.visibility = View.GONE
                val checkDetails = itemView.findViewById<RelativeLayout>(R.id.checkDetails)
                checkDetails.visibility = View.VISIBLE

                itemNameTextView.text = item["serviceName"] as? String ?: ""
                itemPriceTextView.text = "RM%.2f".format(item["price"] as? Double ?: 0.0)
                //TODO put cat here
//                itemStockTextView.text = "Service : ${item["price"]}"
                itemStockTextView.text = "Category: ${order["category"]}"

                if (item.containsKey("imageUrl")) {
                    Glide.with(this).load(item["imageUrl"] as String).into(imageView)
                }

                checkDetails.setOnClickListener {
                    showProductDetailsDialog(item, order)
                }

                itemsContainer.addView(itemView)
            }
        }
    }

    private fun showProductDetailsDialog(item: Map<String, Any>, order: Map<String, Any>) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pending_order_details, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val closeDialogButton = dialogView.findViewById<ImageView>(R.id.ivCloseDialog)
        val statusTitle = dialogView.findViewById<TextView>(R.id.statusTitle)
        val statusDescription = dialogView.findViewById<TextView>(R.id.statusDescription)
        val itemNameTextView = dialogView.findViewById<TextView>(R.id.itemNameTextView)
        val itemPriceTextView = dialogView.findViewById<TextView>(R.id.itemPriceTextView)
        val itemStockTextView = dialogView.findViewById<TextView>(R.id.itemStockTextView)
        val itemImageView = dialogView.findViewById<ImageView>(R.id.itemImageView)
        val addressTitleTextView = dialogView.findViewById<TextView>(R.id.addressTitleTextView)
        val addressDetailsTextView1 = dialogView.findViewById<TextView>(R.id.addressDetailsTextView1)
        val addressDetailsTextView2 = dialogView.findViewById<TextView>(R.id.addressDetailsTextView2)
        val orderIDTextView = dialogView.findViewById<TextView>(R.id.orderID)
        val sellerNameTextView = dialogView.findViewById<TextView>(R.id.sellerNameTextView)
        val paymentMethodTextView = dialogView.findViewById<TextView>(R.id.tvPayment)
        val orderDateTextView = dialogView.findViewById<TextView>(R.id.tvDate)
        val shippingDateTextView = dialogView.findViewById<TextView>(R.id.tvDate2)
        val completeDateTextView = dialogView.findViewById<TextView>(R.id.tvDate3)
        val userNameTextView = dialogView.findViewById<TextView>(R.id.tvName)
        val bookingDate = dialogView.findViewById<TextView>(R.id.bookingDate)
        val bookingTime = dialogView.findViewById<TextView>(R.id.bookingTime)

        bookingDate.text = "Service Date : ${order["serviceDate"]}"
        bookingTime.text = "Service Time : ${order["pickUpTime"]}"

        val tvPayment = dialogView.findViewById<TextView>(R.id.tvPayment)
        val tvOrderText = dialogView.findViewById<TextView>(R.id.tvOrderText)
        tvPayment.visibility = GONE
        tvOrderText.text = "Booking by : "

        val rating_section = dialogView.findViewById<LinearLayout>(R.id.rating_section)
        val tvRate = dialogView.findViewById<TextView>(R.id.tvRate)
        val tvReview = dialogView.findViewById<TextView>(R.id.tvReview)
        val review_end_line = dialogView.findViewById<View>(R.id.review_end_line)

        // Add pet section
        val petSection = dialogView.findViewById<RelativeLayout>(R.id.petSection)
        val petName = dialogView.findViewById<TextView>(R.id.petName)
        val petAge = dialogView.findViewById<TextView>(R.id.petAge)
        val petBreed = dialogView.findViewById<TextView>(R.id.petBreed)
        val petGender = dialogView.findViewById<TextView>(R.id.petGender)
        val petWeight = dialogView.findViewById<TextView>(R.id.petWeight)
        val petSpecies = dialogView.findViewById<TextView>(R.id.petSpecies)

        val pet = order["pet"] as? Map<String, Any>
        if (pet != null) {
            petName.text = "Pet Name : ${pet["petName"]}"
            petAge.text = "Pet Age     : ${pet["petAge"]}"
            petBreed.text = "Pet Breed : ${pet["petBreed"]}"
            petGender.text = "Pet Gender : ${pet["petGender"]}"
            petWeight.text = "Pet Weight : ${pet["petWeight"]}"
            petSpecies.text = "Pet Species : ${pet["petSpecies"]}"

            addPetFilesToContainer(dialogView, pet, order["username"].toString())
        }

        // Load pet image
        val petImageView = dialogView.findViewById<ImageView>(R.id.petImageView)
        val petImageUrl = pet?.get("petImageUrl") as String?
        if (!petImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(petImageUrl).circleCrop().into(petImageView)
        }

        val response : LinearLayout = dialogView.findViewById(R.id.response)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmBtn)
        val rejectButton = dialogView.findViewById<Button>(R.id.rejectBtn)

        when (status) {
            "unsuccessful" -> {
                statusTitle.text = "Status: Unsuccessful"
                val unsuccessfulStatus = order["status"] as? String ?: "Unknown"
                statusDescription.text = "Reason: $unsuccessfulStatus"
                response.visibility = View.GONE

                val rejectionTimestampValue = order["rejectionTimestamp"]
                if (rejectionTimestampValue is com.google.firebase.Timestamp) {
                    val date = rejectionTimestampValue.toDate()
                    val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                    val formattedDate = dateFormat.format(date)
                    shippingDateTextView.visibility = View.VISIBLE
                    shippingDateTextView.text = "Rejection date: $formattedDate"
                }

                rating_section.visibility = View.GONE
                review_end_line.visibility = View.GONE

            }
            "pending" -> {
                statusTitle.text = "Status: Pending"
                response.visibility = View.VISIBLE

                if(order["status"]!!.equals("pendingConfirmation")) {
                    statusDescription.text = "Please confirm or reject the service."

                    confirmButton.setOnClickListener {
                        val updatedOrder = order.toMutableMap()
                        updatedOrder["status"] = "pendingPayment"
                        updatedOrder["sellerConfirmationTimestamp"] = com.google.firebase.Timestamp.now()

                        // Update the document in Firestore
                        db.collection("pendingServices").document(order["serviceId"].toString())
                            .update(updatedOrder)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Service status updated to pending payment", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                // Optionally, refresh the view or reload the data
                                loadOrders("pendingServices")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error updating service status: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    rejectButton.setOnClickListener {
                        rejectService(order["serviceId"].toString())
                        dialog.dismiss()
                    }
                }else{
                    statusDescription.text = "Service confirmed, wait for customer to pay."
                    response.visibility = View.GONE
                }

                rating_section.visibility = View.GONE
                review_end_line.visibility = View.GONE

            }
            "upcoming" -> {
                statusTitle.text = "Status: Upcoming"
                statusDescription.text = "Service confirmed and scheduled."
                response.visibility = View.GONE

                shippingDateTextView.visibility = View.VISIBLE
                val shippingTimestampValue = order["confirmationTimestamp"]
                if (shippingTimestampValue is com.google.firebase.Timestamp) {
                    val date = shippingTimestampValue.toDate()
                    val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                    val formattedDate = dateFormat.format(date)
                    shippingDateTextView.text = "Confirmation date: $formattedDate"
                } else {
                    shippingDateTextView.text = "Confirmation date: N/A"
                }

                rating_section.visibility = View.GONE
                review_end_line.visibility = View.GONE
            }
            "complete" -> {
                statusTitle.text = "Status: Completed"
                statusDescription.text = "Service has been completed."
                response.visibility = View.GONE

                shippingDateTextView.visibility = View.VISIBLE
                val shippingTimestampValue = order["confirmationTimestamp"]
                if (shippingTimestampValue is com.google.firebase.Timestamp) {
                    val date = shippingTimestampValue.toDate()
                    val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                    val formattedDate = dateFormat.format(date)
                    shippingDateTextView.text = "Confirmation date: $formattedDate"
                } else {
                    shippingDateTextView.text = "Confirmation date: N/A"
                }

                completeDateTextView.visibility = View.VISIBLE
                val shippingTimestampValue2 = order["completedDate"]
                if (shippingTimestampValue2 is com.google.firebase.Timestamp) {
                    val date = shippingTimestampValue2.toDate()
                    val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                    val formattedDate = dateFormat.format(date)
                    completeDateTextView.text = "Completion date: $formattedDate"
                } else {
                    completeDateTextView.text = "Completion date: N/A"
                }

                rating_section.visibility = View.VISIBLE
                review_end_line.visibility = View.VISIBLE
                tvRate.setText("Rating and Review : ${order["rating"]}")
                tvReview.setText("${order["review"]}")
            }
        }

        // Set product details
        itemNameTextView.text = item["serviceName"] as String
        itemPriceTextView.text = "RM%.2f".format(item["price"] as Double)

        //TODO put cat here
//        itemStockTextView.text = "Quantity: ${item["price"]}"
        itemStockTextView.text = "Category: ${order["category"]}"

        // Load product image
        val imageUrl = item["imageUrl"] as String?
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(itemImageView)
        }

        // Set order details
        orderIDTextView.text = "Service ID: ${order["serviceId"] as String}"
        sellerNameTextView.text = "${item["merchantName"] as String}"
//        paymentMethodTextView.text = "Payment method: ${order["paymentMethod"] as String? ?: "N/A"}"
        userNameTextView.text = "${order["username"] as String}"


        // Load user profile image
        val userProfileImage = dialogView.findViewById<ImageView>(R.id.buyerImage)
        val buyername = order["username"] as String
        db.collection("users").document(buyername).get()
            .addOnSuccessListener { userDocument ->
                val profileImageUrl = userDocument.getString("profileImage")
                if (profileImageUrl != null) {
                    Glide.with(this)
                        .load(userDocument.getString("profileImage"))
                        .error(R.drawable.account_icon_lightgrey)
                        .circleCrop()
                        .into(userProfileImage)
                } else {
                    userProfileImage.setImageResource(R.drawable.account_icon_white)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ShowRatingDialog", "Error loading user profile: ${e.message}")
                userProfileImage.setImageResource(R.drawable.account_icon_lightgrey)
            }

        // Format the order date
        val timestampValue = order["timestamp"]
        if (timestampValue is com.google.firebase.Timestamp) {
            val date = timestampValue.toDate()
            val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
            val formattedDate = dateFormat.format(date)
            orderDateTextView.text = "Booking date: $formattedDate"
        } else {
            orderDateTextView.text = "Booking date: N/A"
        }

        // Set address details
        val address = order["address"] as Map<String, Any>
        addressTitleTextView.setText("Store Address : ")
        addressDetailsTextView1.text = "${item["merchantName"]}" + " | " + address["phone"].toString()
        addressDetailsTextView2.text = address["address"].toString() + "\nPostal : " + address["postalCode"].toString() + " " + address["city"].toString()

        // Close dialog on button click
        closeDialogButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addPetFilesToContainer(dialogView: View, pet: Map<String, Any>, buyerName: String) {
        val fileContainer = dialogView.findViewById<LinearLayout>(R.id.itemsContainer)
        fileContainer.removeAllViews()

        val petName = pet["petName"] as? String ?: "Unknown Pet"
        Toast.makeText(context, "sellerName: $buyerName", Toast.LENGTH_SHORT).show()
        Toast.makeText(context, "petName: $petName", Toast.LENGTH_SHORT).show()


        db.collection("users").document(buyerName).collection("pets").document(petName)
            .collection("files").get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot) {
                        val fileUrl = document.getString("url") ?: continue
                        val fileName = document.getString("name") ?: continue

                        val fileItemView = layoutInflater.inflate(R.layout.item_file, fileContainer, false)
                        val tvFileName = fileItemView.findViewById<TextView>(R.id.tvFileName)
                        val btnRemoveFile = fileItemView.findViewById<ImageButton>(R.id.btnRemoveFile)

                        tvFileName.text = fileName
                        btnRemoveFile.visibility = View.GONE

                        fileItemView.setOnClickListener {
                            showFilePreviewDialog(fileUrl)
                        }

                        fileContainer.addView(fileItemView)
                    }
                } else {
                    val tvNoFiles = TextView(context)
                    tvNoFiles.text = "No files for this pet"
                    fileContainer.addView(tvNoFiles)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CheckAppointmentFragment", "Error loading pet files: ${e.message}")
            }
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

    private fun confirmService(serviceId: String, order: Map<String, Any>) {
        val upcomingService = order.toMutableMap()
        upcomingService["status"] = "upcoming"
        upcomingService["confirmationTimestamp"] = com.google.firebase.Timestamp.now()

        db.collection("upcomingServices").add(upcomingService)
            .addOnSuccessListener {
                db.collection("pendingServices").document(serviceId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Service confirmed and moved to upcoming", Toast.LENGTH_SHORT).show()
                        loadOrders("pendingServices")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error deleting pending service: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error confirming service: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectService(serviceId: String) {
        storeUnsuccessfulService(serviceId, "Reject by Seller")
    }

    private fun storeUnsuccessfulService(serviceId: String, status: String) {
        db.collection("pendingServices").document(serviceId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val serviceData = document.data?.toMutableMap() ?: mutableMapOf()
                    serviceData["status"] = status
                    serviceData["rejectionTimestamp"] = com.google.firebase.Timestamp.now()

                    db.collection("unsuccessfulServices").add(serviceData)
                        .addOnSuccessListener {
                            db.collection("pendingServices").document(serviceId).delete()
                            Toast.makeText(context, "Service marked as unsuccessful", Toast.LENGTH_SHORT).show()
                            loadOrders("pendingServices")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error storing unsuccessful service: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun checkUnconfirmedServices() {
        val threeDaysAgo = com.google.firebase.Timestamp.now().toDate().time - (3 * 24 * 60 * 60 * 1000)
        db.collection("pendingServices")
            .whereLessThan("timestamp", com.google.firebase.Timestamp(threeDaysAgo / 1000, 0))
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    storeUnsuccessfulService(document.id, "Seller no reply (3 days)")
                }
            }
    }

    private fun checkExpiredServices() {
        val currentDate = com.google.firebase.Timestamp.now().toDate()
        db.collection("pendingServices")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val serviceData = document.data
                    val category = serviceData["category"] as? String
                    val serviceDate = (serviceData["serviceDate"] as? String)?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                    }
                    val checkinDate = (serviceData["checkinDate"] as? String)?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                    }

                    if (category == "Boarding" && checkinDate != null && checkinDate.before(currentDate)) {
                        storeUnsuccessfulService(document.id, "Expired (seller not confirming)")
                    } else if (category != "Boarding" && serviceDate != null && serviceDate.before(currentDate)) {
                        storeUnsuccessfulService(document.id, "Expired (seller not confirming)")
                    }
                }
            }
    }

    private fun checkUnpaidConfirmedServices() {
        val currentDate = com.google.firebase.Timestamp.now().toDate()
        db.collection("pendingServices")
            .whereEqualTo("status", "pendingPayment")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val serviceData = document.data
                    val category = serviceData["category"] as? String
                    val serviceDate = (serviceData["serviceDate"] as? String)?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                    }
                    val checkinDate = (serviceData["checkinDate"] as? String)?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                    }

                    if ((category != "Boarding" && serviceDate != null && serviceDate.before(currentDate)) ||
                        (category == "Boarding" && checkinDate != null && checkinDate.before(currentDate))) {
                        storeUnsuccessfulService(document.id, "Expired (user not paying)")
                    }
                }
            }
    }

    private fun checkDelayedPayments() {
        val threeDaysAgo = com.google.firebase.Timestamp.now().toDate().time - (3 * 24 * 60 * 60 * 1000)
        db.collection("pendingServices")
            .whereEqualTo("status", "pendingPayment")
            .whereLessThan("sellerConfirmationTimestamp", com.google.firebase.Timestamp(threeDaysAgo / 1000, 0))
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    storeUnsuccessfulService(document.id, "User not paying (3 days)")
                }
            }
    }

}