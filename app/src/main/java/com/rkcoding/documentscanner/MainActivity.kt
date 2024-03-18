package com.rkcoding.documentscanner

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rkcoding.documentscanner.ui.theme.DocumentScannerTheme
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
        if (isGranted){
            downloadPdf()
        }else{
            Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()

        }
    }

    @SuppressLint("Recycle")

    override fun onCreate(savedInstanceState: Bundle?) {

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setPageLimit(8)
            .setGalleryImportAllowed(true)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)


        super.onCreate(savedInstanceState)
        setContent {
            DocumentScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

//                    val scope = rememberCoroutineScope()

                    var imageUri by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if(result.resultCode == RESULT_OK){
                                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                                imageUri = scanningResult?.pages?.map { uri ->  uri.imageUri } ?: emptyList()

                                // save pdf to fileDirectory
                                scanningResult?.pdf?.let { pdfResult ->
                                    val pdfUri = pdfResult.uri
                                    val pdfInputStream = contentResolver.openInputStream(pdfUri)
                                    val pdfFile = File(filesDir, "Scan.pdf")
                                    val pdfOutputStream = FileOutputStream(pdfFile)

                                    pdfInputStream?.use { input ->
                                        pdfOutputStream.use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                }

                            }
                        }
                    )


                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        imageUri.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "uri image",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it).build()
                                        )
                                    }
                                    .addOnFailureListener{
                                        Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            }
                            ) {
                                Text(text = "Scan Pdf")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            AnimatedVisibility(visible = imageUri.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(
                                            applicationContext,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED
                                        ){
                                            downloadPdf()
                                        }else{
                                            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }

                                    }
                                ) {
                                    Text(text = "Save pdf")
                                }
                            }



                        }


                    }

                }
            }
        }
    }

    private fun downloadPdf() {
        val sourceFile = File(filesDir, "scanned_document.pdf")
        val sourceUri = Uri.fromFile(sourceFile)

        val destinationUri = Uri.fromFile(File(getExternalFilesDir(null), "downloaded_scanned_document.pdf"))

        try {
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(applicationContext, "Scanned PDF downloaded successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Failed to download scanned PDF", Toast.LENGTH_SHORT).show()
        }
    }


}



