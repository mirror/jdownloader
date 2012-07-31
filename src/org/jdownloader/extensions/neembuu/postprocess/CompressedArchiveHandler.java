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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jpfm.FileAttributesProvider;
import jpfm.fs.splitfs.CascadableSplitFS;
import jpfm.volume.vector.VectorRootDirectory;

import org.jdownloader.extensions.neembuu.DownloadSession;
import org.jdownloader.extensions.neembuu.NB_VirtualFileSystem;

/**
 * This starts a unrar/extraction process on the virtual compressed archive file.
 * The advantage of extracting virtual file, instead of split is
 * that, extraction speed is not more than download speed.
 * If extraction speed is more than download speed, unrar will
 * simply quit, and then the unrar process will have to be restarted.
 * The resultant file is a real file in this case. 
 * It can be made a virtual one, that would be useful if the archive/RAR being
 * extracted contains another archive/RAR which must be extracted.
 * Handling such a case is possible, but currently it is not supported,
 * as it will encourage such complex ways of packaging videos.
 * Seeking is not possible in the resultant file.
 * Seeking require a rarindex file to be available.
 * For a 700MB rar, a rarindex is around 200KB.
 * Rarindex maps compressed fileoffset to uncompressed file offset.
 * It also stores value of offsets from which extraction can start.
 * Creation of rarindex requires the complete file to be present.
 * So someone with the complete file, must make the rar index and share 
 * it on some website. This would require creation of a distribution
 * system like opensubtitles.org ... the only difference being that
 * it would be for rarindex files. Finding a matching rarindex for a given
 * rar file would require effort from the user. It is unlikely user will
 * follow such a complex procedure. Anyway mostly people watch videos 
 * linearly, which is still possible using this. They can 
 * seek video as long they remain in the region extracted,
 * they cannot forward beyond region extracted.
 * @author Shashank Tulsyan
 */
public class CompressedArchiveHandler implements PostProcessor{

    //@Override
    public boolean canHandle(List<DownloadSession> sessions) {
        if (sessions.size() < 0){return false;}
        synchronized (sessions) {
            boolean oldStyle = !sessions.get(0).getWatchAsYouDownloadSession().getSeekableConnectionFile().getName().toLowerCase().endsWith(".rar");
            for (DownloadSession jdds : sessions) {
                int index = -2;
                String n = jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getName();
                try {
                    if(oldStyle)
                        index = Integer.parseInt(n.substring(n.length() - 2));
                    else {
                        n = n.substring(n.indexOf(".part")+5); 
                        n = n.substring(0,n.indexOf(".rar"));
                        index = Integer.parseInt(n);
                    }
                } catch (Exception a) {
                    return false;
                }
                if (jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getDownloadConstrainHandler().index() < 0) {
                    jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getDownloadConstrainHandler().setIndex(index);
                }
                if (index < 0) { return false; }
            }
        }
        return true;
    }

    // @Override
    public boolean handle(List<DownloadSession> sessions, NB_VirtualFileSystem fileSystem, String mntLoc) {
        if (!canHandle(sessions)) { throw new IllegalArgumentException("Cannot handle"); }

        Set<FileAttributesProvider> files = new LinkedHashSet<FileAttributesProvider>();
        synchronized (sessions) {
            for (DownloadSession jdds : sessions) {
                files.add(jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile());
            }
        }

        String name = sessions.get(0).getDownloadLink().getFinalFileName();
        name = name.substring(0, name.lastIndexOf("."));

        CascadableSplitFS.CascadableSplitFSProvider cascadableSplitFS = new CascadableSplitFS.CascadableSplitFSProvider(files, name);

        fileSystem.cascadeMount(cascadableSplitFS);

        // remove splited files .part1.rar , .part2.rar ... from virtual folder
        // to avoid confusion
        VectorRootDirectory vrd = (VectorRootDirectory) fileSystem.getRootDirectory();
        synchronized (sessions) {
            for (DownloadSession jdds : sessions) {
                vrd.remove(jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile());
                // the file does not exist, therefore the open button should be
                // disabled
                jdds.getWatchAsYouDownloadSession().getHttpFilePanel().enableOpenButton(false);
            }
        }

        return true;
    }
    
}
