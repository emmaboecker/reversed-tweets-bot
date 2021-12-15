package de.stckoverflw.reversetweets.twitter

import de.stckoverflw.reversetweets.TwitterBot
import de.stckoverflw.reversetweets.TwitterBot.client
import de.stckoverflw.reversetweets.TwitterBot.twitter
import de.stckoverflw.reversetweets.config.Config
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import mu.KotlinLogging

private val LOG = KotlinLogging.logger { }

suspend fun setUpFilteredStreamRules() {
    // Get existing TweetRules
    val rules = twitter.retrieveFilteredStreamRules()
    LOG.debug("Found ${rules.size} rules!")
    // Delete existing TweetRules
    if (!rules.isNullOrEmpty()) {
        LOG.info("Deleting existing stream rules")
        client.post<HttpResponse>("https://api.twitter.com/2/tweets/search/stream/rules") {
            header(HttpHeaders.Authorization, "Bearer ${Config.TWITTER_BEARER_TOKEN}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = buildJsonObject {
                putJsonObject("delete") {
                    putJsonArray("ids") {
                        rules.forEach {
                            add(it.id)
                        }
                    }
                }
            }
        }
    }

    // Add Rules to reply to users
    Config.TWITTER_REPLY_USERS.forEach {
        twitter.addFilteredStreamRule("from:$it", "$it's tweets")
        LOG.debug("Added '$it' to reply users")
    }

    LOG.info("Replying to: ${Config.TWITTER_REPLY_USERS.joinToString(", ")}")

    twitter.addFilteredStreamRule("to:${TwitterBot.self.name}", "replies")
}
