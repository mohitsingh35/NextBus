package com.ncs.nextbus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.material.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.ComposeNavigator
import coil.compose.AsyncImage
import com.ncs.nextbus.feature_google_places.presentation.GooglePlacesInfoViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private lateinit var locationClient: FusedLocationProviderClient

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    private val locationPermissionCode = 1
    private lateinit var locationClient: FusedLocationProviderClient
    private var isLocationPermissionGranted by mutableStateOf(false)
    private lateinit var databaseReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = Firebase.database.reference.child("bus_location")
        val departure = intent.getStringExtra("departure")
        val destination = intent.getStringExtra("destination")
        val busNum = intent.getStringExtra("busnum")
        val busDetailsbynum = intent.getSerializableExtra("busdetails") as? RealtimeDB
        Log.d("bus num check", busDetailsbynum.toString())
        setContent {
            var showLoading by remember {
                mutableStateOf(false)
            }
            if (showLoading){
                loadingscreen()
            }
            val googleViewmodel: GooglePlacesInfoViewModel = hiltViewModel()
            val viewModel:MainActivityViewModel= hiltViewModel()
            if (departure != null && destination != null) {
                MainContent(
                    googleViewmodel = googleViewmodel,
                    departure = departure,
                    destination = destination
                )
            } else if (busNum != null) {
                MainContent(googleViewmodel = googleViewmodel, busNum = busNum, busdetailsbynum = busDetailsbynum)
            } else {
                MainContent(googleViewmodel = googleViewmodel)
            }
        }
    }

    @Composable
    private fun MainContent(googleViewmodel:GooglePlacesInfoViewModel,departure:String?=null,destination:String?=null,busNum:String?=null,busdetailsbynum:RealtimeDB?=null) {
        if (!isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            MapContent(googleViewModel = googleViewmodel, departure = departure, destination = destination, busNum = busNum, busdetailsbynum = busdetailsbynum)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionGranted = true
        } else {
            Toast.makeText(this,"Give Location Permissions to proceed",Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun MapContent(viewModel: MainActivityViewModel= hiltViewModel(),googleViewModel:GooglePlacesInfoViewModel,departure:String?=null,destination:String?=null,busNum:String?=null,busdetailsbynum:RealtimeDB?=null) {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        val res=viewModel.res.value
        val cameraPositionState = rememberCameraPositionState()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val cameraPosition = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                cameraPositionState.position = cameraPosition
            }
        }
        var isMapLoaded by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(key1 = true){
            googleViewModel.evenFlow.collectLatest { event ->
                when(event){
                    is GooglePlacesInfoViewModel.UIEvent.ShowSnackBar ->{
                        if (event.message!=""){
                        snackbarHostState.showSnackbar(
                            message = event.message
                        )
                        }
                    }
                }
            }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
            content = {
                GoogleMapView(
                    modifier = Modifier.fillMaxSize(),
                    googlePlacesInfoViewModel = googleViewModel,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                    cameraPositionState = cameraPositionState, destination = destination, departure = departure, busNum = busNum
                    , busdetailsbynum = busdetailsbynum
                )
                if(!isMapLoaded){
                    AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(Color.White)
                                .wrapContentSize()
                        )
                    }
                }
            })
//        NextBusTheme {
//            GoogleMapView(
//                modifier = Modifier.fillMaxSize(),
//                onMapLoaded = {
//                    isMapLoaded = true
//                },
//                googlePlacesInfoViewModel = glaces
//            )
//        }


//                GoogleMap(
//                    modifier = Modifier.fillMaxSize(),
//                    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = true),
//                    cameraPositionState = cameraPositionState,
//                    properties = MapProperties(isMyLocationEnabled = true)
//                ) {
//                    Marker(
//                        state=MarkerState(markerPosition),
//                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
//                    )
//
//                }

    }
}
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GoogleMapView(modifier: Modifier,onMapLoaded: () -> Unit, googlePlacesInfoViewModel: GooglePlacesInfoViewModel,
                  cameraPositionState: CameraPositionState,viewModel: MainActivityViewModel= hiltViewModel()
                  ,departure:String?=null,destination:String?=null,busNum:String?=null,busdetailsbynum:RealtimeDB?=null) {
    if (departure==null && destination==null && busNum==null) {
        var poi by remember {
            mutableStateOf("")
        }
        val res = viewModel.res.value
        if (res.item.isNotEmpty()) {
            val location = res.item[0].item
            var mapProperties by remember {
                mutableStateOf(
                    MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = true,
                        isBuildingEnabled = false
                    )
                )
            }
            var uiSettings by remember {
                mutableStateOf(
                    MapUiSettings(compassEnabled = false, zoomControlsEnabled = false)
                )
            }
            val context = LocalContext.current
            val sheetState = rememberModalBottomSheetState()
            var showBottomSheet by remember { mutableStateOf(false) }
            val clickedBus = remember { mutableStateOf<RealtimeDB?>(null) }
            val camerastate= rememberCameraPositionState()
            if (clickedBus.value!=null) {
                val userLatLng =
                    LatLng(clickedBus.value?.item?.latitude!!, clickedBus.value?.item?.longitude!!)
                val cameraPosition = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                camerastate.position = cameraPosition
            }
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)

            ) { contentPadding ->
                Box() {
                    GoogleMap(
                        modifier = modifier.padding(contentPadding),
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        cameraPositionState = if (clickedBus.value==null) cameraPositionState else camerastate,
                        onMapLoaded = onMapLoaded,
                        onPOIClick = {
                            Log.d("TAG", "POI clicked: ${it.name}")
                            poi = it.name
                        }
                    ) {

                        LaunchedEffect(
                            clickedBus.value?.item?.latitude,
                            clickedBus.value?.item?.latitude
                        ) {
                            googlePlacesInfoViewModel.getDirection(
                                origin = "${clickedBus.value?.item?.latitude}, ${clickedBus.value?.item?.longitude}",
                                destination = "${clickedBus.value?.item?.endinglat}, ${clickedBus.value?.item?.endinglong}",
                                key = MapKey.KEY
                            )
                        }
                        val markerClick: (Marker) -> Boolean = {
                            false
                        }
                        for (i in 0 until res.item.size) {
                            if (res.item[i].item?.fueltype=="EV") {
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title =  res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.bluebus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else if(res.item[i].item?.fueltype=="DIESEL"){
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.redbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else{
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.greenbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                        }
                        if (clickedBus.value != null) {
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.startinglat!!,
                                    clickedBus.value?.item?.startinglong!!
                                ),
                                title = "Starting Station",
                                context = context,
                                iconResourceId = R.drawable.start,
                            )
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.endinglat!!,
                                    clickedBus.value?.item?.endinglong!!
                                ),
                                title = "Destination Station",
                                context = context,
                                iconResourceId = R.drawable.finish,
                            )
                        }
                        Polyline(
                            points = googlePlacesInfoViewModel.polyLinesPoints.value,
                            onClick = {
                                Log.d("TAG", "${it.points} was clicked")
                            },
                            color = Color.Black
                        )

                    }
                    Box(modifier = Modifier.padding(start = 10.dp, top = 10.dp)){
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .clickable {
                                        val intent = Intent(context, FrontScreen::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                    }, contentAlignment = Alignment.Center){
                                Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = "", tint = Color.White )

                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier
                                .height(40.dp)
                                .width(120.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Black), contentAlignment = Alignment.Center){
                                Text(text = "Buses Near You", fontSize = 15.sp, fontWeight = FontWeight.Light, color = Color.White)
                            }
                        }

                    }
                }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                        },
                        sheetState = sheetState
                    ) {
                        Log.d("testse", clickedBus.value.toString())
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                                .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nearby Buses",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = "${clickedBus.value?.item?.start} <-> ${clickedBus.value?.item?.destination}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(30.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${clickedBus.value?.item?.busName}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${clickedBus.value?.item?.busNum}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Fuel : ${clickedBus.value?.item?.fueltype}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Reaching ${clickedBus.value?.item?.destination} in ",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Color.Black
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "15",
                                                    color = Color.White,
                                                    fontSize = 20.sp
                                                )
                                                Text(
                                                    text = "min",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                }
                                Spacer(modifier = Modifier.height(30.dp))
                                Text(
                                    text = "Driver Details",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(30.dp))

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(
                                            text = "${clickedBus.value?.item?.driverName}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color.Black
                                            ), contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = clickedBus.value?.item?.driverprofilepic,
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }else if (destination!=null && departure !=null){
        var poi by remember {
            mutableStateOf("")
        }
        val res = viewModel.res.value
        if (res.item.isNotEmpty()) {
            val location = res.item[0].item
            var mapProperties by remember {
                mutableStateOf(
                    MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = true,
                        isBuildingEnabled = false
                    )
                )
            }
            var uiSettings by remember {
                mutableStateOf(
                    MapUiSettings(compassEnabled = false, zoomControlsEnabled = false)
                )
            }
            val context = LocalContext.current
            val sheetState = rememberModalBottomSheetState()
            var showBottomSheet by remember { mutableStateOf(true) }
            val clickedBus = remember { mutableStateOf<RealtimeDB?>(null) }
            var showBusDetails = remember { mutableStateOf<RealtimeDB?>(null) }
            val camerastate= rememberCameraPositionState()
            if (clickedBus.value!=null) {
                val userLatLng =
                    LatLng(clickedBus.value?.item?.latitude!!, clickedBus.value?.item?.longitude!!)
                val cameraPosition = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                camerastate.position = cameraPosition
            }
            var buscount=0

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)

            ) { contentPadding ->
                Box(){

                    GoogleMap(
                        modifier = modifier.padding(contentPadding),
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        cameraPositionState = if (clickedBus.value==null) cameraPositionState else camerastate,
                        onMapLoaded = onMapLoaded,
                        onPOIClick = {
                            Log.d("TAG", "POI clicked: ${it.name}")
                            poi = it.name
                        }
                    ) {

                        LaunchedEffect(
                            clickedBus.value?.item?.latitude,
                            clickedBus.value?.item?.latitude
                        ) {
                            googlePlacesInfoViewModel.getDirection(
                                origin = "${clickedBus.value?.item?.latitude}, ${clickedBus.value?.item?.longitude}",
                                destination = "${clickedBus.value?.item?.endinglat}, ${clickedBus.value?.item?.endinglong}",
                                key = MapKey.KEY
                            )
                        }
                        val markerClick: (Marker) -> Boolean = {
                            false
                        }
                        for (i in 0 until res.item.size) {
                            if (res.item[i].item?.fueltype=="EV") {
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title =  res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.bluebus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else if(res.item[i].item?.fueltype=="DIESEL"){
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.redbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else{
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.greenbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                        }
                        if (clickedBus.value != null) {
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.startinglat!!,
                                    clickedBus.value?.item?.startinglong!!
                                ),
                                title = "Starting Station",
                                context = context,
                                iconResourceId = R.drawable.start,
                            )
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.endinglat!!,
                                    clickedBus.value?.item?.endinglong!!
                                ),
                                title = "Destination Station",
                                context = context,
                                iconResourceId = R.drawable.finish,
                            )
                        }
                        Polyline(points = googlePlacesInfoViewModel.polyLinesPoints.value, onClick = {
                            Log.d("TAG", "${it.points} was clicked")
                        }, color = Color.Black)

                    }

                    Box(modifier = Modifier.padding(start = 10.dp, top = 10.dp)){
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .clickable {
                                        val intent = Intent(context, FrontScreen::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                    }, contentAlignment = Alignment.Center){
                                Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = "", tint = Color.White )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier
                                .height(40.dp)
                                .width(120.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Black)
                                .clickable {
                                    if (buscount != 0) {
                                        showBusDetails.value = null
                                        showBottomSheet = true
                                    }
                                }, contentAlignment = Alignment.Center){
                                Text(text = "Active Buses", fontSize = 15.sp, fontWeight = FontWeight.Light, color = Color.White)
                            }
                        }

                    }


                }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                            showBusDetails.value=null
                        },
                        sheetState = sheetState
                    ) {
                        if (showBusDetails.value==null) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.4f)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Active Buses",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "in route ${departure} <-> $destination",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 15.sp
                                    )
                                    var showerror by remember {
                                        mutableStateOf(false)
                                    }
                                    if (showerror){
                                        context.showMsg("No Buses in this route")
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    LazyColumn() {
                                        items(1) {
                                            for (i in 0 until res.item.size) {
                                                if (res.item[i].item?.start == departure && res.item[i].item?.destination == destination) {
                                                    eachRow(item = res.item[i]) {
                                                        showBusDetails.value=res.item[i]
                                                        clickedBus.value=res.item[i]
                                                    }
                                                    buscount++
                                                }
                                            }
                                            if (buscount==0){
                                                showerror=true
                                                showBusDetails.value=clickedBus.value
                                            }
                                            if (buscount!=0){
                                                showBusDetails.value=clickedBus.value
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showBusDetails.value!=null){
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nearby Buses",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = "${showBusDetails.value!!.item?.start} <-> ${showBusDetails.value!!.item?.destination}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(30.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${showBusDetails.value!!.item?.busName}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${showBusDetails.value!!.item?.busNum}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Fuel : ${showBusDetails.value!!.item?.fueltype}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Reaching ${showBusDetails.value!!.item?.destination} in ",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Color.Black
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "15",
                                                    color = Color.White,
                                                    fontSize = 20.sp
                                                )
                                                Text(
                                                    text = "min",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                }
                                Spacer(modifier = Modifier.height(30.dp))
                                Text(
                                    text = "Driver Details",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(30.dp))

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(
                                            text = "${showBusDetails.value!!.item?.driverName}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color.Black
                                            ), contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = showBusDetails.value!!.item?.driverprofilepic,
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            }

                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
    else if(busNum!=null){
        var poi by remember {
            mutableStateOf("")
        }
        val res = viewModel.res.value
        if (res.item.isNotEmpty()) {
            val location = res.item[0].item
            var mapProperties by remember {
                mutableStateOf(
                    MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = true,
                        isBuildingEnabled = false
                    )
                )
            }
            var uiSettings by remember {
                mutableStateOf(
                    MapUiSettings(compassEnabled = false, zoomControlsEnabled = false)
                )
            }
            val context = LocalContext.current
            val sheetState = rememberModalBottomSheetState()
            var showBottomSheet by remember { mutableStateOf(true) }
            val clickedBus = remember { mutableStateOf<RealtimeDB?>(null) }
            var showBusDetails = remember { mutableStateOf<RealtimeDB?>(null) }
            val camerastate= rememberCameraPositionState()
            val camerastate2= rememberCameraPositionState()

            if (clickedBus.value!=null) {
                val userLatLng =
                    LatLng(clickedBus.value?.item?.latitude!!, clickedBus.value?.item?.longitude!!)
                val cameraPosition = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                camerastate.position = cameraPosition
            }
            if (busdetailsbynum!=null){
            LaunchedEffect(key1 =busdetailsbynum?.key!! ){
                val userLatLng =
                    LatLng(busdetailsbynum.item?.latitude!!, busdetailsbynum.item?.longitude!!)
                val cameraPosition = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                camerastate2.position = cameraPosition
            }
            }
            for (i in 0 until res.item.size){
                if (res.item[i].item?.busNum==busNum){
                    showBusDetails.value=res.item[i]
                }
            }
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)

            ) { contentPadding ->
                Box(){
                    GoogleMap(
                        modifier = modifier.padding(contentPadding),
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        cameraPositionState = if (clickedBus.value==null) camerastate2  else camerastate,
                        onMapLoaded = onMapLoaded,
                        onPOIClick = {
                            Log.d("TAG", "POI clicked: ${it.name}")
                            poi = it.name
                        }
                    ) {

                        LaunchedEffect(
                            clickedBus.value?.item?.latitude,
                            clickedBus.value?.item?.latitude
                        ) {
                            googlePlacesInfoViewModel.getDirection(
                                origin = "${clickedBus.value?.item?.latitude}, ${clickedBus.value?.item?.longitude}",
                                destination = "${clickedBus.value?.item?.endinglat}, ${clickedBus.value?.item?.endinglong}",
                                key = MapKey.KEY
                            )
                        }
                        val markerClick: (Marker) -> Boolean = {
                            false
                        }
                        for (i in 0 until res.item.size) {
                            if (res.item[i].item?.fueltype=="EV") {
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title =  res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.bluebus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else if(res.item[i].item?.fueltype=="DIESEL"){
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.redbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                            else{
                                MapMarker(
                                    modifier = Modifier,
                                    position = LatLng(
                                        res.item[i].item?.latitude!!,
                                        res.item[i].item?.longitude!!
                                    ),
                                    title = res.item[i].item?.busNum!!,
                                    context = context,
                                    iconResourceId = R.drawable.greenbus,
                                    onInfoWindowClick = {
                                        showBottomSheet = true
                                        clickedBus.value = res.item[i]
                                    }
                                )
                            }
                        }
                        if (clickedBus.value != null) {
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.startinglat!!,
                                    clickedBus.value?.item?.startinglong!!
                                ),
                                title = "Starting Station",
                                context = context,
                                iconResourceId = R.drawable.start,
                            )
                            MapMarker(
                                modifier = Modifier,
                                position = LatLng(
                                    clickedBus.value?.item?.endinglat!!,
                                    clickedBus.value?.item?.endinglong!!
                                ),
                                title = "Destination Station",
                                context = context,
                                iconResourceId = R.drawable.finish,
                            )
                        }
                        Polyline(points = googlePlacesInfoViewModel.polyLinesPoints.value, onClick = {
                            Log.d("TAG", "${it.points} was clicked")
                        }, color = Color.Black)

                    }
                    Box(modifier = Modifier.padding(start = 10.dp, top = 10.dp)){
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .clickable {
                                        val intent = Intent(context, FrontScreen::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                    }, contentAlignment = Alignment.Center){
                                Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = "", tint = Color.White )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier
                                .height(40.dp)
                                .width(120.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Black)
                                .clickable {
                                    for (i in 0 until res.item.size) {
                                        if (res.item[i].item?.busNum == busNum) {
                                            showBusDetails.value = res.item[i]
                                        }
                                    }
                                    showBottomSheet = true
                                    clickedBus.value=null
                                }, contentAlignment = Alignment.Center){
                                Text(text = "$busNum", fontSize = 15.sp, fontWeight = FontWeight.Light, color = Color.White)
                            }
                        }

                    }


                }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                            showBusDetails.value=null
                        },
                        sheetState = sheetState
                    ) {
                        if (showBusDetails.value==null) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Nearby Buses",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(15.dp))
                                    Text(
                                        text = "${clickedBus.value?.item?.start} <-> ${clickedBus.value?.item?.destination}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${clickedBus.value?.item?.busName}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${clickedBus.value?.item?.busNum}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Fuel : ${clickedBus.value?.item?.fueltype}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Reaching ${clickedBus.value?.item?.destination} in ",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        Color.Black
                                                    ), contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "15",
                                                        color = Color.White,
                                                        fontSize = 20.sp
                                                    )
                                                    Text(
                                                        text = "min",
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                    }
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Text(
                                        text = "Driver Details",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.padding(top = 10.dp)) {
                                            Text(
                                                text = "${clickedBus.value?.item?.driverName}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 22.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Color.Black
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = clickedBus.value?.item?.driverprofilepic,
                                                contentDescription = "",
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (showBusDetails.value!=null){
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Nearby Buses",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(15.dp))
                                    Text(
                                        text = "${showBusDetails.value!!.item?.start} <-> ${showBusDetails.value!!.item?.destination}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${showBusDetails.value!!.item?.busName}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${showBusDetails.value!!.item?.busNum}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Fuel : ${showBusDetails.value!!.item?.fueltype}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Reaching ${showBusDetails.value!!.item?.destination} in ",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        Color.Black
                                                    ), contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "15",
                                                        color = Color.White,
                                                        fontSize = 20.sp
                                                    )
                                                    Text(
                                                        text = "min",
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                    }
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Text(
                                        text = "Driver Details",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(30.dp))

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.padding(top = 10.dp)) {
                                            Text(
                                                text = "${showBusDetails.value!!.item?.driverName}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 22.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Color.Black
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = showBusDetails.value!!.item?.driverprofilepic,
                                                contentDescription = "",
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun MapMarker(
    modifier: Modifier,
    context: Context,
    position: LatLng,
    title: String,
    snippet: String?="",
    onInfoWindowClick: (Marker) -> Unit = {},
    @DrawableRes iconResourceId: Int
) {
    val icon = bitmapDescriptor(
        context, iconResourceId
    )

        Marker(
            position = position,
            title = title,
            snippet = snippet,
            icon = icon,
            onInfoWindowClick = onInfoWindowClick
        )

}
fun bitmapDescriptor(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {

    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bm = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}
@Composable
fun eachRow(item:RealtimeDB,onClick:()->Unit){
    Column(
        Modifier
            .height(70.dp)
            .clickable { onClick() }
            .fillMaxWidth()) {
        Text(text = item.item?.busNum!!, fontSize = 20.sp)
        Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
            Text(text = item.item?.busName!!, fontSize = 14.sp)
            Text(text = item.item?.fueltype!!, fontSize = 14.sp)
        }
    }
}