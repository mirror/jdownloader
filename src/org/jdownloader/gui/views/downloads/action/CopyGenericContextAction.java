package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.download.HashInfo;

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.translate._JDT;

public class CopyGenericContextAction extends CustomizableTableContextAppAction implements ActionContext {
    private static final String PATTERN_NAME             = "{name}";
    private static final String PATTERN_NAME_NOEXT       = "{name_noext}";
    private static final String PATTERN_NEWLINE          = "{newline}";
    private static final String PATTERN_COMMENT          = "{comment}";
    private static final String PATTERN_HASH             = "{hash}";
    private static final String PATTERN_FILESIZE         = "{filesize}";
    private static final String PATTERN_FILESIZE_KIB     = "{filesize_kib}";
    private static final String PATTERN_FILESIZE_MIB     = "{filesize_mib}";
    private static final String PATTERN_FILESIZE_GIB     = "{filesize_gib}";
    private static final String PATTERN_URL              = "{url}";
    private static final String PATTERN_HOST             = "{host}";
    private static final String PATTERN_URL_CONTAINER    = "{url.container}";
    private static final String PATTERN_URL_ORIGIN       = "{url.origin}";
    private static final String PATTERN_URL_CONTENT      = "{url.content}";
    private static final String PATTERN_URL_REFERRER     = "{url.referrer}";
    private static final String PATTERN_ARCHIVE_PASSWORD = "{archive.password}";
    private static final String PATTERN_TYPE             = "{type}";
    private static final String PATTERN_EXTENSION        = "{ext}";
    private static final String PATTERN_PATH             = "{path}";

    public CopyGenericContextAction() {
        super(true, true);
        setIconKey(IconKey.ICON_COPY);
        setName(_GUI.T.CopyGenericContextAction());
        setAccelerator(KeyEvent.VK_C);
    }

    public static String getTranslationForPatternPackages() {
        return _JDT.T.CopyGenericContextAction_getTranslationForPatternPackages_v2();
    }

    public static String getTranslationForPatternLinks() {
        return _JDT.T.CopyGenericContextAction_getTranslationForPatternLinks_v2();
    }

    public static String getTranslationForSmartSelection() {
        return _JDT.T.CopyGenericContextAction_getTranslationForSmartSelection();
    }

    private String patternPackages;

    @Customizer(link = "#getTranslationForPatternPackages")
    public String getPatternPackages() {
        return patternPackages;
    }

    public void setPatternPackages(String copyPattern) {
        this.patternPackages = copyPattern;
        setTooltipText(_GUI.T.CopyGenericContextAction_tt(getPatternPackages() + " - " + getPatternLinks()));
    }

    private String patternLinks;

    @Customizer(link = "#getTranslationForPatternLinks")
    public String getPatternLinks() {
        return patternLinks;
    }

    public void setPatternLinks(String patternLinks) {
        this.patternLinks = patternLinks;
        setTooltipText(_GUI.T.CopyGenericContextAction_tt(getPatternPackages() + " - " + getPatternLinks()));
    }

    private boolean smartSelection;

    @Customizer(link = "#getTranslationForSmartSelection")
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
        final StringBuilder sb = new StringBuilder();
        final SelectionInfo<?, ?> selectionInfo = getTable().getSelectionInfo();
        if (isSmartSelection()) {
            int children = 0;
            for (final PackageView<?, ?> pv : selectionInfo.getPackageViews()) {
                final List<AbstractNode> childs = (List<AbstractNode>) pv.getChildren();
                children += childs.size();
                if (children > 1) {
                    break;
                }
            }
            final boolean contentPermission = children == 1;
            for (final PackageView<?, ?> pv : selectionInfo.getPackageViews()) {
                final AbstractPackageNode<?, ?> pkg = pv.getPackage();
                add(sb, pkg, false);
                final List<AbstractNode> childs = (List<AbstractNode>) pv.getChildren();
                for (final AbstractNode c : childs) {
                    add(sb, c, contentPermission);
                }
            }
        } else {
            final List<AbstractNode> selection = selectionInfo.getRawSelection();
            final boolean contentPermission = selection.size() == 1 && selection.get(0) instanceof AbstractPackageChildrenNode;
            for (final AbstractNode pv : selection) {
                add(sb, pv, contentPermission);
            }
        }
        ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
    }

    private final String getUrlByType(final UrlDisplayType dt, final AbstractNode node) {
        final DownloadLink link;
        if (node instanceof DownloadLink) {
            link = (DownloadLink) node;
        } else if (node instanceof CrawledLink) {
            link = ((CrawledLink) node).getDownloadLink();
        } else {
            return null;
        }
        switch (dt) {
        case CUSTOM:
            return link.getCustomUrl();
        case REFERRER:
            return link.getReferrerUrl();
        case CONTAINER:
            return link.getContainerUrl();
        case ORIGIN:
            return link.getOriginUrl();
        case CONTENT:
            if (link.getContentUrl() != null) {
                return link.getContentUrl();
            }
            return link.getPluginPatternMatcher();
        default:
            return null;
        }
    }

    private final String formatFileSize(final long fileSize, SIZEUNIT sizeUnit) {
        return SIZEUNIT.formatValue(sizeUnit, fileSize);
    }

    private final String toUpperCase(final String input) {
        if (input != null) {
            return input.toUpperCase(Locale.ENGLISH);
        } else {
            return null;
        }
    }

    private final String replaceDate(String line) {
        while (true) {
            final String timeFormat[] = new Regex(line, "(\\{date_(.*?)\\})").getRow(0);
            if (timeFormat != null) {
                try {
                    final SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormat[1], Locale.ENGLISH);
                    line = line.replace(timeFormat[0], dateFormat.format(new Date(System.currentTimeMillis())));
                } catch (final Throwable e) {
                    line = line.replace(timeFormat[0], "");
                }
            } else {
                return line;
            }
        }
    }

    public void add(StringBuilder sb, AbstractNode pv, final boolean contentPermission) {
        String line = null;
        if (pv instanceof FilePackage) {
            line = getPatternPackages();
            line = replaceDate(line);
            final FilePackage pkg = (FilePackage) pv;
            final FilePackageView fpv = new FilePackageView(pkg);
            fpv.aggregate();
            line = line.replace(PATTERN_TYPE, "Package");
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(pkg)));
            line = line.replace(PATTERN_COMMENT, nulltoString(pkg.getComment()));
            final long fileSize = fpv.getSize();
            line = line.replace(PATTERN_FILESIZE, nulltoString(formatFileSize(fileSize, SIZEUNIT.B)));
            line = line.replace(PATTERN_FILESIZE_KIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.KiB)));
            line = line.replace(PATTERN_FILESIZE_MIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.MiB)));
            line = line.replace(PATTERN_FILESIZE_GIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.GiB)));
            line = line.replace(PATTERN_HOST, nulltoString(null));
            line = line.replace(PATTERN_NEWLINE, CrossSystem.getNewLine());
            final String name = pkg.getName();
            line = line.replace(PATTERN_NAME, nulltoString(name));
            line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(null));
            line = line.replace(PATTERN_NAME_NOEXT, nulltoString(null));
            line = line.replace(PATTERN_EXTENSION, nulltoString(null));
            for (final HashInfo.TYPE hashType : HashInfo.TYPE.values()) {
                line = line.replace("{" + hashType.name().replace("-", "").toLowerCase(Locale.ENGLISH) + "}", nulltoString(null));
            }
            line = line.replace(PATTERN_HASH, nulltoString(null));
            line = line.replace(PATTERN_URL, nulltoString(null));
            line = line.replace(PATTERN_URL_CONTAINER, nulltoString(null));
            line = line.replace(PATTERN_URL_CONTENT, nulltoString(null));
            line = line.replace(PATTERN_URL_ORIGIN, nulltoString(null));
            line = line.replace(PATTERN_URL_REFERRER, nulltoString(null));
        } else if (pv instanceof DownloadLink) {
            line = getPatternLinks();
            line = replaceDate(line);
            final DownloadLink link = (DownloadLink) pv;
            line = line.replace(PATTERN_TYPE, "Link");
            line = line.replace(PATTERN_HOST, nulltoString(link.getHost()));
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(link)));
            line = line.replace(PATTERN_COMMENT, nulltoString(link.getComment()));
            final long fileSize = link.getView().getBytesTotalEstimated();
            line = line.replace(PATTERN_FILESIZE, nulltoString(formatFileSize(fileSize, SIZEUNIT.B)));
            line = line.replace(PATTERN_FILESIZE_KIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.KiB)));
            line = line.replace(PATTERN_FILESIZE_MIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.MiB)));
            line = line.replace(PATTERN_FILESIZE_GIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.GiB)));
            line = line.replace(PATTERN_NEWLINE, CrossSystem.getNewLine());
            final String name = link.getView().getDisplayName();
            line = line.replace(PATTERN_NAME, nulltoString(name));
            line = line.replace(PATTERN_NAME_NOEXT, nulltoString(Files.getFileNameWithoutExtension(name)));
            line = line.replace(PATTERN_EXTENSION, nulltoString(toUpperCase(Files.getExtension(name))));
            final HashInfo hashInfo = link.getHashInfo();
            for (final HashInfo.TYPE hashType : HashInfo.TYPE.values()) {
                final String hashString;
                if (hashInfo != null && hashInfo.getType() == hashType) {
                    hashString = hashInfo.getHash();
                } else {
                    hashString = null;
                }
                line = line.replace("{" + hashType.name().replace("-", "").toLowerCase(Locale.ENGLISH) + "}", nulltoString(hashString));
            }
            line = line.replace(PATTERN_HASH, nulltoString(hashInfo != null ? hashInfo.getHash() : null));
            line = line.replace(PATTERN_URL, nulltoString(link.getView().getDisplayUrl()));
            line = line.replace(PATTERN_URL_CONTAINER, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.CONTAINER, link)));
            if (contentPermission) {
                line = line.replace(PATTERN_URL_CONTENT, nulltoString(getUrlByType(UrlDisplayType.CONTENT, link)));
            } else {
                line = line.replace(PATTERN_URL_CONTENT, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.CONTENT, link)));
            }
            line = line.replace(PATTERN_URL_ORIGIN, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.ORIGIN, link)));
            line = line.replace(PATTERN_URL_REFERRER, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.REFERRER, link)));
        } else if (pv instanceof CrawledLink) {
            line = getPatternLinks();
            line = replaceDate(line);
            final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(Arrays.asList(new AbstractNode[] { pv }), 1);
            if (archives != null && archives.size() == 1) {
                line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(archives.get(0).getFinalPassword()));
            } else {
                line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(null));
            }
            final CrawledLink link = (CrawledLink) pv;
            line = line.replace(PATTERN_TYPE, "Link");
            line = line.replace(PATTERN_HOST, nulltoString(link.getHost()));
            line = line.replace(PATTERN_COMMENT, nulltoString(link.getDownloadLink().getComment()));
            line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(link)));
            final long fileSize = link.getSize();
            line = line.replace(PATTERN_FILESIZE, nulltoString(formatFileSize(fileSize, SIZEUNIT.B)));
            line = line.replace(PATTERN_FILESIZE_KIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.KiB)));
            line = line.replace(PATTERN_FILESIZE_MIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.MiB)));
            line = line.replace(PATTERN_FILESIZE_GIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.GiB)));
            line = line.replace(PATTERN_NEWLINE, CrossSystem.getNewLine());
            final String name = link.getDownloadLink().getView().getDisplayName();
            line = line.replace(PATTERN_NAME, nulltoString(name));
            line = line.replace(PATTERN_NAME_NOEXT, nulltoString(Files.getFileNameWithoutExtension(name)));
            line = line.replace(PATTERN_EXTENSION, nulltoString(toUpperCase(Files.getExtension(name))));
            final HashInfo hashInfo = link.getDownloadLink().getHashInfo();
            for (final HashInfo.TYPE hashType : HashInfo.TYPE.values()) {
                final String hashString;
                if (hashInfo != null && hashInfo.getType() == hashType) {
                    hashString = hashInfo.getHash();
                } else {
                    hashString = null;
                }
                line = line.replace("{" + hashType.name().replace("-", "").toLowerCase(Locale.ENGLISH) + "}", nulltoString(hashString));
            }
            line = line.replace(PATTERN_HASH, nulltoString(hashInfo != null ? hashInfo.getHash() : null));
            line = line.replace(PATTERN_URL, nulltoString(link.getDownloadLink().getView().getDisplayUrl()));
            line = line.replace(PATTERN_URL_CONTAINER, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.CONTAINER, link)));
            if (contentPermission) {
                line = line.replace(PATTERN_URL_CONTENT, nulltoString(getUrlByType(UrlDisplayType.CONTENT, link)));
            } else {
                line = line.replace(PATTERN_URL_CONTENT, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.CONTENT, link)));
            }
            line = line.replace(PATTERN_URL_ORIGIN, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.ORIGIN, link)));
            line = line.replace(PATTERN_URL_REFERRER, nulltoString(LinkTreeUtils.getUrlByType(UrlDisplayType.REFERRER, link)));
        } else if (pv instanceof CrawledPackage) {
            line = getPatternPackages();
            line = replaceDate(line);
            final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(Arrays.asList(new AbstractNode[] { pv }), 1);
            if (archives != null && archives.size() == 1) {
                line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(archives.get(0).getFinalPassword()));
            } else {
                line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(null));
            }
            final CrawledPackage pkg = (CrawledPackage) pv;
            final CrawledPackageView fpv = new CrawledPackageView();
            final boolean readL = pkg.getModifyLock().readLock();
            try {
                fpv.setItems(pkg.getChildren());
                line = line.replace(PATTERN_TYPE, "Package");
                line = line.replace(PATTERN_COMMENT, nulltoString(pkg.getComment()));
                line = line.replace(PATTERN_PATH, nulltoString(LinkTreeUtils.getDownloadDirectory(pkg)));
                final long fileSize = fpv.getFileSize();
                line = line.replace(PATTERN_FILESIZE, nulltoString(formatFileSize(fileSize, SIZEUNIT.B)));
                line = line.replace(PATTERN_FILESIZE_KIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.KiB)));
                line = line.replace(PATTERN_FILESIZE_MIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.MiB)));
                line = line.replace(PATTERN_FILESIZE_GIB, nulltoString(formatFileSize(fileSize, SIZEUNIT.GiB)));
                line = line.replace(PATTERN_NEWLINE, CrossSystem.getNewLine());
                final String name = pkg.getName();
                line = line.replace(PATTERN_NAME, nulltoString(name));
                line = line.replace(PATTERN_NAME_NOEXT, nulltoString(null));
                line = line.replace(PATTERN_ARCHIVE_PASSWORD, nulltoString(null));
                line = line.replace(PATTERN_EXTENSION, nulltoString(null));
                for (final HashInfo.TYPE hashType : HashInfo.TYPE.values()) {
                    line = line.replace("{" + hashType.name().replace("-", "").toLowerCase(Locale.ENGLISH) + "}", nulltoString(null));
                }
                line = line.replace(PATTERN_HASH, nulltoString(null));
                line = line.replace(PATTERN_URL, nulltoString(null));
                line = line.replace(PATTERN_URL_CONTAINER, nulltoString(null));
                line = line.replace(PATTERN_URL_CONTENT, nulltoString(null));
                line = line.replace(PATTERN_URL_ORIGIN, nulltoString(null));
                line = line.replace(PATTERN_URL_REFERRER, nulltoString(null));
                line = line.replace(PATTERN_HOST, nulltoString(null));
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }
        if (StringUtils.isNotEmpty(line)) {
            if (sb.length() > 0) {
                sb.append(CrossSystem.getNewLine());
            }
            sb.append(line);
        }
    }

    private String nulltoString(Object comment) {
        return comment == null ? "" : comment + "";
    }

    private PackageControllerTable<?, ?> getTable() {
        if (MainTabbedPane.getInstance().isDownloadView()) {
            return DownloadsTable.getInstance();
        } else if (MainTabbedPane.getInstance().isLinkgrabberView()) {
            return LinkGrabberTable.getInstance();
        }
        return null;
    }
}
