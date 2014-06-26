package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.translate._JDT;

public class SplitPackagesByHost extends CustomizableTableContextAppAction<FilePackage, DownloadLink> implements ActionContext {

    /**
     * 
     */
    private static final long serialVersionUID = 2636706677433058054L;

    public SplitPackagesByHost() {
        super();
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    private LocationInList location = LocationInList.AFTER_SELECTION;

    @Customizer(name = "Add package at")
    public LocationInList getLocation() {
        return location;
    }

    public void setLocation(LocationInList location) {
        this.location = location;
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final HashMap<FilePackage, HashMap<String, ArrayList<DownloadLink>>> splitMap = new HashMap<FilePackage, HashMap<String, ArrayList<DownloadLink>>>();
                int insertAt = -1;
                switch (getLocation()) {

                case BEFORE_SELECTION:
                    insertAt = Integer.MAX_VALUE;

                }
                for (AbstractNode child : getSelection().getChildren()) {
                    if (child instanceof DownloadLink) {
                        final DownloadLink cL = (DownloadLink) child;
                        final FilePackage parent = cL.getParentNode();
                        HashMap<String, ArrayList<DownloadLink>> parentMap = splitMap.get(parent);
                        if (parentMap == null) {
                            parentMap = new HashMap<String, ArrayList<DownloadLink>>();
                            splitMap.put(parent, parentMap);
                        }
                        final String host = cL.getDomainInfo().getTld();
                        ArrayList<DownloadLink> hostList = parentMap.get(host);
                        if (hostList == null) {
                            hostList = new ArrayList<DownloadLink>();
                            parentMap.put(host, hostList);
                        }
                        hostList.add(cL);

                        switch (getLocation()) {
                        case AFTER_SELECTION:

                            insertAt = Math.max(insertAt, DownloadController.getInstance().indexOf(((DownloadLink) child).getParentNode()) + 1);

                            break;
                        case BEFORE_SELECTION:

                            insertAt = Math.min(insertAt, DownloadController.getInstance().indexOf(((DownloadLink) child).getParentNode()));

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
                final HashMap<String, FilePackage> mergedPackages = new HashMap<String, FilePackage>();
                final Iterator<Entry<FilePackage, HashMap<String, ArrayList<DownloadLink>>>> it = splitMap.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<FilePackage, HashMap<String, ArrayList<DownloadLink>>> next = it.next();
                    final FilePackage sourcePackage = next.getKey();
                    final HashMap<String, ArrayList<DownloadLink>> items = next.getValue();
                    final Iterator<Entry<String, ArrayList<DownloadLink>>> it2 = items.entrySet().iterator();
                    while (it2.hasNext()) {
                        final Entry<String, ArrayList<DownloadLink>> next2 = it2.next();
                        final String host = next2.getKey();
                        final String newPackageName = getNewPackageName(nameFactory, sourcePackage.getName(), host);
                        final FilePackage newPkg;
                        if (merge) {
                            FilePackage destPackage = mergedPackages.get(newPackageName);
                            if (destPackage == null) {
                                destPackage = FilePackage.getInstance();

                                sourcePackage.copyPropertiesTo(destPackage);
                                destPackage.setName(newPackageName);
                                mergedPackages.put(newPackageName, destPackage);
                            }
                            newPkg = destPackage;
                        } else {
                            newPkg = FilePackage.getInstance();

                            sourcePackage.copyPropertiesTo(newPkg);
                            newPkg.setName(newPackageName);
                        }

                        DownloadController.getInstance().moveOrAddAt(newPkg, next2.getValue(), 0, insertAt);

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
