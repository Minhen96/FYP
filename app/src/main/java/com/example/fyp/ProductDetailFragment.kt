package com.example.fyp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.properties.Delegates

class ProductDetailFragment : Fragment() {

    private lateinit var username: String
    private lateinit var sourceFragment: String
    private var merchantName: String = ""
    private lateinit var homeButton: ImageButton
    private lateinit var productDetailImage: String
    private lateinit var collectionName: String
    private lateinit var addToCartBtn: TextView
    private lateinit var chatBtn: TextView
    private lateinit var productStock: TextView
    private lateinit var noReviewText: TextView
    private lateinit var reviewsLayout: LinearLayout

    private lateinit var from: String
    private lateinit var product: Product
    private lateinit var service: Service
    private var isService by Delegates.notNull<Boolean>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = arguments?.getParcelable("product") ?: Product()
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        sourceFragment = arguments?.getString("sourceFragment").toString()

        isService = product.isService.toBoolean()
        Toast.makeText(context, "isService: ${isService}", Toast.LENGTH_SHORT).show()

        productDetailImage = product.imageUrl
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(tag)
        transaction?.commit()
        // Add the current fragment to the stack
        (activity as? MainActivity)?.fragmentStack?.push(tag)
    }

    private fun updateProfileButtonAppearance() {
        homeButton.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.home_icon_white)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_detail, container, false)

        // Get a reference to the ImageButton
        homeButton = view.findViewById(R.id.homeButton)
        updateProfileButtonAppearance()

        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val chatButton = view.findViewById<ImageButton>(R.id.chatButton)
        val profileButton = view.findViewById<ImageButton>(R.id.profileButton)

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
        profileButton.setOnClickListener {
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        noReviewText = view.findViewById(R.id.noReviewText)
        addToCartBtn = view.findViewById(R.id.addToCartBtn)
        chatBtn = view.findViewById(R.id.chatBtn)

        chatBtn.setOnClickListener {
            openChatWithSeller()
        }

        if (sourceFragment == "MerchantFragment") {
            addToCartBtn.visibility = GONE
            chatBtn.visibility = GONE
            profileButton.apply {
                setBackgroundResource(R.drawable.toolbar_background_focus)
                setImageResource(R.drawable.account_icon_white)
            }
            homeButton.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.home_icon)
            }
            cartButton?.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.cart_icon)
            }

        } else if (sourceFragment == "CartFragment") {
            cartButton.apply {
                setBackgroundResource(R.drawable.toolbar_background_focus)
                setImageResource(R.drawable.cart_icon_white)
            }
            homeButton.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.home_icon)
            }
            profileButton?.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.account_icon_lightgrey)
            }
        } else if (sourceFragment == "ProductListFragment") {
            homeButton.apply {
                setBackgroundResource(R.drawable.toolbar_background_focus)
                setImageResource(R.drawable.home_icon_white)
            }
            cartButton?.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.cart_icon)
            }
            profileButton?.apply {
                setBackgroundResource(R.drawable.border)
                setImageResource(R.drawable.account_icon_lightgrey)
            }
        }


        val back = view.findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            handleBackNavigation()
        }

        productStock = view.findViewById(R.id.productStock)

        return view
    }


    private fun openChatWithSeller() {

        if (username == null || product == null || product.merchantName == null) {
            Toast.makeText(context, "Username or Merchant Name is null", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            db.collection("chats")
                .whereEqualTo("user", username)
                .whereEqualTo("merchantName", product.merchantName)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Create a new chat
                        createNewChat()
                    } else {
                        val chatId = documents.documents[0].id
                        openChatRoom(chatId)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun createNewChat() {
        val chatId = db.collection("chats").document().id

        val chat = hashMapOf(
            "user" to username,
            "merchantName" to product.merchantName,
            "lastMessage" to "",
            "lastMessageTimestamp" to null
        )

        db.collection("chats").document(chatId)
            .set(chat)
            .addOnSuccessListener {
                openChatRoom(chatId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error creating chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChatRoom(chatId: String) {
        val fragment = ChatRoomFragment().apply {
            arguments = Bundle().apply {
                putString("username", username)
                putString("merchantName", merchantName)
                putString("otherUser", product.merchantName)
                putString("chatId", chatId)
                putString("viewType", "user")
            }
        }
        (activity as? MainActivity)?.replaceFragment(fragment, "ChatRoomFragment")
    }

    fun handleBackNavigation() {
//        when (sourceFragment) {
//            "CartFragment" -> navigateToCart()
//            "MerchantFragment" -> navigateToMerchant()
//            "ProductListFragment" -> navigateToProductList()
//            else -> (activity as? MainActivity)?.popBackStack()
//        }
        (activity as? MainActivity)?.popBackStack()
    }

    private fun navigateToMerchant() {
        val fragment = MerchantFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        fragment.arguments = bundle
        replaceFragment(fragment, "MerchantFragment")
//        (activity as? MainActivity)?.replaceFragment(fragment, "MerchantFragment")
    }

    private fun navigateToCart() {
        val fragment = CartFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        fragment.arguments = bundle
        replaceFragment(fragment, "CartFragment")
//        (activity as? MainActivity)?.replaceFragment(fragment, "CartFragment")
    }

    private fun navigateToProductList() {
        val fragment = ProductListFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        fragment.arguments = bundle
        replaceFragment(fragment, "ProductListFragment")
//        (activity as? MainActivity)?.replaceFragment(fragment, "ProductListFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productImage = view.findViewById<ImageView>(R.id.productImage)
        val productName = view.findViewById<TextView>(R.id.productName)
        val productSoldCount = view.findViewById<TextView>(R.id.productSoldCount)
        val productPrice = view.findViewById<TextView>(R.id.productPrice)
        val productRating = view.findViewById<TextView>(R.id.productRating)
        val serviceHours = view.findViewById<TextView>(R.id.serviceHours)
        val unavailableDays = view.findViewById<TextView>(R.id.unavailableDays)
        val unavailableDates = view.findViewById<TextView>(R.id.unavailableDates)

        productName.text = product.name
        productPrice.text = "RM ${product.price}"

        if (isService) {
            productSoldCount.visibility = View.GONE
            productStock.visibility = View.GONE
            serviceHours.text = "Operating Hours : ${product.startHour} - ${product.endHour}"
            unavailableDays.text = "Closed on ${product.unavailableDays.joinToString(", ")}"
            unavailableDates.text = "Unavailable Dates : ${product.unavailableDates.joinToString(", ")}"
            serviceHours.visibility = View.VISIBLE
            unavailableDays.visibility = View.VISIBLE
            unavailableDates.visibility = View.VISIBLE
            addToCartBtn.text = "Add to Wishlist"
            collectionName = "services"
        } else {
            productSoldCount.text = "${product.salesCount} Sold"
            if(product.stock == 0){
                productStock.text = "Stock: ${product.stock} (Sold Out)"
            }else{
                productStock.text = "Stock: ${product.stock}"
            }

            productStock.visibility = View.VISIBLE
            serviceHours.visibility = View.GONE
            unavailableDays.visibility = View.GONE
            unavailableDates.visibility = View.GONE
            addToCartBtn.text = "Add to Cart"
            collectionName = "products"
        }

        val averageRating = String.format("%.2f", product.averageRating)
        productRating.text = "$averageRating (${product.reviewCount})"

        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(R.drawable.account_icon_white)
            .error(R.drawable.temp_pet_icon)
            .into(productImage)

        val productFullDescription = view.findViewById<TextView>(R.id.productFullDescription)
        productFullDescription.text = product.description


        //setting UI of details bar
        val productDescription = view.findViewById<RelativeLayout>(R.id.productDescription)
        val productReviews = view.findViewById<RelativeLayout>(R.id.productReviews)
        val productSeller = view.findViewById<RelativeLayout>(R.id.productSeller)
        val descriptionTitle = view.findViewById<TextView>(R.id.descriptionTitle)
        val sellerTitle = view.findViewById<TextView>(R.id.sellerTitle)
        val reviewTitle = view.findViewById<TextView>(R.id.reviewTitle)

        if (sourceFragment == "MerchantFragment"){
            productSeller.visibility = GONE
        }else {
            productSeller.visibility = VISIBLE
        }

        val descriptionLayout = view.findViewById<LinearLayout>(R.id.descriptionLayout)
        reviewsLayout = view.findViewById(R.id.reviewsLayout)
        val sellerLayout = view.findViewById<LinearLayout>(R.id.sellerLayout)

        val description_line: View = view.findViewById(R.id.description_line)
        val seller_line: View = view.findViewById(R.id.seller_line)
        val review_line: View = view.findViewById(R.id.review_line)

        //default
        descriptionLayout.visibility = View.VISIBLE
        sellerLayout.visibility = View.GONE
        reviewsLayout.visibility = View.GONE

        description_line.visibility = View.VISIBLE
        seller_line.visibility = View.GONE
        review_line.visibility = View.GONE

        productDescription.setOnClickListener {
            noReviewText.visibility = GONE
            descriptionLayout.visibility = View.VISIBLE
            sellerLayout.visibility = View.GONE
            reviewsLayout.visibility = View.GONE

            description_line.visibility = View.VISIBLE
            seller_line.visibility = View.GONE
            review_line.visibility = View.GONE
        }
        descriptionTitle.setOnClickListener {
            noReviewText.visibility = GONE
            descriptionLayout.visibility = View.VISIBLE
            sellerLayout.visibility = View.GONE
            reviewsLayout.visibility = View.GONE

            description_line.visibility = View.VISIBLE
            seller_line.visibility = View.GONE
            review_line.visibility = View.GONE
        }

        val sellerName: String = product.merchantName
        productSeller.setOnClickListener {
            noReviewText.visibility = GONE
            showSellerDetails(view, sellerName)
        }
        sellerTitle.setOnClickListener {
            noReviewText.visibility = GONE
            showSellerDetails(view, sellerName)
        }

        productReviews.setOnClickListener {
            descriptionLayout.visibility = View.GONE
            sellerLayout.visibility = View.GONE
            reviewsLayout.visibility = View.VISIBLE

            description_line.visibility = View.GONE
            seller_line.visibility = View.GONE
            review_line.visibility = View.VISIBLE

            fetchAndDisplayReviews()
        }
        reviewTitle.setOnClickListener {
            descriptionLayout.visibility = View.GONE
            sellerLayout.visibility = View.GONE
            reviewsLayout.visibility = View.VISIBLE

            description_line.visibility = View.GONE
            seller_line.visibility = View.GONE
            review_line.visibility = View.VISIBLE

            fetchAndDisplayReviews()
        }

        addToCartBtn.setOnClickListener {
            if (isService) {
                addToWishlist(product)
            } else {
                addToCart(product)
            }
        }

    }


    private fun updateProductRating(view: View) {
        val productRating = view.findViewById<TextView>(R.id.productRating)
        val averageRating = String.format("%.2f", product.averageRating.toFloat())
        productRating.text = "$averageRating (${product.reviewCount})"
    }

    private fun fetchAndDisplayReviews() {
        reviewsLayout.removeAllViews() // Clear existing views

        val reviewsRef = db.collection("merchants").document(product.merchantName)
            .collection(collectionName).document(product.name)
            .collection("reviews")


        reviewsRef.get()
            .addOnSuccessListener { documents ->
                val size = documents.size()

                if (size == 0) {
                    noReviewText.visibility = VISIBLE
                    Toast.makeText(requireContext(), "No reviews found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                } else {
                    noReviewText.visibility = GONE
                }

                for (document in documents) {
//                    val reviewId = document.id
                    val username = document.getString("username") ?: "Anonymous"

                    // Fetch user data for each review
                    db.collection("users").document(username)
                        .get()
                        .addOnSuccessListener { userDocument ->
                            createReviewView(document, userDocument)
                            Toast.makeText(requireContext(), "Review created for $username", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ProductDetailFragment", "Error fetching user data for $username", exception)
                            createReviewView(document, null)
                            Toast.makeText(requireContext(), "Error fetching user data for $username", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching reviews: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProductDetailFragment", "Error fetching reviews", exception)
            }
    }

    private fun createReviewView(document: DocumentSnapshot, userDocument: DocumentSnapshot?) {
        val reviewView = layoutInflater.inflate(R.layout.item_review, null)

        val userProfileImage = reviewView.findViewById<ImageView>(R.id.userProfileImage)
        val usernameTextView = reviewView.findViewById<TextView>(R.id.usernameTextView)
        val ratingBar = reviewView.findViewById<RatingBar>(R.id.ratingBar)
        val reviewTextView = reviewView.findViewById<TextView>(R.id.reviewTextView)
        val dateTextView = reviewView.findViewById<TextView>(R.id.date)

        val username = document.getString("username") ?: "Anonymous"
        val rating = document.getLong("rating")?.toFloat() ?: 0f
        val review = document.getString("review") ?: ""

        // Format the review date
        val timestampValue = document.getTimestamp("timestamp")
        if (timestampValue != null) {
            val date = timestampValue.toDate()
            val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(date)
            dateTextView.text = formattedDate
        } else {
            dateTextView.text = "N/A"
        }

        usernameTextView.text = username
        ratingBar.rating = rating
        reviewTextView.text = review

        // Load user profile image
        val profileImageUrl = userDocument?.getString("profileImage")
        if (profileImageUrl != null) {
            Glide.with(this)
                .load(profileImageUrl)
                .error(R.drawable.account_icon_lightgrey)
                .circleCrop()
                .into(userProfileImage)
        } else {
            userProfileImage.setImageResource(R.drawable.account_icon_lightgrey)
        }

        reviewsLayout.addView(reviewView)
    }

    private fun addToWishlist(product: Product) {
        val wishlistRef = db.collection("users").document(username).collection("wishlist").document(product.name)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(wishlistRef)
            if (snapshot.exists()) {
                throw Exception("Service already in wishlist")
            } else {
                val wishlistItem = hashMapOf(
                    "serviceName" to product.name,
                    "addressName" to product.addressName,
                    "price" to product.price,
                    "merchantName" to product.merchantName,
                    "imageUrl" to product.imageUrl,
                    "startHour" to product.startHour,
                    "endHour" to product.endHour,
                    "unavailableDays" to product.unavailableDays,
                    "unavailableDates" to product.unavailableDates
                )
                transaction.set(wishlistRef, wishlistItem)
            }
        }.addOnSuccessListener {
            Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCart(product: Product) {
        collectionName = if (isService) "wishlist" else "cart"
        val cartRef = db.collection("users").document(username).collection(collectionName).document(product.name)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(cartRef)
            if (snapshot.exists()) {
                val currentQuantity = snapshot.getLong("quantity") ?: 0
                if (currentQuantity < product.stock) {
                    transaction.update(cartRef, "quantity", currentQuantity + 1)
                } else {
                    throw Exception("Cannot exceed available stock")
                }
            } else {
                val cartItem = hashMapOf(
                    "productName" to product.name,
                    "price" to product.price,
                    "quantity" to 1,
                    "merchantName" to product.merchantName,
                    "imageUrl" to product.imageUrl
                )
                transaction.set(cartRef, cartItem)
            }
        }.addOnSuccessListener {
            val notice = if (isService) "wishlist" else "cart"
            Toast.makeText(context, "Added to $notice", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to add to cart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSellerDetails(view: View, sellerName: String) {
        val descriptionLayout = view.findViewById<LinearLayout>(R.id.descriptionLayout)
        val reviewsLayout = view.findViewById<LinearLayout>(R.id.reviewsLayout)
        val sellerLayout = view.findViewById<LinearLayout>(R.id.sellerLayout)

        val description_line: View = view.findViewById(R.id.description_line)
        val seller_line: View = view.findViewById(R.id.seller_line)
        val review_line: View = view.findViewById(R.id.review_line)

        descriptionLayout.visibility = View.GONE
        sellerLayout.visibility = View.VISIBLE
        reviewsLayout.visibility = View.GONE

        description_line.visibility = View.GONE
        seller_line.visibility = View.VISIBLE
        review_line.visibility = View.GONE

        // Populate seller details
        val tvMerchantName = view.findViewById<TextView>(R.id.tvMerchantName)
        val product_service_amount = view.findViewById<TextView>(R.id.product_service_amount)
        val sellerAddress = view.findViewById<TextView>(R.id.sellerAddress)

        db.collection("merchants").document(sellerName).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    tvMerchantName.text = document.id
                    val ivMerchantStore = view.findViewById<ImageView>(R.id.ivMerchantStore)
                    Glide.with(this)
                        .load(document.getString("profileImage"))
                        .circleCrop()
                        .error(R.drawable.account_icon_lightgrey)
                        .into(ivMerchantStore)
                    val productCount = document.getLong("productAmount") ?: 0
                    val serviceCount = document.getLong("serviceAmount") ?: 0
                    product_service_amount.text = "This store is providing $productCount products and $serviceCount services."

                    // Fetch the full address using product.addressName
                    fetchMerchantAddress(sellerName, product.addressName, sellerAddress)
                } else {
                    tvMerchantName.text = "N/A"
                    product_service_amount.text = "This store is providing 0 products and 0 services."
                    sellerAddress.text = "N/A"
                }
            }
            .addOnFailureListener { e ->
                tvMerchantName.text = "Error"
                product_service_amount.text = "Error fetching details"
                sellerAddress.text = "Error"
            }

        // Handle click on merchant profile
        val ivToMerchantProfile = view.findViewById<ImageView>(R.id.ivToMerchantProfile)
        ivToMerchantProfile.setOnClickListener {

            val fragment = MerchantProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            bundle.putString("sellerName", product.merchantName)
            bundle.putString("sourceFragment", sourceFragment)
            fragment.arguments = bundle
            (activity as? MainActivity)?.replaceFragment(fragment, "MerchantProfileFragment")
        }
    }

    private fun fetchMerchantAddress(sellerName: String, addressName: String, sellerAddress: TextView) {
        db.collection("merchants").document(sellerName)
            .collection("addresses").document(addressName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fullAddress = document.getString("address")
                    sellerAddress.text = fullAddress
                } else {
                    sellerAddress.text = "Address not found"
                }
            }
            .addOnFailureListener { e ->
                sellerAddress.text = "Error fetching address"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
