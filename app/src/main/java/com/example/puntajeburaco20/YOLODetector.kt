package com.example.puntajeburaco20

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class YOLODetector(private val context: Context) {
    private val modelPath = "best_float32.tflite"
    private val labelPath = "labels.txt"
    private var interpreter: Interpreter? = null
    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0
    private var labels = mutableListOf<String>()
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build() // preprocess input

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.7F
    }
    init{
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        options.numThreads = 4
        interpreter = Interpreter(model, options)
        val inputShape = interpreter!!.getInputTensor(0).shape()
        val outputShape = interpreter!!.getOutputTensor(0).shape()

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]
        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun detectObjects(bitmap: Bitmap): List<BoundingBox>? {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer
        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray)
        //Log.d("YOLODetector", "Output shape: $bestBoxes")
        bestBoxes?.forEach { box ->
            Log.d("YOLODetector", "Clase: ${box.clsName}, Confianza: ${box.cnf}")
        }
        return bestBoxes

    }
    data class BoundingBox(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
        val cnf: Float,
        val cls: Int,
        val clsName: String
    )
    private fun bestBox(array: FloatArray) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }



        /*
        private var interpreter: Interpreter

        init {
            interpreter = Interpreter(loadModelFile(context, "best_float32.tflite"))
        }

        private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
            val fileDescriptor = context.assets.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        }

        /**
         * Esta función es un ejemplo. Debes ajustar la preparación de entrada y salida
         * según el formato que tu modelo exportado TFLite utiliza.
         */
        fun detectObjects(bitmap: Bitmap): MutableList<String> {

            // Redimensiona la imagen al tamaño esperado, por ejemplo, 640x640
            val inputByteBuffer = prepareImageForModel(bitmap)

            // Define la salida: ajusta el tamaño según lo que espere tu modelo
            // Por ejemplo, si el modelo produce una salida de forma [1, 25200]:
            val output = Array(1) { Array(15) { FloatArray(8400) } }

            interpreter.run(inputByteBuffer, output)

            val detectedObjects = mutableListOf<String>()
            val labels = loadLabels(context, "labels.txt")

            Log.d("YOLODetector", "Output shape: ${output[0].size}")
            // Procesar las detecciones (asumiendo que los 4 primeros valores son las coordenadas y el último es la probabilidad)
            for (i in output[0].indices) {
                val detection = output[0][i]
                Log.d("YOLODetector", "Output aaa: $detection")

                val x = detection[0]  // Centro X
                val y = detection[1]  // Centro Y
                val w = detection[2]  // Ancho
                val h = detection[3]  // Alto
                val confidence = detection[4] // Confianza del modelo
                //Log.d("YOLODetector", "Confidence: $confidence")

                if (confidence > 0.2) { // Umbral de confianza
                    Log.d("YOLODetector", "Entró")
                    val scores = detection.sliceArray(5 until detection.size).toList()  // Convierte FloatArray a List<Float>
                    val classIndex = scores.indexOf(scores.maxOrNull())  // Busca el índice del valor máximo
                    val label = labels[classIndex] // Nombre de la clase detectada

                    detectedObjects.add("$label (confianza: ${"%.2f".format(confidence)})")
                }
            }

            return detectedObjects
        }
        fun prepareImageForModel(originalBitmap: Bitmap): ByteBuffer {
            // 1. Resize the image to 640x640
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 640, 640, true)

            // 2. Create a ByteBuffer with the correct size
            val inputShape = intArrayOf(1, 640, 640, 3) // [batch, height, width, channels]
            val bufferSize = inputShape.reduce { acc, i -> acc * i } * 4 // 4 bytes for float32
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
            byteBuffer.order(ByteOrder.nativeOrder())

            // 3. Normalize pixel values and copy to ByteBuffer
            val pixels = IntArray(640 * 640)
            resizedBitmap.getPixels(pixels, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

            var pixelIndex = 0
            for (i in 0 until 640) {
                for (j in 0 until 640) {
                    val pixel = pixels[pixelIndex++]
                    // Normalize pixel values to 0.0 to 1.0
                    val r = ((pixel shr 16 and 0xFF) / 255.0f)
                    val g = ((pixel shr 8 and 0xFF) / 255.0f)
                    val b = ((pixel and 0xFF) / 255.0f)

                    byteBuffer.putFloat(r)
                    byteBuffer.putFloat(g)
                    byteBuffer.putFloat(b)
                }
            }

            byteBuffer.rewind() // Reset the buffer's position to the beginning

            // 4. Return both the resized Bitmap and the ByteBuffer
            return byteBuffer
        }
        private fun loadLabels(context: Context, filename: String): List<String> {
            return context.assets.open(filename).bufferedReader().readLines()
        }
        */


}
