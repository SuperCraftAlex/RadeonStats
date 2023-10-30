enum class StatTypes(
    val sensors: String? = null,
    val radeontop: String? = null,
    val lspci: String? = null
) {
    NONE,

    BUS(radeontop = "bus"),

    DEVICE(lspci = "Device"),
    SDEVICE(lspci = "SDevice"),
    CLASS(lspci = "Class"),
    VENDOR(lspci = "Vendor"),
    SVENDOR(lspci = "SVendor"),
    REVISION(lspci = "Rev"),
    ProgIf(lspci = "ProgIf"),
    DRIVER(lspci = "Driver"),
    MODULE(lspci = "Module"),
    IOMMUGROUP(lspci = "IOMMUGroup"),

    VDDGFX(sensors = "vddgfx"),
    FAN(sensors = "fan1"),
    EDGE(sensors = "edge"),
    JUNCTION(sensors = "junction"),
    MEM(sensors = "mem"),
    PPT(sensors = "PPT"),

    GPU(radeontop = "gpu"), // graphics pipe
    EVENT_ENGINE(radeontop = "ee"),
    VERTEX_GROUP_TESSELATOR(radeontop = "vgt"),
    TEXTURE_ADDRESSER(radeontop = "ta"),
    SHADER_EXPORT(radeontop = "sx"),
    SEQUENCER_INST_CACHE(radeontop = "sh"),
    SHADER_INTERPOLATOR(radeontop = "spi"),
    SCAN_CONVERTER(radeontop = "sc"),
    PRIMITIVE_ASSEMBLY(radeontop = "pa"),
    DEPTH_BLOCK(radeontop = "db"),
    COLOR_BLOCK(radeontop = "cb"),
    VRAM(radeontop = "vram"),
    GTT(radeontop = "gtt"),
    MEM_CLK(radeontop = "mclk"),
    SHADER_CLK(radeontop = "sclk"),
    ;

    override fun toString(): String =
        this.name.lowercase()
}

abstract class Data

class StringData(val value: String): Data() {
    override fun toString(): String =
        value
}

class NumberData(val value: Number): Data() {
    override fun toString(): String =
        value.toString()
}

class PercentData(val value: Number): Data() {
    override fun toString(): String =
        "$value%"
}

enum class Unit(
    val prefix: String
) {
    TERA("T"),
    GIGA("G"),
    MEGA("M"),
    KILO("K"),
    NONE(""),

    ;

    override fun toString(): String =
        prefix

    companion object {
        fun from(prefix: String): Unit =
            entries.find { it.prefix == prefix.uppercase() }
                ?: throw IllegalArgumentException("Unknown unit $prefix")
    }
}

class ClockData(
    val percent: Number,
    val speed: Number,
    val unit: Unit,
): Data() {

    val max: Number
        get() = speed.toDouble() / percent.toDouble() * 100.0

    override fun toString(): String =
        "$speed ${unit}hz ($percent%)"
}

class MemoryData(
    val percent: Number,
    val used: Number,
    val unit: Unit
): Data() {

    val max: Number
        get() = used.toDouble() / percent.toDouble() * 100.0

    override fun toString(): String =
        "$used ${unit}B ($percent%)"
}

class VoltageData(
    val value: Number
): Data() {
    override fun toString(): String =
        "$value V"
}

class TemperatureData(
    val input: Number,
    val crit: Number,
    val emergency: Number,
): Data() {
    override fun toString(): String =
        "$input°C | crit: $crit°C | emerg: $emergency°C"
}

class FanData(
    val input: Number,
    val min: Number,
    val max: Number,
): Data() {
    override fun toString(): String =
        "$input | min: $min | max: $max"
}

class PowerData(
    val average: Number,
    val cap: Number
): Data() {
    override fun toString(): String =
        "$average W | $cap W"
}

class ToolNotInstalledException(name: String):
    Exception("Tool $name is not installed")

fun checkInstalled(name: String) {
    val process = Runtime.getRuntime().exec("which $name")
    process.waitFor()
    if (process.exitValue() != 0) {
        throw ToolNotInstalledException(name)
    }
}

private fun Int.asBusId(): String =
    this.toString(16).padStart(2, '0')

typealias GPUData = Map<StatTypes, Collection<Data>>

typealias MutableGPUData = MutableMap<StatTypes, MutableList<Data>>

fun createGPUData(): MutableGPUData =
    mutableMapOf()

operator fun MutableGPUData.plusAssign(other: GPUData) {
    other.forEach { (key, value) ->
        this[key] = ((this[key] ?: mutableListOf()) + value).toMutableList()
    }
}

operator fun GPUData.plus(other: GPUData): GPUData =
    (this.asSequence() + other.asSequence())
        .groupBy({ it.key }, { it.value })
        .mapValues { it.value.flatten() }
        .toMap()

fun sensorsInfo(name: String, bus: Int): GPUData {
    checkInstalled("sensors")
    val cmd = "sensors -A -u $name-pci-${bus.asBusId()}00"
    val process = Runtime.getRuntime().exec(cmd)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw Exception("sensors exited with code ${process.exitValue()}")
    }
    val result = process.inputStream.bufferedReader().readLines().drop(1)
    var key: StatTypes = StatTypes.NONE
    val data = mutableMapOf<StatTypes, MutableList<String>>()
    for (line in result) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            continue
        }
        if (line.startsWith("  ")) {
            data[key]!! += trimmed
        } else {
            val keys = trimmed.substringBefore(':')
            key = StatTypes.entries.find { it.sensors == keys }
                ?: StatTypes.NONE
            data[key] = mutableListOf()
        }
    }
    val out = mutableMapOf<StatTypes, MutableList<Data>>()
    data.forEach { entry ->
        if (entry.value.isEmpty())
            return@forEach

        val first = entry.value.first()
        val prefix = first.substring(0, first.find { it.isDigit() }?.let { first.indexOf(it) } ?: first.length)

        val value = when (prefix) {
            "in" -> VoltageData(first.split(':')[1].toDouble())
            "temp" -> {
                val values = entry.value.map { it.split(':')[1].toDouble() }
                // ignore values[2] (crit_hyst)
                TemperatureData(
                    values[0],
                    values.getOrNull(1)
                        ?: Int.MAX_VALUE,
                    values.getOrNull(3)
                        ?: Int.MAX_VALUE
                )
            }
            "fan" -> {
                val values = entry.value.map { it.split(':')[1].toDouble() }
                FanData(
                    values[0],
                    values[1],
                    values[2]
                )
            }
            "power" -> {
                val values = entry.value.map { it.split(':')[1].toDouble() }
                PowerData(
                    values[0],
                    values.getOrNull(1)
                        ?: Int.MAX_VALUE
                )
            }
            else -> StringData(entry.value.joinToString(", "))
        }

        out[entry.key] = mutableListOf(value)
    }
    return out
}

fun pciInfo(bus: Int): GPUData {
    checkInstalled("lspci")
    if (bus > 0xff) {
        throw IllegalArgumentException("Bus number must be in range 0-255")
    }
    val cmd = "lspci -m -v -k -s ${bus.asBusId()}:00.0"
    val process = Runtime.getRuntime().exec(cmd)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw Exception("lspci exited with code ${process.exitValue()}")
    }
    val result = process.inputStream.bufferedReader().readText().trim().lines()
    val data = result.associate {
        val pos = it.indexOf(":")
        val keys = it.substring(0, pos).trim()
        val value = it.substring(pos+1).trim()
        val key = StatTypes.entries.find { s -> s.lspci == keys }
            ?: StatTypes.NONE
        key to listOf(value)
    }
    val x = data.entries.associate { entry ->
        val key = entry.key
        val value = entry.value.first()

        val new = when(key) {
            StatTypes.REVISION,
            StatTypes.ProgIf,
            StatTypes.IOMMUGROUP -> {
                NumberData(value.toInt(16))
            }
            else -> StringData(value)
        }

        key to listOf(new)
    }
    return x
}

fun String.afterNumber(): String {
    val pos = this.indexOfLast { it.isDigit() }
    return this.substring(pos + 1)
}

fun String.beforeLetter(): String {
    val pos = this.indexOfFirst { it.isLetter() }
    return this.substring(0, pos)
}

fun radeontopInfo(bus: Int): GPUData {
    checkInstalled("radeontop")
    val cmd = "radeontop -d - -b $bus -l 1"
    val process = Runtime.getRuntime().exec(cmd)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw Exception("radeontop exited with code ${process.exitValue()}")
    }
    val result = process.inputStream.bufferedReader().readLines()
    val dataText = result.last().substringAfter(": ")
    val data = dataText.split(", ")
    val dx = data.map { it.split(' ') }
    val dx2 = dx.map { it[0] to it.subList(1, it.size) }
    val dx3 = dx2.map {
        val key = it.first
        val vOld = it.second
        val stat = StatTypes.entries.find { s -> s.radeontop == key }
            ?: StatTypes.NONE

        val vNew = when (stat) {
            StatTypes.VRAM,
            StatTypes.GTT
            -> MemoryData(
                vOld[0].substringBefore('%').toDouble(),
                vOld[1].beforeLetter().toDouble(),
                Unit.from(vOld[1].afterNumber().first().toString())
            )

            StatTypes.MEM_CLK,
            StatTypes.SHADER_CLK
            -> ClockData(
                vOld[0].substringBefore('%').toDouble(),
                vOld[1].beforeLetter().toDouble(),
                Unit.from(vOld[1].afterNumber().first().toString())
            )

            else -> PercentData(
                vOld[0].substringBefore('%').toDouble()
            )
        }

        stat to listOf(vNew)
    }
    return dx3.toMap()
}

fun GPUData.printData() {
    forEach {
        println("${it.key} = ${it.value.joinToString(", ")}")
    }
}