package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

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
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
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

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    public ConfirmAction(boolean autostart, SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        if (autostart != org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue()) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -2, 0, 8, 10)));
            this.autostart = true;
        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setSmallIcon(NewTheme.I().getIcon("go-next", 20));
            this.autostart = false;
        }
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
                                protected JPanel createBottomButtonPanel() {

                                    JPanel ret = new JPanel(new MigLayout("ins 0", "[]", "0[]0"));
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
                ArrayList<FilePackage> fpkgs = new ArrayList<FilePackage>();
                ArrayList<CrawledLink> clinks = new ArrayList<CrawledLink>();
                for (AbstractNode node : si.getRawSelection()) {
                    if (node instanceof CrawledPackage) {
                        /* first convert all CrawledPackages to FilePackages */
                        List<CrawledLink> links = new ArrayList<CrawledLink>(((CrawledPackage) node).getView().getItems());
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
