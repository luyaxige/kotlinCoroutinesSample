import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class Print(clazz: KClass<*>) {

    private val timeTag = SimpleDateFormat("hh:MM:ss.SSS")
    private val classTag: String = clazz.simpleName ?: "Unknown Class"

    /**
     * t = time
     * */
    fun t(printText: () -> String) {
        println("${timeTag.format(Date())} ${printText()}")
    }

    /**
     * c = class
     * divider line
     * */
    fun c(printText: () -> String) {
        println("${printText()} $classTag ${printText()}")
    }

}

fun <T : Any> T.printer(): Print {
    return Print(this::class)
}