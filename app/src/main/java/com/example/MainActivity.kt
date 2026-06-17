package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OneUIAppContainer()
            }
        }
    }
}

@Composable
fun OneUIAppContainer() {
    var currentScreen by remember { mutableStateOf("home") }
    var previousScreen by remember { mutableStateOf("home") }
    var installedApps by remember { mutableStateOf(setOf<String>()) }

    val navigateTo: (String) -> Unit = { screen ->
        previousScreen = currentScreen
        currentScreen = screen
    }

    when (currentScreen) {
        "home" -> {
            OneUIHomeScreen(
                installedApps = installedApps,
                onLaunchApp = { appId ->
                    when (appId) {
                        "camera" -> navigateTo("camera")
                        "play_store" -> navigateTo("play_store")
                        "galaxy_store" -> navigateTo("galaxy_store")
                    }
                }
            )
        }
        "camera" -> {
            CameraScreen(
                installedApps = installedApps,
                onInstallApp = { appId -> installedApps = installedApps + appId },
                onUninstallApp = { appId -> installedApps = installedApps - appId },
                onBackPressed = { currentScreen = "home" },
                onNavigateToPlayStore = { navigateTo("play_store") },
                onNavigateToGalaxyStore = { navigateTo("galaxy_store") }
            )
        }
        "play_store" -> {
            PlayStoreScreen(
                installedApps = installedApps,
                onInstallApp = { appId -> installedApps = installedApps + appId },
                onUninstallApp = { appId -> installedApps = installedApps - appId },
                onDismiss = { currentScreen = previousScreen }
            )
        }
        "galaxy_store" -> {
            GalaxyStoreScreen(
                installedApps = installedApps,
                onInstallApp = { appId -> installedApps = installedApps + appId },
                onUninstallApp = { appId -> installedApps = installedApps - appId },
                onDismiss = { currentScreen = previousScreen }
            )
        }
    }
}

// Emulated Enums for Camera configuration
enum class CameraMode(val label: String) {
    PANORAMA("Panorama"),
    PRO("Pro"),
    LIVE_FOCUS("Live focus"),
    PHOTO("Photo"),
    VIDEO("Video"),
    CONTINUOUS_SHOT("Continuous shot")
}

enum class ZoomLevel(val ratio: Float, val label: String) {
    WIDE(0.6f, "0.5x"),
    NORMAL(1.0f, "1.0x")
}

enum class FlashState {
    OFF, ON, AUTO
}

enum class TimerState(val seconds: Int, val label: String) {
    OFF(0, "OFF"),
    S2(2, "2s"),
    S5(5, "5s"),
    S10(10, "10s")
}

enum class AspectState(val ratioText: String, val ratioFloat: Float) {
    AS_9_16("9:16", 9f / 16f),
    AS_3_4("3:4", 3f / 4f),
    AS_1_1("1:1", 1f),
    AS_FULL("Full", 0f) // Dynamic fill
}

enum class CameraFilter(val label: String, val matrixDelta: ColorMatrix?) {
    NONE("Original", null),
    WARM("Warm Sun", ColorMatrix(floatArrayOf(
        1.15f, 0.05f, 0f, 0f, 0f,
        0f, 1.05f, 0f, 0f, 0f,
        0f, 0f, 0.85f, 0f, 0f,
        0f, 0f, 0f, 1.00f, 0f
    ))),
    COOL("Ice Ocean", ColorMatrix(floatArrayOf(
        0.85f, 0f, 0f, 0f, 0f,
        0f, 1.05f, 0f, 0f, 0f,
        0f, 0f, 1.25f, 0f, 0f,
        0f, 0f, 0f, 1.00f, 0f
    ))),
    VINTAGE("Retro Sepia", ColorMatrix(floatArrayOf(
        0.90f, 0.15f, 0.05f, 0f, 10f,
        0.05f, 0.80f, 0.15f, 0f, 5f,
        0.05f, 0.05f, 0.70f, 0f, 0f,
        0.00f, 0.00f, 0.00f, 1.00f, 0f
    ))),
    BW("B&W Slate", ColorMatrix().apply { setToSaturation(0f) }),
    CYBER("Cyber Punk", ColorMatrix(floatArrayOf(
        1.20f, 0f, 0.30f, 0f, 20f,
        0f, 0.80f, 0f, 0f, 0f,
        0.20f, 0f, 1.30f, 0f, 30f,
        0f, 0f, 0f, 1.00f, 0f
    ))),
    CYBER_NEON("Cyber Neon", ColorMatrix(floatArrayOf(
        1.0f, 0f, 1.0f, 0f, 40f,
        0f, 0.7f, 0.5f, 0f, 10f,
        0.5f, 0f, 1.5f, 0f, 50f,
        0f, 0f, 0f, 1.0f, 0f
    ))),
    VAPORWAVE("Vaporwave", ColorMatrix(floatArrayOf(
        0.8f, 0.3f, 0.1f, 0f, 30f,
        0.1f, 1.2f, 0.1f, 0f, 20f,
        0.2f, 0.1f, 1.4f, 0f, 40f,
        0f, 0f, 0f, 1.0f, 0f
    ))),
    DREAMY_GLOW("Dreamy Glow", ColorMatrix(floatArrayOf(
        1.1f, 0.1f, 0.1f, 0f, 15f,
        0.1f, 1.1f, 0.1f, 0f, 15f,
        0.1f, 0.1f, 1.2f, 0f, 20f,
        0f, 0f, 0f, 1.0f, 0f
    ))),
    VINTAGE_PASTEL("Vintage Pastel", ColorMatrix(floatArrayOf(
        0.95f, 0.05f, 0.05f, 0f, 10f,
        0.05f, 0.90f, 0.05f, 0f, 10f,
        0.05f, 0.05f, 0.85f, 0f, 10f,
        0f, 0f, 0f, 1.0f, 0f
    ))),
    GOOGLE_HDR("Pixel HDR+ AI", ColorMatrix(floatArrayOf(
        1.10f, 0.00f, 0.00f, 0f, 15f,
        0.00f, 1.15f, 0.00f, 0f, 10f,
        0.00f, 0.00f, 1.20f, 0f, 20f,
        0f, 0f, 0f, 1.0f, 0f
    )))
}

data class CapturedItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val filterApplied: CameraFilter,
    val zoomLevel: ZoomLevel,
    val isVideo: Boolean = false,
    val videoDurationSec: Int = 0,
    val isBurst: Boolean = false,
    val burstCount: Int = 0,
    val filePath: String? = null
)

fun saveCapturedItems(context: Context, items: List<CapturedItem>) {
    try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, CapturedItem::class.java)
        val adapter = moshi.adapter<List<CapturedItem>>(type)
        val json = adapter.toJson(items)
        val sharedPrefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("captured_items", json).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadCapturedItems(context: Context): List<CapturedItem> {
    try {
        val sharedPrefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("captured_items", null)
        if (!json.isNullOrEmpty()) {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, CapturedItem::class.java)
            val adapter = moshi.adapter<List<CapturedItem>>(type)
            return adapter.fromJson(json) ?: emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return listOf(
        CapturedItem(
            timestamp = "Yesterday, 3:45 PM",
            filterApplied = CameraFilter.COOL,
            zoomLevel = ZoomLevel.NORMAL
        )
    )
}

@Composable
fun CameraXViewfinder(
    isFrontCamera: Boolean,
    zoomState: ZoomLevel,
    flashState: FlashState,
    imageCapture: ImageCapture,
    onCameraReady: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(isFrontCamera, cameraProviderFuture, zoomState, flashState) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            val boundCamera = try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (ex: Exception) {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }
            onCameraReady(boundCamera)
            
            try {
                val zoomRatio = if (zoomState == ZoomLevel.WIDE) 0.6f else 1.0f
                boundCamera.cameraControl.setZoomRatio(zoomRatio)
            } catch (zoomEx: Exception) {
                zoomEx.printStackTrace()
            }
            
            try {
                val flashMode = when (flashState) {
                    FlashState.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashState.ON -> ImageCapture.FLASH_MODE_ON
                    FlashState.AUTO -> ImageCapture.FLASH_MODE_AUTO
                }
                imageCapture.flashMode = flashMode
            } catch (flashEx: Exception) {
                flashEx.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    installedApps: Set<String>,
    onInstallApp: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onBackPressed: () -> Unit,
    onNavigateToPlayStore: () -> Unit,
    onNavigateToGalaxyStore: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Intercept back button to return to home screen
    BackHandler {
        onBackPressed()
    }

    // Permission State
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    // Request permission immediately on launch
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // CameraX instance hooks
    val imageCapture = remember { ImageCapture.Builder().build() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    
    // States
    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var zoomState by remember { mutableStateOf(ZoomLevel.NORMAL) }
    var flashState by remember { mutableStateOf(FlashState.OFF) }
    var timerState by remember { mutableStateOf(TimerState.OFF) }
    var aspectState by remember { mutableStateOf(AspectState.AS_FULL) }
    var activeFilter by remember { mutableStateOf(CameraFilter.NONE) }
    var isFrontCamera by remember { mutableStateOf(false) }
    
    // Features & Settings overrides
    var gridLinesEnabled by remember { mutableStateOf(false) }
    var locationTagsEnabled by remember { mutableStateOf(false) }
    var shutterSoundEnabled by remember { mutableStateOf(true) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var activeSticker by remember { mutableStateOf<String?>(null) }
    
    // Live action / animations state
    var isCapturing by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoRecordingDuration by remember { mutableStateOf(0) }
    var showFlashOverlay by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(0) }
    var burstCounter by remember { mutableStateOf(0) }
    
    // Tap to Focus State
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }
    
    // Pro Mode Slider States
    var proISO by remember { mutableStateOf(100f) }
    var proShutter by remember { mutableStateOf(125f) } // 1x / Value
    var proManualBlurFocus by remember { mutableStateOf(1.0f) } // Manual focusing bokeh depth
    
    // Dialogue / sheet toggles
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStickersSheet by remember { mutableStateOf(false) }
    var showFiltersSheet by remember { mutableStateOf(false) }
    var showGalleryView by remember { mutableStateOf(false) }
    var showStoreView by remember { mutableStateOf(false) }
    var showPlayStoreView by remember { mutableStateOf(false) }
    
    // List of captured items
    val capturedItems = remember { 
        val list = mutableStateListOf<CapturedItem>()
        list.addAll(loadCapturedItems(context))
        list
    }

    LaunchedEffect(capturedItems.toList()) {
        saveCapturedItems(context, capturedItems)
    }

    // Video Recording Timer logic
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoRecordingDuration = 0
            while (isRecordingVideo) {
                delay(1000)
                videoRecordingDuration++
            }
        }
    }

    // Helper capturing trigger
    val triggerActualCapture: (Boolean, Boolean) -> Unit = { isVideo, isBurst ->
        coroutineScope.launch {
            if (shutterSoundEnabled) {
                // Mimic standard click
            }
            if (!isVideo) {
                showFlashOverlay = true
                delay(100)
                showFlashOverlay = false
            }
            
            val timeStamp = SimpleDateFormat("h:mm a, MMM dd", Locale.getDefault()).format(Date())
            
            // Check if we can capture using the real camera
            if (cameraPermissionState.status.isGranted && !isVideo && !isBurst) {
                val file = java.io.File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            capturedItems.add(
                                CapturedItem(
                                    timestamp = timeStamp,
                                    filterApplied = activeFilter,
                                    zoomLevel = zoomState,
                                    filePath = file.absolutePath
                                )
                            )
                            isCapturing = false
                        }
                        override fun onError(exception: ImageCaptureException) {
                            // Fallback to simulated if real fails
                            capturedItems.add(
                                CapturedItem(
                                    timestamp = timeStamp,
                                    filterApplied = activeFilter,
                                    zoomLevel = zoomState,
                                    filePath = null
                                )
                            )
                            isCapturing = false
                        }
                    }
                )
            } else {
                // Fallback / simulated flow
                if (isBurst) {
                    capturedItems.add(
                        CapturedItem(
                            timestamp = timeStamp,
                            filterApplied = activeFilter,
                            zoomLevel = zoomState,
                            isBurst = true,
                            burstCount = 10
                        )
                    )
                } else {
                    capturedItems.add(
                        CapturedItem(
                            timestamp = timeStamp,
                            filterApplied = activeFilter,
                            zoomLevel = zoomState,
                            isVideo = isVideo,
                            videoDurationSec = if (isVideo) videoRecordingDuration else 0
                        )
                    )
                }
                isCapturing = false
            }
        }
    }

    val onShutterPressed = {
        if (isCapturing || isRecordingVideo) {
            // Do nothing or stop recording
            if (isRecordingVideo) {
                isRecordingVideo = false
                triggerActualCapture(true, false)
            }
        } else {
            isCapturing = true
            
            // Handle timer delay
            if (timerState != TimerState.OFF && currentMode != CameraMode.VIDEO) {
                coroutineScope.launch {
                    countdownValue = timerState.seconds
                    while (countdownValue > 0) {
                        delay(1000)
                        countdownValue--
                    }
                    if (currentMode == CameraMode.CONTINUOUS_SHOT) {
                        // Trigger Continuous shot
                        burstCounter = 1
                        while (burstCounter <= 10) {
                            delay(120)
                            burstCounter++
                        }
                        burstCounter = 0
                        triggerActualCapture(false, true)
                    } else {
                        triggerActualCapture(false, false)
                    }
                }
            } else {
                // Direct Capture
                if (currentMode == CameraMode.VIDEO) {
                    isRecordingVideo = true
                } else if (currentMode == CameraMode.CONTINUOUS_SHOT) {
                    coroutineScope.launch {
                        burstCounter = 1
                        while (burstCounter <= 10) {
                            delay(120)
                            burstCounter++
                        }
                        burstCounter = 0
                        triggerActualCapture(false, true)
                    }
                } else {
                    triggerActualCapture(false, false)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ================== TOP STATUS & TOOLBAR ==================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(vertical = 4.dp)
            ) {
                // Real-Time System Status Bar
                var systemTimeStr by remember { mutableStateOf("") }
                var systemDateStr by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val sdfDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                    while (true) {
                        val d = Date()
                        systemTimeStr = sdfTime.format(d)
                        systemDateStr = sdfDate.format(d)
                        delay(1000)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$systemDateStr  $systemTimeStr",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi Connected",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.SignalCellular4Bar,
                            contentDescription = "Cell Network 5G",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "5G",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "98%",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Battery Full Charging",
                                tint = Color(0xFF34A853),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top Title / Option "Sticker"
                    Text(
                        text = "Sticker",
                        color = if (activeSticker != null) Color(0xFFFDD835) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .testTag("sticker_top_tab")
                            .clickable {
                                showStickersSheet = true
                            }
                            .padding(vertical = 4.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Store Shortcut
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF34A853).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF34A853).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToPlayStore() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Store",
                                tint = Color(0xFF34A853),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Play Store",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Galaxy Store Shortcut
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFE91E63).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToGalaxyStore() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Galaxy Store",
                                tint = Color(0xFFFF4081),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Galaxy Store",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row of Tools evenly spaced
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back & Settings
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackPressed,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Home Screen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Flash icon (state cyclic)
                    IconButton(
                        onClick = {
                            flashState = when (flashState) {
                                FlashState.OFF -> FlashState.AUTO
                                FlashState.AUTO -> FlashState.ON
                                FlashState.ON -> FlashState.OFF
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("flash_button")
                    ) {
                        val flashIcon = when (flashState) {
                            FlashState.OFF -> Icons.Rounded.FlashOff
                            FlashState.ON -> Icons.Rounded.FlashOn
                            FlashState.AUTO -> Icons.Rounded.FlashAuto
                        }
                        val tintColor = if (flashState == FlashState.OFF) Color.White else Color(0xFFFDD835)
                        Icon(
                            imageVector = flashIcon,
                            contentDescription = "Flash State",
                            tint = tintColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Camera Timer options icon
                    IconButton(
                        onClick = {
                            timerState = when (timerState) {
                                TimerState.OFF -> TimerState.S2
                                TimerState.S2 -> TimerState.S5
                                TimerState.S5 -> TimerState.S10
                                TimerState.S10 -> TimerState.OFF
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("timer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (timerState == TimerState.OFF) Icons.Default.TimerOff else Icons.Default.Timer,
                                contentDescription = "Timer Settings",
                                tint = if (timerState == TimerState.OFF) Color.White else Color(0xFFFDD835),
                                modifier = Modifier.size(24.dp)
                            )
                            if (timerState != TimerState.OFF) {
                                Text(
                                    text = timerState.label.replace("s", ""),
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Aspect Ratio Icon
                    IconButton(
                        onClick = {
                            aspectState = when (aspectState) {
                                AspectState.AS_9_16 -> AspectState.AS_3_4
                                AspectState.AS_3_4 -> AspectState.AS_1_1
                                AspectState.AS_1_1 -> AspectState.AS_9_16 // Skipping AS_FULL for simple viewport
                                else -> AspectState.AS_9_16
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("aspect_ratio_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = aspectState.ratioText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Filters/Effects Icon (Right)
                    IconButton(
                        onClick = { showFiltersSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("effects_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Effects/Filters",
                            tint = if (activeFilter != CameraFilter.NONE) Color(0xFFFDD835) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ================== CAMERA PREVIEW CONTAINER ==================
            // Center camera viewfinder occupying maximum available space to emulate dynamic screen sizes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Apply Aspect Ratio Constraint Based on Configuration
                val previewModifier = if (aspectState == AspectState.AS_FULL) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .aspectRatio(aspectState.ratioFloat)
                        .fillMaxWidth()
                }

                // Main simulated screen sensor Box
                Box(
                    modifier = previewModifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    focusPoint = offset
                                    isFocusing = true
                                    coroutineScope.launch {
                                        delay(1800)
                                        isFocusing = false
                                    }
                                }
                            )
                        }
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        // Real hardware sensor stream
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Mirror front-camera view
                                    scaleX = if (isFrontCamera) -1f else 1f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CameraXViewfinder(
                                isFrontCamera = isFrontCamera,
                                zoomState = zoomState,
                                flashState = flashState,
                                imageCapture = imageCapture,
                                onCameraReady = { boundCamera = it }
                            )
                            
                            // App filter overlay on top of hardware camera feed
                            val matrix = activeFilter.matrixDelta
                            if (matrix != null) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_camera_preview),
                                    contentDescription = "",
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = ColorFilter.colorMatrix(matrix),
                                    alpha = 0.05f // very subtle tint overlay or transparent layer
                                )
                            }
                        }
                    } else {
                        val scaleFactor by animateFloatAsState(
                            targetValue = if (zoomState == ZoomLevel.WIDE) 1.45f else 1.0f,
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        )
                        
                        val flipRotation by animateFloatAsState(
                            targetValue = if (isFrontCamera) 180f else 0f,
                            animationSpec = tween(500, easing = LinearOutSlowInEasing)
                        )

                        // Textured outdoor image placeholder
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.img_camera_preview),
                                contentDescription = "Camera viewfinder live texture picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scaleFactor * (if (isFrontCamera) -1f else 1f)
                                        scaleY = scaleFactor
                                        rotationY = flipRotation
                                    },
                                colorFilter = activeFilter.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                            )

                            // Glassmorphic Request permission Banner overlay
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.75f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(24.dp)
                                    .clickable { cameraPermissionState.launchPermissionRequest() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera Icon",
                                        tint = Color(0xFFFDD835),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            "Simulated Viewfinder Active",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Tap to enable your real mobile camera",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3x3 Grid Overlay if Enabled
                    if (gridLinesEnabled) {
                        CustomGridLines()
                    }

                    // Tapping Focus locked indicator
                    if (isFocusing && focusPoint != null) {
                        FocusIndicator(offset = focusPoint!!)
                    }

                    // Live Filter Banner Overlay
                    if (activeFilter != CameraFilter.NONE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Filter: ${activeFilter.label}",
                                color = Color(0xFFFDD835),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Samsung Secure Dynamic NPU Upscaling Badge
                    if (installedApps.contains("neural_super_res")) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color(0xFFFF4081).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "AI Super Resolution Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "NPU AI UPSCALE (120MP)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Live FPS Overlay HUD (Optimized 60fps / 120fps via requestAnimationFrame with withFrameNanos)
                    if (installedApps.contains("fps_perf_monitor")) {
                        var liveFpsValue by remember { mutableStateOf(60) }
                        var liveCpuValue by remember { mutableStateOf(16) }
                        var liveTempCelsius by remember { mutableStateOf(35.2f) }
                        
                        LaunchedEffect(Unit) {
                            var lastFpsUpdateNanos = System.nanoTime()
                            var frameCount = 0
                            while (true) {
                                withFrameNanos { timeNanos ->
                                    // requestAnimationFrame tick
                                    frameCount++
                                    val elapsedNanos = timeNanos - lastFpsUpdateNanos
                                    if (elapsedNanos >= 600_000_000L) { // calculate every 600ms
                                        val actualFps = ((frameCount * 1_000_000_000L) / elapsedNanos).toInt()
                                        liveFpsValue = actualFps.coerceIn(58, 122)
                                        frameCount = 0
                                        lastFpsUpdateNanos = timeNanos
                                        
                                        // Update simulated stress indicators slowly
                                        liveCpuValue = (12..28).random()
                                        liveTempCelsius = (342..358).random() / 10f
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = 52.dp)
                                .padding(12.dp)
                                .background(Color(0xFF161616).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF34A853), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF34A853), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LIVE PERF MONITOR", color = Color(0xFF34A853), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("FPS: $liveFpsValue Hz", color = Color.White, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text("CPU STRESS: $liveCpuValue%", color = Color.White, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text("TEMP: $liveTempCelsius °C", color = Color.White, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }

                    // Vintage Retro VHS scanlines & live green OSD text overlay (Animated at 60fps via requestAnimationFrame)
                    if (installedApps.contains("vhs_vintage_multiplier")) {
                        var scanLinePhase by remember { mutableStateOf(0f) }
                        LaunchedEffect(Unit) {
                            val startNanos = System.nanoTime()
                            while (true) {
                                withFrameNanos { timeNanos ->
                                    val elapsedMs = (timeNanos - startNanos) / 1_000_000f
                                    scanLinePhase = (elapsedMs * 0.015f) % 20f
                                }
                            }
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scanlineSpacing = 20f
                            var y0 = scanLinePhase
                            while (y0 < size.height) {
                                drawLine(
                                    color = Color.Green.copy(alpha = 0.06f),
                                    start = Offset(0f, y0),
                                    end = Offset(size.width, y0),
                                    strokeWidth = 1.5f
                                )
                                y0 += scanlineSpacing
                            }
                        }
                        
                        var liveVhsTime by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)
                            while (true) {
                                liveVhsTime = sdf.format(Date()).uppercase()
                                delay(1000)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "REC  ▶  OSD  VHS  120 FPS\nAUTO-AESTHETIC UPGRADE\nDATE: " + liveVhsTime,
                                color = Color.Green,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.BottomStart),
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Active Sticker Floating View
                    if (activeSticker != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = activeSticker!!,
                                    fontSize = 72.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Video Recording indicators (Blinking Rec symbol)
                    if (isRecordingVideo) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                                .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val alphaPulse by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer { alpha = alphaPulse }
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(
                                    "REC %02d:%02d",
                                    videoRecordingDuration / 60,
                                    videoRecordingDuration % 60
                                ),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mode Specific Overlays (e.g., PRO overlay metadata)
                    if (currentMode == CameraMode.PRO) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("PRO SETTINGS", color = Color(0xFFFDD835), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ISO: ${proISO.toInt()}", color = Color.White, fontSize = 11.sp)
                            Text("SHUTTER: 1/${proShutter.toInt()}s", color = Color.White, fontSize = 11.sp)
                            Text("LENS DEPTH: ${String.format("%.1f", proManualBlurFocus)}x", color = Color.White, fontSize = 11.sp)
                        }
                    } else if (currentMode == CameraMode.LIVE_FOCUS) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Live Focus Active (~blur enabled)",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    } else if (currentMode == CameraMode.PANORAMA) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.SwapHorizontalCircle, "Sweep", tint = Color.Green, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Sweep slowly left/right", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Burst Shooting Overlay Counter
                    if (burstCounter > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(100.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$burstCounter",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Active Countdown Number Timer overlay
                    if (countdownValue > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$countdownValue",
                                color = Color(0xFFFDD835),
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.animateContentSize()
                            )
                        }
                    }
                }
            }

            // ================== BOTTOM CONTROLS & SELECTION ==================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                // Above mode selector: Zoom selectors (leaves icon)centered horizontally
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF222222), RoundedCornerShape(30.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Wide Angle zoom button (3 leaves representation)
                        IconButton(
                            onClick = { zoomState = ZoomLevel.WIDE },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (zoomState == ZoomLevel.WIDE) Color.White else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = "Wide Lens",
                                tint = if (zoomState == ZoomLevel.WIDE) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Standard Normal zoom button (1 leaf representation)
                        IconButton(
                            onClick = { zoomState = ZoomLevel.NORMAL },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (zoomState == ZoomLevel.NORMAL) Color.White else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Standard Lens",
                                tint = if (zoomState == ZoomLevel.NORMAL) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Horizontal scrollable One UI Camera Modes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val listState = rememberLazyListState()
                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        contentPadding = PaddingValues(horizontal = 140.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(CameraMode.values().toList()) { index, mode ->
                            val isSelected = currentMode == mode
                            if (isSelected) {
                                // Selected capsule with dark label
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(20.dp))
                                        .clickable { currentMode = mode }
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                        .testTag("mode_${mode.name.lowercase()}_pill")
                                ) {
                                    Text(
                                        text = mode.label,
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Unselected standard labels
                                Text(
                                    text = mode.label,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { 
                                            currentMode = mode 
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                        .testTag("mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Capturing row: Gallery (Left) | Shutter (Center) | Flip camera (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Thumbnail Circle button (Left)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("gallery_thumbnail")
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .clickable { showGalleryView = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val lastItem = if (capturedItems.isNotEmpty()) capturedItems.last() else null
                        if (lastItem?.filePath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = java.io.File(lastItem.filePath)),
                                contentDescription = "Gallery Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                                colorFilter = lastItem.filterApplied.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_camera_preview),
                                contentDescription = "Gallery Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                                colorFilter = lastItem?.filterApplied?.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                            )
                        }
                        
                        // Badge count overlay
                        if (capturedItems.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFFFDD835), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${capturedItems.size - 1}",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Elegant Large Samsung Style Shutter representation
                    val shutterScale by animateFloatAsState(
                        targetValue = if (isCapturing || isRecordingVideo) 0.85f else 1.0f,
                        animationSpec = tween(150)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .graphicsLayer {
                                scaleX = shutterScale
                                scaleY = shutterScale
                            }
                            .testTag("shutter_button")
                            .clickable { onShutterPressed() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Ring shadow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, Color.White, CircleShape)
                        )
                        // Inner ring gap spacing, filled core
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxSize()
                                .background(
                                    if (currentMode == CameraMode.VIDEO && isRecordingVideo) Color.Red else Color.White,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentMode == CameraMode.VIDEO && !isRecordingVideo) {
                                // Camera Video Red center point
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                    }

                    // Flip Camera Button (Right)
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("flip_camera_button")
                            .background(Color(0xFF1E1E1E), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera Front/Rear",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Camera shutter flash animation visual overlay
        if (showFlashOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Settings Dialog Panel
        if (showSettingsDialog) {
            CameraSettingsDialog(
                gridEnabled = gridLinesEnabled,
                onGridToggle = { gridLinesEnabled = it },
                locationTagsEnabled = locationTagsEnabled,
                onLocationTagsToggle = { locationTagsEnabled = it },
                shutterSoundEnabled = shutterSoundEnabled,
                onShutterSoundToggle = { shutterSoundEnabled = it },
                hapticFeedbackEnabled = hapticFeedbackEnabled,
                onHapticFeedbackToggle = { hapticFeedbackEnabled = it },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Stickers Overlay Selector sheet
        if (showStickersSheet) {
            StickersBottomPicker(
                activeSticker = activeSticker,
                installedApps = installedApps,
                onStickerSelected = { activeSticker = it },
                onDismiss = { showStickersSheet = false }
            )
        }

        // Filters Overlay Selector tray
        if (showFiltersSheet) {
            FiltersBottomPicker(
                activeFilter = activeFilter,
                installedApps = installedApps,
                onFilterSelected = { activeFilter = it },
                onDismiss = { showFiltersSheet = false },
                onOpenStore = {
                    showFiltersSheet = false
                    showStoreView = true
                },
                onOpenPlayStore = {
                    showFiltersSheet = false
                    showPlayStoreView = true
                }
            )
        }

        // Photo Gallery Viewer Screen
        if (showGalleryView) {
            GalleryViewScreen(
                capturedItems = capturedItems,
                onDelete = { item -> capturedItems.remove(item) },
                onDismiss = { showGalleryView = false },
                isFrontCamera = isFrontCamera,
                installedApps = installedApps
            )
        }

        // Galaxy App & Game Store Screen
        if (showStoreView) {
            GalaxyStoreScreen(
                installedApps = installedApps,
                onInstallApp = onInstallApp,
                onUninstallApp = onUninstallApp,
                onDismiss = { showStoreView = false }
            )
        }

        // Google Play Store Screen
        if (showPlayStoreView) {
            PlayStoreScreen(
                installedApps = installedApps,
                onInstallApp = onInstallApp,
                onUninstallApp = onUninstallApp,
                onDismiss = { showPlayStoreView = false }
            )
        }
    }
}

// 3x3 Grid Overlay Composable
@Composable
fun CustomGridLines() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Horizontal lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hStep = size.height / 3f
            val wStep = size.width / 3f
            
            // Draw horizontal dividers
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(0f, hStep),
                end = Offset(size.width, hStep),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(0f, hStep * 2),
                end = Offset(size.width, hStep * 2),
                strokeWidth = 1f
            )

            // Draw vertical dividers
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(wStep, 0f),
                end = Offset(wStep, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(wStep * 2, 0f),
                end = Offset(wStep * 2, size.height),
                strokeWidth = 1f
            )
        }
    }
}

// Tap focus indicator drawing yellow locks
@Composable
fun FocusIndicator(offset: Offset) {
    val scale = remember { Animatable(1.5f) }
    val opacity = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        delay(1200)
        opacity.animateTo(0.0f, animationSpec = tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity.value }
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = offset.x.dp / 2f - 30.dp,
                    y = offset.y.dp / 2f - 30.dp
                )
                .size(60.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .border(2.dp, Color(0xFFFDD835), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Focus Lock",
                tint = Color(0xFFFDD835),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Custom Settings Dialog matching One UI spacing & cards
@Composable
fun CameraSettingsDialog(
    gridEnabled: Boolean,
    onGridToggle: (Boolean) -> Unit,
    locationTagsEnabled: Boolean,
    onLocationTagsToggle: (Boolean) -> Unit,
    shutterSoundEnabled: Boolean,
    onShutterSoundToggle: (Boolean) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F0F))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Camera settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scrollable options content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "General",
                        color = Color(0xFFFDD835),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Option Item Card - Grid
                    SettingSwitchCard(
                        title = "Grid lines",
                        subtitle = "Overlays a clean grid rule of thirds on the viewfinder sensor",
                        checked = gridEnabled,
                        onCheckedChange = onGridToggle
                    )

                    // Option Item Card - Sound
                    SettingSwitchCard(
                        title = "Shutter sound",
                        subtitle = "Play standard audios on capturing items",
                        checked = shutterSoundEnabled,
                        onCheckedChange = onShutterSoundToggle
                    )

                    // Option Item Card - Haptic
                    SettingSwitchCard(
                        title = "Vibration feedback",
                        subtitle = "Buzz slightly on selecting layouts or shutter taps",
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackToggle
                    )

                    // Option Item Card - Locations
                    SettingSwitchCard(
                        title = "Location tags",
                        subtitle = "Store GPS location markers within file elements",
                        checked = locationTagsEnabled,
                        onCheckedChange = onLocationTagsToggle
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Information",
                        color = Color(0xFFFDD835),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // About Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Samsung Camera UI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Pixel perfect One UI Replica v14.0.01", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Crafted flawlessly using Jetpack Compose", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Setting item row
@Composable
fun SettingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFDD835),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color(0xFF262626)
                )
            )
        }
    }
}

// Sticker Picker Tray Overlay
@Composable
fun StickersBottomPicker(
    activeSticker: String?,
    installedApps: Set<String>,
    onStickerSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val stickers = remember(installedApps) {
        val baseStickers = mutableListOf("🕶️", "🐱", "👨", "👑", "✨", "🔥", "❤️", "🍿")
        if (installedApps.contains("cyberpunk_pack")) {
            baseStickers.addAll(listOf("🤖", "👾", "☄️", "🛸"))
        }
        if (installedApps.contains("dreamy_pack")) {
            baseStickers.addAll(listOf("🎀", "🧁", "🌟", "🌸"))
        }
        baseStickers
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Sticker Overlays", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option to clear sticker
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (activeSticker == null) Color(0xFFFDD835) else Color(0xFF262626)
                                )
                                .clickable { onStickerSelected(null) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("None", color = if (activeSticker == null) Color.Black else Color.White, fontSize = 14.sp)
                        }
                    }

                    items(stickers) { sticker ->
                        val isSelected = activeSticker == sticker
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFFFDD835) else Color(0xFF262626)
                                )
                                .clickable { onStickerSelected(sticker) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sticker, fontSize = 36.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Live Filter Selector Tray Overlay
@Composable
fun FiltersBottomPicker(
    activeFilter: CameraFilter,
    installedApps: Set<String>,
    onFilterSelected: (CameraFilter) -> Unit,
    onDismiss: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenPlayStore: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Camera Filters", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Get extra filters in Play Store & Galaxy Store", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val filterList = CameraFilter.values()
                    items(filterList.size) { index ->
                        val filter = filterList[index]
                        val isSelected = activeFilter == filter
                        val isLocked = when (filter) {
                            CameraFilter.CYBER_NEON, CameraFilter.VAPORWAVE -> !installedApps.contains("cyberpunk_pack")
                            CameraFilter.DREAMY_GLOW, CameraFilter.VINTAGE_PASTEL -> !installedApps.contains("dreamy_pack")
                            CameraFilter.GOOGLE_HDR -> !installedApps.contains("pixel_camera_filter")
                            else -> false
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(82.dp)
                                .clickable {
                                    if (isLocked) {
                                        if (filter == CameraFilter.GOOGLE_HDR) {
                                            onOpenPlayStore()
                                        } else {
                                            onOpenStore()
                                        }
                                    } else {
                                        onFilterSelected(filter)
                                    }
                                }
                        ) {
                            // Mini Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        if (isSelected && !isLocked) Color(0xFFFDD835) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_camera_preview),
                                    contentDescription = filter.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = if (filter.matrixDelta != null) ColorFilter.colorMatrix(filter.matrixDelta) else null
                                )
                                
                                // Lock overlay
                                if (isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked filter pack",
                                            tint = Color(0xFFFDD835),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = filter.label,
                                color = if (isLocked) Color.White.copy(alpha = 0.4f) else if (isSelected) Color(0xFFFDD835) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected && !isLocked) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// Media Gallery View Screen
@Composable
fun GalleryViewScreen(
    capturedItems: List<CapturedItem>,
    onDelete: (CapturedItem) -> Unit,
    onDismiss: () -> Unit,
    isFrontCamera: Boolean,
    installedApps: Set<String>
) {
    var selectedItemForDetail by remember { mutableStateOf<CapturedItem?>(null) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0C0C))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Camera Roll (${capturedItems.size})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (capturedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Empty Image",
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No captured elements yet", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(capturedItems) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { selectedItemForDetail = item }
                            ) {
                                if (item.filePath != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = java.io.File(item.filePath)),
                                        contentDescription = "Captured Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                                        colorFilter = item.filterApplied.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_camera_preview),
                                        contentDescription = "Captured Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                                        colorFilter = item.filterApplied.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                                    )
                                }

                                // Overlay icons representing bursts / videos
                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, "Video", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                } else if (item.isBurst) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("BURST • ${item.burstCount}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High Resolution Full Element details modal
            if (selectedItemForDetail != null) {
                DetailViewerDialog(
                    item = selectedItemForDetail!!,
                    isFrontCamera = isFrontCamera,
                    onDelete = {
                        onDelete(selectedItemForDetail!!)
                        selectedItemForDetail = null
                    },
                    onDismiss = { selectedItemForDetail = null },
                    installedApps = installedApps
                )
            }
        }
    }
}

// Gallery Detail viewer with delete, filter names, specs
@Composable
fun DetailViewerDialog(
    item: CapturedItem,
    isFrontCamera: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    installedApps: Set<String>
) {
    var isEraserActive by remember { mutableStateOf(false) }
    val eraserPaths = remember { mutableStateListOf<Offset>() }
    var isErasingInProgress by remember { mutableStateOf(false) }
    var objectErasedSuccessfully by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text("Viewer details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (installedApps.contains("magic_eraser")) {
                            IconButton(onClick = { 
                                isEraserActive = !isEraserActive 
                                eraserPaths.clear()
                                objectErasedSuccessfully = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "Magic Eraser",
                                    tint = if (isEraserActive) Color(0xFFFDD835) else Color.White
                                )
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }

                // Focal Large image display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(isEraserActive) {
                            if (isEraserActive) {
                                detectTapGestures { offset ->
                                    eraserPaths.add(offset)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.filePath != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = java.io.File(item.filePath)),
                            contentDescription = "Full Review photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                            colorFilter = item.filterApplied.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_camera_preview),
                            contentDescription = "Full Review photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = if (isFrontCamera) 180f else 0f },
                            colorFilter = item.filterApplied.matrixDelta?.let { ColorFilter.colorMatrix(it) }
                        )
                    }

                    // If magic eraser is active we paint translucent red dots where they touch
                    if (isEraserActive && !objectErasedSuccessfully) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            eraserPaths.forEach { pos ->
                                drawCircle(
                                    color = Color.Red.copy(alpha = 0.5f),
                                    radius = 35f,
                                    center = pos
                                )
                            }
                        }
                    }

                    // Bottom info label for erasing tutorial
                    if (isEraserActive && !objectErasedSuccessfully && !isErasingInProgress) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Touch to paint items pink to erase",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Remove button when some points are marked
                    if (isEraserActive && eraserPaths.isNotEmpty() && !objectErasedSuccessfully && !isErasingInProgress) {
                        Button(
                            onClick = {
                                isErasingInProgress = true
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ERASE CHOSEN OBJECTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Erasing in progress loading indicator
                    if (isErasingInProgress) {
                        LaunchedEffect(Unit) {
                            delay(1800)
                            isErasingInProgress = false
                            objectErasedSuccessfully = true
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFFF4081))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("AI Generative Inpainting...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Reconstructing background contents", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }

                    // Erased success badge overlay
                    if (objectErasedSuccessfully) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(Color(0xFF34A853), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Success", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Removed ${eraserPaths.size} object(s) successfully!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Info card at the bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414))
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (item.isVideo) "Video Capture Recording" else if (item.isBurst) "Continuous Burst Capture" else "Captured Photo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Taken on: ${item.timestamp}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TECHNICAL ATTRIBUTES",
                        color = Color(0xFFFDD835),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Aspect Size", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text("12 MP • 9:16", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Capture Filter", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text(item.filterApplied.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Zoom Scale", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text(item.zoomLevel.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

enum class StoreApp(
    val id: String,
    val title: String,
    val description: String,
    val size: String,
    val icon: String,
    val category: String,
    val rating: String
) {
    SPACE_ARCADE(
        "space_arcade",
        "Galaxy Retro Arcade Shooter",
        "Tailored arcade space action! Command a galaxy defense ship, fire photon blasts, and dodge falling space rocks. Highly optimized for Samsung 120Hz displays.",
        "1.2 MB",
        "🚀",
        "Games • Action & Adventure",
        "4.9★"
    ),
    CYBERPUNK_PACK(
        "cyberpunk_pack",
        "Cyberpunk Neon Theme & Stickers",
        "Infuse neon magenta styling into your lenses! Unlocks exclusive live stickers (🤖, 👾, ☄️, 🛸) and dynamic camera filters: Cyber Neon and Vaporwave.",
        "650 KB",
        "🪐",
        "Photography • Aesthetic",
        "4.8★"
    ),
    DREAMY_PACK(
        "dreamy_pack",
        "Dreamy Glow Soft-Portrait Upgrade",
        "Bring professional bokeh and pastel aura to your selfies. Adds kawaii stickers (🎀, 🧁, 🌟, 🌸) and two soft-portrait glowing photo filters.",
        "840 KB",
        "🌸",
        "Photography • Beautify",
        "4.7★"
    ),
    VHS_VINTAGE(
        "vhs_vintage_multiplier",
        "Retro Film VHS Overlay Mode",
        "Burn analog videotape effects directly onto your pictures. Unlocks dynamic CRT green safe lines, analog watermarkers, and retro screen glitches.",
        "320 KB",
        "📼",
        "Camera Utilities",
        "4.6★"
    ),
    NEURAL_SUPER_RES(
        "neural_super_res",
        "NPU Super Resolution AI Core",
        "Connects to secure Galaxy Cloud Super Resolution API. Unlocks deep learning photo upscaling (120 Megapixel dynamic upscale). Input custom API keys.",
        "150 KB",
        "⚡",
        "System Tools • AI Engine",
        "4.9★"
    )
}

// Galaxy Store High-Fidelity Simulator dialog screen
@Composable
fun GalaxyStoreScreen(
    installedApps: Set<String>,
    onInstallApp: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Featured, 1: Games, 2: My Apps
    var showGameDialog by remember { mutableStateOf(false) }
    var activeApiKeyInput by remember { mutableStateOf("") }
    var showApiKeyManager by remember { mutableStateOf(false) }

    // Simulated download maps
    val coroutineScope = rememberCoroutineScope()
    val downloadProgressMap = remember { mutableStateMapOf<String, Float>() }

    val filteredApps = StoreApp.values().filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Samsung Galaxy Store
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        text = "Galaxy Store",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Refresh, "Reset Search", tint = Color.White)
                    }
                }

                // Beautiful Pink Store Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF4081), Color(0xFFE91E63), Color(0xFF9C27B0))
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("PRO CAMERA PACK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Unleash Galaxy NPU",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get upscaling filters, retro scanlines & interactive game modules",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Samsung Style Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps, games and plugins", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF4081),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                // Elegant Segment Tab Header
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0A0A0A),
                    contentColor = Color(0xFFFF4081)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("FEATURED", color = if (selectedTab == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("GAMES", color = if (selectedTab == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("MY APPS", color = if (selectedTab == 2) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display Filtered App List
                val displayList = remember(selectedTab, searchQuery) {
                    when (selectedTab) {
                        0 -> filteredApps
                        1 -> filteredApps.filter { it.id == "space_arcade" }
                        else -> filteredApps.filter { installedApps.contains(it.id) }
                    }
                }

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingBag, "Empty Store", tint = Color.DarkGray, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No plugins or apps found", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        displayList.forEach { app ->
                            item {
                                val isInstalled = installedApps.contains(app.id)
                                val progress = downloadProgressMap[app.id]

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // App launcher icon box
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.sweepGradient(
                                                        colors = listOf(Color(0xFF333333), Color(0xFF222222))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(app.icon, fontSize = 28.sp)
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Details Title & description
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = app.category,
                                                color = Color(0xFFFF4081),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = app.description,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                lineHeight = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(app.rating, color = Color(0xFFFDD835), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(app.size, color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Dynamic Samsung style Circular download action or normal install button
                                        Box(contentAlignment = Alignment.Center) {
                                            if (progress != null && progress < 1f) {
                                                // Simulated downloading circular stroke indicator
                                                CircularProgressIndicator(
                                                    progress = progress,
                                                    color = Color(0xFFFF4081),
                                                    strokeWidth = 3.dp,
                                                    modifier = Modifier.size(46.dp)
                                                )
                                                Text(
                                                    text = "${(progress * 100).toInt()}%",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else if (progress != null && progress >= 1f && progress < 1.1f) {
                                                // Installation step
                                                CircularProgressIndicator(
                                                    color = Color(0xFF00E676),
                                                    strokeWidth = 3.dp,
                                                    modifier = Modifier.size(46.dp)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Ready",
                                                    tint = Color(0xFF00E676),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                // Primary action trigger
                                                if (isInstalled) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Button(
                                                            onClick = {
                                                                if (app.id == "space_arcade") {
                                                                    showGameDialog = true
                                                                } else if (app.id == "neural_super_res") {
                                                                    showApiKeyManager = true
                                                                } else {
                                                                    // Simple dynamic action toast indicator
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(
                                                                text = if (app.id == "space_arcade") "PLAY" else if (app.id == "neural_super_res") "API" else "ACTIVE",
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Uninstall",
                                                            color = Color.Red.copy(alpha = 0.6f),
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.clickable { onUninstallApp(app.id) }
                                                        )
                                                    }
                                                } else {
                                                    // Trigger Simulated Installation procedure
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                downloadProgressMap[app.id] = 0f
                                                                var currentVal = 0f
                                                                while (currentVal < 1f) {
                                                                    delay(120)
                                                                    currentVal += 0.1f + (Math.random() * 0.1f).toFloat()
                                                                    downloadProgressMap[app.id] = currentVal.coerceAtMost(1f)
                                                                }
                                                                // Set status installing
                                                                downloadProgressMap[app.id] = 1.05f
                                                                delay(1800)
                                                                // Fully installed
                                                                onInstallApp(app.id)
                                                                downloadProgressMap.remove(app.id)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = "Install",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Get", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive arcade shooter popup
    if (showGameDialog) {
        Dialog(
            onDismissRequest = { showGameDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                RetroSpaceGame(onDismiss = { showGameDialog = false })
            }
        }
    }

    // Dynamic Secure Camera API Key settings
    if (showApiKeyManager) {
        Dialog(onDismissRequest = { showApiKeyManager = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Secure API Key settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showApiKeyManager = false }) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The system reads standard keys directly from secure environment declarations at compile time, matching AI Studio standards.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Show current compile key if found
                    val activeSecretStr = BuildConfig.CAMERA_API_KEY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222222))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("COMPILE TIME KEY (BuildConfig)", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = if (activeSecretStr.isEmpty()) "UNSET (Using default trial engine)" else activeSecretStr,
                                color = if (activeSecretStr.isEmpty()) Color.Red else Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = activeApiKeyInput,
                        onValueChange = { activeApiKeyInput = it },
                        placeholder = { Text("Input dynamic secure API key overrides", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF4081),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showApiKeyManager = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Galaxy AI backend", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 100% playable retro space arcade shooting mini-game
@Composable
fun RetroSpaceGame(onDismiss: () -> Unit) {
    val score = remember { mutableStateOf(0) }
    val highScore = remember { mutableStateOf(0) }
    val playerHearts = remember { mutableStateOf(3) }
    val gameOver = remember { mutableStateOf(false) }

    val laserOffsets = remember { mutableStateListOf<Offset>() }
    val meteoriteOffsets = remember { mutableStateListOf<Offset>() }
    val explosionSparks = remember { mutableStateListOf<Offset>() } // Spawn points for sparks

    // Player position
    var playerX by remember { mutableStateOf(400f) }

    // Dimensions
    var constraintsWidth by remember { mutableStateOf(1000f) }
    var constraintsHeight by remember { mutableStateOf(1600f) }

    // Game loop tick using Kotlin coroutines
    LaunchedEffect(gameOver.value) {
        if (!gameOver.value) {
            while (true) {
                delay(60)

                // 1. Move active projectile lasers up
                val currentLasers = laserOffsets.toList()
                laserOffsets.clear()
                currentLasers.forEach { laser ->
                    if (laser.y > 0) {
                        laserOffsets.add(Offset(laser.x, laser.y - 45f))
                    }
                }

                // 2. Move meteorite obstacles down
                val currentMeteors = meteoriteOffsets.toList()
                meteoriteOffsets.clear()
                currentMeteors.forEach { meteor ->
                    if (meteor.y < constraintsHeight) {
                        meteoriteOffsets.add(Offset(meteor.x, meteor.y + 12f))
                    } else {
                        // Reached bottom - deduct player life
                        playerHearts.value--
                        if (playerHearts.value <= 0) {
                            gameOver.value = true
                            if (score.value > highScore.value) {
                                highScore.value = score.value
                            }
                        }
                    }
                }

                // 3. Projectile - obstacle overlap check (Collisions)
                val lasersToClear = mutableListOf<Offset>()
                val meteorsToClear = mutableListOf<Offset>()

                laserOffsets.forEach { laser ->
                    meteoriteOffsets.forEach { meteor ->
                        // Simple radius collision check
                        val distance = Math.sqrt(
                            Math.pow((laser.x - meteor.x).toDouble(), 2.0) +
                            Math.pow((laser.y - meteor.y).toDouble(), 2.0)
                        )
                        if (distance < 55) {
                            lasersToClear.add(laser)
                            meteorsToClear.add(meteor)
                            score.value += 10
                            // Spawn explosive spark target
                            explosionSparks.add(meteor)
                        }
                    }
                }
                laserOffsets.removeAll(lasersToClear)
                meteoriteOffsets.removeAll(meteorsToClear)
            }
        }
    }

    // Obstacle Spawner ticking loop
    LaunchedEffect(gameOver.value) {
        if (!gameOver.value) {
            while (true) {
                delay(1200) // Spawns every 1.2s
                val spawnX = (Math.random() * (constraintsWidth - 100f)).toFloat() + 50f
                meteoriteOffsets.add(Offset(spawnX, 10f))
            }
        }
    }

    // Spark fader timer
    LaunchedEffect(explosionSparks.size) {
        if (explosionSparks.isNotEmpty()) {
            delay(400)
            explosionSparks.clear()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .padding(16.dp)
    ) {
        constraintsWidth = maxWidth.value * 2.5f // Convert or estimate scale ratio
        constraintsHeight = maxHeight.value * 2.5f

        // Stars overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 2f, center = Offset(size.width * 0.15f, size.height * 0.2f))
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 3f, center = Offset(size.width * 0.72f, size.height * 0.11f))
            drawCircle(color = Color.White.copy(alpha = 0.2f), radius = 2.5f, center = Offset(size.width * 0.45f, size.height * 0.48f))
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 3.5f, center = Offset(size.width * 0.88f, size.height * 0.75f))
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 2f, center = Offset(size.width * 0.24f, size.height * 0.65f))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Stats HUD Row at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Exit Retro Game", tint = Color.White)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "SCORE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${score.value}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        val hasHeart = index < playerHearts.value
                        Icon(
                            imageVector = if (hasHeart) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Health Indicator",
                            tint = if (hasHeart) Color.Red else Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // High Score panel
            Text(
                text = "HIGH SCORE: ${highScore.value}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            // Game Interactive Canvas Zone
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                if (gameOver.value) {
                    // Game Over Visual Cover Card
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "MISSION FAILED",
                                color = Color.Red,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Final Space Ranger Score: ${score.value}",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Button(
                                onClick = {
                                    score.value = 0
                                    playerHearts.value = 3
                                    laserOffsets.clear()
                                    meteoriteOffsets.clear()
                                    gameOver.value = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("REDEPLOY STARSHIP", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Render Active Game Space Projectiles & Meteorites & Spark effects
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Projectile beams (Pink laser strings)
                        laserOffsets.forEach { pos ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (pos.x / 2.5f).dp, y = (pos.y / 2.5f).dp)
                                    .size(width = 4.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFFF4081))
                            )
                        }

                        // Spark particle blasts
                        explosionSparks.forEach { pos ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (pos.x / 2.5f - 16f).dp, y = (pos.y / 2.5f - 16f).dp)
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💥", fontSize = 24.sp)
                            }
                        }

                        // Descending Obstacle Meteorites
                        meteoriteOffsets.forEach { pos ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (pos.x / 2.5f).dp, y = (pos.y / 2.5f).dp)
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("☄️", fontSize = 28.sp)
                            }
                        }

                        // Floating Starship Avatar (Rocket) at bottom
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(x = ((playerX - constraintsWidth / 2f) / 2.5f).dp, y = (-20).dp)
                                .size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚀", fontSize = 38.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual touchpad d-pad controllers (Left, Right, Shoot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left movement D-Pad anchor
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .clickable {
                                playerX = (playerX - 90f).coerceAtLeast(60f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, "Move Left", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Right movement D-Pad anchor
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .clickable {
                                playerX = (playerX + 90f).coerceAtMost(constraintsWidth - 60f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, "Move Right", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Photon Firing Button (Big pink circular trigger)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.sweepGradient(
                                colors = listOf(Color(0xFFFF4081), Color(0xFFE91E63))
                            )
                        )
                        .clickable {
                            if (!gameOver.value) {
                                // Double barrel lasers spawned at current rocket center
                                laserOffsets.add(Offset(playerX - 25f, constraintsHeight - 150f))
                                laserOffsets.add(Offset(playerX + 25f, constraintsHeight - 150f))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("BLAST", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

enum class PlayStoreApp(
    val id: String,
    val title: String,
    val description: String,
    val size: String,
    val icon: String,
    val rating: String,
    val category: String
) {
    PIXEL_HDR("pixel_camera_filter", "Pixel Camera HDR+ Mod", "Computational high dynamic range filters for pro high-fidelity camera details.", "4.2 MB", "📸", "4.8★", "Photography"),
    MAGIC_ERASER("magic_eraser", "Google Magic Eraser Plugin", "Paint on photos to intelligently isolate and erase background clutter.", "1.2 MB", "🪄", "4.9★", "Extensions"),
    PERF_HUD("fps_perf_monitor", "FPS & Perf Monitor Meter", "Renders a live hardware stats monitor showing frame rates, CPU stress, and battery temp.", "650 KB", "📊", "4.7★", "Utilities"),
    RETRO_FLAPPY("play_games_flappy", "Retro Flappy Bird Arcade", "Relive the legendary side-scroller bird arcade. Tap to flap and escape pipeline obstacles.", "1.8 MB", "🐥", "4.6★", "Games")
}

@Composable
fun PlayStoreScreen(
    installedApps: Set<String>,
    onInstallApp: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val downloadProgressMap = remember { mutableStateMapOf<String, Float>() }
    var activePlayingAppId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Google Play Style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Outlined styled Google search bar bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps & games...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.LightGray, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F9D58),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF334155),
                            unfocusedContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Play Store",
                        color = Color(0xFF34A853),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category pill selects
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("All", "Photography", "Utilities", "Games").forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) Color(0xFF0F9D58) else Color(0xFF334155))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(cat, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Apps List representation
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val appsFiltered = PlayStoreApp.values().filter { app ->
                        (selectedCategory == "All" || app.category.contains(selectedCategory, ignoreCase = true)) &&
                        (app.title.contains(searchQuery, ignoreCase = true) || app.description.contains(searchQuery, ignoreCase = true))
                    }

                    if (appsFiltered.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No matching products found.", color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }

                    items(appsFiltered.size) { index ->
                        val app = appsFiltered[index]
                        val isInstalled = installedApps.contains(app.id)
                        val progress = downloadProgressMap[app.id]

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Icon
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.icon, fontSize = 32.sp)
                                }

                                // Info Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(app.category + " • " + app.rating + " • " + app.size, color = Color(0xFF0F9D58), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.description, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 14.sp)

                                    if (progress != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = progress,
                                            color = Color(0xFF0F9D58),
                                            trackColor = Color(0xFF334155),
                                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                        )
                                        Text("Downloading: ${(progress * 100).toInt()}%", color = Color.LightGray, fontSize = 9.sp)
                                    }
                                }

                                // CTA buttons
                                if (isInstalled) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                if (app.id == "play_games_flappy") {
                                                    activePlayingAppId = app.id
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                                            shape = RoundedCornerShape(18.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (app.id == "play_games_flappy") "PLAY" else "OPEN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Text(
                                            text = "Uninstall",
                                            color = Color.Red.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clickable { onUninstallApp(app.id) }
                                                .padding(4.dp)
                                        )
                                    }
                                } else if (progress == null) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                downloadProgressMap[app.id] = 0.1f
                                                for (i in 2..10) {
                                                    delay(140)
                                                    downloadProgressMap[app.id] = i / 10f
                                                }
                                                delay(150)
                                                downloadProgressMap.remove(app.id)
                                                onInstallApp(app.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("INSTALL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Embedded play game portal dialog
            if (activePlayingAppId == "play_games_flappy") {
                FlappyRetroGame(
                    onDismiss = { activePlayingAppId = null }
                )
            }
        }
    }
}

data class FlappyPipe(
    var x: Float,
    val gapY: Float,
    var passed: Boolean = false
)

@Composable
fun FlappyRetroGame(onDismiss: () -> Unit) {
    val score = remember { mutableStateOf(0) }
    val highScore = remember { mutableStateOf(0) }
    val birdY = remember { mutableStateOf(200f) }
    val birdVelocity = remember { mutableStateOf(0f) }
    val gameActive = remember { mutableStateOf(false) }
    val gameOver = remember { mutableStateOf(false) }
    val pipes = remember { mutableStateListOf<FlappyPipe>() }
    
    fun flap() {
        if (gameOver.value) {
            birdY.value = 200f
            birdVelocity.value = -8f
            pipes.clear()
            pipes.add(FlappyPipe(400f, 150f))
            pipes.add(FlappyPipe(650f, 220f))
            score.value = 0
            gameOver.value = false
            gameActive.value = true
        } else if (!gameActive.value) {
            pipes.clear()
            pipes.add(FlappyPipe(400f, 150f))
            pipes.add(FlappyPipe(650f, 220f))
            gameActive.value = true
            birdVelocity.value = -10f
        } else {
            birdVelocity.value = -10f
        }
    }

    LaunchedEffect(gameActive.value, gameOver.value) {
        if (gameActive.value && !gameOver.value) {
            while (gameActive.value && !gameOver.value) {
                delay(24)
                
                birdVelocity.value += 0.7f
                birdY.value += birdVelocity.value

                if (birdY.value < 0f || birdY.value > 380f) {
                    gameOver.value = true
                    gameActive.value = false
                }

                for (i in pipes.indices.reversed()) {
                    val pipe = pipes[i]
                    pipe.x -= 4.5f

                    val birdX = 70f
                    val pipeWidth = 50f
                    val pipeGapHeight = 120f

                    if (pipe.x < birdX + 24f && pipe.x + pipeWidth > birdX) {
                        if (birdY.value < pipe.gapY || birdY.value + 24f > pipe.gapY + pipeGapHeight) {
                            gameOver.value = true
                            gameActive.value = false
                        }
                    }

                    if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                        pipe.passed = true
                        score.value++
                        if (score.value > highScore.value) {
                            highScore.value = score.value
                        }
                    }

                    if (pipe.x < -60f) {
                        pipes.removeAt(i)
                        val lastPipeX = if (pipes.isEmpty()) 300f else pipes.last().x
                        val randomGapY = (80..240).random().toFloat()
                        pipes.add(FlappyPipe(lastPipeX + 250f, randomGapY))
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4FC3F7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(width = 330.dp, height = 520.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable { flap() }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Close Game", tint = Color.White)
                    }

                    Box(modifier = Modifier.offset(x = 40.dp, y = 50.dp).background(Color.White.copy(alpha = 0.5f), CircleShape).size(width = 80.dp, height = 30.dp))
                    Box(modifier = Modifier.offset(x = 180.dp, y = 80.dp).background(Color.White.copy(alpha = 0.5f), CircleShape).size(width = 100.dp, height = 35.dp))

                    pipes.forEach { pipe ->
                        Box(
                            modifier = Modifier
                                .offset(x = pipe.x.dp, y = 0.dp)
                                .size(width = 50.dp, height = pipe.gapY.dp)
                                .background(
                                    Color(0xFF2E7D32),
                                    RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                )
                                .border(1.5.dp, Color.White)
                        )

                        val lowerPipeTop = pipe.gapY + 120f
                        Box(
                            modifier = Modifier
                                .offset(x = pipe.x.dp, y = lowerPipeTop.dp)
                                .size(width = 50.dp, height = (400f - lowerPipeTop).coerceAtLeast(120f).dp)
                                .background(
                                    Color(0xFF2E7D32),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                                .border(1.5.dp, Color.White)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 70.dp, y = birdY.value.dp)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐥", fontSize = 28.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF8B5A2B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color(0xFF7CB342))
                        )
                    }

                    if (!gameActive.value && !gameOver.value) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("FLAPPY RETRO BIRD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap anywhere to Flap Wings", color = Color(0xFFFFEB3B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Evade pipelines to score!", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    if (gameOver.value) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💥 CRASHED!", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("YOUR SCORE: ${score.value}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("HIGH SCORE: ${highScore.value}", color = Color(0xFFFFEB3B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { flap() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("REPLAY RETRO", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("SCORE: ${score.value}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("HI: ${highScore.value}", color = Color(0xFFFFEB3B), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ==========================================
// SAMSUNG ONE UI LAUNCHER HOME SCREEN REPLICA
// ==========================================
@Composable
fun OneUIHomeScreen(
    installedApps: Set<String>,
    onLaunchApp: (String) -> Unit
) {
    var weatherState by remember { mutableStateOf(0) } // 0: Sunny, 1: Rainy, 2: Snowy
    var showPhoneDialer by remember { mutableStateOf(false) }
    var showChatMessages by remember { mutableStateOf(false) }
    var showBrowserSearch by remember { mutableStateOf(false) }
    var showSettingsInfo by remember { mutableStateOf(false) }
    var showFlappyGameDirect by remember { mutableStateOf(false) }

    // Weather options
    val weatherIcons = listOf("☀️", "🌧️", "❄️")
    val weatherLabels = listOf("Sunny • Seattle 21°C", "Rainy • Los Angeles 16°C", "Snowy • New York -2°C")
    val weatherBgColors = listOf(
        Color(0xFFE28743).copy(alpha = 0.15f),
        Color(0xFF2A4B7C).copy(alpha = 0.15f),
        Color(0xFF7CB9E8).copy(alpha = 0.15f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color(0xFF131722), Color(0xFF07090E)),
                    center = Offset(400f, 500f),
                    radius = 900f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main desktop layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Simulated Status Bar Top Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, "WiFi", tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.SignalCellularAlt, "Network", tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.BatteryChargingFull, "Battery", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Text("100%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Samsung Dynamic Weather and Clock Card widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { weatherState = (weatherState + 1) % 3 }
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(weatherBgColors[weatherState])
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date()),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weatherLabels[weatherState],
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap widget to cycle cities",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = weatherIcons[weatherState],
                        fontSize = 38.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Glassmorphic Google Pill Search widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                    .clickable { showBrowserSearch = true }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, "Search", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search apps, files, or web...",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("🎙️", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Grid of Icons (Symmetrical 4x4 Grid layout)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Camera app launcher
                item {
                    LauncherIconItem(
                        iconEmoji = "📸",
                        label = "Camera",
                        bgColor = Color(0xFF2C2C2C),
                        borderColor = Color(0xFFFF4081),
                        onClick = { onLaunchApp("camera") }
                    )
                }

                // 2. Play Store
                item {
                    LauncherIconItem(
                        iconEmoji = "🛍️",
                        label = "Play Store",
                        bgColor = Color(0xFFFFFFFF),
                        borderColor = Color(0xFF0F9D58),
                        onClick = { onLaunchApp("play_store") }
                    )
                }

                // 3. Galaxy Store
                item {
                    LauncherIconItem(
                        iconEmoji = "🌌",
                        label = "Galaxy Store",
                        bgColor = Color(0xFFE91E63),
                        borderColor = Color(0xFFFF4081),
                        onClick = { onLaunchApp("galaxy_store") }
                    )
                }

                // 4. Phone dialer
                item {
                    LauncherIconItem(
                        iconEmoji = "📞",
                        label = "Phone",
                        bgColor = Color(0xFF4CAF50),
                        borderColor = Color(0xFF81C784),
                        onClick = { showPhoneDialer = true }
                    )
                }

                // 5. Messages
                item {
                    LauncherIconItem(
                        iconEmoji = "💬",
                        label = "Messages",
                        bgColor = Color(0xFF1E88E5),
                        borderColor = Color(0xFF64B5F6),
                        onClick = { showChatMessages = true }
                    )
                }

                // 6. Chrome/Internet browser
                item {
                    LauncherIconItem(
                        iconEmoji = "🌐",
                        label = "Internet",
                        bgColor = Color(0xFF0288D1),
                        borderColor = Color(0xFF4FC3F7),
                        onClick = { showBrowserSearch = true }
                    )
                }

                // 7. Settings
                item {
                    LauncherIconItem(
                        iconEmoji = "⚙️",
                        label = "Settings",
                        bgColor = Color(0xFF607D8B),
                        borderColor = Color(0xFF90A4AE),
                        onClick = { showSettingsInfo = true }
                    )
                }

                // Dynamic App installations from Play Store:
                // If Flappy Bird is installed
                if (installedApps.contains("play_games_flappy")) {
                    item {
                        LauncherIconItem(
                            iconEmoji = "🐥",
                            label = "Flappy Retro",
                            bgColor = Color(0xFFFFEB3B),
                            borderColor = Color(0xFFFFC107),
                            onClick = { showFlappyGameDirect = true }
                        )
                    }
                }

                // If Perf Meter is installed
                if (installedApps.contains("fps_perf_monitor")) {
                    item {
                        LauncherIconItem(
                            iconEmoji = "📊",
                            label = "Perf HUD",
                            bgColor = Color(0xFF009688),
                            borderColor = Color(0xFF4DB6AC),
                            onClick = { onLaunchApp("camera") }
                        )
                    }
                }

                // If Magic Eraser is installed
                if (installedApps.contains("magic_eraser")) {
                    item {
                        LauncherIconItem(
                            iconEmoji = "🪄",
                            label = "Eraser Plugin",
                            bgColor = Color(0xFF9C27B0),
                            borderColor = Color(0xFFBA68C8),
                            onClick = { onLaunchApp("camera") }
                        )
                    }
                }

                // If Pixel HDR Camera is installed
                if (installedApps.contains("pixel_camera_filter")) {
                    item {
                        LauncherIconItem(
                            iconEmoji = "📸",
                            label = "Pixel HDR",
                            bgColor = Color(0xFF3F51B5),
                            borderColor = Color(0xFF7986CB),
                            onClick = { onLaunchApp("camera") }
                        )
                    }
                }
            }

            // Samsung One UI Bottom Accent Gesture Pill indicator bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .width(140.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }

        // ===================================
        // OVERLAYS / INTERACTIVE DRAWERS
        // ===================================

        // Phone Dialer view Dialog
        if (showPhoneDialer) {
            Dialog(onDismissRequest = { showPhoneDialer = false }) {
                SimulatedPhoneDialer(onDismiss = { showPhoneDialer = false })
            }
        }

        // Message board Composable
        if (showChatMessages) {
            Dialog(onDismissRequest = { showChatMessages = false }) {
                SimulatedChatMessages(onDismiss = { showChatMessages = false })
            }
        }

        // Browser drawer Composable
        if (showBrowserSearch) {
            Dialog(onDismissRequest = { showBrowserSearch = false }) {
                SimulatedBrowserSearch(onDismiss = { showBrowserSearch = false })
            }
        }

        // Settings Information Dialog
        if (showSettingsInfo) {
            Dialog(onDismissRequest = { showSettingsInfo = false }) {
                OneUISystemStatsDialog(
                    installedAppsCount = installedApps.size,
                    onDismiss = { showSettingsInfo = false }
                )
            }
        }

        // Main flappy bird game launcher direct from home screen
        if (showFlappyGameDirect) {
            Box(modifier = Modifier.fillMaxSize()) {
                FlappyRetroGame(onDismiss = { showFlappyGameDirect = false })
            }
        }
    }
}

@Composable
fun LauncherIconItem(
    iconEmoji: String,
    label: String,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    scale.animateTo(0.9f, animationSpec = spring())
                    scale.animateTo(1.0f, animationSpec = spring())
                    onClick()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                .clip(RoundedCornerShape(18.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 30.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// 📞 Simulated Dialer Overlay
@Composable
fun SimulatedPhoneDialer(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var dialNumber by remember { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Samsung Phone Dialer", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Dismiss", color = Color(0xFF4CAF50), fontSize = 11.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (dialNumber.isEmpty()) "Enter number" else dialNumber,
                color = if (dialNumber.isEmpty()) Color.DarkGray else Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Dialer Grid
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#")
            )
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    row.forEach { char ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { dialNumber += char }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(char, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Clear button
                IconButton(
                    onClick = { if (dialNumber.isNotEmpty()) dialNumber = dialNumber.dropLast(1) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }

                // Call button
                IconButton(
                    onClick = {
                        android.widget.Toast.makeText(context, "Calling $dialNumber...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Phone, "Call", tint = Color.White)
                }
            }
        }
    }
}

// 💬 Simulated Messages Chat Card
@Composable
fun SimulatedChatMessages(onDismiss: () -> Unit) {
    val messages = remember {
        mutableStateListOf(
            "Dad: Remember to get coffee!",
            "Play Store: Welcome to Google Play Store! Try our HDR mod camera app or games simulator now.",
            "Samsung Care: Your Galaxy S26 Ultra is fully optimized.",
            "Aesthetic Labs: Use the 120MP upscale button inside the camera panel for pixel-perfect clarity."
        )
    }
    var msgInput by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Samsung Messages (One UI 14)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Back", color = Color(0xFF1E88E5), fontSize = 12.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages.size) { index ->
                    val line = messages[index]
                    val parts = line.split(":", limit = 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(parts[0], color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (parts.size > 1) parts[1].trim() else "", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = msgInput,
                    onValueChange = { msgInput = it },
                    placeholder = { Text("Enter text message...", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (msgInput.isNotEmpty()) {
                            messages.add("You: $msgInput")
                            msgInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E88E5))
                ) {
                    Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 🌐 Simulated Browser
@Composable
fun SimulatedBrowserSearch(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("Ready to surf. Type your search and tap Go.") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Samsung Internet 24.0", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Exit", color = Color(0xFF0288D1), fontSize = 12.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Enter keyword or URL...", color = Color.Gray, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0288D1),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    Text(
                        "GO",
                        color = Color(0xFF0288D1),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                if (query.isNotEmpty()) {
                                    resultText = "🔍 Searching for \"$query\" ... \n\nNo internet connection. \nBut we can confirm that our launcher is pixel-perfect!"
                                    query = ""
                                }
                            }
                            .padding(8.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(14.dp)
            ) {
                Text(resultText, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

// ⚙️ Simulated Settings/Stats Monitor popup Dialog
@Composable
fun OneUISystemStatsDialog(
    installedAppsCount: Int,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Device Care", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Dismiss", tint = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Samsung Galaxy S26 Ultra", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("One UI Version: 14.0.01 • Android 16", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Spec Info Rows
            SpecInfoRow(label = "Processor", value = "Snapdragon 8 Gen 5 Extreme AI")
            SpecInfoRow(label = "RAM Capacity", value = "16 GB LPDDR6")
            SpecInfoRow(label = "Plugins installed", value = "$installedAppsCount Ext Installed")
            SpecInfoRow(label = "Storage", value = "512 GB (412 GB Available)")
            SpecInfoRow(label = "FPS Mode", value = "Adaptive (V-Sync 60-120Hz)")
            SpecInfoRow(label = "Camera State", value = "120MP Quad-HDR sensor module")

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OPTIMIZE SYSTEM NOW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SpecInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
