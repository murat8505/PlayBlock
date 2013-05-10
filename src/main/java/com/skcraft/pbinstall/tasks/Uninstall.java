package com.skcraft.pbinstall.tasks;

import com.skcraft.playblock.util.PlayBlockPaths;

/**
 * Uninstalls all native libraries.
 */
public class Uninstall extends DirectoryDelete {

    public Uninstall() {
        super(PlayBlockPaths.getPlayBlockLibsDir());
    }

}
