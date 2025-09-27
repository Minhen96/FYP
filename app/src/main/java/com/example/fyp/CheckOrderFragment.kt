package com.example.fyp

import android.app.AlertDialog
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale


class CheckOrderFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_check_order, container, false)

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
        val shippingTab = view.findViewById<RelativeLayout>(R.id.shipping)
        val completeTab = view.findViewById<RelativeLayout>(R.id.completed)
        val pendingTitle = view.findViewById<TextView>(R.id.pendingTitle)
        val shippingTitle = view.findViewById<TextView>(R.id.shippingTitle)
        val completeTitle = view.findViewById<TextView>(R.id.completedTitle)
        val pendingLine = view.findViewById<View>(R.id.pendingline)
        val shippingLine = view.findViewById<View>(R.id.shippingline)
        val completeLine = view.findViewById<View>(R.id.completedline)

        val unsuccessTab = view.findViewById<RelativeLayout>(R.id.unsuccess)
        val unsuccessTitle = view.findViewById<TextView>(R.id.unsuccessTitle)
        val unsuccessLine = view.findViewById<View>(R.id.unsuccessline)

        itemsContainer = view.findViewById(R.id.itemsContainer)

        pendingTab.setOnClickListener {
            status = "pending"
            pendingLine.visibility = VISIBLE
            shippingLine.visibility = GONE
            completeLine.visibility = GONE
            loadOrders("pendingOrders")
        }
        pendingTitle.setOnClickListener {
            status = "pending"
            pendingLine.visibility = VISIBLE
            shippingLine.visibility = GONE
            completeLine.visibility = GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingOrders")
        }

        shippingTab.setOnClickListener {
            status = "shipping"
            pendingLine.visibility = GONE
            shippingLine.visibility = VISIBLE
            completeLine.visibility = GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("shippingOrders")
        }
        shippingTitle.setOnClickListener {
            status = "shipping"
            pendingLine.visibility = GONE
            shippingLine.visibility = VISIBLE
            completeLine.visibility = GONE
            unsuccessLine.visibility = View.GONE
            loadOrders("shippingOrders")
        }

        completeTab.setOnClickListener {
            status = "complete"
            pendingLine.visibility = GONE
            shippingLine.visibility = GONE
            completeLine.visibility = VISIBLE
            unsuccessLine.visibility = View.GONE
            loadOrders("pendingOrders")
        }
        completeTitle.setOnClickListener {
            status = "complete"
            pendingLine.visibility = GONE
            shippingLine.visibility = GONE
            completeLine.visibility = VISIBLE
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
            completeLine.visibility = View.GONE
            unsuccessLine.visibility = View.VISIBLE
            loadUnsuccessfulItems()
        }

        // Set default to Pending
        status = "pending"
        pendingLine.visibility = View.VISIBLE
        shippingLine.visibility = View.GONE
        loadOrders("pendingOrders")

        val back = view.findViewById<ImageView>(R.id.backButton)
        back.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
        }

        return view
    }

    private fun loadUnsuccessfulItems() {
        status = "unsuccessful"

        db.collection("unsuccessfulOrders")
            .get()
            .addOnSuccessListener { documents ->
                val orders = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val orderData = document.data.toMutableMap()
                    orderData["orderId"] = document.id
                    // Check if any item in the order belongs to the current merchant
                    val items = orderData["items"] as? List<Map<String, Any>> ?: continue
                    if (items.any { it["merchantName"] == merchantName }) {
                        orders.add(orderData)
                    }
                }
                displayItems(orders)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading unsuccessful orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOrders(statusCollection: String) {
        db.collection(statusCollection)
            .get()
            .addOnSuccessListener { documents ->
                val orders = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    val orderData = document.data.toMutableMap()
                    orderData["orderId"] = document.id
                    // Check if any item in the order belongs to the current merchant
                    val items = orderData["items"] as? List<Map<String, Any>> ?: continue
                    if (items.any { it["merchantName"] == merchantName }) {
                        orders.add(orderData)
                    }
                }
                displayItems(orders)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading pending orders: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val imageView = itemView.findViewById<ImageView>(R.id.itemImageView)

                itemView.setBackgroundResource(R.drawable.custom_input_address)

                itemStockTextView.visibility = VISIBLE
                val plus_minus = itemView.findViewById<LinearLayout>(R.id.plus_minus)
                plus_minus.visibility = GONE
                val itemCheckBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                itemCheckBox.visibility = GONE
                val checkDetails = itemView.findViewById<RelativeLayout>(R.id.checkDetails)
                checkDetails.visibility = VISIBLE

                itemNameTextView.text = item["productName"] as? String ?: ""
                itemPriceTextView.text = "RM%.2f".format(item["price"] as? Double ?: 0.0)
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
            statusDescription.text = "Please confirm order and prepare shipping."

            shippingDateTextView.visibility = GONE
            response.visibility = VISIBLE
            val confirmButton = dialogView.findViewById<Button>(R.id.confirmBtn)
            val rejectButton = dialogView.findViewById<Button>(R.id.rejectBtn)

            confirmButton.setOnClickListener {
                moveOrderToShipping(order["orderId"].toString(), order, item)
                dialog.dismiss()
            }

            rejectButton.setOnClickListener {
                rejectOrder(order["orderId"].toString())
                dialog.dismiss()
            }

            rating_section.visibility = GONE
            review_end_line.visibility = GONE
        } else if (status.equals("shipping")){
            statusTitle.text = "Status : Shipping"
            statusDescription.text = "Shipping in progress, waiting for customer confirmation."
            response.visibility = GONE

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

            rating_section.visibility = GONE
            review_end_line.visibility = GONE
        } else{
            statusTitle.text = "Status : Completed"
            statusDescription.text = "Order delivered successfully."
            response.visibility = GONE

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
            completeDateTextView.visibility = VISIBLE
            val completeTimestampValue = order["completedDate"]
            if (completeTimestampValue is com.google.firebase.Timestamp) {
                val date = completeTimestampValue.toDate()
                val dateFormat = SimpleDateFormat("d/M/yyyy hh:mm:ssa", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                completeDateTextView.text = "Complete date: $formattedDate"
            } else {
                completeDateTextView.text = "Complete date: N/A"
            }

            rating_section.visibility = VISIBLE
            review_end_line.visibility = VISIBLE
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

    private fun moveOrderToShipping(orderId: String, order: Map<String, Any>, item: Map<String, Any>) {
        // Create a new shipping order with only the selected item
        val shippingOrder = mutableMapOf<String, Any>()
        shippingOrder["orderId"] = orderId
        shippingOrder["username"] = order["username"] as String
        shippingOrder["address"] = order["address"] as Map<String, Any>
        shippingOrder["timestamp"] = order["timestamp"] as com.google.firebase.Timestamp
        shippingOrder["shippingTimestamp"] = com.google.firebase.Timestamp.now() // Add the shipping timestamp
        shippingOrder["status"] = "shipping"
        shippingOrder["items"] = listOf(item)

        // Add the new shipping order
        db.collection("shippingOrders").add(shippingOrder)
            .addOnSuccessListener {
                // Remove the item from the pending order
                db.collection("pendingOrders").document(orderId).get()
                    .addOnSuccessListener { documentSnapshot ->
                        val pendingOrder = documentSnapshot.data
                        if (pendingOrder != null) {
                            val items = pendingOrder["items"] as MutableList<Map<String, Any>>
                            items.removeAll { it["productName"] == item["productName"] && it["merchantName"] == item["merchantName"] }

                            if (items.isEmpty()) {
                                // If no items left, delete the entire order
                                db.collection("pendingOrders").document(orderId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Item moved to shipping!", Toast.LENGTH_SHORT).show()
                                        loadOrders("pendingOrders")
                                    }
                            } else {
                                // Update the pending order with remaining items
                                db.collection("pendingOrders").document(orderId).update("items", items)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Item moved to shipping!", Toast.LENGTH_SHORT).show()
                                        loadOrders("pendingOrders")
                                    }
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error moving item to shipping: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectOrder(orderId: String) {
        storeUnsuccessfulOrder(orderId, "Reject by Seller")
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

}