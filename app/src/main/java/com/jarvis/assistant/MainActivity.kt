package com.jarvis.assistant

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jarvis.assistant.ui.JarvisScreen
import com.jarvis.assistant.ui.JarvisViewModel
import com.jarvis.assistant.ui.theme.JarvisDeepBlack
import com.jarvis.assistant.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    // Permission launcher for runtime permission requests
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.refreshPermissions()
        if (isGranted) {
            Toast.makeText(this, "Permission granted. Neural subsystems online.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied. Some capabilities may be restricted.", Toast.LENGTH_LONG).show()
        }
    }

    // Document Picker launcher for Storage Access Framework
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onUserSubmittedText("I selected file: ${uri.lastPathSegment ?: uri.toString()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JarvisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisDeepBlack
                ) {
                    JarvisScreen(
                        viewModel = viewModel,
                        onRequestPermission = { permission ->
                            requestPermissionLauncher.launch(permission)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}
