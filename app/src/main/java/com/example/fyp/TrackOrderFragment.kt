package com.example.fyp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale


class TrackOrderFragment : Fragment() {

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

        checkUnconfirmedOrders()
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
        val view = inflater.inflate(R.layout.fragment_track_order, container, false)

        profileButton = view.findViewById(R.id.profileButton)
        updateProfileButtonAppearance()

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

        itemsContainer = view.findViewById(R.id.itemsContainer)

        val pendingTab = view.findViewById<RelativeLayout>(R.id.pending)
        val shippingTab = view.findViewById<RelativeLayout>(R.id.shipping)
        val completeTab = view.findViewById<RelativeLayout>(R.id.completed)
        val pendingTitle = view.findViewById<TextView>(R.id.tabPending)
        val shippingTitle = view.findViewById<TextView>(R.id.tabShipping)
        val completeTitle = view.findViewById<TextView>(R.id.completedTitle)
        val pendingLine = view.findViewById<View>(R.id.pending_line)
        val shippingLine = view.findViewById<View>(R.id.shipping_line)
        val completeLine = view.findViewById<View>(R.id.completedline)

        val unsuccessTab = view.findViewById<RelativeLayout>(R.id.unsuccess)
        val unsuccessTitle = view.findViewById<TextView>(R.id.unsuccessTitle)
        val unsuccessLine = view.findViewById<View>(R.id.unsuccessline)

        pendingTab.setOnClickListener {
            status = "pending"
            pendingLine.visibility = View.VISIBLE
            shippingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingOrders")
        }
        pendingTitle.setOnClickListener {
            status = "pending"
            pendingLine.visibility = View.VISIBLE
            shippingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingOrders")
        }

        shippingTab.setOnClickListener {
            status = "shipping"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.VISIBLE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("shippingOrders")
        }
        shippingTitle.setOnClickListener {
            status = "shipping"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.VISIBLE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("shippingOrders")
        }

        completeTab.setOnClickListener {
            status = "complete"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.GONE
            completeLine.visibility = View.VISIBLE
            unsuccessLine.visibility = View.GONE
            loadOrders("completeOrders")
        }
        completeTitle.setOnClickListener {
            status = "complete"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.GONE
            completeLine.visibility = View.VISIBLE
            unsuccessLine.visibility = View.GONE
            loadOrders("completeOrders")
        }

        unsuccessTab.setOnClickListener {
            status = "unsuccessful"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.GONE
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.VISIBLE
            loadUnsuccessfulItems()
        }
        unsuccessTitle.setOnClickListener {
            status = "unsuccessful"
            pendingLine.visibility = View.GONE
            shippingLine.visibility = View.GONE
            shippingLine.visibility = View.GONE
            unsuccessLine.visibility = View.VISIBLE
            loadUnsuccessfulItems()
        }

        // Set default to Pending
        status = "pending"
        pendingLine.visibility = View.VISIBLE
        shippingLine.visibility = View.GONE
        completeLine.visibility = View.GONE
        loadOrders("pendingOrders")
//        loadShippingOrders()

        val back = view.findViewById<ImageView>(R.id.backButton)
        back.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
        }

        return view
    }

    private fun loadUnsuccessfulItems() {

        db.collection("unsuccessfulOrders")
            .get()
            .addOnSuccessListener { documents ->
                val orders = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val orderData = document.data.toMutableMap()
                    orderData["orderId"] = document.id
                    // Check if any item in the order belongs to the current merchant
                    val items = orderData["items"] as? List<Map<String, Any>> ?: continue
                    if (orderData["username"] == username) {
                        orders.add(orderData)
                    }
                }
                displayItems(orders)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading unsuccessful orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOrders(status: String) {
        db.collection(status)
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                val orders = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val orderData = document.data.toMutableMap()
                    orderData["orderId"] = document.id // Add order ID
                    orders.add(orderData)
                }
                displayItems(orders)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayItems(orders: List<Map<String, Any>>) {
        itemsContainer.removeAllViews()
        for (order in orders) {
            val items = order["items"] as List<Map<String, Any>>
            val groupedItems = items.groupBy { it["merchantName"] as String }
            for ((merchantName, merchantItems) in groupedItems) {
                val sellerView = layoutInflater.inflate(R.layout.item_cart_header, itemsContainer, false)
                sellerView.findViewById<TextView>(R.id.sellerNameTextView).text = merchantName
                itemsContainer.addView(sellerView)

                sellerView.setBackgroundResource(R.drawable.custom_category_background)
                for (item in merchantItems) {
                    val itemView = layoutInflater.inflate(R.layout.item_cart, itemsContainer, false)
                    val itemNameTextView = itemView.findViewById<TextView>(R.id.itemNameTextView)
                    val itemPriceTextView = itemView.findViewById<TextView>(R.id.itemPriceTextView)
                    val itemStockTextView = itemView.findViewById<TextView>(R.id.itemStockTextView)
                    val imageView = itemView.findViewById<ImageView>(R.id.itemImageView)

                    itemView.setBackgroundResource(R.drawable.custom_input_address)

                    itemStockTextView.visibility = View.VISIBLE
                    val plus_minus = itemView.findViewById<LinearLayout>(R.id.plus_minus)
                    plus_minus.visibility = View.GONE
                    val itemCheckBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                    itemCheckBox.visibility = View.GONE
                    val checkDetails = itemView.findViewById<RelativeLayout>(R.id.checkDetails)
                    checkDetails.visibility = View.VISIBLE

                    itemNameTextView.text = item["productName"] as String
                    itemPriceTextView.text = "RM%.2f".format(item["price"])
                    itemStockTextView.text = "x${item["quantity"]}"

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
        val addressDetailsTextView1 = dialogView.findViewById<TextView>(R.id.addressDetailsTextView1)
        val addressDetailsTextView2 = dialogView.findViewById<TextView>(R.id.addressDetailsTextView2)
        val orderIDTextView = dialogView.findViewById<TextView>(R.id.orderID)
        val sellerNameTextView = dialogView.findViewById<TextView>(R.id.sellerNameTextView)
        val paymentMethodTextView = dialogView.findViewById<TextView>(R.id.tvPayment)
        val orderDateTextView = dialogView.findViewById<TextView>(R.id.tvDate)
        val shippingDateTextView = dialogView.findViewById<TextView>(R.id.tvDate2)
        val completeDateTextView = dialogView.findViewById<TextView>(R.id.tvDate3)
        val userNameTextView = dialogView.findViewById<TextView>(R.id.tvName)
        val rating_section = dialogView.findViewById<LinearLayout>(R.id.rating_section)
        val tvRate = dialogView.findViewById<TextView>(R.id.tvRate)
        val tvReview = dialogView.findViewById<TextView>(R.id.tvReview)
        val review_end_line = dialogView.findViewById<View>(R.id.review_end_line)

        val bookingDate = dialogView.findViewById<TextView>(R.id.bookingDate)
        val bookingTime = dialogView.findViewById<TextView>(R.id.bookingTime)
        val petSection = dialogView.findViewById<RelativeLayout>(R.id.petSection)
        val petLine = dialogView.findViewById<View>(R.id.petLine)
        bookingDate.visibility = GONE
        bookingTime.visibility = GONE
        petSection.visibility = GONE
        petLine.visibility = GONE

        val response : LinearLayout = dialogView.findViewById(R.id.response)

        if (status == "unsuccessful") {
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
        } else if (status.equals("pending")){
            statusTitle.text = "Status : Pending"
            statusDescription.text = "Waiting for seller to confirm..."
            response.visibility = View.GONE
            shippingDateTextView.visibility = GONE
        } else if (status.equals("shipping")){
            statusTitle.text = "Status : Shipping"
            statusDescription.text = "Shipping in progress, please confirm on arrival."

            shippingDateTextView.visibility = VISIBLE
            val shippingTimestampValue = order["shippingTimestamp"]
            if (shippingTimestampValue is com.google.firebase.Timestamp) {
                val date = shippingTimestampValue.toDate()
                val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                shippingDateTextView.text = "Shipping date: $formattedDate"
            } else {
                shippingDateTextView.text = "Shipping date: N/A"
            }

            response.visibility = View.VISIBLE
            val receiveButton = dialogView.findViewById<Button>(R.id.confirmBtn)
            val missButton = dialogView.findViewById<Button>(R.id.rejectBtn)
            receiveButton.text = "Received"
            missButton.visibility = View.GONE

            receiveButton.setOnClickListener {
                //move the shippingOrder to completeOrder, also including all of the data
                showRatingDialog(order)
                dialog.dismiss()
            }
        } else{
            statusTitle.text = "Status : Completed"
            statusDescription.text = "Service has been completed."
            response.visibility = View.GONE

            shippingDateTextView.visibility = VISIBLE
            val shippingTimestampValue = order["shippingTimestamp"]
            if (shippingTimestampValue is com.google.firebase.Timestamp) {
                val date = shippingTimestampValue.toDate()
                val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                shippingDateTextView.text = "Shipping date: $formattedDate"
            } else {
                shippingDateTextView.text = "Shipping date: N/A"
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

        // Set product details
        itemNameTextView.text = item["productName"] as String
        itemPriceTextView.text = "RM%.2f".format(item["price"] as Double)
        itemStockTextView.text = "Quantity: ${item["quantity"]}"

        // Load product image
        val imageUrl = item["imageUrl"] as String?
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(itemImageView)
        }

        // Set order details
        orderIDTextView.text = "Order ID: ${order["orderId"] as String}"
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
            orderDateTextView.text = "Order date: $formattedDate"
        } else {
            orderDateTextView.text = "Order date: N/A"
        }

        // Set address details
        val address = order["address"] as Map<String, Any>
        addressDetailsTextView1.text = address["id"].toString() + " | " + address["phone"].toString()
        addressDetailsTextView2.text = address["address"].toString() + "\nPostal : " + address["postalCode"].toString() + " " + address["city"].toString()

        // Close dialog on button click
        closeDialogButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRatingDialog(order: Map<String, Any>) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating_review, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val reviewEditText = dialogView.findViewById<EditText>(R.id.reviewEditText)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val skipButton = dialogView.findViewById<Button>(R.id.skipButton)
        val ivCloseDialog = dialogView.findViewById<ImageView>(R.id.ivCloseDialog)

        ivCloseDialog.setOnClickListener{
            dialog.dismiss()
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val review = reviewEditText.text.toString()
            completeOrderWithRating(order, rating, review)
            dialog.dismiss()
        }

        skipButton.setOnClickListener {
            completeOrderWithRating(order, 0f, "")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun completeOrderWithRating(order: Map<String, Any>, rating: Float, review: String) {
        val orderId = order["orderId"] as String
        val completeOrder = order.toMutableMap()
        completeOrder["status"] = "completed"
        completeOrder["completedDate"] = com.google.firebase.Timestamp.now()
        completeOrder["rating"] = rating
        completeOrder["review"] = review

        // Calculate total items sold
        val items = order["items"] as List<Map<String, Any>>
        val totalItemsSold = items.sumBy { (it["quantity"] as Long).toInt() }
        completeOrder["totalItemsSold"] = totalItemsSold

        db.collection("completeOrders").document(orderId).set(completeOrder)
            .addOnSuccessListener {
                // Remove the order from shippingOrders
                db.collection("shippingOrders").document(orderId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Order marked as completed!", Toast.LENGTH_SHORT).show()
                        loadOrders("shippingOrders") // Reload the shipping orders
                        if (rating > 0f || review.isNotEmpty()) {
                            updateProductRating(order, rating, review)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error removing from shipping orders: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error marking order as completed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductRating(order: Map<String, Any>, rating: Float, review: String) {
        val items = order["items"] as List<Map<String, Any>>
        for (item in items) {
            val productId = item["productName"] as String
            val sellerName = item["merchantName"] as String
            val quantity = (item["quantity"] as Number).toLong() // Convert to Long

            val productRef = db.collection("merchants").document(sellerName)
                .collection("products").document(productId)

            productRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    val currentRating = document.getDouble("averageRating") ?: 0.0
                    val currentReviewCount = document.getLong("reviewCount") ?: 0
                    val currentSalesCount = document.getLong("salesCount") ?: 0

                    val newReviewCount = if (rating > 0f || review.isNotEmpty()) currentReviewCount + 1 else currentReviewCount
                    val newAverageRating = if (newReviewCount > 0) (currentRating * currentReviewCount + rating) / newReviewCount else currentRating
                    val newSalesCount = currentSalesCount + quantity

                    val updateData = mapOf(
                        "averageRating" to newAverageRating,
                        "reviewCount" to newReviewCount,
                        "salesCount" to newSalesCount
                    ) as Map<String, Any>

                    productRef.update(updateData)

                    if (rating > 0f || review.isNotEmpty()) {
                        val reviewData = mapOf(
                            "rating" to rating,
                            "review" to review,
                            "username" to order["username"],
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        productRef.collection("reviews").add(reviewData)
                    }
                }
            }
        }
    }

    private fun storeUnsuccessfulOrder(orderId: String, status: String) {
        db.collection("pendingOrders").document(orderId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val orderData = document.data
                    orderData?.put("status", status)
                    orderData?.put("rejectionTimestamp", com.google.firebase.Timestamp.now())

                    db.collection("unsuccessfulOrders").add(orderData!!)
                        .addOnSuccessListener {
                            db.collection("pendingOrders").document(orderId).delete()
                            Toast.makeText(context, "Order marked as unsuccessful", Toast.LENGTH_SHORT).show()
                            loadOrders("pendingOrders")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error storing unsuccessful order: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun checkUnconfirmedOrders() {
        val threeDaysAgo = com.google.firebase.Timestamp.now().toDate().time - (3 * 24 * 60 * 60 * 1000)
        db.collection("pendingOrders")
            .whereLessThan("timestamp", com.google.firebase.Timestamp(threeDaysAgo / 1000, 0))
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    storeUnsuccessfulOrder(document.id, "Seller no reply (3 days)")
                }
            }
    }


    companion object {
        private const val TAG = "TrackOrderFragment"
    }

}

