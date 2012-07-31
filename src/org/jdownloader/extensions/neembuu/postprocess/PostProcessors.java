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
import java.util.LinkedList;
import java.util.List;

import org.jdownloader.extensions.neembuu.DownloadSession;
import org.jdownloader.extensions.neembuu.NB_VirtualFileSystem;

/**
 * 
 * @author Shashank Tulsyan
 */
public class PostProcessors {
    
    /**
     * This stores the list of layers of virtualization. 
     * Some sad folks on the net upload in extremely irritating fashion.
     * They first rar the file, then zip it, then split it ..... the chain goes on.
     * This is more often done for warez and seldom for videos.
     * Right now we just handle one layer, but theoretically it is possible to go 
     * to any number of layers. 
     */
    private final LinkedList<PostProcessor> list = new LinkedList<PostProcessor>();
    
    private PostProcessors(){
        
    }

    public static PostProcessors postProcess(List<DownloadSession> sessions, NB_VirtualFileSystem nbvfs) {
        PostProcessors postProcessors = new PostProcessors(); 
        
        String mountLocation = sessions.get(0).getWatchAsYouDownloadSession().getMountLocation().getAbsolutePath();
                
        HJSplitsHandler hjsh = new HJSplitsHandler();
        CompressedArchiveHandler cah = new CompressedArchiveHandler();
        if (hjsh.canHandle(sessions)) {
            hjsh.handle(sessions, nbvfs, mountLocation);
            postProcessors.list.add(hjsh);
        }//else if(cah.canHandle(sessions)) {}

        SimplyOpenTheVideoFile simplyOpenTheVideoFile = new SimplyOpenTheVideoFile();

        if (!simplyOpenTheVideoFile.handle(sessions, nbvfs, mountLocation)) {
            // open mount location can we cannot find even one file that we can open
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File(mountLocation));
            } catch (Exception a) {
                // ignore
            }
        }
        
        return postProcessors;
    }
    
    public void downloadComplete(DownloadSession ds){
        for(PostProcessor pp : list){
            if(pp instanceof CompressedArchiveHandler) return;
            if(pp instanceof HJSplitsHandler){
                // We can join all splits, open the video file
                // JD already does that so we ignore it for now.
                // If need arises we shall handle the joining part here.
                // We can also handle the archive extraction part if need arises
                return;
            }
        }
        File f = new File(ds.getDownloadLink().getFileOutput());
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
