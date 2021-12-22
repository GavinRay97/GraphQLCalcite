rootProject.name = "GraphQLCalcite"

// Awful hack in settings.grade to reflectively enable logging
// See: https://youtrack.jetbrains.com/issue/IDEA-191119
// See: https://github.com/gradle/gradle/issues/19340
val LOG_LEVEL = LogLevel.DEBUG
setGradleLogLevelReflectively(LOG_LEVEL)

fun setGradleLogLevelReflectively(logLevel: LogLevel) {
    val LoggerFactory = Class.forName("org.slf4j.LoggerFactory")
    val OutputEventListenerBackedLoggerContext =
        Class.forName("org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext")
    val OutputEventListener = Class.forName("org.gradle.internal.logging.events.OutputEventListener")
    val StandardOutputListener = Class.forName("org.gradle.api.logging.StandardOutputListener")
    val StreamBackedStandardOutputListener =
        Class.forName("org.gradle.internal.logging.text.StreamBackedStandardOutputListener")
    val StyledTextOutput = Class.forName("org.gradle.internal.logging.text.StyledTextOutput")
    val StreamingStyledTextOutput = Class.forName("org.gradle.internal.logging.text.StreamingStyledTextOutput")
    val StyledTextOutputBackedRenderer =
        Class.forName("org.gradle.internal.logging.console.StyledTextOutputBackedRenderer")

    val newStreamBackedStandardOutputListener =
        StreamBackedStandardOutputListener.getDeclaredConstructor(java.io.OutputStream::class.java)
    val newStreamingStyledTextOutput = StreamingStyledTextOutput.getDeclaredConstructor(StandardOutputListener)
    val newStyledTextOutputBackedRenderer = StyledTextOutputBackedRenderer.getDeclaredConstructor(StyledTextOutput)

    val gradleLoggerFactory = LoggerFactory.getDeclaredMethod("getILoggerFactory").invoke(null)
    OutputEventListenerBackedLoggerContext.getDeclaredMethod("setLevel", LogLevel::class.java)
        .invoke(gradleLoggerFactory, logLevel)

    val streamBackedStandardOutputListener = newStreamBackedStandardOutputListener.newInstance(System.out)
    val streamingStyledTextOutput = newStreamingStyledTextOutput.newInstance(streamBackedStandardOutputListener)
    val styledTextOutputBackedRenderer = newStyledTextOutputBackedRenderer.newInstance(streamingStyledTextOutput)
    OutputEventListenerBackedLoggerContext.getDeclaredMethod("setOutputEventListener", OutputEventListener)
        .invoke(gradleLoggerFactory, styledTextOutputBackedRenderer)
}


