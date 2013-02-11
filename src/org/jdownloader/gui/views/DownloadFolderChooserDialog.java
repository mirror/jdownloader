package org.jdownloader.gui.views;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledPackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.ExtFileSystemView;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.HomeFolder;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.userio.NewUIO;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.DownloadPath;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

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
        if (path != null && path.getName().equals(CrawledPackage.PACKAGETAG)) {
            subfolder = true;
            this.path = path.getParentFile();
        }
        setView(JsonConfig.create(GraphicalUserInterfaceSettings.class).getFileChooserView());
    }

    protected Icon getDirectoryIcon(Icon ret, File f) {
        if (f.getName().equals("Desktop")) {
            return NewTheme.I().getIcon("desktop", 18);
        } else if ((f.getPath().equals(ExtFileSystemView.VIRTUAL_NETWORKFOLDER_XP) || f.getPath().equals(ExtFileSystemView.VIRTUAL_NETWORKFOLDER)) || (f.getPath().startsWith("\\") && f.getPath().indexOf("\\", 2) < 0)) {
            //
            return NewTheme.I().getIcon("network-idle", 18);
        } else if (f.getPath().length() == 3 && f.getPath().charAt(1) == ':' && (f.getPath().charAt(0) + "").matches("[a-zA-Z]{1}")) {
            //
            return NewTheme.I().getIcon("harddisk", 16);
        } else if (f instanceof HomeFolder) {

            if (((HomeFolder) f).getName().equals(HomeFolder.DOWNLOADS)) {
                return NewTheme.I().getIcon("download", 18);
            } else if (((HomeFolder) f).getName().equals(HomeFolder.MUSIC)) {
                return NewTheme.I().getIcon("audio", 18);
            } else if (((HomeFolder) f).getName().equals(HomeFolder.PICTURES)) {
                return NewTheme.I().getIcon("image", 18);
            } else if (((HomeFolder) f).getName().equals(HomeFolder.DOCUMENTS)) {
                return NewTheme.I().getIcon("document", 18);
            } else if (((HomeFolder) f).getName().equals(HomeFolder.VIDEOS)) {
                //
                return NewTheme.I().getIcon("video", 18);

            }

        }

        return NewTheme.I().getIcon("folder", 18);
    }

    @Override
    protected File[] createReturnValue() {

        JsonConfig.create(GraphicalUserInterfaceSettings.class).setFileChooserView(getView());
        if (isMultiSelection()) {
            File[] files = fc.getSelectedFiles();
            return files;
        } else {
            File f = fc.getSelectedFile();
            if (f == null) {
                String path = getText();
                if (!StringUtils.isEmpty(path)) {
                    // if (path.start)

                    f = new File(path);

                    if (isSambaFolder(f)) return null;
                } else {
                    return null;
                }

            }
            if (cbPackage != null && cbPackage.isSelected()) {
                return new File[] { new File(f, CrawledPackage.PACKAGETAG) };
            } else {
                if (f.exists() && !f.canWrite()) return null;
                return new File[] { f };
            }

        }
    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel ret = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        ExtTextField lbl = new ExtTextField();
        if (path != null) {
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
        }
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

    public static File open(File path, boolean packager, String title) throws DialogClosedException, DialogCanceledException {

        if (path != null && !CrossSystem.isAbsolutePath(path.getPath())) {
            path = new File(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), path.getPath());
        }
        final File path2 = path;
        final DownloadFolderChooserDialog d = new DownloadFolderChooserDialog(path, title, _GUI._.OpenDownloadFolderAction_actionPerformed_save_(), null);
        d.setPackageSubFolderSelectionVisible(packager);
        if (CrossSystem.isOpenFileSupported()) {
            d.setLeftActions(new AppAction() {
                {
                    setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(d.getSelection()[0] == null ? path2 : d.getSelection()[0]);
                }

            });
        }

        d.setQuickSelectionList(DownloadPath.loadList(path != null ? path.getAbsolutePath() : null));
        d.setPreSelection(path);
        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);

        final File[] dest = Dialog.getInstance().showDialog(d);
        if (dest[0].getParentFile() != null && !dest[0].getParentFile().exists() && !dest[0].exists()) {
            handleNonExistingFolders(dest[0]);
        }
        if (!isDownloadFolderValid(dest[0])) { return null; }
        DownloadPath.saveList(dest[0].getAbsolutePath());
        return dest[0];
    }

    public static void handleNonExistingFolders(File file) {
        try {

            Dialog.getInstance().showConfirmDialog(0, _GUI._.DownloadFolderChooserDialog_handleNonExistingFolders_title_(), _GUI._.DownloadFolderChooserDialog_handleNonExistingFolders_msg_(file.getAbsolutePath()));
            if (!file.mkdirs()) {
                NewUIO.I().showErrorMessage(_GUI._.DownloadFolderChooserDialog_handleNonExistingFolders_couldnotcreatefolder(file.getAbsolutePath()));
            }
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private void setPackageSubFolderSelectionVisible(boolean packager) {
        this.packageSubFolderSelectionVisible = packager;
    }

    public boolean isPackageSubFolderSelectionVisible() {
        return packageSubFolderSelectionVisible;
    }

}
