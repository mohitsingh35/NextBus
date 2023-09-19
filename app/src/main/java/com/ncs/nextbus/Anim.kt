package com.ncs.nextbus

import com.google.android.gms.maps.model.LatLng

class Anim {
    fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
        val lat = (b.latitude - a.latitude) * fraction + a.latitude
        val lng = (b.longitude - a.longitude) * fraction + a.longitude
        return LatLng(lat, lng)
    }
}