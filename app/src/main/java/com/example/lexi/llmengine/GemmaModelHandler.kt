package com.example.lexi.llmengine

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.ByteArrayOutputStream
import java.io.File

class GemmaModelHandler(
    private val context: Context,
    private val modelFileName: String = "gemma-4-E2B-it.litertlm"
) {
    private var engine: Engine? = null

    private val modelFile: File
        get() = File(context.getExternalFilesDir("models"), modelFileName)

    private val cacheDir: String
        get() = context.cacheDir.absolutePath

    fun initialize() {

        val engineConfig = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            visionBackend = Backend.CPU(),
            cacheDir = cacheDir
        )
        engine = Engine(engineConfig).also { it.initialize() }
    }

    fun analyze(prompt: String, bitmap: Bitmap): String {
        val eng = engine ?: error("Engine not initialized. Call initialize() first.")
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArrayImg = stream.toByteArray()

        return eng.createConversation().use { conversation ->
            val response = conversation.sendMessage(
                Contents.of(
                    Content.ImageBytes(byteArrayImg),
                    Content.Text(prompt)
                )
            )
            response?.toString() ?: "No response from model"
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }
}