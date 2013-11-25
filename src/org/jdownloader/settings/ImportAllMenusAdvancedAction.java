package org.jdownloader.settings;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.filechooser.FileFilter;

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

public class ImportAllMenusAdvancedAction implements AdvancedAction {
    public ImportAllMenusAdvancedAction() {

    }

    @Override
    public String getName() {
        return "Import";
    }

    @Override
    public void actionPerformed() {

        ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.ImportAllMenusAdvancedAction_actionPerformed(), null, null);
        d.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {

                return _GUI._.lit_directory();
            }

            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(MenuManagerDownloadTabBottomBar.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerDownloadTableContext.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerLinkgrabberTabBottombar.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerLinkgrabberTableContext.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerMainToolbar.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerTrayIcon.getInstance().getFileExtension())) return true;
                if (f.getName().endsWith(MenuManagerMainmenu.getInstance().getFileExtension())) return true;
                return f.isDirectory();
            }
        });

        d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        d.setMultiSelection(true);

        d.setStorageID("menus");
        d.setType(FileChooserType.SAVE_DIALOG);
        try {
            Dialog.getInstance().showDialog(d);

            for (File f : d.getSelection()) {
                if (f.getName().endsWith(MenuManagerDownloadTabBottomBar.getInstance().getFileExtension())) MenuManagerDownloadTabBottomBar.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerDownloadTableContext.getInstance().getFileExtension())) MenuManagerDownloadTableContext.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerLinkgrabberTabBottombar.getInstance().getFileExtension())) MenuManagerLinkgrabberTabBottombar.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerLinkgrabberTableContext.getInstance().getFileExtension())) MenuManagerLinkgrabberTableContext.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerMainToolbar.getInstance().getFileExtension())) MenuManagerMainToolbar.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerTrayIcon.getInstance().getFileExtension())) MenuManagerTrayIcon.getInstance().importFrom(f);
                if (f.getName().endsWith(MenuManagerMainmenu.getInstance().getFileExtension())) MenuManagerMainmenu.getInstance().importFrom(f);
            }

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
