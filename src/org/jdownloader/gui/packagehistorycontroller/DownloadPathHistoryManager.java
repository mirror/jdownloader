package org.jdownloader.gui.packagehistorycontroller;

import java.util.LinkedHashSet;
import java.util.List;

import org.appwork.utils.Lists;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class DownloadPathHistoryManager extends HistoryManager<DownloadPath> {
    private static final DownloadPathHistoryManager INSTANCE = new DownloadPathHistoryManager();

    /**
     * get the only existing instance of PackageHistoryManager. This is a singleton
     * 
     * @return
     */
    public static DownloadPathHistoryManager getInstance() {
        return DownloadPathHistoryManager.INSTANCE;
    }

    /**
     * Create a new instance of PackageHistoryManager. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private DownloadPathHistoryManager() {
        super(CFG_LINKGRABBER.CFG.getDownloadDestinationHistory(), 25);

    }

    @Override
    public void add(String packageName) {
        CFG_LINKGRABBER.CFG.setLatestDownloadDestinationFolder(packageName);
        super.add(packageName);
    }

    @Override
    protected void save(List<DownloadPath> list) {
        CFG_LINKGRABBER.CFG.setDownloadDestinationHistory(list);
    }

    @Override
    protected DownloadPath createNew(String name) {
        return new DownloadPath(name);
    }

    public List<String> listPathes(String... strings) {
        List<DownloadPath> l = list();
        LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        if (strings != null) {
            for (String s : strings) {
                if (StringUtils.isNotEmpty(s)) dupe.add(s);
            }
        }
        for (DownloadPath p : l) {
            if (p != null && StringUtils.isNotEmpty(p.getName())) dupe.add(p.getName());
        }
        return Lists.unique(dupe);
    }
}
