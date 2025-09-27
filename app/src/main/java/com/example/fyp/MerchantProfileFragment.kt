package com.example.fyp

import android.graphics.Outline
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class MerchantProfileFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var sourceFragment: String
    private var sellerName: String = ""
    private lateinit var product: Product
    private lateinit var homeButton: ImageButton
    private lateinit var db: FirebaseFirestore

    private lateinit var ivMerchantStore: ImageView
    private lateinit var tvMerchantName: TextView
    private lateinit var merchantDescription: TextView
    private lateinit var productListingBtn: TextView
    private lateinit var serviceListingBtn: TextView
    private lateinit var productsListingLayout: LinearLayout
    private var isProductListing = true
    private lateinit var productsGridLayout: GridLayout
    private lateinit var showButtonText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        sellerName = arguments?.getString("sellerName").toString()
        product = arguments?.getParcelable("product") ?: Product()
        sourceFragment = arguments?.getString("sourceFragment").toString()

        db = FirebaseFirestore.getInstance()
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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_merchant, container, false)

        homeButton = view.findViewById(R.id.homeButton)

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

        if (sourceFragment == "MerchantFragment") {
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

        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        ivEdit.visibility = GONE

        val showProductButton: RelativeLayout = view.findViewById(R.id.showProductButton)
        showProductButton.visibility = GONE

        val manage_address: TextView = view.findViewById(R.id.manage_address)
        manage_address.visibility = GONE

        val current_order: TextView = view.findViewById(R.id.current_order)
        current_order.visibility = GONE

        val current_appointment: TextView = view.findViewById(R.id.current_appointment)
        current_appointment.visibility = GONE

        val uploadProductButton: RelativeLayout = view.findViewById(R.id.uploadProductButton)
        uploadProductButton.visibility = GONE

        val removeMerchantAccountButton: Button = view.findViewById(R.id.removeMerchantAccountButton)
        removeMerchantAccountButton.visibility = GONE

        val back: ImageView = view.findViewById(R.id.back)
        back.visibility = VISIBLE
        back.setOnClickListener {
//            val fragment = ProductDetailFragment()
//            val bundle = Bundle()
//            bundle.putParcelable("product", product)
//            bundle.putString("username", username)
//            bundle.putString("merchantName", merchantName)
//            bundle.putString("sourceFragment", sourceFragment)
//            fragment.arguments = bundle
//            replaceFragment(fragment)
            handleBackNavigation()
        }


        ivMerchantStore = view.findViewById(R.id.ivMerchantStore)
        tvMerchantName = view.findViewById(R.id.tvMerchantName)
        merchantDescription = view.findViewById(R.id.merchantDescription)
        productListingBtn = view.findViewById(R.id.productsListingButton)
        serviceListingBtn = view.findViewById(R.id.servicesListingButton)
        productsListingLayout = view.findViewById(R.id.productsListing)

        productListingBtn.setOnClickListener {
            toggleListing(true)
        }

        serviceListingBtn.setOnClickListener {
            toggleListing(false)
        }

        productsGridLayout = view.findViewById(R.id.productsGridLayout)
        productsListingLayout.visibility = GONE
        productsGridLayout.visibility = VISIBLE

        fetchMerchantData(sellerName)
        toggleListing(true)

        return view
    }

    fun handleBackNavigation() {
        if (activity is MainActivity) {
            (activity as MainActivity).popBackStack()
        }
    }

    private fun populateProductsInGrid(sellerName: String, productOrService: String) {
        productsGridLayout.removeAllViews()
        db.collection("merchants").document(sellerName).collection(productOrService).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val productView = LayoutInflater.from(context).inflate(R.layout.item_product, productsGridLayout, false)
                    val tvProductName = productView.findViewById<TextView>(R.id.productName)
                    val tvProductPrice = productView.findViewById<TextView>(R.id.productPrice)
                    val productRating = productView.findViewById<TextView>(R.id.productRating)
                    val ivProductImage = productView.findViewById<ImageView>(R.id.productImage)

                    val edit_button = productView.findViewById<TextView>(R.id.edit_button)
                    edit_button.visibility = GONE

                    tvProductName.text = document.id
                    tvProductPrice.text = "$${document.getDouble("price")}"
                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl).into(ivProductImage)
                    }
                    productRating.text = String.format("%.2f (%d)", document.getDouble("averageRating")?:0.00, document.getLong("reviewCount")?:0)

                    val productName = document.id
                    val product = document.toObject(Product::class.java)
                    product.name = productName
                    productView.setOnClickListener{

                        if (productOrService == "products") {
                            product!!.isService = "false"
                        } else {
                            product!!.isService = "true"
                        }
//                        val fragment = ProductDetailFragment()
//                        val bundle = Bundle().apply {
//                            putString("username", username)
//                            putString("merchantName", merchantName)
//                            putParcelable("product", product)
//                            putString("sourceFragment", "MerchantProfileFragment")
//                        }
//                        fragment.arguments = bundle
//                        replaceFragment(fragment, "ProductDetailFragment")

                        val fragment = ProductDetailFragment()
                        val bundle = Bundle()
                        bundle.putString("username", username)
                        bundle.putString("merchantName", merchantName)
                        bundle.putParcelable("product", product)
                        fragment.arguments = bundle
                        (activity as? MainActivity)?.replaceFragment(fragment, "ProductDetailFragment")
                    }

                    val params = GridLayout.LayoutParams()
                    params.width = (resources.displayMetrics.widthPixels / 3) - 20
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT
                    params.setMargins(10, 10, 10, 10)
                    productView.layoutParams = params

                    productsGridLayout.addView(productView)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchMerchantData(sellerName: String,) {
        db.collection("merchants").document(sellerName).get().addOnSuccessListener { document ->
            if (document.exists()) {
                tvMerchantName.text = sellerName
                merchantDescription.text = document.getString("description")
                val imageUrl = document.getString("profileImage")
                if (!imageUrl.isNullOrEmpty()) {
                    ivMerchantStore.setBackgroundResource(R.drawable.circular_border)
                    ivMerchantStore.setCircularImage()
                    Glide.with(this).load(imageUrl).circleCrop().into(ivMerchantStore)
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error fetching merchant data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleListing(isProduct: Boolean) {
        isProductListing = isProduct
        productsListingLayout.removeAllViews()
        if (isProductListing) {
            productListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service)
            productListingBtn.setTypeface(productListingBtn.typeface, Typeface.BOLD)
            serviceListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service_not_select)
            serviceListingBtn.setTypeface(serviceListingBtn.typeface, Typeface.NORMAL)
            populateProductsInGrid(sellerName, "products")
        } else {
            serviceListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service)
            serviceListingBtn.setTypeface(serviceListingBtn.typeface, Typeface.BOLD)
            productListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service_not_select)
            productListingBtn.setTypeface(productListingBtn.typeface, Typeface.NORMAL)
            populateProductsInGrid(sellerName, "services")
        }
    }

}