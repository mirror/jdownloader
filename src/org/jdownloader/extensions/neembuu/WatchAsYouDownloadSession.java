/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.io.File;

import javax.swing.JPanel;

import neembuu.vfs.file.SeekableConnectionFile;

import org.jdownloader.extensions.neembuu.gui.HttpFilePanel;

/**
 * 
 * @author Shashank Tulsyan
 */
public interface WatchAsYouDownloadSession {
    SeekableConnectionFile getSeekableConnectionFile();

    NB_VirtualFileSystem getVirtualFileSystem();

    JPanel getFilePanel();

    HttpFilePanel getHttpFilePanel();

    boolean isMounted();

    File getMountLocation();

    void waitForDownloadToFinish() throws Exception;

    /**
     * @return number of bytes already downloaded for this given file.
     */
    long getTotalDownloaded();
}
