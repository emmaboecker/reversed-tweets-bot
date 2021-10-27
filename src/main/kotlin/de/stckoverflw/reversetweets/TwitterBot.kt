package de.stckoverflw.reversetweets

import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.stream.StreamRules
import io.github.redouane59.twitter.dto.tweet.TweetType
import de.stckoverflw.reversetweets.config.Config
import de.stckoverflw.reversetweets.twitter.credentials
import de.stckoverflw.reversetweets.twitter.getFlippedImages
import de.stckoverflw.reversetweets.twitter.getMediaIds
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

object TwitterBot {
    lateinit var twitter: TwitterClient

    lateinit var client: HttpClient

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
        "Nihaachu",
        "Ph1LzA",
        "Punztw",
    )

    /**
     *  List of users that can delete tweets by replying to it
     */
    private val deleteUser = listOf(
        "ReversingT",
        "tycmdesrever_",
        "revrsemcythate",
        "Reversed_McYt",
        "l4zs1",
        "AmelieHuhChamp",
        "StckOverflw",
        "StylesTheEditor",
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

        client = HttpClient(OkHttp) {
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
        println("Added ${users.size} user to reply to")
        twitter.addFilteredStreamRule("to:ReversedMcYt", "replies")
        println("Starting 1st stream")

        startStream()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun startStream() {
        println("Starting new Stream")
        twitter.startFilteredStream {
            if (deleteUser.contains(twitter.getUserFromUserId(it.authorId).name)) {
                if (it.tweetType == TweetType.REPLIED_TO) {
                    if (it.text.lowercase(Locale.getDefault()).contains("/delete")) {
                        if (twitter.getUserFromUserName("ReversedMcYt").id.equals(it.inReplyToUserId)) {
                            println("Delete Tweet:")
                            println(twitter.getTweet(it.inReplyToStatusId).text)
                            println("Because ${it.user.name} replied:")
                            println(it.text)
                            twitter.deleteTweet(it.inReplyToStatusId)
                        }
                    }
                }
            } else if (users.contains(twitter.getUserFromUserId(it.authorId).name)) {
                if (it.tweetType != TweetType.RETWEETED) {
                    println("${it.text} by ${twitter.getUserFromUserId(it.authorId).name}")
                    GlobalScope.launch {
                        val splittedText = it.text.split(' ')
                        var cleanText = ""
                        splittedText.forEach { text ->
                            if ((!text.startsWith("@")) && (!text.startsWith("http"))) {
                                cleanText = cleanText.plus("$text ")
                            }
                        }
                        if (!it.attachments.mediaKeys.isNullOrEmpty()) {
                            val mediaUrls = getFlippedImages(it.id)
                            val mediaIds = getMediaIds(mediaUrls)
                            twitter.postTweet(cleanText.reversed(), it.id, mediaIds.joinToString(","))
                        } else {
                            twitter.postTweet(cleanText.reversed(), it.id)
                        }
                    }
                }
            }
        }.await()

        // delay(Duration.Companion.seconds(60))
        startStream()
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