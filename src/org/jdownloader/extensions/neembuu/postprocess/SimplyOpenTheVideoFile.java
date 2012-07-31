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
package org.jdownloader.extensions.neembuu.postprocess;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import jpfm.DirectoryStream;
import jpfm.FileAttributesProvider;
import jpfm.FileType;
import jpfm.fs.BasicFileSystem;

import org.appwork.utils.logging.Log;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.extensions.neembuu.DownloadSession;
import org.jdownloader.extensions.neembuu.NB_VirtualFileSystem;
import org.jdownloader.extensions.neembuu.NeembuuExtension;
/**
 * 
 * @author Shashank Tulsyan
 */
public class SimplyOpenTheVideoFile implements PostProcessor {

    private static String[] knownExtension = { "avi", "mpg", "mpeg", "webm", "mp4", "mp3", "rmvb", "mkv", "flv", "wma", "wmv", "ogg", "ogm", "flac" };

    // @Override
    public boolean canHandle(List<DownloadSession> sessions) {
        return true;
    }

    // @Override
    public boolean handle(List<DownloadSession> sessions, NB_VirtualFileSystem bfs, String mountLocation) {
        DirectoryStream root = (DirectoryStream) bfs.getRootAttributes();

        String vlcPath = null;// todo : put internal vlc path here
        if (vlcPath != null) {
            if (vlcPath.trim().length() == 0) {
                vlcPath = null;
            }
        }

        return findAndOpenVideo(root, mountLocation, bfs);
    }

    private boolean findAndOpenVideo(DirectoryStream ds, String path, BasicFileSystem bfs) {
        Log.L.info(" inside " + ds);
        for (FileAttributesProvider fap : ds) {
            if (fap.getFileType() == FileType.FOLDER) {
                return findAndOpenVideo((DirectoryStream) fap, path + File.separatorChar + fap.getName(), bfs);
            } else if (canBeOpenedInMediaPlayer(fap.getName())) {
                try {
                    File f = new File(path, fap.getName());
                    if(!tryOpeningUsingVLC(f))
                        java.awt.Desktop.getDesktop().open(f);
                    return true;
                } catch (Exception a) {
                    Log.L.log(Level.SEVERE,"Could not open the file",a);
                    return false;
                }
            } else {
                Log.L.info("cannot open " + fap.getName());
            }
        }
        return false;
    }

    public static boolean tryOpeningUsingVLC(File f){
        String vlcLoc = NeembuuExtension.getInstance().getVlcLocation();
        if(vlcLoc!=null){
            try{
                ProcessBuilder pb = ProcessBuilderFactory.create(vlcLoc,f.getAbsolutePath());
                Process p = pb.start();
                return true;
            }catch(Exception any){
                return false;
            }
        }
        return false;
    }
    
    private boolean canBeOpenedInMediaPlayer(String name) {
        name = name.toLowerCase();
        for (int i = 0; i < knownExtension.length; i++) {
            if (name.endsWith(knownExtension[i])) { return true; }
        }
        return false;
    }
    
    
}
