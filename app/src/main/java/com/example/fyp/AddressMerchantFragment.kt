package com.example.fyp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddressMerchantFragment : Fragment() {

    private lateinit var profileButton: ImageButton
    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var addressContainer: LinearLayout
    private lateinit var addButton: RelativeLayout

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

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
        val view = inflater.inflate(R.layout.fragment_address_merchant, container, false)

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

        addressContainer = view.findViewById(R.id.addressContainer)
        addButton = view.findViewById(R.id.uploadProductButton)

        addButton.setOnClickListener {
            showAddAddressDialog()
        }

        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            (activity as? MainActivity)?.popBackStack()
        }

        fetchAddresses()

        return view
    }

    private fun fetchAddresses() {
        user?.let {
            db.collection("merchants").document(merchantName).collection("addresses")
                .get()
                .addOnSuccessListener { documents ->
                    addressContainer.removeAllViews()
                    for (document in documents) {
                        val address = document.data
                        addAddressView(document.id, address)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }

    private fun addAddressView(id: String, address: Map<String, Any>) {
        val addressView = LayoutInflater.from(context).inflate(R.layout.item_address, addressContainer, false)
        val firstRow = addressView.findViewById<TextView>(R.id.firstRow)
        val secondRow = addressView.findViewById<TextView>(R.id.secondRow)
        val thirdRow = addressView.findViewById<TextView>(R.id.thirdRow)

        firstRow.text = "${id}   |   ${address["phone"]}"
        secondRow.text = address["address"] as String
        thirdRow.text = "${address["postalCode"]} , ${address["city"]}"

        val btnRemove = addressView.findViewById<TextView>(R.id.btnRemove)
        val btnEdit = addressView.findViewById<TextView>(R.id.btnEdit)

        btnRemove.setOnClickListener {
            val message = "Are you sure you want to remove this address?"
            showConfirmationDialog(requireContext(), message) {
                // Code to execute when Yes is clicked
                removeAddress(id)
            }
        }

        btnEdit.setOnClickListener {
            showEditAddressDialog(id, address)
        }

        addressContainer.addView(addressView)
    }

    private fun removeAddress(id: String) {
        // Check if the address is used in products or services
        db.collection("merchants").document(merchantName).collection("products")
            .whereEqualTo("addressName", id)
            .get()
            .addOnSuccessListener { productDocs ->
                if (productDocs.isEmpty) {
                    db.collection("merchants").document(merchantName).collection("services")
                        .whereEqualTo("addressName", id)
                        .get()
                        .addOnSuccessListener { serviceDocs ->
                            if (serviceDocs.isEmpty) {
                                // Address is not used, proceed with removal
                                user?.let {
                                    db.collection("merchants").document(merchantName).collection("addresses").document(id)
                                        .delete()
                                        .addOnSuccessListener {
                                            fetchAddresses()
                                            Toast.makeText(context, "Address removed", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to remove address", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Address is in use, cannot remove", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            Toast.makeText(context, "Failed to check address usage", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Address is in use, cannot remove", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(context, "Failed to check address usage", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddAddressDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_address, null)
        val etName: EditText = dialogView.findViewById(R.id.etName)
        val etPhone: EditText = dialogView.findViewById(R.id.etPhone)
        val etAddress: EditText = dialogView.findViewById(R.id.etAddress)
        val etCity: EditText = dialogView.findViewById(R.id.etCity)
        val etPostalCode: EditText = dialogView.findViewById(R.id.etPostalCode)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val saveButton: TextView = dialogView.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            val name = etName.text.toString()
            val phone = etPhone.text.toString()
            val address = etAddress.text.toString()
            val city = etCity.text.toString()
            val postalCode = etPostalCode.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty() && city.isNotEmpty() && postalCode.isNotEmpty()) {
                val addressData = hashMapOf(
                    "phone" to phone,
                    "address" to address,
                    "city" to city,
                    "postalCode" to postalCode
                )
                user?.let {
                    db.collection("merchants").document(merchantName).collection("addresses").document(name)
                        .set(addressData)
                        .addOnSuccessListener {
                            builder.dismiss()
                            fetchAddresses()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                }
            }
        }

        val ivCloseDialog = dialogView.findViewById<ImageView>(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss() // Close the dialog
        }

        builder.show()
    }

    private fun showEditAddressDialog(id: String, address: Map<String, Any>) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_address, null)
        val etName: EditText = dialogView.findViewById(R.id.etName)
        val etPhone: EditText = dialogView.findViewById(R.id.etPhone)
        val etAddress: EditText = dialogView.findViewById(R.id.etAddress)
        val etCity: EditText = dialogView.findViewById(R.id.etCity)
        val etPostalCode: EditText = dialogView.findViewById(R.id.etPostalCode)

        etName.setText(id)
        etPhone.setText(address["phone"] as String)
        etAddress.setText(address["address"] as String)
        etCity.setText(address["city"] as String)
        etPostalCode.setText(address["postalCode"] as String)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val saveButton: TextView = dialogView.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            val name = etName.text.toString()
            val phone = etPhone.text.toString()
            val addressText = etAddress.text.toString()
            val city = etCity.text.toString()
            val postalCode = etPostalCode.text.toString()

            if (phone.isNotEmpty() && addressText.isNotEmpty() && city.isNotEmpty() && postalCode.isNotEmpty()) {
                val addressData = mapOf(
                    "phone" to phone,
                    "address" to addressText,
                    "city" to city,
                    "postalCode" to postalCode
                )
                user?.let {
                    db.collection("merchants").document(merchantName).collection("addresses").document(id)
                        .set(addressData).addOnSuccessListener {
                            if (!id.equals(name)){
                                changePetDocumentId(id,name)
                            }
                            builder.dismiss()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                }
            }
        }

        val ivCloseDialog = dialogView.findViewById<ImageView>(R.id.ivCloseDialog)
        ivCloseDialog.setOnClickListener {
            builder.dismiss() // Close the dialog
        }

        builder.show()
    }

    private fun changePetDocumentId(oldDocumentId: String, newDocumentId: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocument = db.collection("merchants").document(merchantName)
        val addressesCollection = userDocument.collection("addresses")

        // Get the data from the old document
        addressesCollection.document(oldDocumentId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val addressData = documentSnapshot.data

                // Create a new document with the new ID and set the data
                addressData?.let {
                    addressesCollection.document(newDocumentId).set(it).addOnSuccessListener {
                        // Delete the old document
                        addressesCollection.document(oldDocumentId).delete().addOnSuccessListener {
                            // Update address references in cart and wishlist for all users
                            updateAddressReferencesForAllUsers(oldDocumentId, newDocumentId)
                            // Update address in products and services
                            updateAddressInProductsAndServices(oldDocumentId, newDocumentId)
                            Toast.makeText(requireContext(), "Document ID changed successfully", Toast.LENGTH_SHORT).show()
                            fetchAddresses() // Reload addresses to reflect changes
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

    private fun updateAddressReferencesForAllUsers(oldDocumentId: String, newDocumentId: String) {
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocs ->
                for (userDoc in userDocs) {
                    userDoc.reference.collection("cart")
                        .whereEqualTo("merchantName", merchantName)
                        .whereEqualTo("addressName", oldDocumentId)
                        .get()
                        .addOnSuccessListener { cartDocs ->
                            for (cartDoc in cartDocs) {
                                cartDoc.reference.update("addressName", newDocumentId)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update cart: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                    userDoc.reference.collection("wishlist")
                        .whereEqualTo("merchantName", merchantName)
                        .whereEqualTo("addressName", oldDocumentId)
                        .get()
                        .addOnSuccessListener { wishlistDocs ->
                            for (wishlistDoc in wishlistDocs) {
                                wishlistDoc.reference.update("addressName", newDocumentId)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update wishlist: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAddressInProductsAndServices(oldDocumentId: String, newDocumentId: String) {
        db.collection("merchants").document(merchantName).collection("products")
            .whereEqualTo("addressName", oldDocumentId)
            .get()
            .addOnSuccessListener { productDocs ->
                for (productDoc in productDocs) {
                    productDoc.reference.update("addressName", newDocumentId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update products: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        db.collection("merchants").document(merchantName).collection("services")
            .whereEqualTo("addressName", oldDocumentId)
            .get()
            .addOnSuccessListener { serviceDocs ->
                for (serviceDoc in serviceDocs) {
                    serviceDoc.reference.update("addressName", newDocumentId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update services: ${e.message}", Toast.LENGTH_SHORT).show()
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
}
