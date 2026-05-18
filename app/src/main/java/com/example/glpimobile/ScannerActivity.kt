package com.example.glpimobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    private val cameraPermissionCode = 100

    // 🔥 OTIMIZAÇÃO 1: Iniciamos os motores do Google apenas UMA vez 🔥
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Controlador de tempo para o texto
    private var lastOcrTime = 0L

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processarImagemDaGaleria(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<ImageButton>(R.id.btn_close_scanner).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.btn_gallery).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            // 🔥 OTIMIZAÇÃO 2: Baixamos ligeiramente a resolução da análise para poupar memória,
            // a câmara continua em alta definição, mas o "cérebro" processa uma imagem mais leve.
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { proxy -> processImageProxy(proxy) }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("SCANNER_DEBUG", "Erro ao iniciar câmara", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            analisarImagemCamera(image) { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun processarImagemDaGaleria(uri: Uri) {
        try {
            isProcessing = true
            val image = InputImage.fromFilePath(this, uri)
            analisarImagemGaleria(image)
        } catch (e: Exception) {
            isProcessing = false
            Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔥 FLUXO 1: ANÁLISE DA CÂMARA (Super Rápida e Otimizada) 🔥
    private fun analisarImagemCamera(image: InputImage, onComplete: () -> Unit) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val valor = barcodes[0].rawValue ?: ""
                    finalizarComSucesso(valor)
                    onComplete()
                } else {
                    // 🔥 OTIMIZAÇÃO 3: O limite de velocidade! Só tenta ler texto a cada 500 milissegundos.
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastOcrTime > 500) {
                        lastOcrTime = currentTime
                        textRecognizer.process(image).addOnSuccessListener { visionText ->
                            val resultado = extrairCodigoUtil(visionText)
                            if (resultado != null) finalizarComSucesso(resultado)
                        }.addOnCompleteListener { onComplete() }
                    } else {
                        // Salta o processamento de texto nesta frame para não fritar o telemóvel
                        onComplete()
                    }
                }
            }
            .addOnFailureListener { onComplete() }
    }

    // 🔥 FLUXO 2: ANÁLISE DA GALERIA (Não precisa de limite de velocidade) 🔥
    private fun analisarImagemGaleria(image: InputImage) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val valor = barcodes[0].rawValue ?: ""
                    finalizarComSucesso(valor)
                } else {
                    textRecognizer.process(image).addOnSuccessListener { visionText ->
                        val resultado = extrairCodigoUtil(visionText)
                        if (resultado != null) {
                            finalizarComSucesso(resultado)
                        } else {
                            Toast.makeText(this, "Nenhum código reconhecido nesta foto.", Toast.LENGTH_LONG).show()
                            isProcessing = false
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Falha ao ler o texto da imagem.", Toast.LENGTH_SHORT).show()
                        isProcessing = false
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao processar código de barras.", Toast.LENGTH_SHORT).show()
                isProcessing = false
            }
    }

    private fun extrairCodigoUtil(visionText: Text): String? {
        val linhas = visionText.textBlocks.flatMap { it.lines }

        for (linha in linhas) {
            val texto = linha.text.trim()

            if (texto.contains("SN:", ignoreCase = true) ||
                texto.contains("S/N:", ignoreCase = true) ||
                texto.contains("Serial:", ignoreCase = true)) {

                val snEncontrado = texto.substringAfter(":").trim()
                if (snEncontrado.isNotEmpty()) return snEncontrado
            }

            if (texto.contains("equipamento:", ignoreCase = true)) {
                val nomeEncontrado = texto.substringAfter(":").trim()
                if (nomeEncontrado.isNotEmpty()) return nomeEncontrado
            }

            if (texto.matches(Regex("^[A-Z0-9]{6,20}$"))) {
                return texto
            }
        }
        return null
    }

    private fun finalizarComSucesso(codigo: String) {
        if (codigo.isEmpty() || codigo.length < 3) return
        if (codigo.contains("Património", ignoreCase = true)) return

        isProcessing = true
        vibrarTelemovel()

        val resultIntent = Intent()
        resultIntent.putExtra("SCAN_RESULT", codigo)
        setResult(RESULT_OK, resultIntent)

        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 100)
    }

    private fun vibrarTelemovel() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.e("SCANNER_DEBUG", "Erro ao vibrar", e)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        // Limpamos a memória quando fechamos a câmara
        barcodeScanner.close()
        textRecognizer.close()
        cameraExecutor.shutdown()
    }
}