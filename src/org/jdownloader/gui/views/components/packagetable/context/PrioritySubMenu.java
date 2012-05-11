package org.jdownloader.gui.views.components.packagetable.context;

import javax.swing.JMenu;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class PrioritySubMenu<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends JMenu {

    private SelectionInfo<PackageType, ChildrenType> si;

    public PrioritySubMenu(SelectionInfo<PackageType, ChildrenType> si) {
        super(_GUI._.PriorityAction_PriorityAction_());
        setIcon(Priority.HIGHER.loadIcon(18));
        this.si = si;

        // setIconKey("priority");
        this.add(new PriorityActionEntry(Priority.LOWER, si));
        this.add(new PriorityActionEntry(Priority.DEFAULT, si));
        this.add(new PriorityActionEntry(Priority.HIGH, si));

        this.add(new PriorityActionEntry(Priority.HIGHER, si));
        this.add(new PriorityActionEntry(Priority.HIGHEST, si));

    }

}
