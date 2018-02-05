package jd.gui.swing.jdgui.views.settings.components;

import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jd.controlling.downloadcontroller.BadDestinationException;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.PathTooLongException;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;

import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.translate._JDT;

public class FolderChooser extends PathChooser implements SettingsComponent {
    /**
     *
     */
    private static final long                     serialVersionUID = 1L;
    private StateUpdateEventSender<FolderChooser> eventSender;
    private boolean                               setting;
    private String                                originalPath     = null;
    private String                                initialPath      = null;

    public FolderChooser() {
        super("FolderChooser", true);
        eventSender = new StateUpdateEventSender<FolderChooser>();
        this.txt.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
                }
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
                }
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
                }
            }
        });
        this.destination.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                final List<String> list = DownloadPathHistoryManager.getInstance().listPaths(initialPath);
                destination.setList(list);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        // CFG_LINKGRABBER.DOWNLOAD_DESTINATION_HISTORY.getEventSender().addListener(, true);
    }

    @Override
    public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new VariableAction(txt, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
        menu.add(new VariableAction(txt, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
        return menu;
    }

    public String getConstraints() {
        return null;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

    public void setPath(final String downloadDestination) {
        if (initialPath == null) {
            initialPath = downloadDestination;
        }
        originalPath = downloadDestination;
        super.setPath(downloadDestination);
    }

    public void setText(String t) {
        setting = true;
        try {
            setPath(t);
        } finally {
            setting = false;
        }
    }

    @Override
    public File doFileChooser() {
        File ret;
        try {
            ret = DownloadFolderChooserDialog.open(new File(txt.getText()), true, _JDT.T.gui_setting_folderchooser_title());
            return ret;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

    public String getText() {
        File file = getFile();
        if (file == null) {
            return null;
        }
        file = checkPath(file, originalPath == null ? null : new File(originalPath));
        if (file == null) {
            return null;
        }
        return file.getAbsolutePath();
    }

    public static File checkPath(File file, File presetPath) {
        String path = file.getAbsolutePath();
        File checkPath = file;
        int index = path.indexOf("<jd:");
        if (index >= 0) {
            path = path.substring(0, index);
            checkPath = new File(path);
        }
        File forbidden = null;
        try {
            DownloadWatchDog.getInstance().validateDestination(checkPath);
        } catch (PathTooLongException e) {
            forbidden = e.getFile();
        } catch (BadDestinationException e) {
            forbidden = e.getFile();
        }
        if (forbidden != null) {
            final File finalForbidden = forbidden;
            new EDTHelper<Void>() {
                @Override
                public Void edtRun() {
                    UIOManager.I().showErrorMessage(_GUI.T.DownloadFolderChooserDialog_handleNonExistingFolders_couldnotcreatefolder(finalForbidden.getAbsolutePath()));
                    return null;
                }
            }.start(true);
            return null;
        }
        if (!checkPath.exists()) {
            if (presetPath != null && presetPath.equals(checkPath)) {
                //
                return file;
            }
            final File createFolder;
            if (index >= 0) {
                createFolder = checkPath;
            } else {
                createFolder = file;
            }
            if (!createFolder.exists()) {
                if (UIOManager.I().showConfirmDialog(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadFolderChooserDialog_handleNonExistingFolders_title_(), _GUI.T.DownloadFolderChooserDialog_handleNonExistingFolders_msg_(createFolder.getAbsolutePath()), null, null, null, FolderChooser.class.getCanonicalName())) {
                    if (!FileCreationManager.getInstance().mkdir(createFolder)) {
                        UIOManager.I().showErrorMessage(_GUI.T.DownloadFolderChooserDialog_handleNonExistingFolders_couldnotcreatefolder(createFolder.getAbsolutePath()));
                        return presetPath;
                    }
                }
            }
        }
        return file;
    }
}
