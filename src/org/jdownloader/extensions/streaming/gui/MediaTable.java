package org.jdownloader.extensions.streaming.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaListController;

public class MediaTable<MediaItemType extends MediaItem> extends BasicJDTable<MediaItemType> {

    public MediaTable(ExtTableModel<MediaItemType> tableModel) {
        super(tableModel);
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, MediaItemType contextObject, final List<MediaItemType> selection, ExtColumn<MediaItemType> column, MouseEvent mouseEvent) {
        popup.add(new AppAction() {
            {
                setName("Refesh Metadata");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MediaArchiveController archive = ((MediaTableModel) getModel()).getExtension().getMediaArchiveController();
                for (MediaItem mi : selection) {
                    archive.refreshMetadata(mi);
                }

            }
        });
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(List<MediaItemType> selectedObjects, KeyEvent evt, boolean direct) {
        try {
            Dialog.getInstance().showConfirmDialog(UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, T._.mediatable_rly_remove_title(), T._.mediatable_rly_remove_msg(selectedObjects.size()));
            ((MediaTableModel<MediaItemType, MediaListController<MediaItemType>>) getModel()).getListController().remove(selectedObjects);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

        return true;
    }

}
