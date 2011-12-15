package org.jdownloader.gui.views.linkgrabber.actions;

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
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -3937346180905569896L;
    private ArrayList<AbstractNode> values;
    private boolean                 autostart;

    public ConfirmAction(boolean autostart, ArrayList<AbstractNode> arrayList) {
        if ((LinkFilterSettings.LINKGRABBER_AUTO_START_ENABLED.getValue() && !autostart) || (autostart && !LinkFilterSettings.LINKGRABBER_AUTO_START_ENABLED.getValue())) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 12);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 9, 10)));
            this.autostart = true;
        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setSmallIcon(NewTheme.I().getIcon("add", 20));
            this.autostart = false;
        }
        this.values = arrayList;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean addTop = LinkFilterSettings.LINKGRABBER_ADD_AT_TOP.getValue();
                ArrayList<FilePackage> fpkgs = new ArrayList<FilePackage>();
                ArrayList<CrawledLink> clinks = new ArrayList<CrawledLink>();
                for (AbstractNode node : values) {
                    if (node instanceof CrawledPackage) {
                        /* first convert all CrawledPackages to FilePackages */
                        CrawledPackage pkg = (CrawledPackage) node;
                        ArrayList<CrawledLink> links = new ArrayList<CrawledLink>(((CrawledPackage) node).getView());
                        ArrayList<FilePackage> packages = LinkCollector.getInstance().removeAndConvert(links);
                        if (packages != null) fpkgs.addAll(packages);
                    } else if (node instanceof CrawledLink) {
                        /* collect all CrawledLinks */
                        clinks.add((CrawledLink) node);
                    }
                }
                /* convert all selected CrawledLinks to FilePackages */
                ArrayList<FilePackage> frets = LinkCollector.getInstance().removeAndConvert(clinks);
                if (frets != null) fpkgs.addAll(frets);
                /* add the converted FilePackages to DownloadController */
                DownloadController.getInstance().addAllAt(fpkgs, addTop ? 0 : -(fpkgs.size() + 10));
                if (autostart) {
                    /* start DownloadWatchDog if wanted */
                    DownloadWatchDog.getInstance().startDownloads();
                }
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        return values != null && values.size() > 0;
    }

}
