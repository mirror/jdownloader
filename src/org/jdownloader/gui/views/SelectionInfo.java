package org.jdownloader.gui.views;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.BinaryLogic;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class SelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    private AbstractNode                             contextObject;
    private List<ChildrenType>                       selectedChildren;
    private List<AbstractNode>                       rawSelection;
    private List<PackageType>                        allPackages;
    private List<PackageType>                        fullPackages;
    private HashSet<AbstractNode>                    rawMap;
    private HashMap<PackageType, List<ChildrenType>> incompleteSelectecPackages;
    private List<PackageType>                        incompletePackages;
    private MouseEvent                               mouseEvent;

    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }

    public KeyEvent getKeyEvent() {
        return keyEvent;
    }

    private KeyEvent                                          keyEvent;
    private PackageControllerTable<PackageType, ChildrenType> table;

    public SelectionInfo(List<AbstractNode> selection) {
        this(null, selection, null);

    }

    public SelectionInfo(AbstractNode contextObject, List<AbstractNode> selection) {
        this(contextObject, selection, null);

    }

    public SelectionInfo(AbstractNode clicked) {
        this(clicked, pack(clicked));
    }

    public SelectionInfo(AbstractNode contextObject, List<AbstractNode> selection, MouseEvent event) {
        this(contextObject, selection, event, null);
    }

    public SelectionInfo(AbstractNode contextObject, List<AbstractNode> selection, MouseEvent event, KeyEvent kEvent) {

        this(contextObject, selection, event, kEvent, null);
    }

    public SelectionInfo(AbstractNode contextObject, List<AbstractNode> selection, MouseEvent event, KeyEvent kEvent, PackageControllerTable<PackageType, ChildrenType> table) {

        this.contextObject = contextObject;
        rawSelection = selection;
        selectedChildren = new ArrayList<ChildrenType>();
        allPackages = new ArrayList<PackageType>();
        fullPackages = new ArrayList<PackageType>();
        incompletePackages = new ArrayList<PackageType>();
        incompleteSelectecPackages = new HashMap<PackageType, List<ChildrenType>>();

        rawMap = new HashSet<AbstractNode>();
        this.mouseEvent = event;
        this.keyEvent = kEvent;
        this.table = table;
        agregate();
    }

    private static List<AbstractNode> pack(AbstractNode clicked) {
        ArrayList<AbstractNode> ret = new ArrayList<AbstractNode>();
        ret.add(clicked);
        return ret;
    }

    public List<PackageType> getIncompletePackages() {
        return incompletePackages;
    }

    public List<PackageType> getAllPackages() {
        return allPackages;
    }

    public List<PackageType> getFullPackages() {
        return fullPackages;
    }

    public void agregate() {
        HashSet<AbstractNode> has = new HashSet<AbstractNode>(rawSelection);
        HashSet<ChildrenType> ret = new HashSet<ChildrenType>();
        HashSet<PackageType> allPkg = new HashSet<PackageType>();
        HashSet<PackageType> fullPkg = new HashSet<PackageType>();
        HashSet<PackageType> incPkg = new HashSet<PackageType>();
        for (AbstractNode node : rawSelection) {
            rawMap.add(node);
            if (node instanceof AbstractPackageChildrenNode) {
                ret.add((ChildrenType) node);
                allPkg.add(((ChildrenType) node).getParentNode());
            } else {

                // if we selected a package, and ALL it's links, we want all
                // links
                // if we selected a package, and nly afew links, we probably
                // want only these few links.
                // if we selected a package, and it is NOT expanded, we want all
                // links
                allPkg.add((PackageType) node);
                if (!((PackageType) node).isExpanded()) {
                    // add allTODO
                    List<ChildrenType> childs = ((PackageType) node).getChildren();
                    ret.addAll(childs);
                    fullPkg.add((PackageType) node);

                } else {
                    List<ChildrenType> childs = ((PackageType) node).getChildren();
                    boolean containsNone = true;
                    boolean containsAll = true;
                    for (ChildrenType l : childs) {
                        if (has.contains(l)) {
                            containsNone = false;
                        } else {
                            containsAll = false;
                        }

                    }
                    if (containsAll || containsNone) {
                        ret.addAll(childs);
                        fullPkg.add((PackageType) node);
                    } else {
                        if (incPkg.add((PackageType) node)) {
                            incompleteSelectecPackages.put((PackageType) node, childs);
                        }

                    }
                }
            }
        }
        selectedChildren.addAll(ret);
        allPackages.addAll(allPkg);
        fullPackages.addAll(fullPkg);
        incompletePackages.addAll(incPkg);

    }

    public boolean isPackageContext() {
        return contextObject != null && contextObject instanceof AbstractPackageNode;
    }

    public boolean isChildContext() {
        return contextObject != null && contextObject instanceof AbstractPackageChildrenNode;
    }

    /**
     * if we have packagecontext, this returns the package, else the child's PACKAGE
     * 
     * @return
     */
    public PackageType getContextPackage() {
        if (contextObject == null) throw new BadContextException("Context is null");
        if (isPackageContext()) {
            return (PackageType) contextObject;
        } else {
            return ((ChildrenType) contextObject).getParentNode();
        }

    }

    /**
     * if this object is a childcontext, this returns the child, else throws exception
     * 
     * @return
     */
    public ChildrenType getContextChild() {
        if (isChildContext()) return (ChildrenType) contextObject;

        throw new BadContextException("Not available in Packagecontext");
    }

    /**
     * @see #getContextChild()
     * @return
     */
    public ChildrenType getChild() {
        return getContextChild();
    }

    /**
     * @see #getContextPackage()
     * @return
     */
    public PackageType getPackage() {
        return getContextPackage();
    }

    public List<ChildrenType> getSelectedChildren() {
        return selectedChildren;
    }

    public boolean isEmpty() {
        return selectedChildren == null || selectedChildren.size() == 0;
    }

    public List<AbstractNode> getRawSelection() {
        return rawSelection;
    }

    /**
     * Returns true if the actuall selection had l selected.
     * 
     * @param l
     * @return
     */
    public boolean rawContains(AbstractNode l) {
        return rawMap.contains(l);
    }

    public List<ChildrenType> getSelectedLinksByPackage(PackageType pkg) {
        List<ChildrenType> ret = incompleteSelectecPackages.get(pkg);
        if (ret != null) return ret;
        return pkg.getChildren();

    }

    public AbstractNode getContextObject() {
        return contextObject;
    }

    /**
     * returns true if the shift key has been pressed when generating this instance
     * 
     * @return
     */
    public boolean isShiftDown() {
        if (keyEvent != null && BinaryLogic.containsSome(keyEvent.getModifiers(), ActionEvent.SHIFT_MASK)) { return true; }
        if (mouseEvent != null && mouseEvent.isShiftDown()) return true;
        return false;
    }

}
