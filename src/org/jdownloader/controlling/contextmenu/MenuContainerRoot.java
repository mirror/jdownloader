package org.jdownloader.controlling.contextmenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.extensions.ExtensionNotLoadedException;

public class MenuContainerRoot extends MenuContainer implements Storable {
    private int version;

    public MenuContainerRoot(/* Storable */) {

    }

    public void setSource(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void validate() {
        validate(this);
    }

    /**
     * Validates the items, and removes invalid entries. replaces generic entries with an actual class instance
     * 
     * @param container
     */
    private boolean validate(MenuItemData container) {
        container._setValidated(true);
        boolean ret = true;
        container._setRoot(_getRoot());
        main: while (true) {
            if (container.getItems() != null) {

                HashMap<MenuItemData, MenuItemData> replaceMap = new HashMap<MenuItemData, MenuItemData>();
                HashSet<Object> set = new HashSet<Object>();
                MenuItemData last = null;
                for (int i = 0; i < container.getItems().size(); i++) {
                    MenuItemData mid = container.getItems().get(i);
                    MenuItemData lr = null;
                    try {
                        try {
                            lr = mid.createValidatedItem();
                            if (lr.getActionData() != null) {
                                lr.createAction(null);
                            }
                        } catch (ClassCurrentlyNotAvailableException e) {
                            // extension not loaded or anything like this.
                            mid._setValidateException(e);
                            lr = mid;
                        }
                        lr._setRoot(_getRoot());
                        if (lr instanceof SeperatorData && (i == 0 || i == container.getItems().size() - 1)) {

                            container.getItems().remove(i);
                            ret = false;
                            continue main;
                        }
                        if (lr instanceof SeperatorData && last instanceof SeperatorData) {

                            container.getItems().remove(i);
                            ret = false;
                            continue main;
                        }

                        if (!set.add(lr._getIdentifier()) && !(lr instanceof SeperatorData) && lr.getType() == Type.ACTION) {

                            container.getItems().remove(i);
                            ret = false;
                            continue main;

                        }
                        if (lr != mid) {
                            // let's replace
                            replaceMap.put(mid, lr);

                        }
                        last = lr;

                    } catch (Exception e) {

                        container.getItems().remove(i);
                        ret = false;
                        continue main;
                        // if (itemsToRemove == null) itemsToRemove = new ArrayList<MenuItemData>();
                        // itemsToRemove.add(mid);
                        // e.printStackTrace();

                    }

                }

                for (int i = 0; i < container.getItems().size(); i++) {
                    MenuItemData mid = container.getItems().get(i);
                    MenuItemData rep = replaceMap.remove(mid);
                    if (rep != null) {
                        container.getItems().set(i, rep);
                        mid = rep;
                    }
                    ret &= validate(mid);

                }
            }
            return ret;
        }

    }

    public MenuContainerRoot _getRoot() {
        return this;
    }

    private void addBranch(MenuItemData menuItemData, MenuItemData lastNode) {
        boolean added = false;
        for (MenuItemData mu : menuItemData.getItems()) {
            if (mu.getActionData() != null) continue;
            if (StringUtils.equals(mu.getClassName(), lastNode.getClassName())) {
                // subfolder found

                if (lastNode.getItems() != null && lastNode.getItems().size() > 0) {

                    addBranch(mu, lastNode.getItems().get(0));
                    added = true;
                } else {
                    return;
                }

            }
        }
        if (!added) {
            menuItemData.getItems().add(lastNode);

        }
    }

    /**
     * add A path
     * 
     * @param path
     * @throws ExtensionNotLoadedException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void add(List<MenuItemData> path) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ExtensionNotLoadedException {

        MenuItemData addAt = this;
        MenuItemData c;
        MenuItemData parent = null;
        main: for (int i = 0; i < path.size(); i++) {
            c = path.get(i);
            try {
                if (c instanceof MenuContainerRoot) {

                    continue;
                }
                Collection<String> ids = addAt._getItemIdentifiers();

                System.out.println(parent);

                if (c.getType() == Type.CONTAINER) {

                    for (MenuItemData mu : addAt.getItems()) {
                        // if (mu.getActionData() != null) continue;
                        if (StringUtils.equals(mu._getIdentifier(), c._getIdentifier())) {
                            // subfolder found
                            addAt = mu;
                            continue main;

                        }
                    }

                } else {
                    if (ids.contains(c._getIdentifier())) {
                        break;
                    }
                }
                int index = parent.getItems().indexOf(c);
                MenuItemData newItem = createInstance(c);
                if (i < path.size() - 1) {
                    // only of the last component is not a container
                    newItem.setItems(new ArrayList<MenuItemData>());
                }

                List<MenuItemData> above = parent.getItems().subList(0, index);
                List<MenuItemData> below = parent.getItems().subList(index + 1, parent.getItems().size());
                index = searchBestPosition(addAt.getItems(), above, below);

                addAt.getItems().add(index, newItem);

                if (newItem.getType() == Type.CONTAINER) {
                    addAt = newItem;
                }
            } finally {
                parent = c;
            }
        }
        // MenuItemData parent=null;
        // for (int i = path.size() - 2; i >= 0; i--) {
        //
        // MenuItemData node = path.get(i);
        //
        // MenuItemData ret;
        //
        // ret = createInstance(node);
        // if (ret instanceof MenuContainerRoot){
        // parent=ret;
        // break;
        // }
        // ret.setItems(new ArrayList<MenuItemData>());
        // ret.add(lastNode);
        // lastNode = ret;
        //
        // }
        // MenuItemData addAt = this;
        // addBranch(this, lastNode);

    }

    private int searchBestPosition(ArrayList<MenuItemData> items, List<MenuItemData> above, List<MenuItemData> below) {

        ArrayList<Object> identList = new ArrayList<Object>();

        for (int i = 0; i < items.size(); i++) {
            identList.add(items.get(i)._getIdentifier());

        }
        int bestMatch = Integer.MAX_VALUE;
        int bestIndex = -1;

        if (above.size() == 0 && below.size() == 0) return 0;
        if (above.size() == 0) {
            for (int b = 0; b < below.size(); b++) {
                MenuItemData bN = below.get(b);
                int bIndex = identList.indexOf(bN._getIdentifier());
                if (bIndex >= 0) { return bIndex; }

            }
        } else if (below.size() == 0) {
            for (int a = above.size() - 1; a >= 0; a--) {

                MenuItemData aN = above.get(a);
                int aIndex = identList.indexOf(aN._getIdentifier());
                if (aIndex >= 0) { return aIndex + 1; }

            }

        }
        boolean lastAWasSep = false;
        boolean lastBwasSep = false;

        main: for (int a = above.size() - 1; a >= 0; a--) {

            MenuItemData aN = above.get(a);

            if (aN instanceof SeperatorData) {
                lastAWasSep = true;
                continue;
            }
            try {
                for (int b = 0; b < below.size(); b++) {
                    if (bestMatch <= 1 + a + b) break main;
                    MenuItemData bN = below.get(b);
                    if (bN instanceof SeperatorData) {
                        lastBwasSep = true;
                        continue;
                    }
                    try {
                        int aIndex = identList.indexOf(aN._getIdentifier());

                        int bIndex = identList.indexOf(bN._getIdentifier());
                        if (lastAWasSep && aIndex >= 0) aIndex++;
                        if (lastBwasSep && bIndex > 0) bIndex--;
                        if (aIndex >= 0 && bIndex >= 0) {
                            int dist = Math.abs(bIndex - aIndex) + a + b;

                            if (dist < bestMatch) {
                                bestMatch = dist;
                                bestIndex = Math.min(aIndex, bIndex) + 1;
                            }

                        } else if (aIndex >= 0) {
                            int dist = 1000 + a + b;

                            if (dist < bestMatch) {
                                bestMatch = dist;
                                bestIndex = aIndex + 1;
                            }
                        } else if (bIndex >= 0) {
                            int dist = 1000 + a + b;

                            if (dist < bestMatch) {
                                bestMatch = dist;
                                bestIndex = bIndex;
                            }
                        }
                    } finally {
                        lastBwasSep = false;
                    }
                }
            } finally {
                lastAWasSep = false;
            }
        }
        if (bestIndex < 0) {
            //
            bestIndex = items.size();
        }

        return bestIndex;
    }

}
