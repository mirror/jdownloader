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
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.translate._JDT;

public class SplitPackagesByHost extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = 2636706677433058054L;

    public SplitPackagesByHost() {

        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    public void actionPerformed(ActionEvent e) {
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final HashMap<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>> splitMap = new HashMap<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>>();

                for (AbstractNode child : getSelection().getChildren()) {
                    if (child instanceof CrawledLink) {
                        final CrawledLink cL = (CrawledLink) child;
                        final CrawledPackage parent = cL.getParentNode();
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
                    }
                }
                final String nameFactory = JsonConfig.create(LinkgrabberSettings.class).getSplitPackageNameFactoryPattern();
                final boolean merge = JsonConfig.create(LinkgrabberSettings.class).isSplitPackageMergeEnabled();
                final HashMap<String, CrawledPackage> mergedPackages = new HashMap<String, CrawledPackage>();
                final Iterator<Entry<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>>> it = splitMap.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<CrawledPackage, HashMap<String, ArrayList<CrawledLink>>> next = it.next();
                    final CrawledPackage sourcePackage = next.getKey();
                    final HashMap<String, ArrayList<CrawledLink>> items = next.getValue();
                    final Iterator<Entry<String, ArrayList<CrawledLink>>> it2 = items.entrySet().iterator();
                    while (it2.hasNext()) {
                        final Entry<String, ArrayList<CrawledLink>> next2 = it2.next();
                        final String host = next2.getKey();
                        final String newPackageName = getNewPackageName(nameFactory, sourcePackage.getName(), host);
                        final CrawledPackage newPkg;
                        if (merge) {
                            CrawledPackage destPackage = mergedPackages.get(newPackageName);
                            if (destPackage == null) {
                                destPackage = new CrawledPackage();
                                destPackage.setExpanded(true);
                                sourcePackage.copyPropertiesTo(destPackage);
                                destPackage.setName(newPackageName);
                                mergedPackages.put(newPackageName, destPackage);
                            }
                            newPkg = destPackage;
                        } else {
                            newPkg = new CrawledPackage();
                            newPkg.setExpanded(true);
                            sourcePackage.copyPropertiesTo(newPkg);
                            newPkg.setName(newPackageName);
                        }
                        LinkCollector.getInstance().moveOrAddAt(newPkg, next2.getValue(), -1);
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
