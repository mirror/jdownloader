package org.jdownloader.controlling.contextmenu;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class MenuStructure implements Storable {

    private MenuContainerRoot root;

    public MenuContainerRoot getRoot() {
        return root;
    }

    public void setRoot(MenuContainerRoot root) {
        this.root = root;
    }

    public ArrayList<String> getUnused() {
        return unused;
    }

    public void setUnused(ArrayList<String> unused) {
        this.unused = unused;
    }

    private ArrayList<String> unused;

    public MenuStructure(/* Storable */) {

    }

    public MenuStructure(MenuContainerRoot root, ArrayList<String> unused) {
        this.root = root;
        this.unused = unused;
    }

}
