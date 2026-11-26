package com.example.pratica14

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapaOSMScreen() {

    val context = LocalContext.current

    // Base de datos Room
    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val dao = remember {
        database.marcadorDao()
    }

    val marcadores by remember {
        dao.obtenerMarcadores()
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var permissionGranted by remember {
        mutableStateOf(false)
    }

    // Solicitar permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { ctx ->

            MapView(ctx).apply {

                setTileSource(TileSourceFactory.OpenTopo)

                setMultiTouchControls(true)

                controller.setZoom(17.0)

                controller.setCenter(
                    GeoPoint(23.2494, -106.4111)
                )

                // Detectar pulsaciones sobre el mapa
                val eventosMapa = MapEventsOverlay(
                    object : MapEventsReceiver {

                        override fun singleTapConfirmedHelper(
                            p: GeoPoint
                        ): Boolean {
                            return false
                        }

                        override fun longPressHelper(
                            p: GeoPoint
                        ): Boolean {

                            // Crear un marcador
                            val nuevoMarcador = MarcadorEntity(
                                latitud = p.latitude,
                                longitud = p.longitude,
                                titulo = "Punto de interés"
                            )

                            // Guardar en Room
                            scope.launch {
                                dao.insertar(nuevoMarcador)
                            }

                            return true
                        }
                    }
                )

                overlays.add(eventosMapa)
            }
        },

        update = { mapView ->

            // Configurar ubicación GPS
            if (permissionGranted &&
                mapView.overlays.none {
                    it is MyLocationNewOverlay
                }
            ) {

                val locationOverlay = MyLocationNewOverlay(
                    GpsMyLocationProvider(context),
                    mapView
                )

                // Personalizar el icono GPS
                val iconoRojo = ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_gps_rojo
                )?.toBitmap()

                if (iconoRojo != null) {

                    locationOverlay.setPersonIcon(iconoRojo)

                    locationOverlay.setDirectionArrow(
                        iconoRojo,
                        iconoRojo
                    )
                }

                locationOverlay.enableMyLocation()

                locationOverlay.enableFollowLocation()

                locationOverlay.runOnFirstFix {

                    mapView.post {

                        val currentLocation =
                            locationOverlay.myLocation

                        if (currentLocation != null) {

                            mapView.controller.setZoom(17.0)

                            mapView.controller.setCenter(
                                currentLocation
                            )
                        }
                    }
                }

                mapView.overlays.add(locationOverlay)
            }

            // Eliminar marcadores visuales anteriores
            mapView.overlays.removeAll {
                it is Marker
            }

            // Mostrar marcadores almacenados en Room
            marcadores.forEach { punto ->

                val marker = Marker(mapView)

                marker.position = GeoPoint(
                    punto.latitud,
                    punto.longitud
                )

                marker.title = punto.titulo

                marker.snippet =
                    "Latitud: ${punto.latitud}\n" +
                            "Longitud: ${punto.longitud}"

                marker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
                )

                mapView.overlays.add(marker)
            }

            mapView.invalidate()
        },

        onRelease = { mapView ->
            mapView.onDetach()
        }
    )
}