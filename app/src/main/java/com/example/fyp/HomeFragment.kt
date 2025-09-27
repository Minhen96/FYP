package com.example.fyp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var username: String
    private var merchantName: String = ""
    private lateinit var homeButton: ImageButton

    private var scrollPosition: Int = 0
    private lateinit var scrollView: ScrollView

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
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

        //products icon
        view.findViewById<RelativeLayout>(R.id.food).setOnClickListener {
            navigateToProductListFragment("Food", null)
        }
        view.findViewById<RelativeLayout>(R.id.snacks).setOnClickListener {
            navigateToProductListFragment("Snacks", null)
        }
        view.findViewById<RelativeLayout>(R.id.toys).setOnClickListener {
            navigateToProductListFragment("Toys", null)
        }
        view.findViewById<RelativeLayout>(R.id.clothes).setOnClickListener {
            navigateToProductListFragment("Clothes", null)
        }
        view.findViewById<RelativeLayout>(R.id.accessories).setOnClickListener {
            navigateToProductListFragment("Accessories", null)
        }
        view.findViewById<RelativeLayout>(R.id.healthcare).setOnClickListener {
            navigateToProductListFragment("Healthcare", null)
        }

        //services icon
        view.findViewById<RelativeLayout>(R.id.grooming_service).setOnClickListener {
            navigateToProductListFragment("Grooming", null)
        }
        view.findViewById<RelativeLayout>(R.id.boarding_service).setOnClickListener {
            navigateToProductListFragment("Boarding", null)
        }
        view.findViewById<RelativeLayout>(R.id.daycare_service).setOnClickListener {
            navigateToProductListFragment("Daycare", null)
        }
        view.findViewById<RelativeLayout>(R.id.training_service).setOnClickListener {
            navigateToProductListFragment("Training", null)
        }
        view.findViewById<RelativeLayout>(R.id.sitting_service).setOnClickListener {
            navigateToProductListFragment("Sitting", null)
        }
        view.findViewById<RelativeLayout>(R.id.walking_service).setOnClickListener {
            navigateToProductListFragment("Walking", null)
        }
        view.findViewById<RelativeLayout>(R.id.vet_service).setOnClickListener {
            navigateToProductListFragment("Vet", null)
        }
        view.findViewById<RelativeLayout>(R.id.adoption_service).setOnClickListener {
            navigateToProductListFragment("Adoption", null)
        }
        view.findViewById<RelativeLayout>(R.id.funeral_service).setOnClickListener {
            navigateToProductListFragment("Funeral", null)
        }

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    navigateToProductListFragment(null, query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

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

    private fun updateProfileButtonAppearance() {
        homeButton?.apply {
            setBackgroundResource(R.drawable.toolbar_background_focus)
            setImageResource(R.drawable.home_icon_white)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the profile icon when leaving the fragment
        resetProfileButtonAppearance()
    }

    private fun resetProfileButtonAppearance() {
        homeButton?.apply {
            setBackgroundResource(android.R.color.transparent)
            setImageResource(R.drawable.home_icon)
        }
    }

    private fun navigateToProductListFragment(category: String?, searchQuery: String?) {
        val productListFragment = ProductListFragment()
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("merchantName", merchantName)
        bundle.putString("category", category)
        bundle.putString("searchQuery", searchQuery)
        productListFragment.arguments = bundle
//        replaceFragment(productListFragment)
        (activity as? MainActivity)?.replaceFragment(productListFragment, "ProductListFragment")
    }

}
