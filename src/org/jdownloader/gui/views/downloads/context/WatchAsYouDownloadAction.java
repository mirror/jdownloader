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
package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.neembuu.NeembuuExtension;
import org.jdownloader.gui.translate._GUI;

/**
 * 
 * @author Shashank Tulsyan
 */
public class WatchAsYouDownloadAction extends ContextMenuAction {

    private final ArrayList<FilePackage>  fps;
    private final boolean                 canHandle;
    private final ArrayList<DownloadLink> dls = new ArrayList<DownloadLink>();

    public WatchAsYouDownloadAction(ArrayList<FilePackage> fps) {
        this.fps = fps;
        if (fps != null && fps.size() > 0) {
            canHandle = NeembuuExtension.canHandle(fps);
            for (FilePackage fp : fps) {
                if (fp == null) continue;// ignore empty entries.
                fp.setProperty(NeembuuExtension.WATCH_AS_YOU_DOWNLOAD_KEY, canHandle);
                dls.addAll(fp.getChildren());
            }
        } else
            canHandle = false;

        init();
    }

    @Override
    protected String getIcon() {
        return "mediaplayer";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_watch_as_you_download() + (fps.size() > 1 ? " (" + fps.size() + ")" : "");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().forceDownload(dls);
    }

    @Override
    public boolean isEnabled() {
        return fps != null && fps.size() > 0 && canHandle;
    }
}
