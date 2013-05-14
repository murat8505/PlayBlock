package com.skcraft.playblock.player;

import net.minecraft.nbt.NBTTagCompound;

import com.skcraft.playblock.PlayBlock;
import com.skcraft.playblock.media.Media;
import com.skcraft.playblock.media.MediaQueue;
import com.skcraft.playblock.media.PlayingMedia;
import com.skcraft.playblock.media.QueueListener;
import com.skcraft.playblock.media.QueueManager;

/**
 * This class manages the server side of the media player.
 */
public class MediaPlayerHost extends MediaPlayer implements QueueListener {

    private QueueManager queueManager;
    private MediaQueue queue;

    /**
     * Construct a new instance.
     */
    public MediaPlayerHost() {
        queueManager = PlayBlock.getRuntime().getQueueManager();
    }

    /**
     * Get the queue.
     * 
     * @return the queue, or null if there is no queue
     */
    public MediaQueue getQueue() {
        return queue;
    }
    
    @Override
    public void setQueueMode(boolean queueMode) {
        // Create a queue if queue mode is turned on
        if (queueManager != null && inQueueMode() != queueMode) {
            if (queueMode) {
                queue = queueManager.createQueue();
                queue.addQueueListener(this);
            } else {
                queue.removeQueueListener(this);
                queue.release();
                queue = null;
            }
        }
        
        super.setQueueMode(queueMode);
    }

    @Override
    public void mediaComplete(Media media) {
    }

    @Override
    public void mediaAdvance(Media media) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("playedUri", media.getUri());
        tag.setInteger("position", -1); // We don't want the video to be skipped ahead
        fireNbtEvent(tag);
    }

    @Override
    public void toNetworkSnapshotNbt(NBTTagCompound tag) {
        toSharedNbt(tag);
        
        if (inQueueMode()) {
            PlayingMedia playing = queue.getCurrentMedia();
            if (playing != null) {
                tag.setString("playedUri", playing.getMedia().getUri());
                tag.setInteger("position", (int) playing.getPosition());
            }
        }
    }

    @Override
    public void fromNetworkSnapshotNbt(NBTTagCompound tag) {
        // State NBT can only come from the server
    }

    @Override
    public void handleNetworkNbtEvent(NBTTagCompound tag) {
        // We don't get anything from the client
    }

}
