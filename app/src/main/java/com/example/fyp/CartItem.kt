package com.example.fyp

data class CartItem(
    var productName: String = "",
    var serviceName: String = "",
    var addressName: String = "",
    var price: Double = 0.0,
    var quantity: Int = 1,
    var stock: Int = 0,
    var merchantName: String = "",
    var imageUrl: String = "",
    var isService: Boolean = false,
    var startHour: String = "",
    var endHour: String = "",
    var unavailableDays: List<String> = listOf(),
    var unavailableDates: List<String> = listOf()
)