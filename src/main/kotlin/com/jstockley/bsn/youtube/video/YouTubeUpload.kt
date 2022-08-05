package com.jstockley.bsn.youtube.video

import com.jstockley.bsn.*
import com.jstockley.bsn.notification.Notification
import mu.KotlinLogging

class YouTubeUpload: Runnable {
    override fun run() {

        val logger = KotlinLogging.logger{}

        val apiKeys: List<String> = getYouTubeCred()

        var index = 0

        while (true) {

            val previousVideoIds: MutableMap<String, String> = getPreviousVideoId()

            var previousVideoAmounts: Map<String, Int> = getPlaylists()

            if(hasConnection()){

                val recentlyUploadedVideos = mutableListOf<YouTubeVideo>()

                val ytPlaylist = YouTubePlaylists(previousVideoAmounts.keys.toMutableList(), apiKeys[index])

                for(playlist in ytPlaylist.getCurrentVideoAmounts().keys){
                    if ((ytPlaylist.getCurrentVideoAmounts()[playlist]!! > previousVideoAmounts[playlist]!!) && previousVideoAmounts[playlist]!! > 0){
                        val video = YouTubeVideo(playlist, apiKeys[index])
                        logger.info { "${video.getChannelName()} upload playlist amount increased: ${previousVideoAmounts[playlist]} -> ${ytPlaylist.getCurrentVideoAmounts()[playlist]}" }
                        // Check if video is not the previous video
                        if(previousVideoIds[playlist] != video.getVideoId()){
                            // Check if video is not a livestream
                            if(!video.isLivestream()){
                                logger.info { "${video.getChannelName()} uploaded a new video" }
                                logger.debug { "${previousVideoIds[playlist]} -> ${video.getVideoId()}" }
                                recentlyUploadedVideos.add(video)
                            }
                        }
                        previousVideoIds[playlist] = video.getVideoId()
                    }
                    //previousVideoAmounts[playlist] = ytPlaylist.getCurrentVideoAmounts()[playlist]!!
                }
                if (recentlyUploadedVideos.isNotEmpty()) {
                    val notif = Notification(recentlyUploadedVideos)
                    notif.send(accountKeys)
                    logger.info { "Sent upload notification for $recentlyUploadedVideos" }
                }

                if (previousVideoAmounts != ytPlaylist.getCurrentVideoAmounts()){
                    previousVideoAmounts = ytPlaylist.getCurrentVideoAmounts()
                    writePlaylists(previousVideoAmounts)
                    writePreviousVideoId(previousVideoIds)
                }

            } else {
                println("No internet connection!")
                logger.error { "No internet connection!" }
            }

            println("YouTube upload sleeping for 10 seconds...")
            Thread.sleep(10000)

            index = selectAPIKey(apiKeys, index)
        }
    }

    private fun selectAPIKey(apiKeys: List<String>, previousIndex: Int): Int{
        return if (previousIndex == apiKeys.size - 1){
            0
        } else {
            previousIndex + 1
        }
    }
}