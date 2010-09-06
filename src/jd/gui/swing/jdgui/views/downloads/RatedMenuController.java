package jd.gui.swing.jdgui.views.downloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RatedMenuController extends ArrayList<RatedMenuItem> {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private ArrayList<RatedMenuItem> sub;
    private ArrayList<RatedMenuItem> main;

    public ArrayList<RatedMenuItem> getMain() {
        return this.main;
    }

    public ArrayList<RatedMenuItem> getSub() {
        return this.sub;
    }

    @SuppressWarnings("unchecked")
    public void init(final int i) {
        // first lets find the i best rated items
        final ArrayList<RatedMenuItem> sorted = (ArrayList<RatedMenuItem>) this.clone();
        Collections.sort(sorted, new Comparator<RatedMenuItem>() {

            public int compare(final RatedMenuItem o1, final RatedMenuItem o2) {
                return o1.getRating() == o2.getRating() ? 0 : o1.getRating() > o2.getRating() ? 1 : -1;
            }

        });

        while (sorted.size() > i) {
            sorted.remove(0);
        }

        // sorted now contains all items in main menu
        this.sub = new ArrayList<RatedMenuItem>();
        this.main = new ArrayList<RatedMenuItem>();
        // split all items in main and submenu, but KEEP the original order
        for (final RatedMenuItem item : this) {
            if (item.isSeparator()) {
                if (this.main.size() > 0 && !this.main.get(this.main.size() - 1).isSeparator()) {
                    this.main.add(item);
                }
                if (this.sub.size() > 0 && !this.sub.get(this.sub.size() - 1).isSeparator()) {
                    this.sub.add(item);
                }
            } else {
                if (sorted.contains(item)) {
                    this.main.add(item);
                } else {
                    this.sub.add(item);
                }
            }
        }
        if (this.main.size() > 0 && !this.main.get(this.main.size() - 1).isSeparator()) {
            this.main.remove(this.main.size() - 1);
        }
        if (this.sub.size() > 0 && !this.sub.get(this.sub.size() - 1).isSeparator()) {
            this.sub.remove(this.sub.size() - 1);
        }

    }

    /**
     * adds the item, and sorts the list based on the ratings
     */
    // public boolean add(final RatedMenuItem ratedMenuItem) {
    // for (int i = 0; i < this.size(); i++) {
    // if (this.get(i).getRating() > ratedMenuItem.getRating()) {
    //
    // this.add(i, ratedMenuItem);
    // return true;
    //
    // }
    // }
    // return super.add(ratedMenuItem);
    //
    // }

}
