package com.example.fyp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class signupFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var email: String
    private lateinit var username: String
    private lateinit var sUsername: EditText
    private lateinit var sEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    fun replaceFragment(fragment: Fragment){
        if(fragment != null){
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainer, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        sEmail = view.findViewById(R.id.etEmail)
        sUsername = view.findViewById(R.id.etUsername)
        val sFullName: EditText = view.findViewById(R.id.etFullName)
        val sPassword: EditText = view.findViewById(R.id.etPassword)
        val sConfirmPassword: EditText = view.findViewById(R.id.etPassword2)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)

        dbRef = FirebaseDatabase.getInstance().getReference("User")

        btnSignUp.setOnClickListener {
            email = sEmail.text.toString()
            val fullName = sFullName.text.toString()
            username = sUsername.text.toString()
            val password = sPassword.text.toString()
            val confirmPassword = sConfirmPassword.text.toString()

            if (email.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                if (password != confirmPassword) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    // Check if the username or email already exists
                    checkUsernameAndEmailExist(username, email, fullName, password)
                }
            }
        }

        val tvLogin : TextView = view.findViewById(R.id.tvLogin)
        tvLogin.setOnClickListener {
            replaceFragment(LoginFragment())
        }

        return view
    }

    private fun checkUsernameAndEmailExist(username: String, email: String, fullName: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { usernameSnapshot ->
                if (!usernameSnapshot.isEmpty) {
                    sUsername.error = "Username already exists"
                } else {
                    firestore.collection("merchants").document(username).get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            sUsername.error = "Username already exists"
                        } else {
                            firestore.collection("users")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { emailSnapshot ->
                                    if (!emailSnapshot.isEmpty) {
                                        sEmail.error = "Email already exists"
                                    } else {
                                        // Proceed with creating the user
                                        signUpUser(email, fullName, username, password)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Error checking email: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error checking username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signUpUser(email: String, fullName: String, username: String, password: String) {
        Toast.makeText(requireContext(), "Signing Up...", Toast.LENGTH_SHORT).show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    saveUserDataToFirestore(user, email, fullName, username)
                } else {
                    Toast.makeText(requireContext(), "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserDataToFirestore(user: FirebaseUser?, email: String, fullName: String, username: String) {
        val userData = hashMapOf(
            "email" to email,
            "fullName" to fullName,
            "username" to username,
            "isMerchant" to "false"
        )

        firestore.collection("users").document(username).set(userData)
            .addOnSuccessListener {
                sendEmailVerification(user)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendEmailVerification(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Verification email sent to ${user.email}", Toast.LENGTH_SHORT).show()
                    val fragment = VerifyEmailFragment()
                    val bundle = Bundle()
                    bundle.putString("username", username)
                    bundle.putString("email", email)
                    fragment.arguments = bundle
                    replaceFragment(fragment)
                } else {
                    Toast.makeText(requireContext(), "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
