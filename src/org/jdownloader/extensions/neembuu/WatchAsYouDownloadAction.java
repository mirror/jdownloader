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

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

/**
 * 
 * @author Shashank Tulsyan
 */
public class WatchAsYouDownloadAction extends AppAction {

    private final java.util.List<FilePackage> fps;
    private final boolean                     canHandle;

    public WatchAsYouDownloadAction(java.util.List<FilePackage> fps) {
        this.fps = fps;
        boolean canH = false;
        if (fps != null && fps.size() > 0) {
            for (FilePackage fp : fps) {
                if (fp == null) continue;// ignore empty entries.
                if (fp.getProperty(NeembuuExtension.WATCH_AS_YOU_DOWNLOAD_KEY, false) == Boolean.FALSE) break;
            }
            canH = true;
        } else
            canH = false;

        canHandle = canH;
        setIconKey("mediaplayer");
        setName(_GUI._.gui_table_contextmenu_watch_as_you_download() + (fps.size() > 1 ? " (" + fps.size() + ")" : ""));
    }

    public void actionPerformed(ActionEvent e) {
        for (FilePackage fp : fps) {
            if (fp == null) continue;// ignore empty entries.
            fp.setProperty(org.jdownloader.extensions.neembuu.NeembuuExtension.INITIATED_BY_WATCH_ACTION, true);

            // resetting all links, everytime, to ensure that not even one file is ignored
            for (DownloadLink link : fp.getChildren()) {
                if (link.getLinkStatus().isPluginActive()) {
                    /*
                     * download is still active, let DownloadWatchdog handle the reset
                     */
                    DownloadWatchDog.getInstance().resetSingleDownloadController(link.getDownloadLinkController());
                } else {
                    /* we can do the reset ourself */
                    DownloadWatchDog.getInstance().removeIPBlockTimeout(link);
                    DownloadWatchDog.getInstance().removeTempUnavailTimeout(link);
                    link.reset();
                }
            }

            DownloadWatchDog.getInstance().forceDownload((java.util.List<DownloadLink>) fp.getChildren());
        }
    }

    @Override
    public boolean isEnabled() {
        return fps != null && fps.size() > 0 && canHandle;
    }
}
