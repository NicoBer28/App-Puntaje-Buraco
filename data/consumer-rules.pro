# Reglas que R8 aplica a la app cuando minifica (las hereda de este módulo).

# LiteRT (ex TensorFlow Lite) accede por JNI a sus clases Java: no se pueden renombrar ni quitar.
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn org.tensorflow.lite.**
-dontwarn com.google.ai.edge.litert.**
