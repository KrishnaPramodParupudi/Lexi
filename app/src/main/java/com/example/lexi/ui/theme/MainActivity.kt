package com.example.lexi.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lexi.llmengine.GemmaModelHandler
import com.example.lexi.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import com.example.lexi.audio.TtsHelper


class MainActivity : ComponentActivity() {

    private lateinit var gemmaHandler: GemmaModelHandler

    private lateinit var ttsHelper: TtsHelper

    private val cameraImageFile: File by lazy {
        File(cacheDir, "camera_capture.jpg")
    }

    private val cameraImageUri: Uri by lazy {
        FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            cameraImageFile
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gemmaHandler = GemmaModelHandler(
            context = this,
            modelFileName = "gemma-4-E2B-it.litertlm"
        )

        ttsHelper = TtsHelper(this)

        setContent {
            val viewModel: MainViewModel = viewModel()

            ttsHelper.onStatus = { msg ->
                viewModel.llmResult = msg
            }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) {
                            viewModel.llmResult = "Loading model... (first launch may take 1-2 min)"
                        }
                        gemmaHandler.initialize()
                        withContext(Dispatchers.Main) {
                            viewModel.isModelReady = true
                            viewModel.llmResult = "Model ready. Capture an image to begin."
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            viewModel.llmResult = "Model failed to load: ${e.message}"
                        }
                    }
                }
            }

            // Camera Launcher
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success ->
                if (success) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bitmap = BitmapFactory.decodeFile(cameraImageFile.absolutePath, opts)
                    viewModel.selectedImageBitmap = bitmap
                    viewModel.llmResult = "Image captured. Tap 'Analyze' to begin."
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    cameraLauncher.launch(cameraImageUri)
                } else {
                    viewModel.llmResult = "Camera permission is required to analyze essays."
                }
            }

            // Gallery Launcher (modern photo picker, no permission needed)
            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val bitmap = BitmapFactory.decodeStream(stream, null, opts)
                            if (bitmap != null) {
                                viewModel.selectedImageBitmap = bitmap
                                viewModel.llmResult = "Image loaded. Tap 'Analyze' to begin."
                            } else {
                                viewModel.llmResult = "Could not read the selected image."
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        viewModel.llmResult = "Failed to load image: ${e.message}"
                    }
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        handler = gemmaHandler,
                        ttsHelper = ttsHelper,
                        onCameraClick = {
                            val permissionCheck = checkSelfPermission(android.Manifest.permission.CAMERA)
                            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(cameraImageUri)
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        onGalleryClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::gemmaHandler.isInitialized) gemmaHandler.close()

        if (::ttsHelper.isInitialized) ttsHelper.shutdown()
    }

    @Composable
    fun MainScreen(
        handler: GemmaModelHandler,
        viewModel: MainViewModel,
        ttsHelper: TtsHelper,
        onCameraClick: () -> Unit,
        onGalleryClick: () -> Unit
    ) {

        var isSpeaking by remember { mutableStateOf(false) }
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        val maxPanelHeight = screenHeightDp * 0.80f
        val minPanelHeight = 120.dp

        var panelHeight by remember { mutableStateOf(minPanelHeight) }
        val density = LocalDensity.current

        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Lexi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (viewModel.isTeacherMode) viewModel.toggleMode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!viewModel.isTeacherMode)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!viewModel.isTeacherMode)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Text("Student") }

                    Button(
                        onClick = { if (!viewModel.isTeacherMode) viewModel.toggleMode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isTeacherMode)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewModel.isTeacherMode)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Text("Teacher") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // IMAGE PREVIEW
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (viewModel.selectedImageBitmap != null) {
                            AsyncImage(
                                model = viewModel.selectedImageBitmap,
                                contentDescription = "Captured Essay",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("No Image Captured", color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CAMERA + GALLERY + ANALYZE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        enabled = viewModel.isModelReady && !viewModel.isProcessing
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (!viewModel.isModelReady) "Load..." else "Camera", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        enabled = viewModel.isModelReady && !viewModel.isProcessing
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.selectedImageBitmap?.let { capturedBitmap ->
                                viewModel.processImageInput(capturedBitmap, handler)
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        enabled = viewModel.selectedImageBitmap != null
                                && !viewModel.isProcessing
                                && viewModel.isModelReady
                    ) {
                        Text(if (viewModel.isProcessing) "Wait..." else "Analyze", fontSize = 13.sp)
                    }
                }
            }

            // ---- RESULT PANEL ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val dragDp = with(density) { dragAmount.toDp() }
                                    panelHeight = (panelHeight - dragDp)
                                        .coerceIn(minPanelHeight, maxPanelHeight)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.Gray, RoundedCornerShape(2.dp))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AI Suggestions",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    ttsHelper.stop()
                                    isSpeaking = false
                                } else {
                                    ttsHelper.speak(viewModel.llmResult)
                                    isSpeaking = true
                                }
                            },
                            enabled = !viewModel.isProcessing && viewModel.llmResult.isNotBlank()
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop reading" else "Read aloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (viewModel.isProcessing || !viewModel.isModelReady) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(viewModel.llmResult, fontSize = 14.sp)
                            }
                        } else {
                            Text(
                                viewModel.llmResult,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }

}