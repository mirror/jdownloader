package org.jdownloader.gui.views.downloads.table.linkproperties;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JComponent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class URLEditorAction extends AppAction {

    private DownloadLink            contextObject;
    private ArrayList<DownloadLink> links;
    private ArrayList<FilePackage>  fps;

    public URLEditorAction(DownloadLink contextObject, ArrayList<DownloadLink> links, ArrayList<FilePackage> fps) {

        setName(_GUI._.ContextMenuFactory_createPopup_url());
        setSmallIcon(NewTheme.I().getIcon("url", 20));
        this.contextObject = contextObject;
        this.links = links;
        this.fps = fps;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            final LinkURLEditor comp = new LinkURLEditor(contextObject, links, fps);
            Dialog.getInstance().showDialog(new AbstractDialog<Object>(Dialog.STYLE_HIDE_ICON | Dialog.BUTTONS_HIDE_OK, _GUI._.ContextMenuFactory_createPopup_url(), null, _GUI._.literally_save(), null) {

                @Override
                protected Object createReturnValue() {

                    return null;
                }

                @Override
                protected boolean isResizable() {
                    return true;
                }

                protected void setReturnmask(final boolean b) {
                    super.setReturnmask(b);
                    if (b) {

                        comp.save();
                    }
                }

                @Override
                public JComponent layoutDialogContent() {
                    return comp;
                }
            });
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
