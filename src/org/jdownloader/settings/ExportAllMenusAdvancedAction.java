package org.jdownloader.settings;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.filechooser.FileFilter;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.gui.jdtrayicon.MenuManagerTrayIcon;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.settings.advanced.AdvancedAction;

public class ExportAllMenusAdvancedAction implements AdvancedAction {
    public ExportAllMenusAdvancedAction() {

    }

    @Override
    public String getName() {
        return "Export";
    }

    @Override
    public void actionPerformed() {

        ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.ExportAllMenusAdvancedAction_actionPerformed(), null, null);
        d.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {

                return _GUI._.lit_directory();
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);
        d.setMultiSelection(false);

        d.setStorageID("menus");
        d.setType(FileChooserType.SAVE_DIALOG);
        try {
            Dialog.getInstance().showDialog(d);

            File saveTo = d.getSelectedFile();
            File file = null;
            int i = 0;
            while (file == null || file.exists()) {

                file = new File(saveTo, "JDownloader Menustructure " + i);

                i++;
            }
            file.mkdirs();
            MenuManagerDownloadTabBottomBar.getInstance().exportTo(file);
            MenuManagerDownloadTableContext.getInstance().exportTo(file);
            MenuManagerLinkgrabberTabBottombar.getInstance().exportTo(file);
            MenuManagerLinkgrabberTableContext.getInstance().exportTo(file);
            MenuManagerMainToolbar.getInstance().exportTo(file);
            MenuManagerTrayIcon.getInstance().exportTo(file);
            MenuManagerMainmenu.getInstance().exportTo(file);
            CrossSystem.openFile(file);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
        } catch (IOException e1) {
            Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
        }
    }
}
