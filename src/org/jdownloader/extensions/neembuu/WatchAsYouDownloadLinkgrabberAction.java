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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 * 
 * @author Shashank Tulsyan
 */
public class WatchAsYouDownloadLinkgrabberAction extends AppAction {
    /**
     *
     */
    private static final long serialVersionUID = -1123905158192679571L;
    private List<CrawledLink> values;
    private boolean           autostart;

    public WatchAsYouDownloadLinkgrabberAction(boolean autostart, List<CrawledLink> arrayList) {
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
         * } else { setName(_GUI._.ConfirmAction_ConfirmAction_context_add()); setSmallIcon(NewTheme.I().getIcon("add", 20)); this.autostart = false; }
         */
        this.values = arrayList;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_ADD_AT_TOP.getValue();
                java.util.List<FilePackage> fpkgs = new ArrayList<FilePackage>();
                java.util.List<CrawledLink> clinks = new ArrayList<CrawledLink>();
                for (AbstractNode node : values) {
                    if (node instanceof CrawledPackage) {
                        /* first convert all CrawledPackages to FilePackages */
                        java.util.List<CrawledLink> links = new ArrayList<CrawledLink>(((CrawledPackage) node).getView().getItems());
                        java.util.List<FilePackage> packages = LinkCollector.getInstance().convert(links, true);
                        if (packages != null) fpkgs.addAll(packages);
                    } else if (node instanceof CrawledLink) {
                        /* collect all CrawledLinks */
                        clinks.add((CrawledLink) node);
                    }
                }
                /* convert all selected CrawledLinks to FilePackages */
                java.util.List<FilePackage> frets = LinkCollector.getInstance().convert(clinks, true);
                boolean canHandle = false;
                if (frets != null) fpkgs.addAll(frets);
                if (fpkgs != null && fpkgs.size() > 0) {
                    canHandle = NeembuuExtension.canHandle(fpkgs);
                    for (FilePackage fp : fpkgs) {
                        if (fp == null) continue;// ignore empty entries.
                        fp.setProperty(NeembuuExtension.WATCH_AS_YOU_DOWNLOAD_KEY, canHandle);
                        fp.setProperty(NeembuuExtension.INITIATED_BY_WATCH_ACTION, canHandle);
                    }
                } else
                    canHandle = false;
                /* add the converted FilePackages to DownloadController */
                DownloadController.getInstance().addAllAt(fpkgs, addTop ? 0 : -(fpkgs.size() + 10));
                if (autostart) {
                    /* start DownloadWatchDog if wanted */
                    DownloadWatchDog.getInstance().startDownloads();
                }
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        if (!NeembuuExtension.isActive()) return false;
        return values != null && values.size() > 0;
    }
}
