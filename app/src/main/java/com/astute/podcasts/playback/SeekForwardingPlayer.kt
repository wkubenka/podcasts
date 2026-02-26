package com.astute.podcasts.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Wraps the underlying player so that Bluetooth AVRCP "next/previous track"
 * commands seek within the current episode instead of changing media items.
 */
class SeekForwardingPlayer(player: Player) : ForwardingPlayer(player) {

    override fun seekToNext() {
        val target = (currentPosition + seekForwardIncrement)
            .coerceAtMost(duration.coerceAtLeast(0))
        seekTo(target)
    }

    override fun seekToPrevious() {
        val target = (currentPosition - seekBackIncrement).coerceAtLeast(0)
        seekTo(target)
    }

    override fun seekToNextMediaItem() {
        seekToNext()
    }

    override fun seekToPreviousMediaItem() {
        seekToPrevious()
    }

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_FORWARD)
            .add(Player.COMMAND_SEEK_BACK)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return getAvailableCommands().contains(command)
    }
}
