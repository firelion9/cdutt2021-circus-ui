import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.Reader
import java.io.Writer
import kotlin.coroutines.Continuation

fun main() {
    val config =
        try {
            File("config.txt").useLines { lines ->
                lines
                    .filter(String::isNotBlank)
                    .map { it.split("=") }
                    .map { val (l, r) = it; l to r }
                    .toMap()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            mapOf("player1" to "pause", "player2" to "pause")
        }

    var process1 =
        ProcessBuilder(config["player1"]).start()
    var process2 =
        ProcessBuilder(config["player2"]).start()

    val delay = config.getOrElse("delayBetweenStepsMillis") { "100" }.toLong()

    Window(size = IntSize(1150, 580), events = WindowEvents(onClose = {
        process1.destroy()
        process2.destroy()
    })) {
        var field by remember { mutableStateOf(GameField(config["houses"]!!)) }
        var gameLog by remember { mutableStateOf("") }
        var steps by remember { mutableStateOf(0) }
        var running by remember { mutableStateOf(config.getOrElse("startOnStartup") { "true" }.toBoolean()) }
        var doStep by remember { mutableStateOf(false) }
        var stoped by remember { mutableStateOf(false) }

        fun stop() {
            process1.destroy()
            process2.destroy()
            stoped = true
        }

        fun restart() {
            stop()

            steps = 0
            process1 = ProcessBuilder(config["player1"]).start()
            process2 = ProcessBuilder(config["player2"]).start()
            gameLog = ""

            field = GameField(config["houses"]!!)
            stoped = false
        }

        MaterialTheme {
            Row {
                Column(modifier = Modifier.wrapContentSize()) {

                    Row(modifier = Modifier.wrapContentSize()) {

                        Button(modifier = Modifier.wrapContentSize(), onClick = {
                            if (stoped) restart()
                            running = !running
                        }) {
                            Text(
                                when {
                                    running -> "suspend"
                                    steps > 0 -> "resume"
                                    else -> "start"
                                }, modifier = Modifier.wrapContentSize())
                        }

                        Button(modifier = Modifier.wrapContentSize(), onClick = {
                            if (!stoped) doStep = true
                        }) {
                            Text("do a step", modifier = Modifier.wrapContentSize())
                        }

                        Button(modifier = Modifier.wrapContentSize(), onClick = ::restart) {
                            Text("restart", modifier = Modifier.wrapContentSize())
                        }

                        if (!stoped) Button(modifier = Modifier.wrapContentSize(), onClick = ::stop) {
                            Text("stop", modifier = Modifier.wrapContentSize())
                        }

                        Spacer(Modifier.width(10.dp))

                        Text("steps: $steps", modifier = Modifier.wrapContentSize().align(Alignment.CenterVertically))

                    }

                    Spacer(Modifier.padding(15.dp))

                    repeat(9) { rowInv ->
                        row(rowInv, field)
                    }
                    Row(modifier = Modifier.height(50.dp).wrapContentWidth()) {
                        Box(modifier = Modifier.width(50.dp).height(50.dp))
                        repeat(12) {
                            Box(modifier = Modifier.width(50.dp).height(50.dp)) {
                                Text(
                                    (it + 'A'.toInt()).toChar().toString(),
                                    color = Color.Magenta,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(15.dp))

                Column(modifier = Modifier.wrapContentSize()) {
                    Box {
                        val state = rememberScrollState(0)
                        Column(
                            Modifier
                                .verticalScroll(state)
                                .fillMaxSize()
                        ) {
                            Text("Game log:\n$gameLog", fontSize = 20.sp)
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(state),
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        LaunchedEffect(field) {
            try {
                val o1 = process1.outputStream.bufferedWriter()
                val o2 = process2.outputStream.bufferedWriter()

                val i1 = process1.inputStream.bufferedReader()
                val i2 = process2.inputStream.bufferedReader()

                o1.appendLine(config["houses"])
                o1.appendLine("0")
                o1.flush()

                o2.appendLine(config["houses"])
                o2.appendLine("1")
                o2.flush()

                gameLog += "houses: ${config["houses"]}\n"

                while (steps < 300) {

                    while (!running && !doStep) delay(1)
                    doStep = false

                    val m1 = i1.readLine()
                    field.doMove(m1)
                    steps++
                    gameLog += "$steps (p1): $m1\n"

                    o2.appendLine(m1)
                    o2.flush()

                    delay(delay)

                    while (!running && !doStep) delay(1)
                    doStep = false

                    val m2 = i2.readLine()
                    field.doMove(m2)
                    steps++
                    gameLog += "$steps (p2): $m2\n"

                    o1.appendLine(m2)
                    o1.flush()

                    delay(delay)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stop()
            }
        }
    }
}

@Composable
fun row(rowInv: Int, field: GameField) {
    val size = 50
    Row(modifier = Modifier.wrapContentSize()) {
        Box(modifier = Modifier.width(size.dp).height(size.dp)) {
            Text((9 - rowInv).toString(), color = Color.Magenta, modifier = Modifier.align(Alignment.Center))
        }
        repeat(12) { col ->
            Box(
                modifier = Modifier.width(size.dp).height(size.dp)
                    .background(if ((col + rowInv) % 2 == 0) Color(0.5f, 0.5f, 0.5f) else Color(0.75f, 0.75f, 0.75f))
            ) {

                val e = field.field[8 - rowInv][col].entity
                val color =
                    if (field.field[8 - rowInv][col].hasHouse) Color.Black
                    else when (e?.player) {
                        0 -> Color.Blue
                        1 -> Color.Red
                        else -> Color.Black
                    }

                when (e?.type) {
                    GameField.Entity.Type.CLOWN -> Text(
                        "C",
                        Modifier.wrapContentSize().align(Alignment.Center),
                        color = color
                    )
                    GameField.Entity.Type.STRONGMAN -> Text(
                        "S",
                        Modifier.wrapContentSize().align(Alignment.Center),
                        color = color
                    )
                    GameField.Entity.Type.ACROBAT -> Text(
                        "A",
                        Modifier.wrapContentSize().align(Alignment.Center),
                        color = color
                    )
                    GameField.Entity.Type.MAGICIAN -> Text(
                        "M",
                        Modifier.wrapContentSize().align(Alignment.Center),
                        color = color
                    )
                    GameField.Entity.Type.TRAINER -> Text(
                        "T",
                        Modifier.wrapContentSize().align(Alignment.Center),
                        color = color
                    )
                    null -> if (field.field[8 - rowInv][col].hasHouse) Text(
                        "H", Modifier.wrapContentSize().align(
                            Alignment.Center
                        ), color = Color.Green
                    )
                }
            }
        }
    }
}
