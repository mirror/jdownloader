package org.jdownloader.gui.packagehistorycontroller;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class PackageHistoryManager extends HistoryManager<PackageHistoryEntry> {
    private static final PackageHistoryManager INSTANCE = new PackageHistoryManager();

    /**
     * get the only existing instance of PackageHistoryManager. This is a singleton
     * 
     * @return
     */
    public static PackageHistoryManager getInstance() {
        return PackageHistoryManager.INSTANCE;
    }

    /**
     * Create a new instance of PackageHistoryManager. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private PackageHistoryManager() {
        super(CFG_LINKGRABBER.CFG.getPackageNameHistory(), 25);

    }

    @Override
    protected void save(List<PackageHistoryEntry> list) {
        CFG_LINKGRABBER.CFG.setPackageNameHistory(list);

    }

    @Override
    protected PackageHistoryEntry createNew(String name) {
        return new PackageHistoryEntry(name);
    }

    public List<PackageHistoryEntry> list(PackageHistoryEntry packageHistoryEntry) {
        List<PackageHistoryEntry> l = list();
        ArrayList<PackageHistoryEntry> ret = new ArrayList<PackageHistoryEntry>();
        ret.add(packageHistoryEntry);
        for (PackageHistoryEntry phe : l) {
            if (!phe.getName().equals(packageHistoryEntry.getName())) {
                ret.add(phe);
            }
        }

        return ret;
    }
}
