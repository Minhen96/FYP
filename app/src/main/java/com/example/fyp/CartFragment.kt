package com.example.fyp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.properties.Delegates

class CartFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var cartButton: ImageButton
    private lateinit var productsCartTab: TextView
    private lateinit var servicesWishlistTab: TextView
    private lateinit var productsLine: View
    private lateinit var servicesLine: View
    private lateinit var itemContainer: LinearLayout
    private lateinit var totalPriceTextView: TextView
    private lateinit var totalBar: RelativeLayout
    private var currentTab = "products"

    private var totalPrice = 0.0
    private var stockAmount by Delegates.notNull<Int>() // Replace with actual stock amount from your data source
    private var currentAmount: Int = 1

    private val db = FirebaseFirestore.getInstance()
    private val checkedItems = mutableMapOf<String, Map<String, Any>>()
    private val itemPrices = mutableMapOf<String, Double>() // Map to hold item prices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
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
    }

    private fun updateProfileButtonAppearance() {
        cartButton.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.cart_icon_white)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        cartButton = view.findViewById(R.id.cartButton)
        updateProfileButtonAppearance()

        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
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

        productsCartTab = view.findViewById(R.id.productsCartTab)
        servicesWishlistTab = view.findViewById(R.id.servicesWishlistTab)
        productsLine = view.findViewById(R.id.products_line)
        servicesLine = view.findViewById(R.id.services_line)
        itemContainer = view.findViewById(R.id.itemContainer)
        totalPriceTextView = view.findViewById(R.id.totalPriceTextView)
        totalBar = view.findViewById(R.id.totalBar)


        stockAmount = 10

        setupTabLayout()
        loadCartItems()

        val checkoutButton = view.findViewById<TextView>(R.id.checkoutBtn)
        checkoutButton.setOnClickListener {
            if (checkedItems.isEmpty()) {
                Toast.makeText(context, "Please select items for checkout", Toast.LENGTH_SHORT).show()
            } else {
                val checkedItemsList = checkedItems.map { (productName, details) ->
                    mapOf(
                        "productName" to productName,
                        "quantity" to details["quantity"],
                        "price" to details["price"],
                        "merchantName" to details["merchantName"],
                        "imageUrl" to details["imageUrl"]
                    )
                }
                val fragment = CheckoutFragment()
                val bundle = Bundle()
                bundle.putString("username", username)
                bundle.putString("merchantName", merchantName)
                bundle.putSerializable("checkedItems", ArrayList(checkedItemsList))
                bundle.putBoolean("isService", false)
                fragment.arguments = bundle
                replaceFragment(fragment)
            }
        }

        return view
    }

    private fun setupTabLayout() {
        productsCartTab.setOnClickListener {
            currentTab = "products"
            updateTabIndicator()
            loadCartItems()
            totalBar.visibility = VISIBLE
        }

        servicesWishlistTab.setOnClickListener {
            currentTab = "services"
            updateTabIndicator()
            loadCartItems()
            totalBar.visibility = GONE
        }
    }

    private fun updateTabIndicator() {
        productsLine.visibility = if (currentTab == "products") View.VISIBLE else View.GONE
        servicesLine.visibility = if (currentTab == "services") View.VISIBLE else View.GONE
    }

    private fun loadCartItems() {
        val collectionName = if (currentTab == "products") "cart" else "wishlist"
        db.collection("users").document(username).collection(collectionName)
            .get()
            .addOnSuccessListener { documents ->

                val items = documents.mapNotNull { it.toObject(CartItem::class.java) }
                val groupedItems = items.groupBy { it.merchantName }
                displayItems(groupedItems)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load items", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayItems(groupedItems: Map<String, List<CartItem>>) {
        itemContainer.removeAllViews()
        totalPrice = 0.0
        checkedItems.clear()
        itemPrices.clear()

        for ((seller, items) in groupedItems) {
            val sellerView = layoutInflater.inflate(R.layout.item_cart_header, itemContainer, false)
            sellerView.findViewById<TextView>(R.id.sellerNameTextView).text = seller
            sellerView.tag = seller  // Add this line to set the tag
            itemContainer.addView(sellerView)

            for (item in items) {
                val itemView = layoutInflater.inflate(R.layout.item_cart, itemContainer, false)
                val checkBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                val imageView = itemView.findViewById<ImageView>(R.id.itemImageView)
                val priceTextView = itemView.findViewById<TextView>(R.id.itemPriceTextView)

                priceTextView.text = "RM%.2f".format(item.price)
                Glide.with(this).load(item.imageUrl).into(imageView)

                itemPrices[item.productName] = item.price

                if (currentTab == "products") {
                    db.collection("merchants").document(item.merchantName).collection("products").document(item.productName)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                item.stock = document.getLong("stock")?.toInt()!!

                                setupItemView(itemView, item, item.stock)
                            }
                            else{
                                val checkBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                                val quantityLayout = itemView.findViewById<LinearLayout>(R.id.plus_minus)
                                val statusTextView = itemView.findViewById<TextView>(R.id.statusTextView) // You'll need to add this to your layout

                                val minusButton = itemView.findViewById<ImageView>(R.id.minusButton)
                                val plusButton = itemView.findViewById<ImageView>(R.id.plusButton)
                                val quantityEditText = itemView.findViewById<EditText>(R.id.quantityEditText)
                                val deleteButton = itemView.findViewById<ImageView>(R.id.deleteButton)

                                checkBox.visibility = View.GONE
                                quantityLayout.visibility = View.VISIBLE
                                minusButton.visibility = GONE
                                plusButton.visibility = GONE
                                quantityEditText.visibility = GONE
                                deleteButton.visibility = VISIBLE
                                statusTextView.visibility = View.VISIBLE
                                statusTextView.text = "Item no longer be sold."

                                deleteButton.setOnClickListener {
                                    removeItemFromFirestore(item)
                                    itemContainer.removeView(itemView)
                                }
                            }
                        }
                    setupProductView(itemView, item, checkBox)
                } else {
                    if (currentTab == "services") {
                        db.collection("merchants").document(item.merchantName)
                            .collection("services").document(item.serviceName)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val bookButton = itemView.findViewById<TextView>(R.id.bookButton)
                                    bookButton.visibility = View.VISIBLE
                                    bookButton.setOnClickListener {
                                        navigateToCheckout(listOf(item))
                                    }
                                } else {
                                    val checkBox =
                                        itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                                    val quantityLayout =
                                        itemView.findViewById<LinearLayout>(R.id.plus_minus)
                                    val statusTextView2 =
                                        itemView.findViewById<TextView>(R.id.statusTextView2) // You'll need to add this to your layout

                                    val bookingOption = itemView.findViewById<RelativeLayout>(R.id.bookingOption)
                                    bookingOption.visibility = VISIBLE
                                    val removeWishButton = itemView.findViewById<ImageView>(R.id.removeWishButton)
                                    removeWishButton.visibility = VISIBLE

                                    checkBox.visibility = View.GONE
                                    quantityLayout.visibility = View.GONE
                                    statusTextView2.visibility = View.VISIBLE
                                    statusTextView2.text = "Item no longer be sold."


                                    val bookButton = itemView.findViewById<TextView>(R.id.bookButton)
                                    bookButton.visibility = View.GONE

                                    removeWishButton.setOnClickListener {
                                        removeItemFromFirestore(item)
                                        itemContainer.removeView(itemView)
                                    }
                                }
                            }
                        setupServiceView(itemView, item, checkBox)
                    }
                }

                setupProductOrServiceDetails(item, itemView)

                itemContainer.addView(itemView)
            }
        }
    }

    private fun setupItemView(itemView: View, item: CartItem, currentStock: Int) {
        val checkBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
        val quantityLayout = itemView.findViewById<LinearLayout>(R.id.plus_minus)
        val statusTextView = itemView.findViewById<TextView>(R.id.statusTextView) // You'll need to add this to your layout

        val minusButton = itemView.findViewById<ImageView>(R.id.minusButton)
        val plusButton = itemView.findViewById<ImageView>(R.id.plusButton)
        val quantityEditText = itemView.findViewById<EditText>(R.id.quantityEditText)
        val deleteButton = itemView.findViewById<ImageView>(R.id.deleteButton)

        when {
//            currentStock == null -> {
//                // Product no longer exists
//                checkBox.visibility = View.GONE
//                quantityLayout.visibility = View.VISIBLE
//                minusButton.visibility = GONE
//                plusButton.visibility = GONE
//                quantityEditText.visibility = GONE
//                deleteButton.visibility = VISIBLE
//                statusTextView.visibility = View.VISIBLE
//                statusTextView.text = "Item no longer be sold."
//            }
            currentStock == 0 -> {
                // Out of stock
                checkBox.visibility = View.GONE
                quantityLayout.visibility = View.VISIBLE
                minusButton.visibility = GONE
                plusButton.visibility = GONE
                quantityEditText.visibility = GONE
                deleteButton.visibility = VISIBLE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = "Out of stock"
            }
            else -> {
                // In stock
                checkBox.visibility = View.VISIBLE
                quantityLayout.visibility = View.VISIBLE
                minusButton.visibility = VISIBLE
                plusButton.visibility = VISIBLE
                quantityEditText.visibility = VISIBLE
                deleteButton.visibility = VISIBLE
                statusTextView.visibility = View.GONE
                setupProductView(itemView, item, checkBox)
            }
        }

        deleteButton.setOnClickListener {
            removeItemFromFirestore(item)
            itemContainer.removeView(itemView)
        }

        // Always show delete button
        itemView.findViewById<ImageView>(R.id.deleteButton).visibility = View.VISIBLE
    }

    private fun setupProductView(itemView: View, item: CartItem, checkBox: CheckBox) {
        val nameTextView = itemView.findViewById<TextView>(R.id.itemNameTextView)
        nameTextView.text = item.productName

        val bookingOption = itemView.findViewById<RelativeLayout>(R.id.bookingOption)
        bookingOption.visibility = GONE

        val quantityLayout = itemView.findViewById<LinearLayout>(R.id.plus_minus)
        quantityLayout.visibility = View.VISIBLE

        val minusButton = itemView.findViewById<ImageView>(R.id.minusButton)
        val plusButton = itemView.findViewById<ImageView>(R.id.plusButton)
        val quantityEditText = itemView.findViewById<EditText>(R.id.quantityEditText)
        val deleteButton = itemView.findViewById<ImageView>(R.id.deleteButton)

        quantityEditText.setText(item.quantity.toString())

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            handleItemCheck(isChecked, item)
        }

        minusButton.setOnClickListener {
            handleQuantityChange(item, quantityEditText, checkBox, -1)
        }

        plusButton.setOnClickListener {
            handleQuantityChange(item, quantityEditText, checkBox, 1)
        }

        quantityEditText.addTextChangedListener(createQuantityTextWatcher(item, quantityEditText, checkBox))

        deleteButton.setOnClickListener {
            removeItemFromFirestore(item)
            itemContainer.removeView(itemView)
        }
    }

    private fun setupServiceView(itemView: View, item: CartItem, checkBox: CheckBox) {
        val nameTextView = itemView.findViewById<TextView>(R.id.itemNameTextView)
        nameTextView.text = item.serviceName

        val bookingOption = itemView.findViewById<RelativeLayout>(R.id.bookingOption)
        bookingOption.visibility = VISIBLE

        Toast.makeText(requireContext(), "test", Toast.LENGTH_SHORT).show()
        val quantityLayout = itemView.findViewById<LinearLayout>(R.id.plus_minus)
        quantityLayout.visibility = View.GONE

        val deleteButton = itemView.findViewById<ImageView>(R.id.removeWishButton)
        deleteButton.setOnClickListener {
            removeItemFromFirestore(item)
            itemContainer.removeView(itemView)
        }

        checkBox.visibility = View.GONE
    }

    private fun handleItemCheck(isChecked: Boolean, item: CartItem) {
        if (isChecked) {
            checkedItems[item.productName] = mapOf(
                "quantity" to item.quantity,
                "price" to item.price,
                "merchantName" to item.merchantName,
                "imageUrl" to item.imageUrl
            )
        } else {
            checkedItems.remove(item.productName)
        }
        recalculateTotalPrice()
    }

    private fun handleQuantityChange(item: CartItem, quantityEditText: EditText, checkBox: CheckBox, change: Int) {
        val newQuantity = item.quantity + change
        when {
            newQuantity < 1 -> {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
            newQuantity > item.stock -> {
                Toast.makeText(context, "Cannot exceed available stock", Toast.LENGTH_SHORT).show()
            }
            else -> {
                item.quantity = newQuantity
                quantityEditText.setText(newQuantity.toString())
                if (checkBox.isChecked) {
                    checkedItems[item.productName] = mapOf(
                        "quantity" to item.quantity,
                        "price" to item.price,
                        "merchantName" to item.merchantName,
                        "imageUrl" to item.imageUrl
                    )
                    recalculateTotalPrice()
                }
                updateItemInFirestore(item)
            }
        }
    }

    private fun createQuantityTextWatcher(item: CartItem, quantityEditText: EditText, checkBox: CheckBox): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toIntOrNull()
                when {
                    input == null || input < 1 -> {
                        item.quantity = 1
                        quantityEditText.setText("1")
                    }
                    input > item.stock -> {
                        item.quantity = item.stock
                        quantityEditText.setText(item.stock.toString())
                        Toast.makeText(context, "Cannot exceed available stock", Toast.LENGTH_SHORT).show()
                    }
                    else -> item.quantity = input
                }
                if (checkBox.isChecked) {
                    checkedItems[item.productName] = mapOf(
                        "quantity" to item.quantity,
                        "price" to item.price,
                        "merchantName" to item.merchantName,
                        "imageUrl" to item.imageUrl
                    )
                    recalculateTotalPrice()
                }
                quantityEditText.setSelection(quantityEditText.text.length)
                updateItemInFirestore(item)
            }
        }
    }

    private fun setupProductOrServiceDetails(item: CartItem, itemView: View) {
        val productOrService = if (currentTab == "products") "products" else "services"
        val dbRef = if (currentTab == "products") db.collection("merchants").document(item.merchantName).collection(productOrService).document(item.productName)
                    else db.collection("merchants").document(item.merchantName).collection(productOrService).document(item.serviceName)
        dbRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val product = if (productOrService == "services") {
                        Product(
                            name = document.id,
                            price = document.getDouble("price") ?: 0.0,
                            description = document.getString("description") ?: "",
                            category = document.getString("category") ?: "",
                            isService = "true",
                            imageUrl = document.getString("imageUrl") ?: "",
                            merchantName = document.getString("merchantName") ?: "",
                            addressName = document.getString("addressName") ?: "",
                            averageRating = document.getDouble("rating")?.toFloat() ?: 0.0f,
                            startHour = document.getString("startHour") ?: "",
                            endHour = document.getString("endHour") ?: "",
                            unavailableDays = document.get("unavailableDays") as? List<String> ?: listOf(),
                            unavailableDates = document.get("unavailableDates") as? List<String> ?: listOf()
                        )
                    } else {
                        Product(
                            name = document.id,
                            price = document.getDouble("price") ?: 0.0,
                            stock = document.getLong("stock")?.toInt() ?: 0,
                            description = document.getString("description") ?: "",
                            category = document.getString("category") ?: "",
                            isService = "false",
                            imageUrl = document.getString("imageUrl") ?: "",
                            merchantName = document.getString("merchantName") ?: "",
                            addressName = document.getString("addressName") ?: "",
                            averageRating = document.getDouble("rating")?.toFloat() ?: 0.0f
                        )
                    }

                    itemView.setOnClickListener {
                        navigateToProductDetail(product)
                    }
                } else {
                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching product details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToProductDetail(product: Product) {
        val fragment = ProductDetailFragment()
        val bundle = Bundle().apply {
            putString("username", username)
            putString("merchantName", merchantName)
            putParcelable("product", product)
            putString("sourceFragment", "CartFragment")
        }
        fragment.arguments = bundle
//        replaceFragment(fragment, "ProductDetailFragment")
        (activity as? MainActivity)?.replaceFragment(fragment, "ProductDetailFragment")
    }

    private fun navigateToCheckout(items: List<CartItem>) {
        val checkedItemsList = ArrayList<Map<String, Any>>()
        items.forEach { item ->
            checkedItemsList.add(
                mapOf(
                    "serviceName" to item.serviceName,
                    "price" to item.price,
                    "merchantName" to item.merchantName,
                    "addressName" to item.addressName,
                    "imageUrl" to item.imageUrl
                )
            )
        }
        val fragment = CheckoutFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        bundle.putSerializable("checkedItems", checkedItemsList)
        bundle.putBoolean("isService", true)
        fragment.arguments = bundle
        replaceFragment(fragment)
    }

    private fun removeItemFromFirestore(item: CartItem) {
        val collectionName = if (currentTab == "products") "cart" else "wishlist"
        val productOrService = if (currentTab == "products") "products" else "services"
        val dbRef = if (currentTab == "products") db.collection("users").document(username).collection(collectionName).document(item.productName)
                    else db.collection("users").document(username).collection(collectionName).document(item.serviceName)
        dbRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "${item.productName} removed from cart", Toast.LENGTH_SHORT).show()
                itemPrices.remove(item.productName)
                checkedItems.remove(item.productName)
                recalculateTotalPrice()

                // Check if this was the last item for the store
                checkAndRemoveEmptyStore(item.merchantName)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove ${item.productName} from cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAndRemoveEmptyStore(merchantName: String) {
        val collectionName = if (currentTab == "products") "cart" else "wishlist"
        db.collection("users").document(username).collection(collectionName)
            .whereEqualTo("merchantName", merchantName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No items left for this store, remove the store header
                    val storeHeader = itemContainer.findViewWithTag<View>(merchantName)
                    storeHeader?.let { itemContainer.removeView(it) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to check remaining items", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recalculateTotalPrice() {
        totalPrice = checkedItems.entries.sumByDouble { (_, details) ->
            val quantity = details["quantity"] as Int
            val price = details["price"] as Double
            price * quantity
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        totalPriceTextView.text = "Total Price : RM%.2f".format(totalPrice)
    }

    private fun updateItemInFirestore(item: CartItem) {
        db.collection("users").document(username).collection("cart")
            .document(item.productName)
            .set(item)
            .addOnSuccessListener {
                // Successfully updated item in Firestore
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update item", Toast.LENGTH_SHORT).show()
            }
    }
}