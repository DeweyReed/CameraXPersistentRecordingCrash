package com.github.deweyreed.camerax.prc

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.deweyreed.camerax.prc.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var facingFront = false
    private lateinit var preview: Preview
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                finish()
                return@registerForActivityResult
            }
            startCamera()
        }.launch(Manifest.permission.CAMERA)

        binding.btnFlip.setOnClickListener {
            facingFront = !facingFront
            startCamera()
        }

        binding.btnRecord.setOnClickListener {
            if (recording == null) {
                @Suppress("MissingPermission")
                recording = videoCapture.output
                    .prepareRecording(
                        this,
                        FileOutputOptions.Builder(
                            File(filesDir, "video.mp4")
                        ).build()
                    )
                    .asPersistentRecording()
                    .start(
                        ContextCompat.getMainExecutor(this)
                    ) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                binding.btnRecord.text = "Stop"
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (event.hasError()) {
                                    Toast.makeText(this, event.error.toString(), Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    binding.btnRecord.text = "Start"
                                }
                            }
                        }
                    }
            } else {
                recording?.run {
                    stop()
                    recording = null
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener(
            {
                recording?.pause()

                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(
                        if (facingFront) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    )
                    .build()

                if (!::preview.isInitialized) {
                    preview = Preview.Builder().build()
                    preview.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                if (!::videoCapture.isInitialized) {
                    videoCapture = VideoCapture.Builder(Recorder.Builder().build())
                        .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                        .build()
                }

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    videoCapture,
                )

                recording?.resume()
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}
