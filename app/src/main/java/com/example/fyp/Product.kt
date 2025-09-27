package com.example.fyp

import android.os.Parcel
import android.os.Parcelable

data class Product(
    var name: String = "",
    var price: Double = 0.0,
    val stock: Int = 0,
    var description: String = "",
    val category: String = "",
    var isService: String = "false",
    var imageUrl: String = "",
    var merchantName: String = "",
    var addressName: String = "",
    var averageRating: Float = 0.0f,
    var reviewCount: Int = 0,
    var salesCount: Int = 0,
    var startHour: String = "",
    var endHour: String = "",
    var unavailableDays: List<String> = listOf(),
    var unavailableDates: List<String> = listOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeDouble(price)
        parcel.writeInt(stock)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeString(isService)
        parcel.writeString(imageUrl)
        parcel.writeString(merchantName)
        parcel.writeString(addressName)
        parcel.writeFloat(averageRating)
        parcel.writeInt(reviewCount)
        parcel.writeInt(salesCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}