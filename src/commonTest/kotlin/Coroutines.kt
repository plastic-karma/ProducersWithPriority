import kotlin.coroutines.CoroutineContext
// https://github.com/Kotlin/kotlinx.coroutines/issues/1996#issuecomment-699606571
expect fun runBlockingTest(block: suspend () -> Unit)
expect val testCoroutineContext: CoroutineContext
