package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.translate._JDT;

public class SplitPackagesByHost extends AppAction {

    /**
     * 
     */
    private static final long                          serialVersionUID = 2636706677433058054L;
    private SelectionInfo<CrawledPackage, CrawledLink> si;

    public SplitPackagesByHost(SelectionInfo<CrawledPackage, CrawledLink> si) {
        this.si = si;
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            @Override
            public void run() {
                HashMap<String, java.util.List<CrawledLink>> splitMap = new HashMap<String, java.util.List<CrawledLink>>();

                CrawledPackage samePkg = null;
                boolean samePackage = true;
                for (AbstractNode child : si.getChildren()) {
                    if (child instanceof CrawledLink) {
                        CrawledLink cL = (CrawledLink) child;
                        if (samePkg == null) {
                            samePkg = cL.getParentNode();
                        } else if (cL.getParentNode() != samePkg) {
                            samePackage = false;
                        }
                        java.util.List<CrawledLink> map = splitMap.get(cL.getHost());
                        if (map == null) {
                            map = new ArrayList<CrawledLink>();
                            splitMap.put(cL.getHost(), map);
                        }
                        map.add(cL);
                    }
                }
                if (!samePackage) {
                    samePkg = null;
                }
                Iterator<Entry<String, java.util.List<CrawledLink>>> it = splitMap.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, java.util.List<CrawledLink>> next = it.next();
                    String host = next.getKey();
                    final java.util.List<CrawledLink> links = next.getValue();
                    final CrawledPackage newPkg = new CrawledPackage();
                    newPkg.setExpanded(true);
                    if (samePkg != null) {
                        samePkg.copyPropertiesTo(newPkg);
                        newPkg.setName(getNewPackageName(samePkg.getName(), host));
                    } else {
                        newPkg.setName(getNewPackageName(null, host));
                    }
                    IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>() {

                        @Override
                        protected Object run() {
                            LinkCollector.getInstance().moveOrAddAt(newPkg, links, -1);
                            return null;
                        }

                    });
                }
            }
        }, true);
    }

    public String getNewPackageName(String oldPackageName, String host) {
        String nameFactory = JsonConfig.create(LinkgrabberSettings.class).getSplitPackageNameFactoryPattern();
        if (StringUtils.isEmpty(nameFactory)) {
            if (!StringUtils.isEmpty(oldPackageName)) return oldPackageName;
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
