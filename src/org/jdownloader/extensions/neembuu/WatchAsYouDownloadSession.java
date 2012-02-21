/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu;

import java.io.File;
import javax.swing.JPanel;
import neembuu.vfs.file.SeekableConnectionFile;

/**
 *
 * @author Shashank Tulsyan
 */
public interface WatchAsYouDownloadSession {
    SeekableConnectionFile getSeekableConnectionFile();
    NBVirtualFileSystem getVirtualFileSystem();
    JPanel getFilePanel();
    
    boolean isMounted();
    void mount()throws Exception;
    void unMount()throws Exception;
    File getMountLocation();
    void waitForDownloadToFinish()throws Exception;
}
