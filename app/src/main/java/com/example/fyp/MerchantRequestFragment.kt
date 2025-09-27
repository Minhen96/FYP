package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class MerchantRequestFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var profileButton: ImageButton
    private lateinit var merchantNameEditText: EditText
    private lateinit var merchantPhoneEditText: EditText
    private lateinit var merchantAddressEditText: EditText
    private lateinit var merchantBankEditText: EditText
    private lateinit var merchantImageView: ImageView
    private lateinit var termsCheckBox: CheckBox
    private lateinit var submitButton: Button

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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_merchant_request, container, false)

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

        val backBtn : ImageView = view.findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            fragment.arguments = bundle
            replaceFragment(fragment)
        }

        merchantNameEditText = view.findViewById(R.id.storeName)
        merchantPhoneEditText = view.findViewById(R.id.phoneNumber)
        merchantBankEditText = view.findViewById(R.id.bankAccount)
        merchantImageView = view.findViewById(R.id.icImage)
        termsCheckBox = view.findViewById(R.id.termsCheckBox)
        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            validateAndSubmit()
        }

        return view
    }

    private fun validateAndSubmit() {
        val name = merchantNameEditText.text.toString().trim()
        val phone = merchantPhoneEditText.text.toString().trim()
        val bank = merchantBankEditText.text.toString().trim()
        val isImageSet = merchantImageView.drawable != null
        val isTermsChecked = termsCheckBox.isChecked

        if (name.isEmpty()) {
            merchantNameEditText.error = "Merchant name is required"
            return
        }

        if (phone.isEmpty()) {
            merchantPhoneEditText.error = "Merchant phone number is required"
            return
        }

        if (bank.isEmpty()) {
            merchantBankEditText.error = "Merchant bank account is required"
            return
        }

        if (!isImageSet) {
            Toast.makeText(context, "Merchant image is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isTermsChecked) {
            Toast.makeText(context, "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show()
            return
        }

        saveToFirestore(name, phone, bank)
    }

    private fun saveToFirestore(name: String, phone: String, bank: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(name).get().addOnSuccessListener { document ->
            if (document.exists()) {
                merchantNameEditText.error = "Merchant name already exists. Please choose another name."
            } else {
                db.collection("merchants").document(name).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        merchantNameEditText.error =
                            "Merchant name already exists. Please choose another name."
                    } else {
                        val merchant = hashMapOf(
                            "username" to username,
                            "phone" to phone,
                            "bank" to bank
                        )

                        val userDocRef = db.collection("users").document(username)
                        val merchantDocRef = db.collection("merchants").document(name)

                        merchantDocRef.set(merchant)
                            .addOnSuccessListener {
                                // Now ensure the user document exists
                                userDocRef.get().addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val merchantData = hashMapOf(
                                            "isMerchant" to "true",
                                            "MerchantName" to name
                                        )
                                        userDocRef.update(merchantData as Map<String, Any>)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Merchant registered successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                val fragment = MerchantFragment()
                                                val bundle = Bundle()
                                                bundle.putString("username", username)
                                                bundle.putString("merchantName", name)
                                                fragment.arguments = bundle
                                                replaceFragment(fragment)
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Failed to update user status: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "User document does not exist",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }.addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to fetch user document: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to register merchant: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
        }
    }



}
