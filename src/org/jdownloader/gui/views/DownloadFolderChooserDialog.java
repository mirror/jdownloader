package org.jdownloader.gui.views;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledPackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.DownloadPath;

public class DownloadFolderChooserDialog extends ExtFileChooserDialog {

    private javax.swing.JCheckBox cbPackage;
    private File                  path;
    private boolean               packageSubFolderSelectionVisible;
    private boolean               subfolder = false;

    /**
     * @param flag
     * @param title
     * @param okOption
     * @param cancelOption
     */
    public DownloadFolderChooserDialog(File path, String title, String okOption, String cancelOption) {
        super(0, title, okOption, cancelOption);
        this.path = path;
        if (path.getName().equals(CrawledPackage.PACKAGETAG)) {
            subfolder = true;
            this.path = path.getParentFile();
        }
    }

    @Override
    protected File[] createReturnValue() {
        if (isMultiSelection()) {
            File[] files = fc.getSelectedFiles();
            return files;
        } else {
            File f = fc.getSelectedFile();
            if (cbPackage != null && cbPackage.isSelected()) {
                return new File[] { new File(f, CrawledPackage.PACKAGETAG) };
            } else {
                return new File[] { f };
            }

        }
    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel ret = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        ExtTextField lbl = new ExtTextField();
        lbl.setText(_GUI._.OpenDownloadFolderAction_layoutDialogContent_current_(path.getAbsolutePath()));
        lbl.setEditable(false);
        if (CrossSystem.isOpenFileSupported()) {
            ret.add(lbl);

            ret.add(new JButton(new AppAction() {
                {
                    setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(path);
                }

            }), "height 20!");
        } else {
            ret.add(lbl, "spanx");
        }

        ret.add(new JSeparator(), "spanx");
        ret.add(new JLabel(_GUI._.OpenDownloadFolderAction_layoutDialogContent_object_()), "spanx");
        ret.add(super.layoutDialogContent(), "spanx");

        return ret;

    }

    protected void modifiyNamePanel(JPanel namePanel) {
        if (isPackageSubFolderSelectionVisible()) {
            namePanel.setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[][]"));
            namePanel.add(new JLabel(_GUI._.SetDownloadFolderInDownloadTableAction_modifiyNamePanel_package_()));
            cbPackage = new javax.swing.JCheckBox();
            cbPackage.setSelected(subfolder);
            namePanel.add(cbPackage);
        }
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must be an existing folder or at least its parent folder must
     * exist
     * 
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) return false;
        if (file.isDirectory()) return true;
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) return true;
        return false;
    }

    public static File open(final File path, boolean packager, String title) throws DialogClosedException, DialogCanceledException {
        final DownloadFolderChooserDialog d = new DownloadFolderChooserDialog(path, title, _GUI._.OpenDownloadFolderAction_actionPerformed_save_(), null);
        d.setPackageSubFolderSelectionVisible(packager);
        if (CrossSystem.isOpenFileSupported()) {
            d.setLeftActions(new AppAction() {
                {
                    setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(d.getSelection()[0] == null ? path : d.getSelection()[0]);
                }

            });
        }

        d.setQuickSelectionList(DownloadPath.loadList(path.getAbsolutePath()));
        d.setPreSelection(path);
        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);

        final File[] dest = Dialog.getInstance().showDialog(d);

        if (!isDownloadFolderValid(dest[0])) return null;
        DownloadPath.saveList(dest[0].getAbsolutePath());
        return dest[0];
    }

    private void setPackageSubFolderSelectionVisible(boolean packager) {
        this.packageSubFolderSelectionVisible = packager;
    }

    public boolean isPackageSubFolderSelectionVisible() {
        return packageSubFolderSelectionVisible;
    }

}
