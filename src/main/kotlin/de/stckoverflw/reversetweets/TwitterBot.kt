package de.stckoverflw.reversetweets

import de.stckoverflw.reversetweets.config.Config
import de.stckoverflw.reversetweets.twitter.credentials
import de.stckoverflw.reversetweets.twitter.flipImages
import de.stckoverflw.reversetweets.twitter.setUpFilteredStreamRules
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.tweet.TweetParameters
import io.github.redouane59.twitter.dto.tweet.TweetType
import io.github.redouane59.twitter.dto.user.UserV2
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.*

import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

private val LOG = KotlinLogging.logger { }

object TwitterBot : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    lateinit var twitter: TwitterClient
    lateinit var client: HttpClient

    lateinit var self: UserV2

    suspend operator fun invoke() {
        coroutineScope {
            twitter = TwitterClient(
                credentials {
                    apiKey = Config.TWITTER_API_KEY
                    apiSecretKey = Config.TWITTER_API_SECRET
                    accessToken = Config.TWITTER_ACCESS_TOKEN
                    accessTokenSecret = Config.TWITTER_ACCESS_SECRET
                }
            )
            client = HttpClient(OkHttp) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
            }
            self = twitter.getUserFromUserName("me")
            setUpFilteredStreamRules()
            LOG.info("Starting Stream")
            twitter.startFilteredStream { tweet ->
                try {
                    val author = twitter.getUserFromUserId(tweet.authorId)
                    if (Config.TWITTER_DELETE_USERS.contains(author.name)) {
                        if (tweet.tweetType == TweetType.REPLIED_TO) {
                            if (tweet.text.lowercase(Locale.getDefault()).contains("/delete")) {
                                if (self.id.equals(tweet.inReplyToUserId)) {
                                    LOG.info("Delete Tweet:")
                                    LOG.info(twitter.getTweet(tweet.inReplyToStatusId).text)
                                    LOG.info("Because ${author.name} replied:")
                                    LOG.info(tweet.text)
                                    twitter.deleteTweet(tweet.inReplyToStatusId)
                                }
                                return@startFilteredStream
                            }
                        }
                    }
                    if (Config.TWITTER_REPLY_USERS.contains(author.name)) {
                        if (tweet.tweetType != TweetType.RETWEETED) {
                            LOG.info("${tweet.text} by ${author.name}")
                            launch {
                                var cleanText = tweet.text
                                    .replace("^(@\\w+ )*".toRegex(), "")
                                tweet.entities.urls?.forEach {
                                    cleanText = cleanText.replace(it.url, "")
                                }

                                val flippedImages = tweet.flipImages()

                                val tweetBuilder = TweetParameters.builder()
                                    .reply(TweetParameters.Reply.builder().inReplyToTweetId(tweet.id).build())
                                    .text(cleanText.reversed())
                                if (flippedImages != null) {
                                    tweetBuilder.media(TweetParameters.Media.builder().mediaIds(flippedImages).build())
                                }
                                twitter.postTweet(tweetBuilder.build())
                            }
                        }
                    }
                } catch (ex: Exception) {
                    LOG.error {
                        "Catched an Error: \nMessage: ${ex.message} \nStacktrace: ${ex.stackTrace}"
                    }
                }
            }.await()

        }
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
