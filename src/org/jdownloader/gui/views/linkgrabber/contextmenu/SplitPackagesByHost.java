package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR;
import org.jdownloader.translate._JDT;

public class SplitPackagesByHost extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements ActionContext {

    /**
     * 
     */
    private static final long serialVersionUID = 2636706677433058054L;

    public SplitPackagesByHost() {
        super();
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    private LocationInList location      = LocationInList.AFTER_SELECTION;
    private boolean        mergePackages = false;

    @Customizer(name = "Merge Packages before splitting?")
    public boolean isMergePackages() {
        return mergePackages;
    }

    public void setMergePackages(boolean mergePackages) {
        this.mergePackages = mergePackages;
    }

    @Customizer(name = "If Merging, ask for new Downloadfolder and package name?")
    public boolean isAskForNewDownloadFolderAndPackageName() {
        return askForNewDownloadFolderAndPackageName;
    }

    public void setAskForNewDownloadFolderAndPackageName(boolean askForNewDownloadFolderIfMerging) {
        this.askForNewDownloadFolderAndPackageName = askForNewDownloadFolderIfMerging;
    }

    private boolean askForNewDownloadFolderAndPackageName = true;

    @Customizer(name = "Add package at")
    public LocationInList getLocation() {
        return location;
    }

    public void setLocation(LocationInList location) {
        this.location = location;
    }

    public void actionPerformed(ActionEvent e) {
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                String newName = null;
                String newDownloadFolder = null;
                if (isMergePackages()) {
                    if (isAskForNewDownloadFolderAndPackageName()) {
                        try {
                            final NewPackageDialog d = new NewPackageDialog(getSelection());

                            Dialog.getInstance().showDialog(d);

                            newName = d.getName();
                            newDownloadFolder = d.getDownloadFolder();
                            if (StringUtils.isEmpty(newName)) {
                                return null;
                            }
                        } catch (Throwable e) {
                            return null;
                        }
                    } else {
                        newName = "";
                        newDownloadFolder = getSelection().getFirstPackage().getRawDownloadFolder();
                    }
                }
                final HashMap<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>> splitMap = new HashMap<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>>();
                int insertAt = -1;
                switch (getLocation()) {
                case BEFORE_SELECTION:
                    insertAt = Integer.MAX_VALUE;
                }
                for (AbstractNode child : getSelection().getChildren()) {
                    if (child instanceof CrawledLink) {
                        final CrawledLink cL = (CrawledLink) child;
                        final CrawledPackage parent = isMergePackages() ? null : cL.getParentNode();

                        HashMap<String, ArrayList<CrawledLink>> parentMap = splitMap.get(parent);
                        if (parentMap == null) {
                            parentMap = new HashMap<String, ArrayList<CrawledLink>>();
                            splitMap.put(parent, parentMap);
                        }
                        final String host = cL.getDomainInfo().getTld();
                        ArrayList<CrawledLink> hostList = parentMap.get(host);
                        if (hostList == null) {
                            hostList = new ArrayList<CrawledLink>();
                            parentMap.put(host, hostList);
                        }
                        hostList.add(cL);
                        switch (getLocation()) {
                        case AFTER_SELECTION:
                            insertAt = Math.max(insertAt, LinkCollector.getInstance().indexOf(((CrawledLink) child).getParentNode()) + 1);
                            break;
                        case BEFORE_SELECTION:
                            insertAt = Math.min(insertAt, LinkCollector.getInstance().indexOf(((CrawledLink) child).getParentNode()));
                            break;
                        case END_OF_LIST:
                            insertAt = -1;
                            break;
                        case TOP_OF_LIST:
                            insertAt = 0;
                            break;
                        }
                    }
                }
                if (insertAt == Integer.MAX_VALUE) {
                    insertAt = 0;
                }
                final String nameFactory = JsonConfig.create(LinkgrabberSettings.class).getSplitPackageNameFactoryPattern();
                final boolean merge = JsonConfig.create(LinkgrabberSettings.class).isSplitPackageMergeEnabled();
                final HashMap<String, CrawledPackage> mergedPackages = new HashMap<String, CrawledPackage>();
                final Iterator<Entry<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>>> it = splitMap.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>> next = it.next();
                    // sourcePackage may be null
                    final CrawledPackage sourcePackage = next.getKey();
                    final HashMap<String, ArrayList<CrawledLink>> items = next.getValue();
                    final Iterator<Entry<String, ArrayList<CrawledLink>>> it2 = items.entrySet().iterator();
                    while (it2.hasNext()) {
                        final Entry<String, ArrayList<CrawledLink>> next2 = it2.next();
                        final String host = next2.getKey();
                        final String newPackageName = getNewPackageName(nameFactory, sourcePackage == null ? newName : sourcePackage.getName(), host);
                        final CrawledPackage newPkg;
                        if (merge) {
                            CrawledPackage destPackage = mergedPackages.get(newPackageName);
                            if (destPackage == null) {
                                destPackage = new CrawledPackage();
                                destPackage.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
                                if (sourcePackage != null) {
                                    sourcePackage.copyPropertiesTo(destPackage);
                                } else {
                                    destPackage.setDownloadFolder(newDownloadFolder);
                                }
                                destPackage.setName(newPackageName);
                                mergedPackages.put(newPackageName, destPackage);
                            }
                            newPkg = destPackage;
                        } else {
                            newPkg = new CrawledPackage();
                            newPkg.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
                            if (sourcePackage != null) {
                                sourcePackage.copyPropertiesTo(newPkg);
                            } else {
                                newPkg.setDownloadFolder(newDownloadFolder);
                            }
                            newPkg.setName(newPackageName);
                        }
                        LinkCollector.getInstance().moveOrAddAt(newPkg, next2.getValue(), 0, insertAt);
                        insertAt++;
                    }
                }
                return null;
            }
        });
    }

    public String getNewPackageName(String nameFactory, String oldPackageName, String host) {
        if (StringUtils.isEmpty(nameFactory)) {
            if (!StringUtils.isEmpty(oldPackageName)) {
                return oldPackageName;
            }
            return host;
        }
        if (!StringUtils.isEmpty(oldPackageName)) {
            nameFactory = nameFactory.replaceAll("\\{PACKAGENAME\\}", oldPackageName);
        } else {
            nameFactory = nameFactory.replaceAll("\\{PACKAGENAME\\}", _JDT._.LinkCollector_addCrawledLink_variouspackage());
        }
        nameFactory = nameFactory.replaceAll("\\{HOSTNAME\\}", host);
        return nameFactory;
    }
}
