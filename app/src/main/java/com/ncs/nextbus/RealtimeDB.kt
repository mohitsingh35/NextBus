package com.ncs.nextbus


import java.io.Serializable

data class RealtimeDB(
    val item: locationData?=null,
    val key: String?=""
)  {

    data class locationData(
        val startinglat:Double?=null,
        val startinglong:Double?=null,
        val endinglat:Double?= null,
        val endinglong:Double?=null,
        val accuracy: Double?=null,
        val latitude: Double?=null,
        val longitude: Double?=null,
        val speed: Double?=null,
        val time: Long?=null,
        val busNum:String?="",
        val busID:String?="",
        val fueltype:String?="",
        val start:String?="",
        val destination:String?="",
        val driverName:String?="",
        val driverphNum:String?="",
        val driverprofilepic:String?="",
        val driverId:String?="",
        val busName:String?="",
    )
}