package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

public class CollapseExpandAllAction extends SelectionBasedToolbarAction {
    public CollapseExpandAllAction() {
        setIconKey(IconKey.ICON_LIST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getTable() != null) {
            SelectionInfo<?, ?> selection = getTable().getSelectionInfo(false, true);
            boolean allexpaned = true;
            ArrayList<AbstractPackageNode> list = new ArrayList<AbstractPackageNode>();
            for (PackageView<?, ?> p : selection.getPackageViews()) {
                if (!p.getPackage().isExpanded()) {
                    allexpaned = false;

                }
                list.add(p.getPackage());

            }

            getTable().getModel().setFilePackageExpand(!allexpaned, list.toArray(new AbstractPackageNode[] {}));
        }
    }

    @Override
    protected String createTooltip() {
        return _GUI._.CollapseExpandAllAction_CollapseExpandAllAction();
    }

    @Override
    public void onKeyModifier(int parameter) {
    }

    @Override
    protected void onSelectionUpdate() {

        if (getTable() == null) {
            setEnabled(false);
            return;
        } else {
            setEnabled(true);
        }

    }

}
