import com.plastickarma.producerswithpriority.EpochGenerator
import com.plastickarma.producerswithpriority.PriorityConfiguration
import com.plastickarma.producerswithpriority.Producer
import com.plastickarma.producerswithpriority.Scheduler
import com.plastickarma.producerswithpriority.WorkStrategy
import com.plastickarma.producerswithpriority.fair
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSpanElement

private val PRODUCER_COLORS =
    mapOf(
        "A" to "#4285F4",
        "B" to "#EA4335",
        "C" to "#34A853",
        "D" to "#FBBC05",
    )
private const val EPOCHS = 60
private const val DEFAULT_DELAY_MS = 80L
private const val STEP_DELTA_MS = 20L
private const val TIMELINE_ID = "timeline"
private const val STATS_ID = "stats"
private const val SHARE_DEFAULT = 50.0
private const val SIM_SELECT_ID = "sim-select"
private const val MODE_CUSTOM = "custom"
private const val PAUSE_BTN_ID = "btn-pause"
private const val TEXT_PAUSE = "Pause"

private val DESCRIPTIONS =
    mapOf(
        "round-robin" to "Cycles A \u2192 B \u2192 C \u2192 D in strict order regardless of share weights.",
        "fair" to "Randomly picks a producer weighted by equal shares \u2014 each producer gets roughly 25% over time.",
        MODE_CUSTOM to "Randomly picks a producer weighted by the share values you set below.",
    )

private var stepDelayMs = DEFAULT_DELAY_MS
private var simulationJob: Job? = null
private var isPaused = false
private var pauseDeferred: CompletableDeferred<Unit>? = null

private fun fixedEpochs(n: Int): EpochGenerator {
    var count = 0
    return { count++ < n }
}

private class ConstantProducer(
    private val value: String,
) : Producer<String> {
    override suspend fun next(): String = value
}

private fun shareInputId(name: String) = "share-$name"

private fun shareFor(name: String): Double {
    val input = document.getElementById(shareInputId(name)) as HTMLInputElement
    return input.value.toDoubleOrNull() ?: SHARE_DEFAULT
}

private fun schedulerFlow(
    producers: List<Pair<PriorityConfiguration, Producer<String>>>,
    strategy: WorkStrategy,
): Flow<String> =
    Scheduler().schedule(
        producers = producers,
        epochs = fixedEpochs(EPOCHS),
        strategy = strategy,
    )

private fun selectedFlow(): Flow<String> {
    val select = document.getElementById(SIM_SELECT_ID) as HTMLSelectElement
    (document.getElementById("description") as HTMLParagraphElement).textContent =
        DESCRIPTIONS[select.value] ?: ""
    return when (select.value) {
        "fair" ->
            fair(
                epochs = fixedEpochs(EPOCHS),
                producers = PRODUCER_COLORS.keys.map { ConstantProducer(it) }.toTypedArray(),
            )
        MODE_CUSTOM ->
            schedulerFlow(
                producers =
                    PRODUCER_COLORS.keys.map { name ->
                        PriorityConfiguration(shares = shareFor(name)) to ConstantProducer(name)
                    },
                strategy = WorkStrategy.DISTRIBUTION,
            )
        else ->
            schedulerFlow(
                producers =
                    PRODUCER_COLORS.keys.map { name ->
                        PriorityConfiguration(shares = 1000.0) to ConstantProducer(name)
                    },
                strategy = WorkStrategy.ROUND_ROBIN,
            )
    }
}

private fun pauseButton() = document.getElementById(PAUSE_BTN_ID) as HTMLButtonElement

private fun setPauseEnabled(enabled: Boolean) {
    pauseButton().disabled = !enabled
}

private fun startSimulation() {
    simulationJob?.cancel()
    isPaused = false
    pauseDeferred = null
    clearVisualization()
    simulationJob = MainScope().launch { runVisualization() }
    pauseButton().textContent = TEXT_PAUSE
    setPauseEnabled(true)
}

private fun togglePause() {
    val pauseBtn = pauseButton()
    if (isPaused) {
        isPaused = false
        pauseDeferred?.complete(Unit)
        pauseDeferred = null
        pauseBtn.textContent = TEXT_PAUSE
    } else {
        isPaused = true
        pauseBtn.textContent = "Continue"
    }
}

private suspend fun awaitUnpaused() {
    if (isPaused) {
        pauseDeferred = CompletableDeferred()
        pauseDeferred?.await()
    }
}

fun main() {
    setupShareSliders()
    setupControls()
}

private fun setupShareSliders() {
    PRODUCER_COLORS.keys.forEach { name ->
        val input = document.getElementById(shareInputId(name)) as HTMLInputElement
        val valueSpan = document.getElementById("${shareInputId(name)}-val") as HTMLSpanElement
        input.oninput = { valueSpan.textContent = input.value }
    }
}

private fun setupControls() {
    val startBtn = document.getElementById("btn-start") as HTMLButtonElement
    val faster = document.getElementById("btn-faster") as HTMLButtonElement
    val slower = document.getElementById("btn-slower") as HTMLButtonElement
    val simSelect = document.getElementById(SIM_SELECT_ID) as HTMLSelectElement

    startBtn.onclick = { startSimulation() }
    pauseButton().onclick = { togglePause() }
    simSelect.onchange = {
        syncSharePanel()
        startSimulation()
    }
    faster.onclick = {
        if (stepDelayMs > STEP_DELTA_MS) {
            stepDelayMs -= STEP_DELTA_MS
        }
    }
    slower.onclick = { stepDelayMs += STEP_DELTA_MS }
}

private fun clearVisualization() {
    (document.getElementById(TIMELINE_ID) as HTMLDivElement).innerHTML = ""
    (document.getElementById(STATS_ID) as HTMLDivElement).innerHTML = ""
}

private fun syncSharePanel() {
    val select = document.getElementById(SIM_SELECT_ID) as HTMLSelectElement
    val shareControls = document.getElementById("share-controls") as HTMLDivElement
    shareControls.style.display =
        when (select.value) {
            MODE_CUSTOM -> "block"
            else -> "none"
        }
}

private suspend fun runVisualization() {
    val counts = mutableMapOf("A" to 0, "B" to 0, "C" to 0, "D" to 0)
    selectedFlow().collect { value ->
        awaitUnpaused()
        counts[value] = (counts[value] ?: 0) + 1
        appendCell(value)
        updateStats(counts)
        delay(stepDelayMs)
    }
    setPauseEnabled(false)
    pauseButton().textContent = TEXT_PAUSE
}

private fun appendCell(value: String) {
    val timeline = document.getElementById(TIMELINE_ID) as HTMLDivElement
    val cell = document.createElement("div") as HTMLDivElement
    cell.className = "cell"
    cell.textContent = value
    cell.style.backgroundColor = PRODUCER_COLORS[value] ?: "#888"
    timeline.appendChild(cell)
    window.requestAnimationFrame { cell.className = "cell visible" }
}

private fun updateStats(counts: Map<String, Int>) {
    val statsDiv = document.getElementById(STATS_ID) as HTMLDivElement
    statsDiv.innerHTML = ""
    counts.forEach { (name, count) ->
        val statEntry = document.createElement("div") as HTMLDivElement
        statEntry.className = "stat"
        statEntry.innerHTML =
            """
            <div class="stat-swatch" style="background:${PRODUCER_COLORS[name]}"></div>
            <span>$name: $count</span>
            """.trimIndent()
        statsDiv.appendChild(statEntry)
    }
}
