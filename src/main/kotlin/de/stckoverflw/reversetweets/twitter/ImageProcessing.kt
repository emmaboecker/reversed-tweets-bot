package de.stckoverflw.reversetweets.twitter

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import de.stckoverflw.reversetweets.TwitterBot
import de.stckoverflw.reversetweets.config.Config
import io.github.redouane59.twitter.dto.tweet.MediaCategory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File
import java.net.URL
import java.util.*
import javax.imageio.ImageIO


fun getFlippedImages(tweetId: String): ArrayList<String> {
    val tweetJsonRaw =
        TwitterBot.twitter.requestHelperV1
            .getRequestWithParameters(
                TwitterBot.twitter.urlHelper.getTweetUrl(tweetId),
                mapOf(
                    Pair("expansions", "attachments.media_keys"),
                    Pair("media.fields", "url")
                ),
                String::class.java).get()

    val json = JsonParser.parseString(tweetJsonRaw)

    val flippedImages = ArrayList<String>()

    println(tweetJsonRaw)

    if (json["includes"] !is JsonNull) {
        if (json["includes"]["media"] !is JsonNull) {
            json["includes"]["media"].asJsonArray.forEach {
                flippedImages.add(Config.IMAGINARY_URL + "/flip?url=" + it["url"].asString)
            }
        }
    }

    return flippedImages
}

fun getMediaIds(imageUrls: ArrayList<String>): ArrayList<String> {
    val mediaIDs = ArrayList<String>()
    val images = ArrayList<File>()
    var currentImage = 1
    imageUrls.forEach {
        val url = URL(it)
        val img = ImageIO.read(url)
        val file = File("downloaded$currentImage.jpg")
        ImageIO.write(img, "jpg", file)
        images.add(file)
        currentImage++
    }
    images.reverse()
    images.forEach {
        val response = TwitterBot.twitter.uploadMedia(it, MediaCategory.TWEET_IMAGE)
        mediaIDs.add(response.mediaId)
    }
    return mediaIDs
}

operator fun JsonElement.get(name: String): JsonElement = asJsonObject[name]
operator fun JsonElement.get(index: Int): JsonElement = asJsonArray[index]