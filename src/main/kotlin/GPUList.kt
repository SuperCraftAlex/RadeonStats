import com.google.gson.Gson

fun LshwData.isCurrentGPU(): Boolean =
    capabilities.fb != "null"

fun LshwData.getBusID(): Int =
    businfo.split("@").last().split(":")[1].toInt(16)

data class LshwData(
    val id: String,
    val `class`: String,
    val claimed: Boolean,
    val handle: String,
    val description: String,
    val product: String,
    val vendor: String,
    val physid: String,
    val businfo: String,
    val logicalname: String,
    val version: String,
    val width: Int,
    val clock: Int,
    val configuration: LshwConfiguration,
    val capabilities: LshwCapabilities
)

data class LshwConfiguration(
    val depth: String,
    val driver: String,
    val latency: String,
    val resolution: String
)

data class LshwCapabilities(
    val vga_controller: Boolean,
    val bus_master: String,
    val cap_list: String,
    val rom: String,
    val fb: String
)

fun lshwFetch(hwclass: String = "display"): Array<LshwData> {
    try {
        checkInstalled("lshw")
        val cmd = "lshw -C $hwclass -json"
        val proc = Runtime.getRuntime().exec(cmd)
        proc.waitFor()
        val json = proc.inputStream.bufferedReader().readText()
        val data = Gson().fromJson(json, Array<LshwData>::class.java)
        return data
    } catch (e: Exception) {
        println("LSHW not found! To see your device, please install LSHW.")
        return emptyArray()
    }
}