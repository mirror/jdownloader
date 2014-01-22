package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class CopyGenericContextAction extends CustomizableTableContextAppAction implements ActionContext {
    private static final String PATTERN_NAME     = "{name}";
    private static final String PATTERN_COMMENT  = "{comment}";
    private static final String PATTERN_SHA256   = "{sha256}";
    private static final String PATTERN_MD5      = "{md5}";
    private static final String PATTERN_FILESIZE = "{filesize}";
    private static final String PATTERN_URL      = "{url}";
    private static final String PATTERN_TYPE     = "{type}";
    private static final String PATTERN_PATH     = "{path}";

    public CopyGenericContextAction() {
        super(true, true);
        setIconKey(IconKey.ICON_COPY);

        setName(_GUI._.CopyGenericContextAction());
        setAccelerator(KeyEvent.VK_C);
    }

    private String patternPackages;

    @Customizer(name = "<html>Pattern for the Packages<br><ul><li>{name}</li><li>{comment}</li><li>{filesize}</li><li>{type}</li><li>{path}</li></ul></html>")
    public String getPatternPackages() {
        return patternPackages;
    }

    public void setPatternPackages(String copyPattern) {
        this.patternPackages = copyPattern;
        setTooltipText(_GUI._.CopyGenericContextAction_tt(getPatternPackages() + " - " + getPatternLinks()));
    }

    private String patternLinks;

    @Customizer(name = "<html>Pattern for the Links<br><ul><li>{name}</li><li>{comment}</li><li>{sha256}</li><li>{md5}</li><li>{filesize}</li><li>{url}</li><li>{type}</li><li>{path}</li></ul></html>")
    public String getPatternLinks() {
        return patternLinks;
    }

    public void setPatternLinks(String patternLinks) {
        this.patternLinks = patternLinks;
        setTooltipText(_GUI._.CopyGenericContextAction_tt(getPatternPackages() + " - " + getPatternLinks()));
    }

    private boolean smartSelection;

    @Customizer(name = "Smartselection")
    public boolean isSmartSelection() {
        return smartSelection;
    }

    public void setSmartSelection(boolean smartSelection) {
        this.smartSelection = smartSelection;
    }

    @Override
    protected void initTableContext(boolean empty, boolean selection) {

    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();
        patternPackages = "";
        patternLinks = PATTERN_TYPE + ";" + PATTERN_NAME + ";" + PATTERN_URL;
        smartSelection = true;
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder sb = new StringBuilder();
        if (isSmartSelection()) {
            for (PackageView<?, ?> pv : getTable().getSelectionInfo().getPackageViews()) {
                AbstractPackageNode<?, ?> pkg = (AbstractPackageNode<?, ?>) pv.getPackage();
                add(sb, pkg);

                List<AbstractNode> childs = (List<AbstractNode>) pv.getChildren();
                for (AbstractNode c : childs) {
                    add(sb, c);
                }

            }
        } else {
            for (AbstractNode pv : getTable().getSelectionInfo().getRawSelection()) {
                add(sb, pv);

            }
        }

        ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
    }

    public void add(StringBuilder sb, AbstractNode pv) {
        String line = null;
        if (pv instanceof FilePackage) {
            line = getPatternPackages();
            FilePackage pkg = (FilePackage) pv;

            FilePackageView fpv = new FilePackageView(pkg);
            fpv.aggregate();
            line = line.replace(PATTERN_TYPE, "Package");
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(pkg)));
            line = line.replace(PATTERN_COMMENT, nulltoString(pkg.getComment()));
            line = line.replace(PATTERN_FILESIZE, nulltoString(fpv.getSize()));
            line = line.replace(PATTERN_MD5, nulltoString(null));
            line = line.replace(PATTERN_NAME, nulltoString(pkg.getName()));
            line = line.replace(PATTERN_SHA256, nulltoString(null));
            line = line.replace(PATTERN_URL, nulltoString(null));

        } else if (pv instanceof DownloadLink) {
            line = getPatternLinks();
            DownloadLink link = (DownloadLink) pv;
            line = line.replace(PATTERN_TYPE, "Link");
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(link)));
            line = line.replace(PATTERN_COMMENT, nulltoString(link.getComment()));
            line = line.replace(PATTERN_FILESIZE, nulltoString(link.getView().getBytesTotalEstimated()));
            line = line.replace(PATTERN_MD5, nulltoString(link.getMD5Hash()));
            line = line.replace(PATTERN_NAME, nulltoString(link.getName()));
            line = line.replace(PATTERN_SHA256, nulltoString(link.getSha1Hash()));
            line = line.replace(PATTERN_URL, nulltoString(link.getBrowserUrl()));

        } else if (pv instanceof CrawledLink) {
            line = getPatternLinks();
            CrawledLink link = (CrawledLink) pv;
            line = line.replace(PATTERN_TYPE, "Link");
            line = line.replace(PATTERN_COMMENT, nulltoString(link.getDownloadLink().getComment()));
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(link)));
            line = line.replace(PATTERN_FILESIZE, nulltoString(link.getSize()));
            line = line.replace(PATTERN_MD5, nulltoString(link.getDownloadLink().getMD5Hash()));
            line = line.replace(PATTERN_NAME, nulltoString(link.getName()));
            line = line.replace(PATTERN_SHA256, nulltoString(link.getDownloadLink().getSha1Hash()));
            line = line.replace(PATTERN_URL, nulltoString(link.getDownloadLink().getBrowserUrl()));

        } else if (pv instanceof CrawledPackage) {
            line = getPatternPackages();
            CrawledPackage pkg = (CrawledPackage) pv;
            CrawledPackageView fpv = new CrawledPackageView();
            boolean readL = pkg.getModifyLock().readLock();
            try {
                fpv.setItems(pkg.getChildren());
                line = line.replace(PATTERN_TYPE, "Package");
                line = line.replace(PATTERN_COMMENT, nulltoString(pkg.getComment()));
                line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(pkg)));
                line = line.replace(PATTERN_FILESIZE, nulltoString(fpv.getFileSize()));
                line = line.replace(PATTERN_MD5, nulltoString(null));
                line = line.replace(PATTERN_NAME, nulltoString(pkg.getName()));
                line = line.replace(PATTERN_SHA256, nulltoString(null));
                line = line.replace(PATTERN_URL, nulltoString(null));
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }
        if (StringUtils.isNotEmpty(line)) {
            if (sb.length() > 0) sb.append("\r\n");
            sb.append(line);
        }
    }

    private String nulltoString(Object comment) {
        return comment == null ? "" : comment + "";
    }

    private PackageControllerTable<?, ?> getTable() {
        if (MainTabbedPane.getInstance().isDownloadView()) {
            return DownloadsTable.getInstance();
        } else if (MainTabbedPane.getInstance().isLinkgrabberView()) { return LinkGrabberTable.getInstance(); }
        return null;
    }

}
