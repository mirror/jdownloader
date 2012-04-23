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
 * 
 * @author Shashank Tulsyan
 */
public class HJSplitsHandler implements PostProcessor {

    // @Override
    public boolean canHandle(List<DownloadSession> sessions) {
        if (sessions.size() < 1) return false;
        synchronized (sessions) {
            for (DownloadSession jdds : sessions) {
                int index = -2;
                String n = jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getName();
                try {
                    index = Integer.parseInt(n.substring(n.length() - 3));
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
    public boolean handle(List<DownloadSession> sessions, NB_VirtualFileSystem bfs, String mntLoc) {
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

        bfs.cascadeMount(cascadableSplitFS);

        // remove splited files .001 , .002 ... from virtual folder
        // to avoid confusion
        NB_VirtualFileSystem fileSystem = (NB_VirtualFileSystem) bfs;
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
