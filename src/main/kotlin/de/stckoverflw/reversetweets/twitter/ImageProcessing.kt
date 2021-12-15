package de.stckoverflw.reversetweets.twitter

import de.stckoverflw.reversetweets.TwitterBot
import de.stckoverflw.reversetweets.config.Config
import io.github.redouane59.twitter.dto.tweet.MediaCategory
import io.github.redouane59.twitter.dto.tweet.Tweet
import java.net.URL

fun Tweet.flipImages(): List<String> = if (media != null) getMediaIds(getFlippedImages(this)) else emptyList()

private fun getFlippedImages(tweet: Tweet): List<String> =
    tweet.media.map { it.mediaUrl }.map { Config.IMAGINARY_URL + "/flip?url=" + it }

private fun getMediaIds(imageUrls: List<String>): List<String> = (
    imageUrls.map {
        URL(it).openStream().readAllBytes()
    } as ArrayList<ByteArray>
    ).apply {
    this.reverse()
}.mapIndexed { index, bytes ->
    TwitterBot.twitter.uploadMedia("image$index.jpg", bytes, MediaCategory.TWEET_IMAGE).mediaId
}
