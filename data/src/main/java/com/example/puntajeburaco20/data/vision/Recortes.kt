package com.example.puntajeburaco20.data.vision

import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Recorte cuadrado de una imagen, en píxeles. */
internal data class Recorte(val x: Int, val y: Int, val lado: Int)

/**
 * Detección por recortes (al estilo SAHI). El modelo trabaja a 640x640: si se le pasa la foto
 * entera, una fila de 50 fichas queda con fichas de 10 o 12 píxeles de ancho y casi no se
 * reconocen. Recorriendo la foto por partes cada ficha llega al modelo varias veces más grande.
 *
 * Todo es Kotlin puro (sin Android) para poder probarlo en la JVM.
 */
internal object Recortes {

    /**
     * Recortes cuadrados de [lado] píxeles que cubren toda la imagen, repartidos de forma pareja
     * y superpuestos en al menos la fracción [solape], para que toda ficha quede entera en alguno.
     */
    fun cubrir(ancho: Int, alto: Int, lado: Int, solape: Float): List<Recorte> {
        require(ancho > 0 && alto > 0 && lado > 0) { "Medidas inválidas" }
        val ladoReal = minOf(lado, ancho, alto)
        val columnas = posiciones(ancho, ladoReal, solape)
        return posiciones(alto, ladoReal, solape).flatMap { y -> columnas.map { x -> Recorte(x, y, ladoReal) } }
    }

    private fun posiciones(largo: Int, lado: Int, solape: Float): List<Int> {
        if (largo <= lado) return listOf(0)
        val paso = lado * (1 - solape)
        val cantidad = ceil((largo - lado) / paso).toInt() + 1
        return List(cantidad) { i -> (i * (largo - lado) / (cantidad - 1f)).roundToInt() }
    }

    /**
     * Lado de recorte para que las fichas lleguen al modelo con unos [anchoObjetivo] píxeles de
     * ancho, que es la escala con la que mejor las reconoce: más chicas se confunden, y más
     * grandes el modelo empieza a ver cada número de un 11 como un 1 aparte.
     *
     * [anchosFichas] es el lado corto (en píxeles de la foto) de las fichas que se vieron en una
     * primera pasada con la foto entera. Si no se vio ninguna, son tan chicas que hay que usar el
     * recorte más chico permitido. El resultado queda entre [fraccionMinima] del lado menor (para
     * acotar la cantidad de recortes) y el lado menor entero.
     */
    fun ladoParaFichas(
        anchosFichas: List<Float>,
        ancho: Int,
        alto: Int,
        entradaModelo: Int,
        anchoObjetivo: Float,
        fraccionMinima: Float,
    ): Int {
        val menor = minOf(ancho, alto)
        val minimo = minOf(maxOf((menor * fraccionMinima).toInt(), entradaModelo), menor)
        if (anchosFichas.isEmpty()) return minimo
        val mediana = anchosFichas.sorted()[anchosFichas.size / 2]
        return (mediana * entradaModelo / anchoObjetivo).toInt().coerceIn(minimo, menor)
    }

    /** Pasa una caja relativa al [recorte] (0 a 1) a coordenadas relativas a la imagen entera. */
    fun aImagen(caja: CajaNormalizada, recorte: Recorte, ancho: Int, alto: Int): CajaNormalizada =
        CajaNormalizada(
            izquierda = (recorte.x + caja.izquierda * recorte.lado) / ancho,
            arriba = (recorte.y + caja.arriba * recorte.lado) / alto,
            derecha = (recorte.x + caja.derecha * recorte.lado) / ancho,
            abajo = (recorte.y + caja.abajo * recorte.lado) / alto,
        )

    /**
     * Si la caja (relativa al recorte) toca un borde del recorte que no es borde de la imagen, la
     * ficha quedó cortada: se descarta porque otro recorte superpuesto la ve entera.
     */
    fun cortadaPorElBorde(caja: CajaNormalizada, recorte: Recorte, ancho: Int, alto: Int, margen: Float): Boolean =
        (recorte.x > 0 && caja.izquierda < margen) ||
            (recorte.y > 0 && caja.arriba < margen) ||
            (recorte.x + recorte.lado < ancho && caja.derecha > 1 - margen) ||
            (recorte.y + recorte.lado < alto && caja.abajo > 1 - margen)

    /**
     * Junta detecciones que se pisan: de mayor a menor confianza, descarta las que se superponen
     * con una ya elegida (IoU ≥ [umbralIou]) o quedan casi enteras dentro de ella (fracción de su
     * área ≥ [umbralContencion], típico de una ficha vista cortada en un recorte y entera en otro).
     * No distingue clases: dos fichas no pueden ocupar el mismo lugar.
     */
    fun fusionar(
        fichas: List<FichaDetectada>,
        umbralIou: Float,
        umbralContencion: Float = SIN_CONTENCION,
    ): List<FichaDetectada> {
        val elegidas = mutableListOf<FichaDetectada>()
        for (ficha in fichas.sortedByDescending { it.confianza }) {
            val repetida = elegidas.any {
                interseccionSobreUnion(it.caja, ficha.caja) >= umbralIou ||
                    fraccionContenida(ficha.caja, dentroDe = it.caja) >= umbralContencion
            }
            if (!repetida) elegidas += ficha
        }
        return elegidas
    }

    fun interseccionSobreUnion(a: CajaNormalizada, b: CajaNormalizada): Float {
        val interseccion = interseccion(a, b)
        val union = a.area + b.area - interseccion
        return if (union <= 0f) 0f else interseccion / union
    }

    /** Qué fracción del área de [caja] queda dentro de [dentroDe]. */
    fun fraccionContenida(caja: CajaNormalizada, dentroDe: CajaNormalizada): Float =
        if (caja.area <= 0f) 0f else interseccion(caja, dentroDe) / caja.area

    private fun interseccion(a: CajaNormalizada, b: CajaNormalizada): Float {
        val ancho = minOf(a.derecha, b.derecha) - maxOf(a.izquierda, b.izquierda)
        val alto = minOf(a.abajo, b.abajo) - maxOf(a.arriba, b.arriba)
        return maxOf(0f, ancho) * maxOf(0f, alto)
    }

    private val CajaNormalizada.area: Float get() = maxOf(0f, derecha - izquierda) * maxOf(0f, abajo - arriba)

    /** Valor de [fusionar] que desactiva el descarte por contención. */
    const val SIN_CONTENCION = Float.MAX_VALUE
}
