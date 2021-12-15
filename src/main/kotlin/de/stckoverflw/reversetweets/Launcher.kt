package de.stckoverflw.reversetweets

import ch.qos.logback.classic.Logger
import de.stckoverflw.reversetweets.config.Config
import org.slf4j.LoggerFactory

suspend fun main() {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Config.LOG_LEVEL
    TwitterBot()
}
