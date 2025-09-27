package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Stack

class MainActivity : AppCompatActivity() {
    val fragmentStack = Stack<String>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateAllUnavailableDates()

        var buttonTest: Button = findViewById(R.id.buttonTest)

        if (savedInstanceState == null) {
            replaceFragment(LoginFragment(), "LoginFragment")
        }

    }


    private fun updateAllUnavailableDates() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        db.collection("merchants")
            .get()
            .addOnSuccessListener { merchantSnapshot ->
                processMerchants(merchantSnapshot.documents, 0, today.time)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching merchants: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun processMerchants(merchants: List<DocumentSnapshot>, index: Int, today: Date) {
        if (index >= merchants.size) {
            runOnUiThread {
            }
            return
        }

        val merchant = merchants[index]
        merchant.reference.collection("services")
            .get()
            .addOnSuccessListener { servicesSnapshot ->
                val batch = db.batch()
                var needUpdate = false


                for (serviceDoc in servicesSnapshot.documents) {
                    val unavailableDates = serviceDoc.get("unavailableDates") as? List<String> ?: continue

                    val updatedDates = unavailableDates.mapNotNull { dateString ->
                        try {
                            val dates = dateString.split(" to ")
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                            when (dates.size) {
                                1 -> {
                                    // Single date
                                    val date = dateFormat.parse(dates[0])
                                    if (date?.after(today) == true) dateString else null
                                }
                                2 -> {
                                    // Date range
                                    val startDate = dateFormat.parse(dates[0])
                                    val endDate = dateFormat.parse(dates[1])
                                    val tomorrow = Calendar.getInstance().apply {
                                        time = today
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }.time

                                    when {
                                        endDate?.before(tomorrow) == true -> null // Remove if end date is before tomorrow
                                        startDate?.before(tomorrow) == true -> {
                                            // Start date has passed or is today, update to start from tomorrow
                                            if (endDate?.after(tomorrow) == true) {
                                                "${dateFormat.format(tomorrow)} to ${dateFormat.format(endDate)}"
                                            } else null
                                        }
                                        else -> dateString // Keep as is if start date is in the future
                                    }
                                }
                                else -> null // Invalid format, remove it
                            }
                        } catch (e: Exception) {
                            Log.e("DateProcessing", "Error parsing date: $dateString", e)
                            null // Remove this date if we can't parse it
                        }
                    }

                    // Only update if there's a change
                    if (updatedDates != unavailableDates) {
                        batch.update(serviceDoc.reference, "unavailableDates", updatedDates)
                        needUpdate = true
                    }
                }


                if (needUpdate) {
                    batch.commit()
                        .addOnSuccessListener {
                            runOnUiThread {
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Error updating dates for ${merchant.id}: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnCompleteListener {
                            // Process next merchant regardless of success or failure
                            processMerchants(merchants, index + 1, today)
                        }
                } else {
                    runOnUiThread {
                    }
                    processMerchants(merchants, index + 1, today)
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error fetching services for ${merchant.id}: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                // Continue to next merchant even if this one fails
                processMerchants(merchants, index + 1, today)
            }
    }

    fun clearStack(){
        if (fragmentStack.isNotEmpty()) {
            fragmentStack.clear()
        }
    }


    fun replaceFragment(fragment: Fragment, fragmentTag: String) {
        if (fragment != null) {
//            if (fragmentStack.isNotEmpty() && fragmentStack.peek() == fragmentTag) {
//                return
//            }
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragmentContainer, fragment, fragmentTag)
            transaction.addToBackStack(fragmentTag)
            transaction.commit()
            fragmentStack.push(fragmentTag)
        }
    }


    fun popBackStack() {
        if (fragmentStack.size > 1) {
            fragmentStack.pop()
            supportFragmentManager.popBackStack()
        } else {
            Toast.makeText(this, "Back button is disabled in this app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHome() {
        val homeFragment = HomeFragment()
        replaceFragment(homeFragment, "HomeFragment")
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        when (currentFragment) {
            is ProductDetailFragment -> currentFragment.handleBackNavigation()
            is MerchantProfileFragment -> currentFragment.handleBackNavigation()
            else -> popBackStack()
        }
    }
}