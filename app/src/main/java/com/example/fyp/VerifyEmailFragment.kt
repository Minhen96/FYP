package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VerifyEmailFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var email: String
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        username = arguments?.getString("username").toString()
        email = arguments?.getString("email").toString()
    }

    fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainer, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_verify_email, container, false)

        val btnVerify: Button = view.findViewById(R.id.btnVerify)
        val btnBack: Button = view.findViewById(R.id.btnBack)

        btnVerify.setOnClickListener {
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(requireContext(), "Email Verified", Toast.LENGTH_SHORT).show()
                        replaceFragment(LoginFragment()) // Replace with your MainFragment
                    } else {
                        deleteUserAndData()
                    }
                } else {
                    deleteUserAndData()
                }
            }
        }

        btnBack.setOnClickListener {
            deleteUserAndData()
        }

        return view
    }

    private fun deleteUserAndData() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(username!!).delete()
                .addOnSuccessListener {
                    user.delete()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                replaceFragment(signupFragment())
                            } else {
                                Toast.makeText(requireContext(), "Failed to delete user account", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            replaceFragment(signupFragment())
        }
    }

}
