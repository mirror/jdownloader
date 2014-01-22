package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

public class ConfirmHashValuesAction extends CustomizableTableContextAppAction implements ActionContext, ExtTableListener, ExtTableModelListener {

    private IncludedSelectionSetup includedSelection;

    public ConfirmHashValuesAction() {
        super(true, true);
        setIconKey(IconKey.ICON_HASHSUM);

        setName(_GUI._.ConfirmHashValuesAction());
        addContextSetup(includedSelection = new IncludedSelectionSetup(LinkGrabberTable.getInstance(), this, this));
    }

    @Override
    protected void initTableContext(boolean empty, boolean selection) {
        super.initTableContext(empty, selection);
    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        PackageControllerTable<?, ?> table = LinkGrabberTable.getInstance();
        final List<CrawledLink> links = new ArrayList<CrawledLink>();

        final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTable.getInstance().getSelectionInfo();
        switch (includedSelection.getSelectionType()) {
        case NONE:
            return;

        case SELECTED:
            links.addAll(selection.getChildren());

            break;
        case UNSELECTED:
            final List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> filters = LinkGrabberTableModel.getInstance().getEnabledTableFilters();
            LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                @Override
                public int returnMaxResults() {
                    return 0;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    if (!selection.contains(node)) {

                        if (true) {
                            for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> filter : filters) {
                                if (filter.isFiltered(node)) { return false; }
                            }
                        }

                        links.add(node);
                    }
                    return false;
                }
            });
            break;
        case ALL:

            links.addAll(LinkGrabberTable.getInstance().getSelectionInfo(false, true).getChildren());
        }

        if (links.size() == 0) return;

        HashMap<String, List<CrawledLink>> map = new HashMap<String, List<CrawledLink>>();

        for (CrawledLink cl : links) {
            List<CrawledLink> list = map.get(cl.getName());
            if (list == null) {
                list = new ArrayList<CrawledLink>();
                map.put(cl.getName(), list);
            }

            list.add(cl);
        }

        main: for (Entry<String, List<CrawledLink>> se : map.entrySet()) {

            List<CrawledLink> list = se.getValue();

            String md5 = null;
            String sha = null;

            for (CrawledLink cl : list) {
                if (cl.getDownloadLink().getMD5Hash() != null) {
                    if (md5 != null && !StringUtils.equalsIgnoreCase(md5, cl.getDownloadLink().getMD5Hash())) {
                        // hashes do not match
                        continue main;
                    } else {
                        md5 = cl.getDownloadLink().getMD5Hash();
                    }
                }

                if (cl.getDownloadLink().getSha1Hash() != null) {
                    if (sha != null && !StringUtils.equalsIgnoreCase(sha, cl.getDownloadLink().getSha1Hash())) {
                        // hashes do not match
                        continue main;
                    } else {
                        sha = cl.getDownloadLink().getSha1Hash();
                    }
                }
            }

            for (CrawledLink cl : list) {
                if (md5 != null) cl.getDownloadLink().setMD5Hash(md5);
                if (sha != null) cl.getDownloadLink().setSha1Hash(sha);
            }
        }

    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {

    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
    }

}
