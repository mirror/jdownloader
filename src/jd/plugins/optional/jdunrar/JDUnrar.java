//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSE the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.optional.jdunrar;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDUnrar extends PluginOptional implements ControlListener {

    public static final String CODER = "JD-Team";

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    /**
     * Wird als reihe für anstehende extracthjobs verwendet
     */
    private ArrayList<DownloadLink> queue;
    /**
     * Ist der startpart schon fertig, aber noch nicht alle anderen archivteile,
     * wird der link auf die wartequeue geschoben
     */
    private ArrayList<DownloadLink> waitQueue;

    public JDUnrar(PluginWrapper wrapper) {
        super(wrapper);
        this.queue = new ArrayList<DownloadLink>();
        this.waitQueue = new ArrayList<DownloadLink>();
        initConfig();
    }

    /**
     * das controllevent fängt heruntergeladene file ab und wertet sie aus
     */
    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);

        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) { return; }
            DownloadLink link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                if (link.getFilePackage().isExtractAfterDownload()) {
                    if (getArchivePartType(link) == JDUnrarConstants.MULTIPART_START_PART || getArchivePartType(link) == JDUnrarConstants.SINGLE_PART_ARCHIVE) {
                        if (archiveIsComplete(link)) {
                            this.addToQueue(link);
                        } else {
                            this.addToWaitQueue(link);
                        }
                    } else {
                        checkWaitQueue();
                    }

                }
            }
        }

    }

    /**
     * prüft die Warteschlange ob nun archive komplett sind und entpackt werden
     * können.
     * 
     */
    private void checkWaitQueue() {
        synchronized (waitQueue) {
            for (int i = waitQueue.size() - 1; i >= 0; i--) {
                if (archiveIsComplete(waitQueue.get(i))) {
                    this.addToQueue(waitQueue.remove(i));
                }
            }
        }

    }
/**
 * Fügt downloadlinks, bei denen der startart zwar schon geladen ist, aber die folgeparts noch nicht zu einer wartequeue 
 * @TODO: Diese Wartequeue muss sessionübergreifend gespeichert werden.
 * @param link
 */
    private void addToWaitQueue(DownloadLink link) {
        synchronized (waitQueue) {
            waitQueue.add(link);
        }
    }

    /**
     * Prüft im zugehörigem Filepackage, ob noch downloadlinks vom archiv
     * ungeladen sind.
     * 
     * @param link
     * @return
     */
    private boolean archiveIsComplete(DownloadLink link) {
        DownloadLink l;
        String pattern = link.getFileOutput().replaceAll(".*part[0-9]+.rar$", "");
        pattern = pattern.replaceAll(".*rar$", "");
        for (int i = 0; i < link.getFilePackage().size(); i++) {
            l = link.getFilePackage().get(i);
            if (l.getFileOutput().startsWith(pattern) && !l.getLinkStatus().hasStatus(LinkStatus.FINISHED)) return false;
        }
        return true;
    }

    /**
     * prüft um welchen archivtyp es sich handelt. Es wird
     * JDUnrarConstants.MULTIPART_START_PART
     * JDUnrarConstants.SINGLE_PART_ARCHIVE JDUnrarConstants.NO_RAR_ARCHIVE
     * JDUnrarConstants.NO_START_PART
     * 
     * @param link
     * @return
     */
    private int getArchivePartType(DownloadLink link) {
        if (link.getFileOutput().matches(".*part[0]*[1].rar$")) return JDUnrarConstants.MULTIPART_START_PART;
        if (!link.getFileOutput().matches(".*part[0-9]+.rar$") && link.getFileOutput().matches(".*rar$")) { return JDUnrarConstants.SINGLE_PART_ARCHIVE; }
        if (!link.getFileOutput().matches(".*rar$")) { return JDUnrarConstants.NO_RAR_ARCHIVE; }
        return JDUnrarConstants.NO_START_PART;
    }

    /**
     * Fügt einen Link der Extractqueue hinzu
     * 
     * @param link
     */
    private void addToQueue(DownloadLink link) {
        synchronized (queue) {
            this.queue.add(link);
        }
        this.startExtraction();

    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private void startExtraction() {
        // TODO Auto-generated method stub

    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdunrar.name", "JD-Unrar");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2851 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        return true;

    }

    public void initConfig() {

    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

}