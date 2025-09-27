package com.example.fyp

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProductListFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var homeButton: ImageButton

    private lateinit var gridLayout: GridLayout
    private var category: String? = null
    private var searchQuery: String? = null

    private var scrollPosition: Int = 0
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        category = arguments?.getString("category").toString()
        searchQuery = arguments?.getString("searchQuery").toString()
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun updateProfileButtonAppearance() {
        homeButton?.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.home_icon_white)
        }
    }

    fun handleBackNavigation() {
        (activity as? MainActivity)?.popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_list, container, false)
        gridLayout = view.findViewById(R.id.gridLayoutProducts)
        scrollView = view.findViewById(R.id.scrollView) // Make sure you have a ScrollView in your layout

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

        val back: ImageView = view.findViewById(R.id.back)
        back.setOnClickListener {
            handleBackNavigation()
        }

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchQuery = query
                    gridLayout.removeAllViews()
                    loadProducts()
                }
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        val categorySpinner: Spinner = view.findViewById(R.id.categorySpinner)
        val categoryAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.categories_products, // Replace with your array of categories
            android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                category = parent?.getItemAtPosition(position).toString()
                gridLayout.removeAllViews()
                loadProducts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                category = null
                gridLayout.removeAllViews()
                loadProducts()
            }
        }

        // Set the spinner to the selected category
        category?.let {
            val position = categoryAdapter.getPosition(it)
            if (position >= 0) {
                searchQuery = null
                categorySpinner.setSelection(position)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore scroll position after the view has been laid out
        view.post {
            scrollView.scrollTo(0, scrollPosition)
        }
    }

    override fun onPause() {
        super.onPause()
        scrollPosition = scrollView.scrollY
    }

    private fun loadProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("merchants").get()
            .addOnSuccessListener { merchants ->
                val merchantList = mutableListOf<String>()
                for (merchant in merchants) {
                    val eachMerchantName = merchant.id
                    if (!eachMerchantName.equals(merchantName)) {
                        merchantList.add(eachMerchantName)

                    }
                }

                if (merchantList.isNotEmpty()) {
                    for (eachMerchantName in merchantList) {
                        fetchProductsForMerchant(eachMerchantName)
                    }
                } else {
                    Toast.makeText(requireContext(), "No merchants found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error getting merchants: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchProductsForMerchant(merchantName: String) {
        val db = FirebaseFirestore.getInstance()
        val queries = mutableListOf<Query>()

        try {
            when (category) {
                "Products" -> {
                    val productCollection = db.collection("merchants").document(merchantName).collection("products")
                    queries.add(productCollection)
                }
                "Services" -> {
                    val serviceCollection = db.collection("merchants").document(merchantName).collection("services")
                    queries.add(serviceCollection)
                }
                else -> {
                    val productCollection = db.collection("merchants").document(merchantName).collection("products")
                    val serviceCollection = db.collection("merchants").document(merchantName).collection("services")
//                    queries.addAll(listOf(productCollection, serviceCollection))
                    queries.add(productCollection)
                    queries.add(serviceCollection)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error creating queries: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        if (queries.isEmpty()) {
            Toast.makeText(requireContext(), "No queries to execute.", Toast.LENGTH_SHORT).show()
            return
        }

        for (query in queries) {
            var queryRef = query as Query
            if (category != null && category != "Products" && category != "Services") {
                queryRef = queryRef.whereEqualTo("category", category)
            }

            queryRef.get()
                .addOnSuccessListener { documents ->
                    val itemList = mutableListOf<Pair<Product, Int>>()

                    try {
                        for (document in documents) {
                            val itemName = document.id
                            var is_Service = document.getString("isService")

                            if (is_Service.equals("true")) {
                                try {
                                    val service = document.toObject(Product::class.java)?.apply {
                                        name = itemName
//                                        merchantName = merchantName
                                        startHour = document.getString("startHour") ?: ""
                                        endHour = document.getString("endHour") ?: ""
                                        unavailableDays = document.get("unavailableDays") as? List<String> ?: listOf()
                                        unavailableDates = document.get("unavailableDates") as? List<String> ?: listOf()
                                        isService = "true"
                                    }
                                    if (service != null) {
                                        service.merchantName = merchantName
                                    }

                                    service?.let {
                                        if (searchQuery.isNullOrEmpty()) {
                                            itemList.add(Pair(it, 0))
                                        } else {
                                            val score = calculateSearchScore(it.name, it.description, searchQuery!!)
                                            if (score > 0) {
                                                itemList.add(Pair(it, score))
                                            } else {

                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "error: ${e}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else {
                                val product = document.toObject(Product::class.java).apply {
                                    name = itemName
//                                    sellerName = merchantName
                                    isService = "false"
                                }
                                product.merchantName = merchantName

                                if (product.stock != 0) {
                                    if (searchQuery.isNullOrEmpty()) {
                                        itemList.add(Pair(product, 0))
                                    } else {
                                        val score = calculateSearchScore(
                                            product.name,
                                            product.description,
                                            searchQuery!!
                                        )
                                        if (score > 0) {
                                            itemList.add(Pair(product, score))
                                        }
                                    }
                                }
                            }
                        }
                    }catch (e:Exception){
                        Toast.makeText(
                            requireContext(),
                            "Error : ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    val sortedItemList = if (searchQuery.isNullOrEmpty()) {
                        itemList
                    } else {
                        itemList.sortedByDescending { it.second }
                    }

                    for ((item, _) in sortedItemList) {
                        addProductToGrid(item)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error getting items for $merchantName: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun calculateSearchScore(name: String, description: String, searchQuery: String): Int {
        val keywordList = searchQuery.split(" ")
        return keywordList.count { keyword ->
            name.contains(keyword, ignoreCase = true) || description.contains(keyword, ignoreCase = true)
        }
    }

    private fun addProductToGrid(product: Product) {
        val productView = layoutInflater.inflate(R.layout.item_product, null)
        val productName = productView.findViewById<TextView>(R.id.productName)
        val productPrice = productView.findViewById<TextView>(R.id.productPrice)
        val productRating = productView.findViewById<TextView>(R.id.productRating)
        val productImage = productView.findViewById<ImageView>(R.id.productImage)

        productName.text = product.name
        productPrice.text = "RM " + product.price.toString()
//        productRating.text = "${product.averageRating.toString()} (${product.reviewCount})"
        productRating.text = String.format("%.2f (%d)", product.averageRating, product.reviewCount)
        Glide.with(this).load(product.imageUrl).into(productImage)

        val edit_button = productView.findViewById<TextView>(R.id.edit_button)
        edit_button.visibility = GONE

        productView.setOnClickListener {
            val fragment = ProductDetailFragment()
            val bundle = Bundle()
            bundle.putParcelable("product", product)
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            bundle.putString("sourceFragment", "ProductListFragment")
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "ProductDetailFragment")
        }

        val params = GridLayout.LayoutParams()
        params.width = (resources.displayMetrics.widthPixels / 3) - 20
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(10, 10, 10, 10)
        productView.layoutParams = params

        gridLayout.addView(productView)
    }
}
