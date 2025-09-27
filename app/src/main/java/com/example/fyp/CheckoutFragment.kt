package com.example.fyp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CheckoutFragment : Fragment() {

    private lateinit var serviceCategory: String
    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var cartButton: ImageButton

    private lateinit var addressSpinner: Spinner
    private lateinit var itemsContainer: LinearLayout
    private lateinit var totalPriceTextView: TextView
    private lateinit var checkoutButton: TextView
    private lateinit var addressDetailsTextView1: TextView
    private lateinit var addressDetailsTextView2: TextView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var checkedItems: ArrayList<Map<String, Any>>
    private lateinit var addresses: List<Address>
    private var selectedAddress: Address? = null
    private var selectedPet: Pet? = null

    private var checkInDate: Calendar? = null
    private var checkOutDate: Calendar? = null
    private lateinit var selectDateButton: TextView
    private lateinit var selectTimeButton: TextView
    private lateinit var selectCheckInDateButton : TextView
    private lateinit var selectCheckOutDateButton : TextView
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private lateinit var unavailableDates: List<String>
    private lateinit var unavailableDays: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString("username").toString()
        merchantName = arguments?.getString("merchantName").toString()
        checkedItems = arguments?.getSerializable("checkedItems") as ArrayList<Map<String, Any>>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_checkout, container, false)

        cartButton = view.findViewById(R.id.cartButton)
        updateCartButtonAppearance()

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
        cartButton.setOnClickListener {
            val fragment = CartFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("merchantName", merchantName)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        addressSpinner = view.findViewById(R.id.addressSpinner)
        itemsContainer = view.findViewById(R.id.itemsContainer)
        totalPriceTextView = view.findViewById(R.id.totalPriceTextView)
        checkoutButton = view.findViewById(R.id.checkoutBtn)

        selectDateButton = view.findViewById(R.id.selectDateButton)
        selectTimeButton = view.findViewById(R.id.selectTimeButton)

        val isService = arguments?.getBoolean("isService", false) ?: false

        var addressTitleTextView = view.findViewById<TextView>(R.id.addressTitleTextView)
        addressDetailsTextView1 = view.findViewById(R.id.addressDetailsTextView1)
        addressDetailsTextView2 = view.findViewById(R.id.addressDetailsTextView2)
        val payment_section = view.findViewById<LinearLayout>(R.id.payment_section)
        val totalBar = view.findViewById<RelativeLayout>(R.id.totalBar)
        val sendRequestTV = view.findViewById<TextView>(R.id.sendRequestTV)
        selectCheckInDateButton = view.findViewById(R.id.selectCheckInDateButton)
        selectCheckOutDateButton = view.findViewById(R.id.selectCheckOutDateButton)
        val petSection: RelativeLayout = view.findViewById(R.id.petSection)

        if (isService) {
            loadUnavailableDates(checkedItems[0]["merchantName"].toString(), checkedItems[0]["serviceName"].toString())

            payment_section.visibility = GONE
            totalBar.visibility = GONE
            addressSpinner.visibility = View.GONE
            addressTitleTextView.setText("Store Address : ")
            petSection.visibility = View.VISIBLE

            val petSpinner: Spinner = view.findViewById(R.id.petSpinner)
            loadPets(petSpinner)
            petSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    selectedPet = parent.getItemAtPosition(position) as Pet
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedPet = null
                }
            }

            fetchServiceCategory(checkedItems[0]["merchantName"].toString(), checkedItems[0]["serviceName"].toString()) { category ->

                loadServiceAddress()
                serviceCategory = category
                if (category == "Boarding"){
                    selectDateButton.visibility = View.GONE
                    selectTimeButton.visibility = View.GONE
                    selectCheckInDateButton.visibility = View.VISIBLE
                    selectCheckOutDateButton.visibility = View.VISIBLE
                }
                else{
                    selectDateButton.visibility = View.VISIBLE
                    selectTimeButton.visibility = View.VISIBLE
                    selectCheckInDateButton.visibility = View.GONE
                    selectCheckOutDateButton.visibility = View.GONE
                    selectTimeButton.setOnClickListener {
                        showTimePicker()
                    }
//                    loadUnavailableDates(checkedItems[0]["merchantName"].toString(), checkedItems[0]["serviceName"].toString())
                }
            }

            checkoutButton.visibility = GONE
            sendRequestTV.visibility = VISIBLE

        } else {
            payment_section.visibility = VISIBLE
            totalBar.visibility = VISIBLE
            petSection.visibility = View.GONE

            selectDateButton.visibility = View.GONE
            selectTimeButton.visibility = View.GONE
            selectCheckInDateButton.visibility = View.GONE
            selectCheckOutDateButton.visibility = View.GONE

            sendRequestTV.visibility = GONE
            checkoutButton.visibility = VISIBLE
            checkoutButton.setText("Pay")

            loadAddresses()

            addressSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    selectedAddress = addresses[position]
                    addressDetailsTextView1.setText(selectedAddress!!.id + " | " + selectedAddress!!.phone)
                    addressDetailsTextView2.setText(selectedAddress!!.address + "\nPostal: " + selectedAddress!!.postalCode + " " + selectedAddress!!.city)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        loadCartItems()
        calculateTotalPrice()

        checkoutButton.setOnClickListener {
            if (selectedAddress == null) {
                Toast.makeText(context, "Please select an address", Toast.LENGTH_SHORT).show()
            } else {
                processOrder()
            }
        }
        sendRequestTV.setOnClickListener {
            if (serviceCategory != "Boarding" && selectedDate != null && !isDateAvailable(selectedDate!!)) {
                Toast.makeText(context, "The selected date is not available", Toast.LENGTH_SHORT).show()
            }
            else if (serviceCategory == "Boarding" && checkInDate != null && !isDateAvailable(checkInDate!!)) {
                Toast.makeText(context, "One or both of the selected dates are not available", Toast.LENGTH_SHORT).show()
            }
            else if (serviceCategory == "Boarding" && checkInDate != null && !isDateAvailable(checkOutDate!!)) {
                Toast.makeText(context, "One or both of the selected dates are not available", Toast.LENGTH_SHORT).show()
            }
            else if (serviceCategory == "Boarding" && (checkInDate == null || checkOutDate == null)){
                Toast.makeText(context, "Please choose both checkin and checkout date", Toast.LENGTH_SHORT).show()

            }
            else if (serviceCategory != "Boarding" && (selectedDate == null || selectedTime == null)){
                Toast.makeText(context, "Please choose both service date and checkin time", Toast.LENGTH_SHORT).show()
            }
            else {
                processOrder()
            }
        }

        return view
    }


    private fun loadPets(petSpinner: Spinner) {
        db.collection("users").document(username).collection("pets").get()
            .addOnSuccessListener { documents ->
                val pets = documents.mapNotNull { doc ->
                    doc.toObject(Pet::class.java)
                }
                val adapter = PetSpinnerAdapter(requireContext(), pets)
                petSpinner.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    class PetSpinnerAdapter(context: Context, private val pets: List<Pet>) : ArrayAdapter<Pet>(context, 0, pets) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createItemView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createItemView(position, convertView, parent)
        }

        private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
            val pet = getItem(position)
            val view = recycledView ?: LayoutInflater.from(context).inflate(R.layout.item_pet_spinner, parent, false)

            val petImageView = view.findViewById<ImageView>(R.id.petImageView)
            val petNameTextView = view.findViewById<TextView>(R.id.petNameTextView)

            petNameTextView.text = pet?.petName
            pet?.petImageUrl?.let { Glide.with(context).load(it).into(petImageView) }

            return view
        }
    }


    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = CustomDatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                if (isDateAvailable(selectedDate)) {
                    this.selectedDate = selectedDate
                    updateSelectedDateTime()
                } else {
                    Toast.makeText(context, "This date is not available", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            unavailableDates,
            unavailableDays
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        selectDateButton.setOnClickListener {
            datePickerDialog.show()
        }
    }

    private fun setupCheckInOutDatePickers(checkInButton: TextView, checkOutButton: TextView) {
        val today = Calendar.getInstance()

        checkInButton.setOnClickListener {
            val datePickerDialog = CustomDatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    if (isDateAvailable(selectedDate)) {
                        checkInDate = selectedDate
                        checkInButton.text = "Check-in: ${formatDate(checkInDate!!)}"
                        // Reset checkout date if it's before the new check-in date
                        if (checkOutDate != null && checkOutDate!!.before(checkInDate)) {
                            checkOutDate = null
                            checkOutButton.text = "Select Check-out Date"
                        }
                    } else {
                        Toast.makeText(context, "This date is not available", Toast.LENGTH_SHORT).show()
                    }
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
                unavailableDates,
                unavailableDays
            )

            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        checkOutButton.setOnClickListener {
            if (checkInDate == null) {
                Toast.makeText(context, "Please select a check-in date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val datePickerDialog = CustomDatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    if (isDateAvailable(selectedDate, true) && isCheckoutDateValid(selectedDate)) {
                        checkOutDate = selectedDate
                        checkOutButton.text = "Check-out: ${formatDate(checkOutDate!!)}"
                    } else {
                        Toast.makeText(context, "This date is not available or is before the check-in date", Toast.LENGTH_SHORT).show()
                    }
                },
                checkInDate!!.get(Calendar.YEAR),
                checkInDate!!.get(Calendar.MONTH),
                checkInDate!!.get(Calendar.DAY_OF_MONTH),
                unavailableDates,
                unavailableDays
            )

            datePickerDialog.datePicker.minDate = checkInDate!!.timeInMillis + 86400000 // Next day after check-in
            datePickerDialog.show()
        }
    }


    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun showTimePicker() {
        val currentTime = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                updateSelectedDateTime()
            },
            currentTime.get(Calendar.HOUR_OF_DAY),
            currentTime.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun updateSelectedDateTime() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm a", Locale.getDefault())

        val dateString = selectedDate?.let { dateFormat.format(it.time) } ?: "Not selected"
        val timeString = selectedTime?.let { timeFormat.format(it.time) } ?: "Not selected"

        selectDateButton.text = "Date: $dateString"
        selectTimeButton.text = "Time: $timeString"
    }

    private fun updateCartButtonAppearance() {
        cartButton.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.cart_icon_white)
        }
    }

    private fun loadServiceAddress() {

        val addressName = checkedItems[0]["addressName"] as String
        val merchantName = checkedItems[0]["merchantName"] as String

        db.collection("merchants").document(merchantName)
            .collection("addresses").document(addressName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val address = document.toObject(Address::class.java)

                    address?.let {
                        addressDetailsTextView1.text = "$merchantName  |  ${it.phone}"
                        addressDetailsTextView2.text = "${it.address}\n${it.postalCode} , ${it.city}"
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to load service address: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAddresses() {
        db.collection("users").document(username).collection("addresses")
            .get()
            .addOnSuccessListener { documents ->
                addresses = documents.mapNotNull { it.toObject(Address::class.java).apply { id = it.id } }
//                val addressStrings = addresses.map { "${it.address}, ${it.city} ${it.postalCode}" }
                val addressSelections = addresses.map { "${it.id}" }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, addressSelections)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                addressSpinner.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to load addresses: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCartItems() {
        itemsContainer.removeAllViews()
        val groupedItems = checkedItems.groupBy { it["merchantName"] as String }
        for ((merchantName, items) in groupedItems) {
            val sellerView = layoutInflater.inflate(R.layout.item_cart_header, itemsContainer, false)
            sellerView.findViewById<TextView>(R.id.sellerNameTextView).text = merchantName
            itemsContainer.addView(sellerView)

            for (item in items) {
                val itemView = layoutInflater.inflate(R.layout.item_cart, itemsContainer, false)

                val plus_minus = itemView.findViewById<LinearLayout>(R.id.plus_minus)
                plus_minus.visibility = GONE

                val itemStockTextView = itemView.findViewById<TextView>(R.id.itemStockTextView)
                val itemCheckBox = itemView.findViewById<CheckBox>(R.id.itemCheckBox)
                val tickIcon = itemView.findViewById<ImageView>(R.id.tickIcon)
                val isService = arguments?.getBoolean("isService", false) ?: false
                if (!isService) {
                    itemStockTextView.visibility = VISIBLE
                    tickIcon.visibility = VISIBLE
                    itemCheckBox.visibility = VISIBLE
                    itemView.findViewById<TextView>(R.id.itemNameTextView).text = item["productName"] as String
                    itemStockTextView.text = "Quantity: ${item["quantity"]}"
                } else{
                    itemStockTextView.visibility = GONE
                    tickIcon.visibility = GONE
                    itemCheckBox.visibility = GONE
                    itemView.findViewById<TextView>(R.id.itemNameTextView).text = item["serviceName"] as String
                }
                // Set item details
                itemView.findViewById<TextView>(R.id.itemPriceTextView).text = "RM%.2f".format(item["price"] as Double)

                // Load image (assuming you have an imageUrl in your item data)
                val imageView = itemView.findViewById<ImageView>(R.id.itemImageView)
                if (item.containsKey("imageUrl")) {
                    Glide.with(this).load(item["imageUrl"] as String).into(imageView)
                }

                itemsContainer.addView(itemView)
            }
        }
    }

    private fun calculateTotalPrice() {
        val isService = arguments?.getBoolean("isService", false) ?: false

        var totalPrice: Double
        if (isService){
            totalPrice = checkedItems[0]["price"] as Double
        } else {
            totalPrice = checkedItems.sumByDouble {
                (it["price"] as Double) * (it["quantity"] as Int)
            }
        }
        totalPriceTextView.text = "Total Price: RM%.2f".format(totalPrice)
    }

    private fun processOrder() {
        val isService = arguments?.getBoolean("isService", false) ?: false

        if (isService) {
            if (selectedPet == null) {
                Toast.makeText(context, "Please select a pet", Toast.LENGTH_SHORT).show()
                return
            }

//            Toast.makeText(context, "serviceCategory : $serviceCategory", Toast.LENGTH_SHORT).show()
//            Toast.makeText(context, "selectedDate : $selectedDate", Toast.LENGTH_SHORT).show()
//            Toast.makeText(context, "selectedTime : $selectedTime", Toast.LENGTH_SHORT).show()
//            Toast.makeText(context, "checkInDate : $checkInDate", Toast.LENGTH_SHORT).show()
//            Toast.makeText(context, "checkOutDate : $checkOutDate", Toast.LENGTH_SHORT).show()

            val merchantName = checkedItems[0]["merchantName"] as String
            val addressName = checkedItems[0]["addressName"] as String

            db.collection("merchants").document(merchantName)
                .collection("addresses").document(addressName)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val address = document.toObject(Address::class.java)

                        val order = hashMapOf(
                            "username" to username,
                            "items" to checkedItems,
                            "totalPrice" to checkedItems.sumByDouble { (it["price"] as Double) },
                            "timestamp" to Timestamp.now(),
                            "address" to address,
                            "category" to serviceCategory,
                            "pet" to selectedPet,
                            "status" to "pendingConfirmation"
                        )

                        if (serviceCategory == "Boarding") {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            order["checkInDate"] = dateFormat.format(checkInDate!!.time)
                            order["checkOutDate"] = dateFormat.format(checkOutDate!!.time)
                        } else {
                            // Store date (just date, no time)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            order["serviceDate"] = dateFormat.format(selectedDate!!.time)

                            // Store time with AM/PM
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            order["pickUpTime"] = timeFormat.format(selectedTime!!.time)
                        }

                        db.collection("pendingServices").add(order)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Booking request sent successfully!", Toast.LENGTH_SHORT).show()
                                navigateToCart()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to place order: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load service address: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

    } else {
            for (item in checkedItems) {
                val productName = item["productName"] as String
                val quantity = item["quantity"] as Int
                val merchantName = item["merchantName"] as String

                db.collection("merchants").document(merchantName)
                    .collection("products").document(productName)
                    .get()
                    .addOnSuccessListener { document ->
                        val currentStock = document.getLong("stock") ?: 0
                        val newStock = currentStock - quantity
                        document.reference.update("stock", newStock)
                    }
            }

            // Create order document
            val order = hashMapOf(
                "username" to username,
                "items" to checkedItems,
                "address" to selectedAddress,
                "totalPrice" to checkedItems.sumByDouble { (it["price"] as Double) * (it["quantity"] as Int) },
                "timestamp" to Timestamp.now()
            )

            db.collection("pendingOrders").add(order)
                .addOnSuccessListener {
                    Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                    clearCart()
                    navigateToCart()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Failed to place order: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun loadUnavailableDates(merchantName: String, serviceName: String) {
        db.collection("merchants").document(merchantName)
            .collection("services").document(serviceName)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    unavailableDates = document.get("unavailableDates") as? List<String> ?: listOf()
                    unavailableDays = document.get("unavailableDays") as? List<String> ?: listOf()
//                    unavailableDays = (document.get("unavailableDays") as? List<Long>)?.map { it.toInt() } ?: listOf()
                    setupDatePicker()
                    setupCheckInOutDatePickers(selectCheckInDateButton, selectCheckOutDateButton)

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load unavailable dates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isDateAvailable(date: Calendar, isCheckout: Boolean = false): Boolean {
        // Check if the date is before today
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        if (date.before(today)) {
            return false
        }

        // If it's a checkout date, make sure it's after the check-in date
        if (isCheckout && checkInDate != null) {
            if (date.before(checkInDate) || date == checkInDate) {
                return false
            }
        }

        val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayName = dayNames[dayOfWeek]

        if (unavailableDays.contains(dayName)) {
            return false
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(date.time)

        for (range in unavailableDates) {
            val (start, end) = range.split(" to ")
            if (dateString in start..end) {
                return false
            }
        }

        return true
    }

    private fun isCheckoutDateValid(checkoutDate: Calendar): Boolean {
        return checkInDate != null && checkoutDate.after(checkInDate)
    }

    private fun fetchServiceCategory(merchantName: String, serviceName: String, callback: (String) -> Unit) {
        db.collection("merchants").document(merchantName)
            .collection("services").document(serviceName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val category = document.getString("category")
                    if (category != null) {
                        callback(category)
                    }
                } else {
                    callback("Unknown")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to fetch service category: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback("Unknown")
            }
    }

    private fun clearCart() {
        for (item in checkedItems) {
            val productName = item["productName"] as String
            db.collection("users").document(username).collection("cart").document(productName).delete()
        }
    }

    private fun navigateToCart() {
        val fragment = CartFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        fragment.arguments = bundle
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}
