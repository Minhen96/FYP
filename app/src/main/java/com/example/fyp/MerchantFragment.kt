package com.example.fyp

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

class MerchantFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var profileButton: ImageButton
    private lateinit var db: FirebaseFirestore
    private lateinit var ivMerchantStore: ImageView
    private lateinit var tvMerchantName: TextView
    private lateinit var merchantDescription: TextView
    private lateinit var productListingBtn: TextView
    private lateinit var serviceListingBtn: TextView
    private lateinit var uploadProductBtn: RelativeLayout
    private lateinit var upload_button_text: TextView
    private var selectedImageUri: Uri? = null
    private lateinit var productsListingLayout: LinearLayout
    private var isProductListing = true
    private lateinit var productsListing: LinearLayout
    private lateinit var productsGridLayout: GridLayout
    private lateinit var showButtonText: TextView
    private var isGridView = false


    private lateinit var dialogUploadProductView: View
    private lateinit var dialogUploadServiceView: View
    private lateinit var dialogEditMerchantView: View
    private lateinit var dialogEditProductView: View
    private lateinit var dialogEditServiceView: View

    private var scrollX = 0
    private var scrollY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
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

    private fun saveScrollPosition() {
        if (isGridView) {
            val scrollView = view?.findViewById<ScrollView>(R.id.gridScrollView)
            scrollY = scrollView?.scrollY ?: 0
        } else {
            val horizontalScrollView = view?.findViewById<HorizontalScrollView>(R.id.productsListingLayout)
            scrollX = horizontalScrollView?.scrollX ?: 0
        }
    }

    private fun restoreScrollPosition() {
        if (isGridView) {
            val scrollView = view?.findViewById<ScrollView>(R.id.gridScrollView)
            scrollView?.post { scrollView.scrollTo(0, scrollY) }
        } else {
            val horizontalScrollView = view?.findViewById<HorizontalScrollView>(R.id.productsListingLayout)
            horizontalScrollView?.post { horizontalScrollView.scrollTo(scrollX, 0) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_merchant, container, false)

        profileButton = view.findViewById(R.id.profileButton)
        updateProfileButtonAppearance()

        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
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

        ivMerchantStore = view.findViewById(R.id.ivMerchantStore)
        tvMerchantName = view.findViewById(R.id.tvMerchantName)
        merchantDescription = view.findViewById(R.id.merchantDescription)
        productListingBtn = view.findViewById(R.id.productsListingButton)
        serviceListingBtn = view.findViewById(R.id.servicesListingButton)
        uploadProductBtn = view.findViewById(R.id.uploadProductButton)
        productsListingLayout = view.findViewById(R.id.productsListing)
        upload_button_text = view.findViewById(R.id.upload_button)

        val editButton = view.findViewById<ImageView>(R.id.ivEdit)
        editButton.setOnClickListener {
            showEditDialog()
        }

        val manage_address = view.findViewById<TextView>(R.id.manage_address)
        manage_address.setOnClickListener {
            val fragment = AddressMerchantFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "AddressMerchantFragment")
        }

        val current_order = view.findViewById<TextView>(R.id.current_order)
        current_order.setOnClickListener {
            val fragment = CheckOrderFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "CheckOrderFragment")
        }

        val current_appointment = view.findViewById<TextView>(R.id.current_appointment)
        current_appointment.setOnClickListener {
            val fragment = CheckAppointmentFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
//            replaceFragment(fragment)
            (activity as? MainActivity)?.replaceFragment(fragment, "CheckAppointmentFragment")
        }

        productListingBtn.setOnClickListener {
            toggleListing(true)
        }

        serviceListingBtn.setOnClickListener {
            toggleListing(false)
        }

        val productsListingLayout: HorizontalScrollView = view.findViewById(R.id.productsListingLayout)
        productsListing = view.findViewById(R.id.productsListing)
        productsGridLayout = view.findViewById(R.id.productsGridLayout)
        val showProductButton: RelativeLayout = view.findViewById(R.id.showProductButton)
        showButtonText = view.findViewById(R.id.show_button)

        // Toggle functionality
        showProductButton.setOnClickListener {
            isGridView = !isGridView
            if (isGridView) {
                productsListingLayout.visibility = View.GONE
                productsGridLayout.visibility = View.VISIBLE
                if(isProductListing){
                    showButtonText.text = "Hide Existing Products"
                    populateProductsInGrid("products")
                } else{
                    showButtonText.text = "Hide Existing Services"
                    populateProductsInGrid("services")
                }
            } else {
                productsListingLayout.visibility = View.VISIBLE
                productsGridLayout.visibility = View.GONE
                if(isProductListing) {
                    showButtonText.text = "Show all Existing Products"
                    populateProductsHorizontally("products")
                } else{
                    showButtonText.text = "Show all Existing Services"
                    populateProductsHorizontally("services")
                }
            }
        }

        fetchMerchantData()
        toggleListing(true)

        val removeMerchantAccountButton: Button = view.findViewById(R.id.removeMerchantAccountButton)
        removeMerchantAccountButton.setOnClickListener {
            val message = "Are you sure you want to remove the merchant account? \nThe products and services will also be removed."
            showConfirmationDialog(requireContext(), message) {
                removeMerchantAccount()
            }
        }

        return view
    }

    private fun updateProductOrServiceView(view: View, document: DocumentSnapshot, productOrService: String) {
        val tvProductName = view.findViewById<TextView>(R.id.productName)
        val tvProductPrice = view.findViewById<TextView>(R.id.productPrice)
        val productStock = view.findViewById<TextView>(R.id.productStock)
        val productRating = view.findViewById<TextView>(R.id.productRating)
        val ivProductImage = view.findViewById<ImageView>(R.id.productImage)
        val editTextButton = view.findViewById<TextView>(R.id.edit_button)

        tvProductName.text = document.id
        tvProductPrice.text = "$${document.getDouble("price")}"

        if (productOrService == "products") {
            productStock.visibility = View.VISIBLE
            val stock = document.getLong("stock")?.toInt() ?: 0
            if (stock == 0) {
                productStock.setTextColor(Color.RED)
                productStock.setBackgroundResource(R.color.light_red)
                productStock.text = "Out of Stock"
            } else {
                productStock.setTextColor(Color.BLACK)
                productStock.setBackgroundResource(R.color.white)
                productStock.text = "Stock: $stock"
            }
        } else {
            productStock.visibility = View.GONE
        }

        val imageUrl = document.getString("imageUrl")
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(ivProductImage)
        }

        productRating.text = String.format("%.2f (%d)", document.getDouble("averageRating") ?: 0.00, document.getLong("reviewCount") ?: 0)

        editTextButton.setOnClickListener {
            if (productOrService == "products") {
                showEditProductDialog(document.id, document.data ?: mapOf())
            } else {
                showEditServiceDialog(document.id, document.data ?: mapOf())
            }
        }

        view.setOnClickListener {
            val product = document.toObject(Product::class.java)?.apply { name = document.id }

            if (productOrService == "products") {
                product!!.isService = "false"
            } else {
                product!!.isService = "true"
            }

            val fragment = ProductDetailFragment()
            val bundle = Bundle()
            bundle.putParcelable("product", product)
            Toast.makeText(context, "isService: ${product!!.isService}", Toast.LENGTH_SHORT).show()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            bundle.putString("sourceFragment", "MerchantFragment")
            fragment.arguments = bundle
            (activity as? MainActivity)?.replaceFragment(fragment, "ProductDetailFragment")
        }
    }

    private fun populateProductsInGrid(productOrService: String) {
        db.collection("merchants").document(merchantName).collection(productOrService)
            .orderBy("modifiedAt", Query.Direction.DESCENDING)  // Add this line
            .get()
            .addOnSuccessListener { documents ->
                val existingViews = productsGridLayout.childCount
                documents.forEachIndexed { index, document ->
                    if (index < existingViews) {
                        // Update existing view
                        val view = productsGridLayout.getChildAt(index)
                        updateProductOrServiceView(view, document, productOrService)
                    } else {
                        // Add new view
                        val productView = LayoutInflater.from(context)
                            .inflate(R.layout.item_product, productsGridLayout, false)
                        updateProductOrServiceView(productView, document, productOrService)
                        val params = GridLayout.LayoutParams().apply {
                            width = (resources.displayMetrics.widthPixels / 3) - 20
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            setMargins(10, 10, 10, 10)
                        }
                        productView.layoutParams = params
                        productsGridLayout.addView(productView)
                    }
                }
                // Remove extra views if any
                while (productsGridLayout.childCount > documents.size()) {
                    productsGridLayout.removeViewAt(productsGridLayout.childCount - 1)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error fetching products: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun populateProductsHorizontally(productOrService: String) {
        db.collection("merchants").document(merchantName).collection(productOrService)
            .orderBy("modifiedAt", Query.Direction.DESCENDING)  // Add this line
            .get()
            .addOnSuccessListener { documents ->
                db.collection("merchants").document(merchantName).collection(productOrService).get()
                    .addOnSuccessListener { documents ->
                        val existingViews = productsListing.childCount
                        documents.forEachIndexed { index, document ->
                            if (index < existingViews) {
                                // Update existing view
                                val view = productsListing.getChildAt(index)
                                updateProductOrServiceView(view, document, productOrService)
                            } else {
                                // Add new view
                                val productView = LayoutInflater.from(context)
                                    .inflate(R.layout.item_product, productsListing, false)
                                updateProductOrServiceView(productView, document, productOrService)
                                productsListing.addView(productView)
                            }
                        }
                        // Remove extra views if any
                        while (productsListing.childCount > documents.size()) {
                            productsListing.removeViewAt(productsListing.childCount - 1)
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error fetching products: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    //about service
    private fun showUploadServiceDialog() {
        dialogUploadServiceView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_service, null)
        val etServiceName = dialogUploadServiceView.findViewById<EditText>(R.id.etServiceName)
        val etServicePrice = dialogUploadServiceView.findViewById<EditText>(R.id.etServicePrice)
        val etServiceDescription = dialogUploadServiceView.findViewById<EditText>(R.id.etServiceDescription)
        val ivServiceImage = dialogUploadServiceView.findViewById<ImageView>(R.id.ivServiceImage)
        val categorySpinner: Spinner = dialogUploadServiceView.findViewById(R.id.spinnerServiceCategory)
        val addressSpinner: Spinner = dialogUploadServiceView.findViewById(R.id.spinnerProductAddress)
        val llSelectedDates = dialogUploadServiceView.findViewById<LinearLayout>(R.id.llSelectedDates)
        val etStartHour = dialogUploadServiceView.findViewById<EditText>(R.id.etStartHour)
        val etEndHour = dialogUploadServiceView.findViewById<EditText>(R.id.etEndHour)

        populateAddressSpinner(addressSpinner)

        val unavailableDays = mutableListOf<String>()
        val unavailableDates = mutableListOf<String>()

        // Initialize UI elements and event listeners
        val mondayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.mondayCheckbox)
        mondayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Monday")
            } else {
                unavailableDays.remove("Monday")
            }
        }
        val tuesdayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.tuesdayCheckbox)
        tuesdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Tuesday")
            } else {
                unavailableDays.remove("Tuesday")
            }
        }
        val wednesdayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.wednesdayCheckbox)
        wednesdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Wednesday")
            } else {
                unavailableDays.remove("Wednesday")
            }
        }
        val thursdayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.thursdayCheckbox)
        thursdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Thursday")
            } else {
                unavailableDays.remove("Thursday")
            }
        }
        val fridayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.fridayCheckbox)
        fridayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Friday")
            } else {
                unavailableDays.remove("Friday")
            }
        }
        val saturdayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.saturdayCheckbox)
        saturdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Saturday")
            } else {
                unavailableDays.remove("Saturday")
            }
        }
        val sundayCheckbox = dialogUploadServiceView.findViewById<CheckBox>(R.id.sundayCheckbox)
        sundayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unavailableDays.add("Sunday")
            } else {
                unavailableDays.remove("Sunday")
            }
        }

//        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectUnavailableDatesButton = dialogUploadServiceView.findViewById<TextView>(R.id.selectUnavailableDatesButton)
                selectUnavailableDatesButton.setOnClickListener {
                    showDateRangePicker { startDate, endDate ->
                        addDateRange(unavailableDates, startDate, endDate)
                        updateSelectedDatesUI(llSelectedDates, unavailableDates)
                    }
                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {}
//        }

        ivServiceImage.setOnClickListener {
            selectImage(PICK_SERVICE_IMAGE_REQUEST)
        }

        etStartHour.addTextChangedListener(TimeInputFormatter(etStartHour))
        etEndHour.addTextChangedListener(TimeInputFormatter(etEndHour))

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogUploadServiceView)
            .create()

        val confirmBtn = dialogUploadServiceView.findViewById<Button>(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
            val serviceName = etServiceName.text.toString()
            val servicePrice = etServicePrice.text.toString().toDouble()
            val serviceDescription = etServiceDescription.text.toString()
            val serviceCategory = categorySpinner.selectedItem.toString()
            val serviceAddress = addressSpinner.selectedItem.toString()
            val startHour = etStartHour.text.toString()
            val endHour = etEndHour.text.toString()

            db.collection("merchants").document(merchantName).collection("products")
                .document(serviceName).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    etServiceName.error = "This name already exists in Products or Services."
                } else {
                    db.collection("merchants").document(merchantName).collection("services")
                        .document(serviceName).get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                etServiceName.error =
                                    "This name already exists in Products or Services."
                            } else {
                                saveServiceData(
                                    serviceName,
                                    servicePrice,
                                    serviceDescription,
                                    serviceCategory,
                                    serviceAddress,
                                    startHour,
                                    endHour,
                                    unavailableDays,
                                    unavailableDates
                                )
                            }
                        }
                }
            }
            builder.dismiss() // Close the dialog
        }

        val backBtn = dialogUploadServiceView.findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            builder.dismiss() // Close the dialog
        }
        val ivCloseDialog = dialogUploadServiceView.findViewById<ImageView>(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss() // Close the dialog
        }

        builder.show()
    }

    private fun addDateRange(unavailableDates: MutableList<String>, startDate: String, endDate: String) {
        val newStart = LocalDate.parse(startDate)
        val newEnd = LocalDate.parse(endDate)

        val iterator = unavailableDates.listIterator()
        while (iterator.hasNext()) {
            val dateRange = iterator.next().split(" to ")
            val existingStart = LocalDate.parse(dateRange[0])
            val existingEnd = LocalDate.parse(dateRange[1])

            if (newEnd < existingStart || newStart > existingEnd) {
                // No overlap
                continue
            }

            // Merge overlapping ranges
            val mergedStart = minOf(newStart, existingStart)
            val mergedEnd = maxOf(newEnd, existingEnd)
            iterator.set("$mergedStart to $mergedEnd")
            return
        }

        // Add new range if no existing range overlaps
        unavailableDates.add("$startDate to $endDate")
    }

    private fun updateSelectedDatesUI(llSelectedDates: LinearLayout, unavailableDates: List<String>) {
        llSelectedDates.removeAllViews()
        unavailableDates.forEach { dateRange ->
            val dateView = LayoutInflater.from(context).inflate(R.layout.item_selected_date, llSelectedDates, false)
            val tvDate = dateView.findViewById<TextView>(R.id.tvDate)
            val ivRemoveDate = dateView.findViewById<ImageView>(R.id.ivRemoveDate)

            tvDate.text = dateRange
            ivRemoveDate.setOnClickListener {
                (unavailableDates as MutableList).remove(dateRange)
                updateSelectedDatesUI(llSelectedDates, unavailableDates)
            }

            llSelectedDates.addView(dateView)
        }
    }

    private fun showDateRangePicker(onDateRangeSelected: (String, String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val endDatePickerDialog = DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDayOfMonth ->
                        val endDate = String.format("%04d-%02d-%02d", endYear, endMonth + 1, endDayOfMonth)
                        onDateRangeSelected(startDate, endDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                endDatePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun saveServiceData(name: String, price: Double, description: String, category: String, addressName: String, startHour: String, endHour: String, unavailableDays: List<String>, unavailableDates: List<String>) {
        val serviceData = hashMapOf(
            "merchantName" to merchantName,
            "category" to category,
            "isService" to "true",
            "price" to price,
            "description" to description,
            "imageUrl" to (selectedImageUri?.toString() ?: ""),
            "addressName" to addressName,
            "startHour" to startHour,
            "endHour" to endHour,
            "unavailableDays" to unavailableDays,
            "unavailableDates" to unavailableDates,
            "modifiedAt" to FieldValue.serverTimestamp()
        )

        db.collection("merchants").document(merchantName).collection("services").document(name).set(serviceData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Service uploaded successfully", Toast.LENGTH_SHORT).show()
                updateServiceCount(1)
                if(isGridView) {
                    populateProductsInGrid("services")
                } else {
                    populateProductsHorizontally("services")
                }
                selectedImageUri = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error uploading service: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditServiceDialog(serviceId: String, serviceData: Map<String, Any>) {
        dialogEditServiceView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_service, null)
        val etServiceName = dialogEditServiceView.findViewById<EditText>(R.id.etServiceName)
        val etServiceCategory = dialogEditServiceView.findViewById<TextView>(R.id.spinnerServiceCategory)
        val etServicePrice = dialogEditServiceView.findViewById<EditText>(R.id.etServicePrice)
        val addressSpinner: Spinner = dialogEditServiceView.findViewById(R.id.spinnerProductAddress)
        val etServiceDescription = dialogEditServiceView.findViewById<EditText>(R.id.etServiceDescription)
        val ivServiceImage = dialogEditServiceView.findViewById<ImageView>(R.id.ivServiceImage)
        val etStartHour = dialogEditServiceView.findViewById<EditText>(R.id.etStartHour)
        val etEndHour = dialogEditServiceView.findViewById<EditText>(R.id.etEndHour)
        val llSelectedDates = dialogEditServiceView.findViewById<LinearLayout>(R.id.llSelectedDates)
        val selectUnavailableDatesButton = dialogEditServiceView.findViewById<TextView>(R.id.selectUnavailableDatesButton)
        val unavailableDays = (serviceData["unavailableDays"] as? List<String>)?.toMutableList() ?: mutableListOf()
        val unavailableDates = (serviceData["unavailableDates"] as? List<String>)?.toMutableList() ?: mutableListOf()

        // Fill in existing data
        etServiceName.setText(serviceId)
        val category = serviceData["category"] as? String ?: ""
        etServiceCategory.setText("Category : ${serviceData["category"] as? String}")
        etServicePrice.setText((serviceData["price"] as? Double)?.toString())
        etServiceDescription.setText(serviceData["description"] as? String)
        etStartHour.setText(serviceData["startHour"] as? String)
        etEndHour.setText(serviceData["endHour"] as? String)
        Glide.with(this).load(serviceData["imageUrl"] as? String).into(ivServiceImage)

        etStartHour.addTextChangedListener(TimeInputFormatter(etStartHour))
        etEndHour.addTextChangedListener(TimeInputFormatter(etEndHour))

        //TODO should save whole address
//        populateAddressSpinner(addressSpinner)
//        addressSpinner.setSelection((addressSpinner.adapter as ArrayAdapter<String>).getPosition(serviceData["addressName"] as? String))
        // Populate the address spinner and set selection once done
        populateAddressSpinner(addressSpinner) {
            val addressName = serviceData["addressName"] as? String
            if (addressName != null) {
                val position = (addressSpinner.adapter as ArrayAdapter<String>).getPosition(addressName)
                addressSpinner.setSelection(position)
            }
        }

        selectUnavailableDatesButton.setOnClickListener {
            showDateRangePicker { startDate, endDate ->
                addDateRange(unavailableDates, startDate, endDate)
                updateSelectedDatesUI(llSelectedDates, unavailableDates)
            }
        }


        val mondayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.mondayCheckbox)
        mondayCheckbox.isChecked = unavailableDays.contains("Monday")
        mondayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Monday") else unavailableDays.remove("Monday")
        }
        val tuesdayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.tuesdayCheckbox)
        tuesdayCheckbox.isChecked = unavailableDays.contains("Tuesday")
        tuesdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Tuesday") else unavailableDays.remove("Tuesday")
        }
        val wednesdayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.wednesdayCheckbox)
        wednesdayCheckbox.isChecked = unavailableDays.contains("Wednesday")
        wednesdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Wednesday") else unavailableDays.remove("Wednesday")
        }
        val thursdayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.thursdayCheckbox)
        thursdayCheckbox.isChecked = unavailableDays.contains("Thursday")
        thursdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Thursday") else unavailableDays.remove("Thursday")
        }
        val fridayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.fridayCheckbox)
        fridayCheckbox.isChecked = unavailableDays.contains("Friday")
        fridayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Friday") else unavailableDays.remove("Friday")
        }
        val saturdayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.saturdayCheckbox)
        saturdayCheckbox.isChecked = unavailableDays.contains("Saturday")
        saturdayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Saturday") else unavailableDays.remove("Saturday")
        }
        val sundayCheckbox = dialogEditServiceView.findViewById<CheckBox>(R.id.sundayCheckbox)
        sundayCheckbox.isChecked = unavailableDays.contains("Sunday")
        sundayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) unavailableDays.add("Sunday") else unavailableDays.remove("Sunday")
        }

        ivServiceImage.setOnClickListener {
            selectImage(PICK_EDIT_SERVICE_IMAGE_REQUEST)
        }

        // Update UI with existing unavailable dates and times
        updateSelectedDatesUI(llSelectedDates, unavailableDates)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogEditServiceView)
            .create()

        val ivCloseDialog: ImageView = dialogEditServiceView.findViewById(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss()
        }

        val removeBtn: Button = dialogEditServiceView.findViewById(R.id.removeBtn)
        removeBtn.setOnClickListener {
            val message = "Are you sure you want to remove this service?"
            showConfirmationDialog(requireContext(), message) {
                removeService(serviceId)
                builder.dismiss()
            }
        }

        val confirmBtn: Button = dialogEditServiceView.findViewById(R.id.confirmBtn)
        confirmBtn.setOnClickListener {

            val newServiceName = etServiceName.text.toString()

            updateServiceData(
                etServiceName,
                serviceId,
                newServiceName,
                etServicePrice.text.toString().toDouble(),
                addressSpinner.selectedItem.toString(),
                etServiceDescription.text.toString(),
                etStartHour.text.toString(),
                etEndHour.text.toString(),
                unavailableDays,
                unavailableDates
            ){
                builder.dismiss()
            }

        }

        builder.show()
    }

    private fun updateServiceData(
        etServiceName: EditText,
        serviceId: String,
        newServiceName: String,
        price: Double,
        addressName: String,
        description: String,
        startHour: String,
        endHour: String,
        unavailableDays: List<String>,
        unavailableDates: List<String>,
        callback: (Boolean) -> Unit
    ) {
        val serviceRef = db.collection("merchants").document(merchantName).collection("services").document(serviceId)
        val updates = mutableMapOf<String, Any>(
            "price" to price,
            "addressName" to addressName,
            "description" to description,
            "startHour" to startHour,
            "endHour" to endHour,
            "unavailableDays" to unavailableDays,
            "unavailableDates" to unavailableDates,
            "modifiedAt" to FieldValue.serverTimestamp()
        )
        var completedCollections = 0
        var totalCollections = 2

        fun checkCompletion() {
            if (completedCollections == totalCollections) {
                callback(true)
            }
        }

        val updateData: (String?) -> Unit = { imageUrl ->
            if (imageUrl != null) {
                updates["imageUrl"] = imageUrl
            }
            serviceRef.update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Service updated successfully", Toast.LENGTH_SHORT).show()

                    if (serviceId != newServiceName) {
                        db.collection("merchants").document(merchantName).collection("products")
                            .document(newServiceName).get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    etServiceName.error =
                                        "This name already exists in Products or Services."
                                } else {
                                    db.collection("merchants").document(merchantName)
                                        .collection("services")
                                        .document(newServiceName).get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                etServiceName.error =
                                                    "This name already exists in Products or Services."
                                            } else {
                                                changeProductOrServiceDocumentId(
                                                    "services",
                                                    serviceId,
                                                    newServiceName
                                                ) {
                                                    if (isGridView) {
                                                        populateProductsInGrid("services")
                                                    } else {
                                                        populateProductsHorizontally("services")
                                                    }
                                                    selectedImageUri = null
                                                    completedCollections++
                                                    checkCompletion()
                                                }
                                            }
                                        }
                                }
                            }
                    } else{
                        if (isGridView) {
                            populateProductsInGrid("services")
                        } else {
                            populateProductsHorizontally("services")
                        }
                        selectedImageUri = null
                        completedCollections++
                        checkCompletion()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating service: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("serviceImages/$merchantName/$newServiceName")
            val uploadTask = storageRef.putFile(selectedImageUri!!)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateData(uri.toString())
                    completedCollections++
                    checkCompletion()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch image URL", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateData(null)
            completedCollections++
            checkCompletion()
        }
    }


    private fun removeService(serviceId: String) {
        db.collection("merchants").document(merchantName).collection("services").document(serviceId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Service removed successfully", Toast.LENGTH_SHORT).show()
                updateServiceCount(-1)
                if (isGridView) {
                    populateProductsInGrid("services")
                } else {
                    populateProductsHorizontally("services")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error removing service: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateServiceCount(amount: Int) {
        val merchantDocRef = db.collection("merchants").document(merchantName)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(merchantDocRef)
            val newServiceCount = snapshot.getLong("serviceAmount")?.plus(amount) ?: amount
            transaction.update(merchantDocRef, "serviceAmount", newServiceCount)
        }
    }

    private fun populateAddressSpinner(spinner: Spinner, callback: (() -> Unit)? = null) {
        db.collection("merchants").document(merchantName).collection("addresses").get()
            .addOnSuccessListener { documents ->
                val addressNames = documents.map { it.id }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, addressNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                // Invoke the callback after the spinner is populated
                callback?.invoke()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load addresses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUploadProductDialog() {
        dialogUploadProductView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_product, null)
        val etProductName = dialogUploadProductView.findViewById<EditText>(R.id.etProductName)
        val etProductPrice = dialogUploadProductView.findViewById<EditText>(R.id.etProductPrice)
        val etProductStock = dialogUploadProductView.findViewById<EditText>(R.id.etProductStock)
        val etProductDescription = dialogUploadProductView.findViewById<EditText>(R.id.etProductDescription)
        val ivProductImage = dialogUploadProductView.findViewById<ImageView>(R.id.ivProductImage)

        val addressSpinner: Spinner = dialogUploadProductView.findViewById(R.id.spinnerProductAddress)
        populateAddressSpinner(addressSpinner)

        val categorySpinner: Spinner = dialogUploadProductView.findViewById(R.id.spinnerProductCategory)

        ivProductImage.setOnClickListener {
            selectImage(PICK_PRODUCT_IMAGE_REQUEST)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogUploadProductView)
            .create()

        val confirmBtn = dialogUploadProductView.findViewById<Button>(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
            val productName = etProductName.text.toString()
            val productPrice = etProductPrice.text.toString().toDouble()
            val productStock = etProductStock.text.toString().toInt()
            val productDescription = etProductDescription.text.toString()
            val productCategory = categorySpinner.selectedItem.toString()
            val productAddress = addressSpinner.selectedItem.toString()

            db.collection("merchants").document(merchantName).collection("products").document(productName).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    etProductName.error = "This name already exists in Products or Services."
                } else {
                    db.collection("merchants").document(merchantName).collection("services")
                        .document(productName).get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            etProductName.error = "This name already exists in Products or Services."
                        } else {
                            saveProductData(
                                productName,
                                productPrice,
                                productStock,
                                productDescription,
                                productCategory,
                                productAddress
                            )
                            builder.dismiss() // Close the dialog
                        }
                    }
                }
            }
        }

        val backBtn = dialogUploadProductView.findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            builder.dismiss() // Close the dialog
        }
        val ivCloseDialog = dialogUploadProductView.findViewById<ImageView>(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss() // Close the dialog
        }

        builder.show()
    }

    private fun saveProductData(name: String, price: Double, stock: Int, description: String, category: String, addressName: String) {
        val productData = hashMapOf(
            "merchantName" to merchantName,  // Include merchantName here
            "category" to category,
            "isService" to "false",
            "price" to price,
            "stock" to stock,
            "description" to description,
            "imageUrl" to (selectedImageUri?.toString() ?: ""),
            "addressName" to addressName,
            "modifiedAt" to FieldValue.serverTimestamp()
        )

        db.collection("merchants").document(merchantName).collection("products").document(name).set(productData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Product uploaded successfully", Toast.LENGTH_SHORT).show()
                updateProductCount(1)
                if(isGridView) {
                    populateProductsInGrid("products")
                } else {
                    populateProductsHorizontally("products")
                }
                selectedImageUri = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error uploading product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeProduct(productId: String) {
        db.collection("merchants").document(merchantName).collection("products").document(productId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Product removed successfully", Toast.LENGTH_SHORT).show()
                updateProductCount(-1)
                if (isGridView) {
                    populateProductsInGrid("products")
                } else {
                    populateProductsHorizontally("products")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error removing product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductCount(amount: Long) {
        val merchantDocRef = db.collection("merchants").document(merchantName)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(merchantDocRef)
            val newProductCount = snapshot.getLong("productAmount")?.plus(amount) ?: amount
            transaction.update(merchantDocRef, "productAmount", newProductCount)
        }
    }

    private fun fetchMerchantData() {
        db.collection("merchants").document(merchantName).get().addOnSuccessListener { document ->
            if (document.exists()) {
                tvMerchantName.text = merchantName
                if (document.getString("description")?.isEmpty() == true){
                    merchantDescription.text = "Here will be the description of the merchant..."
                } else{
                    merchantDescription.text = document.getString("description")
                }

                val imageUrl = document.getString("profileImage")
                if (!imageUrl.isNullOrEmpty()) {
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
        productsListing.removeAllViews()
        productsGridLayout.removeAllViews()

        if (isProductListing) {
            productListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service)
            productListingBtn.setTypeface(productListingBtn.typeface, Typeface.BOLD)
            serviceListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service_not_select)
            serviceListingBtn.setTypeface(serviceListingBtn.typeface, Typeface.NORMAL)

            upload_button_text.text = "Upload New Products"
            uploadProductBtn.setOnClickListener { showUploadProductDialog() }
        } else {
            serviceListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service)
            serviceListingBtn.setTypeface(serviceListingBtn.typeface, Typeface.BOLD)
            productListingBtn.setBackgroundResource(R.drawable.custom_input_upload_product_service_not_select)
            productListingBtn.setTypeface(productListingBtn.typeface, Typeface.NORMAL)

            upload_button_text.text = "Add New Services"
            uploadProductBtn.setOnClickListener { showUploadServiceDialog() }
        }

        if (isGridView) {
            productsListingLayout.visibility = View.GONE
            productsGridLayout.visibility = View.VISIBLE
            showButtonText.text = if (isProductListing) "Hide Existing Products" else "Hide Existing Services"
            populateProductsInGrid(if (isProductListing) "products" else "services")
        } else {
            productsListingLayout.visibility = View.VISIBLE
            productsGridLayout.visibility = View.GONE
            showButtonText.text = if (isProductListing) "Show all Existing Products" else "Show all Existing Services"
            populateProductsHorizontally(if (isProductListing) "products" else "services")
        }
    }

    private fun showEditProductDialog(productId: String, productData: Map<String, Any>) {
        dialogEditProductView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_product, null)
        val etProductName = dialogEditProductView.findViewById<EditText>(R.id.etProductName)
        val etProductCategory = dialogEditProductView.findViewById<TextView>(R.id.spinnerProductCategory)
        val addressSpinner: Spinner = dialogEditProductView.findViewById(R.id.spinnerProductAddress)
        val etProductPrice = dialogEditProductView.findViewById<EditText>(R.id.etProductPrice)
        val etProductStock = dialogEditProductView.findViewById<EditText>(R.id.etProductStock)
        val etProductDescription = dialogEditProductView.findViewById<EditText>(R.id.etProductDescription)
        val ivProductImage = dialogEditProductView.findViewById<ImageView>(R.id.ivProductImage)

        // Fill in existing data
        etProductName.setText(productId)
        etProductCategory.setText("Category : " + productData["category"] as? String)
        etProductPrice.setText((productData["price"] as? Double)?.toString())
        etProductStock.setText((productData["stock"] as? Long)?.toString())
        etProductDescription.setText(productData["description"] as? String)
        Glide.with(this).load(productData["imageUrl"] as? String).into(ivProductImage)

        populateAddressSpinner(addressSpinner) {
            val addressName = productData["addressName"] as? String
            if (addressName != null) {
                val position = (addressSpinner.adapter as ArrayAdapter<String>).getPosition(addressName)
                addressSpinner.setSelection(position)
            }
        }

        ivProductImage.setOnClickListener {
            selectImage(PICK_EDIT_PRODUCT_IMAGE_REQUEST)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogEditProductView)
            .create()

        val ivCloseDialog: ImageView = dialogEditProductView.findViewById(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss()
        }

        val removeBtn: Button = dialogEditProductView.findViewById(R.id.removeBtn)
        removeBtn.setOnClickListener {
            val message = "Are you sure you want to remove this product?"
            showConfirmationDialog(requireContext(), message) {
                removeProduct(productId)
                builder.dismiss()
            }
        }

        val confirmBtn: Button = dialogEditProductView.findViewById(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
            val newProductName = etProductName.text.toString()
            val newProductPrice = etProductPrice.text.toString().toDoubleOrNull()
            val newProductStock = etProductStock.text.toString().toIntOrNull()
            val newProductDescription = etProductDescription.text.toString()

            if (newProductPrice != null && newProductStock != null) {
                updateProductData(
                    etProductName,
                    productId,
                    newProductName,
                    newProductPrice,
                    newProductStock,
                    newProductDescription,
                    addressSpinner.selectedItem.toString()
                ){
                    builder.dismiss()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter valid values for price and stock.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.show()
    }

    private fun updateProductData(etProductName: EditText, oldProductId: String, newProductId: String, price: Double, stock: Int, description: String, addressName: String, callback: (Boolean) -> Unit) {
        val productRef = db.collection("merchants").document(merchantName).collection("products").document(oldProductId)
        val updates = mutableMapOf<String, Any>(
            "price" to price,
            "stock" to stock,
            "description" to description,
            "addressName" to addressName,
            "modifiedAt" to FieldValue.serverTimestamp()
        )

        var completedCollections = 0
        var totalCollections = 2

        fun checkCompletion() {
            if (completedCollections == totalCollections) {
                callback(true)
            }
        }

        val updateData: (String?) -> Unit = { imageUrl ->
            if (imageUrl != null) {
                updates["imageUrl"] = imageUrl
            }
            productRef.update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Product updated successfully", Toast.LENGTH_SHORT).show()

                    if (oldProductId != newProductId) {
                        db.collection("merchants").document(merchantName).collection("products")
                            .document(newProductId).get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                etProductName.error =
                                    "This name already exists in Products or Services."
                            } else {
                                db.collection("merchants").document(merchantName)
                                    .collection("services")
                                    .document(newProductId).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            etProductName.error =
                                                "This name already exists in Products or Services."
                                        } else {
                                            changeProductOrServiceDocumentId(
                                                "products",
                                                oldProductId,
                                                newProductId
                                            ) {
                                                if (isGridView) {
                                                    populateProductsInGrid("products")
                                                } else {
                                                    populateProductsHorizontally("products")
                                                }
                                                selectedImageUri = null
                                                completedCollections++
                                                checkCompletion()
                                            }
                                        }
                                    }
                            }

                        }
                    } else{
                        if (isGridView) {
                            populateProductsInGrid("products")
                        } else {
                            populateProductsHorizontally("products")
                        }
                        selectedImageUri = null
                        completedCollections++
                        checkCompletion()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating product: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("productImages/$username/$newProductId")
            val uploadTask = storageRef.putFile(selectedImageUri!!)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateData(uri.toString())
                    completedCollections++
                    checkCompletion()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch image URL", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateData(null)
            completedCollections++
            checkCompletion()
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
        if (resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            when (requestCode) {
                PICK_PRODUCT_IMAGE_REQUEST -> {
                    val imageView = dialogUploadProductView.findViewById<ImageView>(R.id.ivProductImage)
                    imageView.setImageURI(selectedImageUri)
                    Glide.with(requireContext()).load(selectedImageUri).into(imageView)
                }
                PICK_SERVICE_IMAGE_REQUEST -> {
                    val imageView = dialogUploadServiceView.findViewById<ImageView>(R.id.ivServiceImage)
                    imageView.setImageURI(selectedImageUri)
                    Glide.with(requireContext()).load(selectedImageUri).into(imageView)
                }
                PICK_EDIT_PRODUCT_IMAGE_REQUEST -> {
                    val imageView = dialogEditProductView.findViewById<ImageView>(R.id.ivProductImage)
                    imageView.setImageURI(selectedImageUri)
                    Glide.with(requireContext()).load(selectedImageUri).into(imageView)
                }
                PICK_EDIT_SERVICE_IMAGE_REQUEST -> {
                    val imageView = dialogEditServiceView.findViewById<ImageView>(R.id.ivServiceImage)
                    imageView.setImageURI(selectedImageUri)
                    Glide.with(requireContext()).load(selectedImageUri).into(imageView)
                }
                PICK_MERCHANT_IMAGE_REQUEST -> {
                    val imageView = dialogEditMerchantView.findViewById<ImageView>(R.id.ivMerchantImage)
                    imageView.setImageURI(selectedImageUri)
                    imageView.setBackgroundResource(R.drawable.circular_border)
                    imageView.setCircularImage()
                    Glide.with(requireContext()).load(selectedImageUri).into(imageView)
                }
            }
        }
    }

    private fun showEditDialog() {
        dialogEditMerchantView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_merchant, null)
        val etMerchantName = dialogEditMerchantView.findViewById<EditText>(R.id.etMerchantName)
        val etEditDescription = dialogEditMerchantView.findViewById<EditText>(R.id.etMerchantDescription)
        val ivEditMerchantStore = dialogEditMerchantView.findViewById<ImageView>(R.id.ivMerchantImage)

        // load existing data
        etMerchantName.setText(merchantName)
        etEditDescription.setText(merchantDescription.text.toString())
        db.collection("merchants").document(merchantName).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val imageUrl = document.getString("profileImage")
                if (imageUrl != null) {
                    ivEditMerchantStore.setBackgroundResource(R.drawable.circular_border)
                    ivEditMerchantStore.setCircularImage()
                    Glide.with(this).load(imageUrl).centerCrop().into(ivEditMerchantStore)
                }
            } else {
                Toast.makeText(requireContext(), "Merchant data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to fetch merchant data", Toast.LENGTH_SHORT).show()
        }

        ivEditMerchantStore.setOnClickListener {
            selectImage(PICK_MERCHANT_IMAGE_REQUEST)
        }

        //create dialog
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogEditMerchantView)
            .create()

        val confirmBtn = dialogEditMerchantView.findViewById<TextView>(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
            val newName = etMerchantName.text.toString()
            val newDescription = etEditDescription.text.toString()

            if (merchantName != newName) {
                db.collection("merchants").document(newName).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            etMerchantName.error =
                                "Merchant name already exists. Please choose another name."
                        } else {
                            db.collection("users").document(newName).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        etMerchantName.error =
                                            "Merchant name already exists. Please choose another name."
                                    } else {
                                        saveMerchantData(newName, newDescription)
                                        builder.dismiss() // Close the dialog
                                    }
                                }.addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else{
                db.collection("users").document(newName).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            etMerchantName.error =
                                "Merchant name already exists. Please choose another name."
                        } else {
                            saveMerchantData(newName, newDescription)
                            builder.dismiss() // Close the dialog
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        val backBtn = dialogEditMerchantView.findViewById<TextView>(R.id.backBtn)
        backBtn.setOnClickListener {
            builder.dismiss() // Close the dialog
        }
        val ivCloseDialog = dialogEditMerchantView.findViewById<ImageView>(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss() // Close the dialog
        }

        builder.show()
    }

    private fun saveMerchantData(newName: String, newDescription: String) {
        val merchantRef = db.collection("merchants").document(merchantName)
        val userRef = db.collection("users").document(username)
        val updates = mutableMapOf<String, Any>(
            "description" to newDescription
        )
        val updatesUser = mutableMapOf<String, Any>(
            "MerchantName" to newName
        )

        val updateData: (String?) -> Unit = { imageUrl ->
            if (imageUrl != null) {
                updates["profileImage"] = imageUrl
            }
            merchantRef.update(updates).addOnSuccessListener {
                userRef.update(updatesUser).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Merchant data updated", Toast.LENGTH_SHORT).show()
                    if (merchantName != newName) {
                        changeMerchantDocumentId(merchantName, newName)
                    }
                    fetchMerchantData()
                    selectedImageUri = null
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update user data", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update merchant data", Toast.LENGTH_SHORT).show()
            }
        }

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("merchantImages/$username")
            val uploadTask = storageRef.putFile(selectedImageUri!!)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateData(uri.toString())
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch image URL", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateData(null)
        }
    }

    private fun removeMerchantAccount() {
        val userRef = db.collection("users").document(username)
        val merchantRef = db.collection("merchants").document(merchantName)

        db.runBatch { batch ->
            // Remove merchant document
            batch.delete(merchantRef)

            // Update user document
            batch.update(userRef, "MerchantName", null)
            batch.update(userRef, "isMerchant", "false")
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Merchant account removed successfully", Toast.LENGTH_SHORT).show()
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error removing merchant account: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeProductOrServiceDocumentId(productOrService: String, oldPSName: String, newPSName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val productOrServiceCollection = db.collection("merchants").document(merchantName).collection(productOrService)

        // Get the data from the old document
        productOrServiceCollection.document(oldPSName).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val productOrServiceData = documentSnapshot.data
                productOrServiceData?.let { data ->
                    // Update the product/service name in the data
                    if (productOrService == "products") {
                        data["productName"] = newPSName
                    } else {
                        data["serviceName"] = newPSName
                    }

                    // Create a new document with the new ID and set the updated data
                    productOrServiceCollection.document(newPSName).set(data).addOnSuccessListener {
                        // Delete the old document
                        productOrServiceCollection.document(oldPSName).delete().addOnSuccessListener {
                            // Update the product/service name in user carts and wishlists
                            updateProductAndServiceNameInUserSubcollections("cart", oldPSName, newPSName, oldPSName, newPSName) { cartSuccess ->
                                updateProductAndServiceNameInUserSubcollections("wishlist", oldPSName, newPSName, oldPSName, newPSName) { wishlistSuccess ->
                                    // Update the product/service name in other collections
                                    updateProductAndServiceNameInCollections(oldPSName, newPSName) { success ->
                                        if (success) {
                                            Toast.makeText(requireContext(), "Document ID and name changed successfully", Toast.LENGTH_SHORT).show()
                                            callback(true)
                                        } else {
                                            Toast.makeText(requireContext(), "Failed to update product/service name in other collections", Toast.LENGTH_SHORT).show()
                                            callback(false)
                                        }
                                    }
                                }
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to delete old document: ${e.message}", Toast.LENGTH_SHORT).show()
                            callback(false)
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to create new document: ${e.message}", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Old document not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to get old document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProductAndServiceNameInUserSubcollections(subcollection: String, oldPSName: String, newPSName: String, oldPSId: String, newPSId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").get().addOnSuccessListener { usersSnapshot ->
            val batch = db.batch()
            var completedUpdates = 0

            for (userDocument in usersSnapshot.documents) {
                val subcollectionRef = userDocument.reference.collection(subcollection)
                subcollectionRef.get().addOnSuccessListener { subcollectionSnapshot ->
                    for (document in subcollectionSnapshot.documents) {
                        if (document.getString("productName") == oldPSName || document.getString("serviceName") == oldPSName) {
                            if (subcollection == "cart") {
                                batch.update(document.reference, "productName", newPSName)
                                batch.delete(subcollectionRef.document(oldPSId))
                                batch.set(subcollectionRef.document(newPSId), document.data!!)
                                batch.update(subcollectionRef.document(newPSId), "productName", newPSName)
                            } else if (subcollection == "wishlist") {
                                batch.update(document.reference, "serviceName", newPSName)
                                batch.delete(subcollectionRef.document(oldPSId))
                                batch.set(subcollectionRef.document(newPSId), document.data!!)
                                batch.update(subcollectionRef.document(newPSId), "serviceName", newPSName)
                            }
                        }
                    }

                    completedUpdates++
                    if (completedUpdates == usersSnapshot.size()) {
                        batch.commit().addOnSuccessListener {
                            callback(true)
                        }.addOnFailureListener {
                            callback(false)
                        }
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun changeMerchantDocumentId(oldDocumentId: String, newDocumentId: String) {
        val db = FirebaseFirestore.getInstance()
        val merchantsCollection = db.collection("merchants")

        // List of known subcollections for a user document
        val knownSubcollections = listOf("products", "services", "addresses")

        merchantsCollection.document(oldDocumentId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val merchantData = documentSnapshot.data

                merchantData?.let { data ->
                    merchantsCollection.document(newDocumentId).set(data).addOnSuccessListener {
                        merchantName = newDocumentId
                        copySubCollections(oldDocumentId, newDocumentId) { success ->
                            if (success) {
                                updateMerchantNameInCollections(oldDocumentId, newDocumentId) { updateSuccess ->
                                    if (updateSuccess) {
                                        updateProductsAndServices(oldDocumentId, newDocumentId) { productsServicesSuccess ->
                                            if (productsServicesSuccess) {
                                                deleteDocumentWithSubcollections(merchantsCollection.document(oldDocumentId), knownSubcollections) { deleteSuccess ->
                                                    if (deleteSuccess) {
                                                        Toast.makeText(requireContext(), "Document ID changed successfully", Toast.LENGTH_SHORT).show()
                                                        fetchMerchantData()
                                                    } else {
                                                        Toast.makeText(requireContext(), "Failed to delete old document and its subcollections", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(requireContext(), "Failed to update products and services", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to update merchantName in some collections", Toast.LENGTH_SHORT).show()
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
                        deleteDocumentWithSubcollections(document.reference, knownSubcollections) { success ->
                            deletedDocs++
                            if (deletedDocs == querySnapshot.size()) {
                                // All documents in this subcollection deleted, move to next subcollection
                                deleteSubcollections(index + 1)
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


    private fun updateProductAndServiceNameInCollections(oldPSName: String, newPSName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("pendingOrders", "shippingOrders", "completeOrders", "pendingServices", "upcomingServices", "completeServices")
        var completedCollections = 0
        var totalCollections = collections.size

        fun checkCompletion() {
            if (completedCollections == totalCollections) {
                callback(true)
            }
        }

        collections.forEach { collectionName ->
            db.collection(collectionName).get().addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                var documentsUpdated = 0

                for (document in querySnapshot.documents) {
                    val items = document.get("items") as? List<Map<String, Any>> ?: continue
                    val updatedItems = items.map { item ->
                        if (item["productName"] == oldPSName && item["merchantName"] == merchantName) {
                            item + ("productName" to newPSName)
                        } else if (item["serviceName"] == oldPSName && item["merchantName"] == merchantName) {
                            item + ("serviceName" to newPSName)
                        } else {
                            item
                        }
                    }
                    if (updatedItems != items) {
                        batch.update(document.reference, "items", updatedItems)
                        documentsUpdated++
                    }
                }

                if (documentsUpdated > 0) {
                    batch.commit().addOnSuccessListener {
                        completedCollections++
                        checkCompletion()
                    }.addOnFailureListener {
                        callback(false)
                    }
                } else {
                    completedCollections++
                    checkCompletion()
                }
            }.addOnFailureListener {
                callback(false)
            }
        }
    }

    private fun updateMerchantNameInCollections(oldMerchantName: String, newMerchantName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("pendingOrders", "shippingOrders", "completeOrders", "pendingServices", "upcomingServices", "completeServices")
        var completedCollections = 0
        var totalCollections = collections.size + 2 // +1 for the chats collection

        fun checkCompletion() {
            if (completedCollections == totalCollections) {
                callback(true)
            }
        }

        collections.forEach { collectionName ->
            db.collection(collectionName).get().addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                var documentsUpdated = 0

                for (document in querySnapshot.documents) {
                    val items = document.get("items") as? List<Map<String, Any>> ?: continue
                    val updatedItems = items.map { item ->
                        if (item["merchantName"] == oldMerchantName) {
                            item + ("merchantName" to newMerchantName)
                        } else {
                            item
                        }
                    }
                    if (updatedItems != items) {
                        batch.update(document.reference, "items", updatedItems)
                        documentsUpdated++
                    }
                }

                if (documentsUpdated > 0) {
                    batch.commit().addOnSuccessListener {
                        completedCollections++
                        checkCompletion()
                    }.addOnFailureListener {
                        callback(false)
                    }
                } else {
                    completedCollections++
                    checkCompletion()
                }
            }.addOnFailureListener {
                callback(false)
            }
        }

        // Update chats collection
        db.collection("chats")
            .whereEqualTo("merchantName", oldMerchantName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "merchantName", newMerchantName)

                    // Update merchant name in messages subcollection
                    document.reference.collection("messages")
                        .whereEqualTo("merchantName", oldMerchantName)
                        .get()
                        .addOnSuccessListener { messagesSnapshot ->
                            for (messageDoc in messagesSnapshot.documents) {
                                batch.update(messageDoc.reference, "merchantName", newMerchantName)
                            }

                            batch.commit().addOnSuccessListener {
                                completedCollections++
                                checkCompletion()
                            }.addOnFailureListener {
                                Log.e("UpdateMerchantName", "Failed to update chat messages", it)
                                callback(false)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("UpdateMerchantName", "Failed to query chat messages", it)
                            callback(false)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("UpdateMerchantName", "Failed to query chats", it)
                callback(false)
            }


        // Update in user's cart and wishlist collections
        updateMerchantNameInUserSubcollections("cart", oldMerchantName, newMerchantName) { cartSuccess ->
            updateMerchantNameInUserSubcollections("wishlist", oldMerchantName, newMerchantName) { wishlistSuccess ->
                if (cartSuccess && wishlistSuccess) {
                    completedCollections++
                    checkCompletion()
                } else {
                    callback(false)
                }
            }
        }
    }

    private fun updateMerchantNameInUserSubcollections(subcollection: String, oldMerchantName: String, newMerchantName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").get().addOnSuccessListener { usersSnapshot ->
            val batch = db.batch()
            var completedUpdates = 0

            for (userDocument in usersSnapshot.documents) {
                val subcollectionRef = userDocument.reference.collection(subcollection)
                subcollectionRef.get().addOnSuccessListener { subcollectionSnapshot ->
                    for (document in subcollectionSnapshot.documents) {
                        if (document.getString("merchantName") == oldMerchantName) {
                            batch.update(document.reference, "merchantName", newMerchantName)
                        }
                    }

                    completedUpdates++
                    if (completedUpdates == usersSnapshot.size()) {
                        batch.commit().addOnSuccessListener {
                            callback(true)
                        }.addOnFailureListener {
                            callback(false)
                        }
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun updateProductsAndServices(oldMerchantId: String, newMerchantId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val merchantsCollection = db.collection("merchants")
        val collections = listOf("products", "services")
        var completedCollections = 0

        collections.forEach { collectionName ->
            merchantsCollection.document(newMerchantId).collection(collectionName).get().addOnSuccessListener { querySnapshot ->
                val batch = db.batch()

                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "merchantName", newMerchantId)
                }

                batch.commit().addOnSuccessListener {
                    completedCollections++
                    if (completedCollections == collections.size) {
                        callback(true)
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            }.addOnFailureListener {
                callback(false)
            }
        }
    }

    private fun copySubCollections(oldDocumentId: String, newDocumentId: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val merchantsCollection = db.collection("merchants")

        // Copy products collection
        val oldAddressesCollection = merchantsCollection.document(oldDocumentId).collection("addresses")
        val newAddressesCollection = merchantsCollection.document(newDocumentId).collection("addresses")

        oldAddressesCollection.get().addOnSuccessListener { productsSnapshot ->
            val batch = db.batch()
            for (document in productsSnapshot.documents) {
                batch.set(newAddressesCollection.document(document.id), document.data!!)
            }
            batch.commit().addOnSuccessListener {
            // Copy products collection
            val oldProductsCollection = merchantsCollection.document(oldDocumentId).collection("products")
            val newProductsCollection = merchantsCollection.document(newDocumentId).collection("products")

            oldProductsCollection.get().addOnSuccessListener { productsSnapshot ->
                val batch = db.batch()
                for (document in productsSnapshot.documents) {
                    batch.set(newProductsCollection.document(document.id), document.data!!)
                }
                batch.commit().addOnSuccessListener {
                    // Copy services collection
                    val oldServicesCollection = merchantsCollection.document(oldDocumentId).collection("services")
                    val newServicesCollection = merchantsCollection.document(newDocumentId).collection("services")

                    oldServicesCollection.get().addOnSuccessListener { servicesSnapshot ->
                        val batch = db.batch()
                        for (document in servicesSnapshot.documents) {
                            batch.set(newServicesCollection.document(document.id), document.data!!)
                        }
                        batch.commit().addOnSuccessListener {
                            callback(true) // Sub-collections copied successfully
                        }.addOnFailureListener {
                            callback(false) // Failed to copy services collection
                        }
                    }.addOnFailureListener {
                        callback(false) // Failed to fetch services collection
                    }
                }.addOnFailureListener {
                    callback(false) // Failed to copy products collection
                }
            }.addOnFailureListener {
                callback(false) // Failed to fetch products collection
            }
        }.addOnFailureListener {
            callback(false) // Failed to fetch products collection
        }
        }.addOnFailureListener {
            callback(false) // Failed to fetch products collection
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

    companion object {
        private const val PICK_PRODUCT_IMAGE_REQUEST = 1
        private const val PICK_SERVICE_IMAGE_REQUEST = 2
        private const val PICK_MERCHANT_IMAGE_REQUEST = 3
        private const val PICK_EDIT_PRODUCT_IMAGE_REQUEST = 4
        private const val PICK_EDIT_SERVICE_IMAGE_REQUEST = 5
    }
}

