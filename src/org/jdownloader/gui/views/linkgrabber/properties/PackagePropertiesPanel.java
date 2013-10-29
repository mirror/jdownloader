package org.jdownloader.gui.views.linkgrabber.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryEntry;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryManager;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.settings.GeneralSettings;

public class PackagePropertiesPanel extends LinkPropertiesPanel {

    @Override
    protected void addFilename(int height, MigPanel p) {

    }

    @Override
    protected void saveInEDT() {
        if (currentPackage != null) {

            Priority priop = priority.getSelectedItem();
            if (priop != null) {

                boolean readL = currentPackage.getModifyLock().readLock();
                try {
                    for (CrawledLink dl : currentPackage.getChildren()) {
                        dl.setPriority(priop);
                    }
                } finally {
                    currentPackage.getModifyLock().readUnlock(readL);
                }

            }
            currentPackage.setComment(comment.getText());

            if (!currentPackage.getName().equals(packagename.getText())) {
                currentPackage.setName(packagename.getText());
                PackageHistoryManager.getInstance().add(packagename.getText());
            }

            if (currentArchive != null) {

                ArrayList<String> passwords = null;
                String txt = password.getText().trim();
                if (txt.startsWith("[") && txt.endsWith("]")) {
                    passwords = JSonStorage.restoreFromString(password.getText(), new TypeRef<ArrayList<String>>() {
                    }, null);
                }
                if (passwords != null && passwords.size() > 0) {
                    currentArchive.getSettings().setPasswords(new HashSet<String>(passwords));
                } else {
                    HashSet<String> hs = new HashSet<String>();
                    if (StringUtils.isNotEmpty(password.getText())) hs.add(password.getText().trim());
                    currentArchive.getSettings().setPasswords(hs);
                }
                currentArchive.getSettings().setAutoExtract(autoExtract.getSelectedItem());

            }
            if (!LinkTreeUtils.getRawDownloadDirectory(currentPackage).equals(new File(destination.getPath()))) {
                currentPackage.setDownloadFolder(destination.getPath());
                DownloadPathHistoryManager.getInstance().add(destination.getPath());
            }

        }
    }

    @Override
    protected void addChecksum(int height, MigPanel p) {

    }

    @Override
    protected void addDownloadPassword(int height, MigPanel p) {

    }

    @Override
    protected void updateInEDT(CrawledLink link, CrawledPackage pkg) {

        if (!comment.hasFocus()) {

            comment.setText(pkg.getComment());

        }
        List<String> pathlist = DownloadPathHistoryManager.getInstance().listPathes(LinkTreeUtils.getRawDownloadDirectory(pkg).getAbsolutePath(), org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());

        if (!destination.getDestination().hasFocus()) {
            destination.setQuickSelectionList(pathlist);
            destination.setFile(LinkTreeUtils.getRawDownloadDirectory(pkg));
        }

        if (!packagename.hasFocus()) {
            packagename.setList(PackageHistoryManager.getInstance().list(new PackageHistoryEntry(pkg.getName())));
            packagename.setSelectedItem(new PackageHistoryEntry(pkg.getName()));
        }
        if (pkg != currentPackage) {
            if (!password.hasFocus()) password.setText("");
        }
        Priority p = pkg.getView().getHighestPriority();

        currentPackage = pkg;
        if (p != pkg.getView().getLowestPriority()) {

            priority.setSelectedItem(null);
        } else {
            priority.setSelectedItem(p);
        }

    }

    // @Override
    // protected void updateArchiveInEDT(Archive archive) {
    // currentArchive = archive;
    // if (!password.hasFocus()) {
    // if (currentArchive.getSettings().getPasswords() == null || currentArchive.getSettings().getPasswords().size() == 0) {
    // password.setText(null);
    // } else if (currentArchive.getSettings().getPasswords().size() == 1) {
    //
    // password.setText(currentArchive.getSettings().getPasswords().iterator().next());
    // } else {
    // password.setText(JSonStorage.toString(currentArchive.getSettings().getPasswords()));
    // }
    // }
    // autoExtract.setSelectedItem(currentArchive.getSettings().getAutoExtract());
    // }

}
