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
import jd.plugins.DownloadLink;
import jpfm.fs.BasicFileSystem;
import org.jdownloader.extensions.neembuu.DownloadSession;

/**
 * 
 * @author Shashank Tulsyan
 */
public class PostProcessors {

    public static void postProcess(List<DownloadSession> sessions, BasicFileSystem bfs, String mountLocation) {
        boolean success = true;
        HJSplitsHandler hjsh = new HJSplitsHandler();
        if (hjsh.canHandle(sessions)) {
            hjsh.handle(sessions, bfs, mountLocation);
        }

        SimplyOpenTheVideoFile simplyOpenTheVideoFile = new SimplyOpenTheVideoFile();

        if (!simplyOpenTheVideoFile.handle(sessions, bfs, mountLocation)) {
            // open mount location can we cannot find even one file that we can
            // open
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File(mountLocation));
            } catch (Exception a) {
                // ignore
            }
        }
    }
    
    public static void downloadComplete(DownloadLink downloadLink){
        File f = new File(downloadLink.getFileOutput());
        if (f.exists()) {
            if(!SimplyOpenTheVideoFile.tryOpeningUsingVLC(f)){
                try{
                    java.awt.Desktop.getDesktop().open(f);
                }catch(Exception ignore){
                    
                }
            }
        }
    }
}
