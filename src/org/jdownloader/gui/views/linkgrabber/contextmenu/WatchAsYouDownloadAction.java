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
package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.neembuu.NeembuuExtension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 * 
 * @author Shashank Tulsyan
 */
public class WatchAsYouDownloadAction extends AppAction {
    /**
     *
     */
    private static final long       serialVersionUID = -1123905158192679571L;
    private ArrayList<AbstractNode> values;
    private boolean                 autostart;

    public WatchAsYouDownloadAction(boolean autostart, ArrayList<AbstractNode> arrayList) {
        // if
        // ((org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue()
        // && !autostart) || (autostart &&
        // !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue()))
        // {
        setName(_GUI._.WatchAsYouDownload_WatchAsYouDownloadAction_());
        Image add = NewTheme.I().getImage("mediaplayer", 20);
        Image play = NewTheme.I().getImage("add", 12);
        setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 9, 10)));
        this.autostart = true;
        /*
         * } else { setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
         * setSmallIcon(NewTheme.I().getIcon("add", 20)); this.autostart =
         * false; }
         */
        this.values = arrayList;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_ADD_AT_TOP.getValue();
                ArrayList<FilePackage> fpkgs = new ArrayList<FilePackage>();
                ArrayList<CrawledLink> clinks = new ArrayList<CrawledLink>();
                for (AbstractNode node : values) {
                    if (node instanceof CrawledPackage) {
                        /* first convert all CrawledPackages to FilePackages */
                        ArrayList<CrawledLink> links = new ArrayList<CrawledLink>(((CrawledPackage) node).getView().getItems());
                        ArrayList<FilePackage> packages = LinkCollector.getInstance().removeAndConvert(links);
                        if (packages != null) fpkgs.addAll(packages);
                    } else if (node instanceof CrawledLink) {
                        /* collect all CrawledLinks */
                        clinks.add((CrawledLink) node);
                    }
                }
                /* convert all selected CrawledLinks to FilePackages */
                ArrayList<FilePackage> frets = LinkCollector.getInstance().removeAndConvert(clinks);
                boolean canHandle = false;
                if (frets != null) fpkgs.addAll(frets);
                if (fpkgs != null && fpkgs.size() > 0) {
                    canHandle = NeembuuExtension.canHandle(fpkgs);
                    for (FilePackage fp : fpkgs) {
                        if (fp == null) continue;// ignore empty entries.
                        fp.setProperty(NeembuuExtension.WATCH_AS_YOU_DOWNLOAD_KEY, canHandle);
                    }
                } else
                    canHandle = false;
                /* add the converted FilePackages to DownloadController */
                DownloadController.getInstance().addAllAt(fpkgs, addTop ? 0 : -(fpkgs.size() + 10));
                if (autostart) {
                    IOEQ.add(new Runnable() {

                        public void run() {
                            /* start DownloadWatchDog if wanted */
                            DownloadWatchDog.getInstance().startDownloads();
                        }

                    }, true);
                }
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        if (!NeembuuExtension.isActive()) return false;
        return values != null && values.size() > 0;
    }
}
