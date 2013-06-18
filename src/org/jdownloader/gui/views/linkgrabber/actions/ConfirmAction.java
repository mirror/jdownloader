package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AppAction {

    /**
     * 
     */
    private static final long                          serialVersionUID = -3937346180905569896L;

    private boolean                                    autostart;
    private SelectionInfo<CrawledPackage, CrawledLink> si;

    public boolean isAutostart() {
        return autostart;
    }

    public ConfirmAction setAutostart(boolean autostart) {
        this.autostart = autostart;
        if (autostart) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -2, 0, 8, 10)));
        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setIconKey(IconKey.ICON_GO_NEXT);
        }
        return this;
    }

    public ConfirmAction(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {

        setAutostart(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue());

        this.si = selectionInfo;

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {

                try {

                    // this validation step also copies the passwords from the CRawledlinks in the archive settings
                    for (Archive a : new ValidateArchiveAction<CrawledPackage, CrawledLink>((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension(), si).getArchives()) {
                        final DummyArchive da = a.createDummyArchive();
                        if (!da.isComplete()) {

                            ConfirmDialog d = new ConfirmDialog(0, _GUI._.ConfirmAction_run_incomplete_archive_title_(a.getName()), _GUI._.ConfirmAction_run_incomplete_archive_msg(), NewTheme.I().getIcon("stop", 32), _GUI._.ConfirmAction_run_incomplete_archive_continue(), null) {
                                protected DefaultButtonPanel createBottomButtonPanel() {

                                    DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[]0");
                                    ret.add(new JButton(new AppAction() {
                                        {
                                            setName(_GUI._.ConfirmAction_run_incomplete_archive_details());
                                        }

                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                Dialog.getInstance().showDialog(new DummyArchiveDialog(da));
                                            } catch (DialogClosedException e1) {
                                                e1.printStackTrace();
                                            } catch (DialogCanceledException e1) {
                                                e1.printStackTrace();
                                            }
                                        }

                                    }), "gapleft 32");
                                    return ret;
                                }

                            };

                            Dialog.getInstance().showDialog(d);
                        }

                    }

                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    Log.exception(e);
                }
                boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_ADD_AT_TOP.getValue();
                java.util.List<FilePackage> fpkgs = new ArrayList<FilePackage>();
                java.util.List<CrawledLink> clinks = new ArrayList<CrawledLink>();
                for (AbstractNode node : si.getRawSelection()) {
                    if (node instanceof CrawledPackage) {
                        /* first convert all CrawledPackages to FilePackages */
                        List<CrawledLink> links = new ArrayList<CrawledLink>(((CrawledPackage) node).getView().getItems());
                        java.util.List<FilePackage> packages = LinkCollector.getInstance().convert(links, true);
                        if (packages != null) fpkgs.addAll(packages);
                    } else if (node instanceof CrawledLink) {
                        /* collect all CrawledLinks */
                        clinks.add((CrawledLink) node);
                    }
                }
                /* convert all selected CrawledLinks to FilePackages */

                java.util.List<FilePackage> frets = LinkCollector.getInstance().convert(clinks, true);
                if (frets != null) fpkgs.addAll(frets);
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

                if (JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled()) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            JDGui.getInstance().requestPanel(UserIF.Panels.DOWNLOADLIST, null);
                        }
                    };

                }
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        return !si.isEmpty();
    }

}
