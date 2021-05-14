package de.stckoverflw.reversetweets

import com.github.redouane59.twitter.TwitterClient
import com.github.redouane59.twitter.dto.stream.StreamRules
import de.stckoverflw.reversetweets.config.Config
import de.stckoverflw.reversetweets.twitter.credentials
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import kotlin.coroutines.resumeWithException

object TwitterBot {
    private lateinit var twitter: TwitterClient

    /**
     * Here are the users you want the bot to reply to
     */
    private val users = listOf(
        "tommyinnit",
        "tommyaltinnit",
        "TubboLive",
        "TubboTWO",
        "JackManifoldTV",
        "JackManifoldTwo",
        "quackity",
        "dreamwastaken",
        "KarlJacobs_",
        "GeorgeNootFound",
        "sapnap",
        "GeorgeNotFound",
        "Dream",
        "WilburSoot",
        "Ranboosaysstuff",
        "ranaltboo",
        "honkkarl",
        "Nihaachu",
        "StckOverflw"
    )

    suspend operator fun invoke() {
        twitter = TwitterClient(credentials {
            apiKey = Config.TWITTER_API_KEY
            apiSecretKey = Config.TWITTER_API_SECRET
            accessToken = Config.TWITTER_ACCESS_TOKEN
            accessTokenSecret = Config.TWITTER_ACCESS_SECRET
        })

        val rules: List<StreamRules.StreamRule>? = twitter.retrieveFilteredStreamRules()
        println("Found ${rules?.count() ?: 0} rules!")

        val client = HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }

        if (!rules.isNullOrEmpty()) {
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

        users.forEach {
            twitter.addFilteredStreamRule("from:$it", "$it's tweets")
        }

        println("Updated rules")

        twitter.startFilteredStream {
            println("${it.text} by ${twitter.getUserFromUserId(it.authorId).name}")
            GlobalScope.launch {
                val splittedText = it.text.split(' ')
                val textWithOutMentions = StringBuilder()
                splittedText.forEach { current ->
                    if (!current.startsWith('@')) {
                        textWithOutMentions.append("$current ")
                    }
                }
                twitter.postTweet(textWithOutMentions.reverse().toString(), it.id)
            }
        }.await()
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> Future<T>.await(): T = suspendCancellableCoroutine { cont ->
    ForkJoinPool.managedBlock(object : ForkJoinPool.ManagedBlocker {
        override fun block(): Boolean {
            try {
                cont.resume(get()) { cont.cancel(it) }
            } catch (e: Throwable) {
                cont.resumeWithException(e)
                return false
            }

            return true
        }

        override fun isReleasable(): Boolean = isDone
    })
}