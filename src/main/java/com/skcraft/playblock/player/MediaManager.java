package com.skcraft.playblock.player;

import static com.skcraft.playblock.util.EnvUtils.getPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;

import com.skcraft.playblock.PlayBlock;
import com.skcraft.playblock.util.EnvUtils.Platform;
import com.skcraft.playblock.util.PlayBlockPaths;
import com.sun.jna.NativeLibrary;

/**
 * Manages media player instances.
 */
public class MediaManager {

    private MediaPlayerFactory factory;
    private MediaRenderer activeRenderer;
    private float volume = 1;

    static {

        for (File file : PlayBlockPaths.getSearchPaths()) {
            NativeLibrary.addSearchPath("libvlc", file.getAbsolutePath());
        }
    }

    /**
     * Construct a new media manager.
     */
    public MediaManager() {
        try {
            factory = new MediaPlayerFactory(getFactoryOptions());
        } catch (Throwable t) {
            PlayBlock.log(Level.WARNING, "Failed to find VLC!", t);
        }
        volume = PlayBlock.getClientRuntime().getClientOptions()
                .getFloat("volume", 1);
    }

    /**
     * Returns whether the VLC library loaded successfully.
     * 
     * @return true if it is available
     */
    public boolean isAvailable() {
        return factory != null;
    }

    /**
     * Generate the VLC options required.
     * 
     * @return the list of options
     */
    private String[] getFactoryOptions() {
        List<String> options = new ArrayList<String>();

        // Don't show a title on the video when media is played
        options.add("--no-video-title-show");

        // Mac OS X support
        if (getPlatform() == Platform.MAC_OS_X) {
            options.add("--vout=macosx");
        }

        String[] arr = new String[options.size()];
        options.toArray(arr);
        return arr;
    }

    /**
     * Returns whether a new player can be acquired and started.
     * 
     * <p>For performance reasons, only one player can be playing at a time.</p>
     * 
     * @return true true if there is a free player available
     */
    public boolean hasNoRenderer() {
        return activeRenderer == null;
    }

    /**
     * Acquire a new renderer and set it as the active renderer.
     * 
     * @param width the width of the video
     * @param height the height of the video
     * @return the renderer
     */
    public MediaRenderer acquireRenderer(int width, int height) {
        if (activeRenderer != null) {
            release(activeRenderer);
        }

        activeRenderer = newRenderer(width, height);
        return activeRenderer;
    }

    /**
     * Create a new renderer to render media on.
     * 
     * <p>The rendered is tracked using this instance and it can be released
     * selectively or all together.</p>
     * 
     * @param width the width of the player
     * @param height the height of the player
     * @return a new media renderer
     */
    protected MediaRenderer newRenderer(int width, int height) {
        if (!isAvailable()) {
            throw new RuntimeException("VLC library is not available!");
        }

        MediaRenderer instance = new MediaRenderer(width, height);
        DirectMediaPlayer player = factory.newDirectMediaPlayer("RGBA", width,
                height, width * 4, instance);
        player.setPlaySubItems(true);
        player.setRepeat(true);
        player.addMediaPlayerEventListener(new PlayerEventListener(instance));
        player.setVolume((int) (volume * 100));
        instance.setVLCJPlayer(player);
        return instance;
    }

    /**
     * Frees up a renderer.
     * 
     * @param instance the renderer
     */
    public void release(MediaRenderer instance) {
        if (!isAvailable()) {
            throw new RuntimeException("VLC library is not available!");
        }

        instance.release();
        if (instance == activeRenderer) {
            activeRenderer = null;
        }
    }

    /**
     * Release all known renderers.
     */
    public void releaseAll() {
        if (activeRenderer != null) {
            release(activeRenderer);
        }
    }

    /**
     * Unload the manager and also any renderers.
     */
    public void unload() {
        releaseAll();

        if (factory != null) {
            factory.release();
            factory = null;
        }
    }

    /**
     * Set the volume of the player.
     * 
     * @param volume the volume
     */
    public void setVolume(float volume) {
        this.volume = volume;
        
        if (activeRenderer != null) {
            activeRenderer.getVLCJPlayer().setVolume((int) (volume * 100));
        }
    }
    
    /**
     * Return the volume of the player.
     * 
     * @return the volume
     */
    public float getVolume() {
        return volume;
    }

}
