package de.stckoverflw.reversetweets.config

import ch.qos.logback.classic.Level
import dev.schlaubi.envconf.environment
import dev.schlaubi.envconf.getEnv

object Config {

    val TWITTER_BEARER_TOKEN: String by environment
    val TWITTER_API_KEY: String by environment
    val TWITTER_API_SECRET: String by environment
    val TWITTER_ACCESS_TOKEN: String by environment
    val TWITTER_ACCESS_SECRET: String by environment

    val TWITTER_REPLY_USERS: List<String> by getEnv {
        it.split(',')
    }
    val TWITTER_DELETE_USERS: List<String> by getEnv {
        it.split(',')
    }

    val IMAGINARY_URL: String by environment

    val LOG_LEVEL: Level by getEnv {
        Level.toLevel(it, Level.INFO)
    }
}
