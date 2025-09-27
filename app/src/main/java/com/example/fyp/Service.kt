package com.example.fyp

import android.os.Parcel
import android.os.Parcelable

data class Service(
    var name: String = "",
    var sellerName: String = "",
    var category: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var imageUrl: String = "",
    var addressName: String = "",
    var startHour: String = "",
    var endHour: String = "",
    var unavailableDays: List<String> = listOf(),
    var unavailableDates: List<String> = listOf(),
    var averageRating: Float = 0.0f,
    var reviewCount: Int = 0,
    var salesCount: Int = 0,
    var isService: Boolean = true
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        name = parcel.readString() ?: "",
        sellerName = parcel.readString() ?: "",
        category = parcel.readString() ?: "",
        price = parcel.readDouble(),
        description = parcel.readString() ?: "",
        imageUrl = parcel.readString() ?: "",
        addressName = parcel.readString() ?: "",
        startHour = parcel.readString() ?: "",
        endHour = parcel.readString() ?: "",
        unavailableDays = parcel.createStringArrayList() ?: listOf(),
        unavailableDates = parcel.createStringArrayList() ?: listOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(sellerName)
        parcel.writeString(category)
        parcel.writeDouble(price)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(addressName)
        parcel.writeString(startHour)
        parcel.writeString(endHour)
        parcel.writeStringList(unavailableDays)
        parcel.writeStringList(unavailableDates)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Service> {
        override fun createFromParcel(parcel: Parcel): Service = Service(parcel)
        override fun newArray(size: Int): Array<Service?> = arrayOfNulls(size)
    }
}
