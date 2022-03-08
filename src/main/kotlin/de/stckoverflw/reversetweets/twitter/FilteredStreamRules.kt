package de.stckoverflw.reversetweets.twitter

import de.stckoverflw.reversetweets.TwitterBot.client
import de.stckoverflw.reversetweets.TwitterBot.self
import de.stckoverflw.reversetweets.TwitterBot.twitter
import de.stckoverflw.reversetweets.config.Config
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val LOG = KotlinLogging.logger { }

suspend fun setUpFilteredStreamRules() {
    // Resolve reply user's ids
    LOG.info("Resolving given reply users")
    val replyUsersIds = Config.TWITTER_REPLY_USERS.map { twitter.getUserFromUserName(it).id }

    // Get existing TweetRules
    val rules = twitter.retrieveFilteredStreamRules()
    LOG.debug("Found ${rules?.size ?: 0} rules!")
    // Delete existing TweetRules
    if (!rules.isNullOrEmpty()) {
        val deletingRules = rules.filter { rule ->
            !replyUsersIds.any { it.equals(rule.value.replace("from:", ""), true) } &&
                    rule.value != "to:${self.id}"
        }
        if (deletingRules.isNotEmpty()) {
            LOG.info("Deleting existing stream rules")
            client.post<HttpResponse>("https://api.twitter.com/2/tweets/search/stream/rules") {
                header(HttpHeaders.Authorization, "Bearer ${Config.TWITTER_BEARER_TOKEN}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                body = buildJsonObject {
                    putJsonObject("delete") {
                        putJsonArray("ids") {
                            deletingRules.forEach {
                                add(it.id)
                            }
                        }
                    }
                }
            }
        } else {
            LOG.info("Skipping Rule Deletion because all existing rules are still applicable")
        }
    } else {
        LOG.info("Skipping Rule Deletion because there are no existing rules")
    }

    val missingRules = if (!rules.isNullOrEmpty()) buildList {
        if (!rules.any { it.value == "to:${self.id}" }) {
            add("to:${self.id}")
        }
        replyUsersIds.filter { id -> !rules.map { it.value }.contains("from:$id") }.forEach {
            add("from:$it")
        }
    } else replyUsersIds.map { "from:$it" } + "to:${self.id}"

    if (missingRules.isNotEmpty()) {
        LOG.info("Adding missing rules: ${missingRules.joinToString()}")
        client.post<HttpResponse>("https://api.twitter.com/2/tweets/search/stream/rules") {
            header(HttpHeaders.Authorization, "Bearer ${Config.TWITTER_BEARER_TOKEN}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = buildJsonObject {
                putJsonArray("add") {
                    missingRules.forEach {
                        addJsonObject {
                            put("value", it)
                        }
                    }
                }
            }
        }
    } else {
        LOG.info("Skipping Rule Adding because all required rules still exist")
    }

    LOG.info("Replying to: ${Config.TWITTER_REPLY_USERS.joinToString(", ")}")
    LOG.debug("Current ruleset: ${twitter.retrieveFilteredStreamRules()?.joinToString { it.value } ?: "no rules"}")
}
