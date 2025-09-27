package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private val EMAIL_ADDRESS_REGEX = "[^@]+@[^.]+\\..+"
    private lateinit var db: FirebaseFirestore
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val etUsername: EditText = view.findViewById(R.id.etUsername)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val tvForgotPassword: TextView = view.findViewById(R.id.tvForgotPassword)
        val tvSignUp: TextView = view.findViewById(R.id.tvSignUp)

        val forgotPasswordDialog: LinearLayout = view.findViewById(R.id.forgotPasswordDialog)
        val ivCloseDialog: ImageView = view.findViewById(R.id.ivCloseDialog)
        val etForgotEmail: EditText = view.findViewById(R.id.etForgotEmail)
        val btnConfirmEmail: Button = view.findViewById(R.id.btnConfirmEmail)

        btnLogin.setOnClickListener {
            username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                val isEmail = username.matches(EMAIL_ADDRESS_REGEX.toRegex())

                if (isEmail) {
                    signInWithEmail(username, password)
                } else {
                    signInWithUsername(username, password)
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            forgotPasswordDialog.visibility = View.VISIBLE
        }

        ivCloseDialog.setOnClickListener {
            forgotPasswordDialog.visibility = View.GONE
        }

        btnConfirmEmail.setOnClickListener {
            val email = etForgotEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordResetEmail(email)
                forgotPasswordDialog.visibility = View.GONE
            }
        }

        tvSignUp.setOnClickListener {
            replaceFragment(signupFragment())
        }

        return view
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                fetchUsernameAndNavigate(email)
            } else {
                Log.w("LoginFragment", "Error signing in: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Error signing in. Please check your email and password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithUsername(username: String, password: String) {
        db.collection("users").document(username).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    val email = document.getString("email")
                    if (email != null) {
                        // Attempt to sign in with the user's email
                        signInWithEmail(email, password)
                    } else {
                        Toast.makeText(requireContext(), "Email not found for this username", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "User does not exist", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("LoginFragment", "Error fetching user data: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchUsernameAndNavigate(email: String) {
        db.collection("users").whereEqualTo("email", email).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documents = task.result?.documents
                if (documents != null && documents.isNotEmpty()) {
                    val userDoc = documents[0]
                    username = userDoc.id
                    val fragment = ProfileFragment()
                    val bundle = Bundle()
                    bundle.putString("username", username)
                    fragment.arguments = bundle
                    replaceFragment(fragment)
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("LoginFragment", "Error fetching username: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
            } else {
                Log.w("LoginFragment", "Error sending password reset email: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
