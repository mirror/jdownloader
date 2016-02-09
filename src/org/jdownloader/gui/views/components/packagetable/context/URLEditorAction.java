package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.linkproperties.LinkURLEditor;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

public class URLEditorAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> {

    public URLEditorAction() {
        super();

        setName(_GUI.T.ContextMenuFactory_createPopup_url());
        setSmallIcon(new AbstractIcon(IconKey.ICON_URL, 20));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            final LinkURLEditor comp = new LinkURLEditor(getSelection());
            Dialog.getInstance().showDialog(new AbstractDialog<Object>(Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_OK, _GUI.T.ContextMenuFactory_createPopup_url(), null, _GUI.T.literally_save(), null) {

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
