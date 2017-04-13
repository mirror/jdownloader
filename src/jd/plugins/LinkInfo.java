package jd.plugins;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.parser.Regex;

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class LinkInfo {

    private final int  partNum;

    private final Icon icon;

    public Icon getIcon() {
        return icon;
    }

    public int getPartNum() {
        return partNum;
    }

    private final ExtensionsFilterInterface extension;

    public ExtensionsFilterInterface getExtension() {
        return extension;
    }

    private LinkInfo(final int partNum, final ExtensionsFilterInterface extension, Icon icon) {
        this.partNum = partNum;
        this.icon = icon;
        this.extension = extension;
    }

    private static final HashMap<String, WeakReference<LinkInfo>> CACHE = new HashMap<String, WeakReference<LinkInfo>>();

    public static LinkInfo getLinkInfo(AbstractPackageChildrenNode abstractChildrenNode) {
        if (abstractChildrenNode != null) {
            final String fileName;
            final String mimeHint;
            if (abstractChildrenNode instanceof DownloadLink) {
                final DownloadLink link = (DownloadLink) abstractChildrenNode;
                fileName = link.getView().getDisplayName();
                mimeHint = link.getMimeHint();
            } else if (abstractChildrenNode instanceof CrawledLink) {
                final CrawledLink link = (CrawledLink) abstractChildrenNode;
                fileName = link.getName();
                final DownloadLink downloadLink = link.getDownloadLink();
                if (downloadLink != null) {
                    mimeHint = downloadLink.getMimeHint();
                } else {
                    mimeHint = null;
                }
            } else {
                fileName = abstractChildrenNode.getName();
                mimeHint = null;
            }
            final String fileNameExtension = Files.getExtension(fileName);
            int num = -1;
            try {
                String partID = new Regex(fileName, "\\.r(\\d+)$").getMatch(0);
                if (partID == null) {
                    partID = new Regex(fileName, "\\.pa?r?t?\\.?(\\d+).*?\\.rar$").getMatch(0);
                }
                if (partID != null) {
                    num = Integer.parseInt(partID);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            final String ID = fileNameExtension + "_" + num + "_" + mimeHint;
            synchronized (CACHE) {
                LinkInfo ret = null;
                WeakReference<LinkInfo> linkInfo = CACHE.get(ID);
                if (linkInfo == null || (ret = linkInfo.get()) == null) {
                    final ExtensionsFilterInterface hint = CompiledFiletypeFilter.getExtensionsFilterInterface(mimeHint);
                    final ExtensionsFilterInterface compiled = CompiledFiletypeFilter.getExtensionsFilterInterface(fileNameExtension);
                    final ExtensionsFilterInterface extension;
                    if (compiled == null || (hint != null && !hint.isSameExtensionGroup(compiled))) {
                        extension = new ExtensionsFilterInterface() {

                            final String  extension;
                            final String  desc;
                            final Pattern pattern;
                            {
                                if (fileNameExtension != null && fileNameExtension.matches("^[a-zA-Z0-9]{1,4}$")) {
                                    extension = fileNameExtension;
                                    desc = fileNameExtension;
                                    pattern = Pattern.compile(Pattern.quote(fileNameExtension), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                                } else {
                                    extension = "";
                                    desc = _GUI.T.settings_linkgrabber_filter_others();
                                    pattern = null;
                                }
                            }

                            @Override
                            public ExtensionsFilterInterface getSource() {
                                return hint;
                            }

                            @Override
                            public Pattern compiledAllPattern() {
                                if (hint != null) {
                                    return hint.compiledAllPattern();
                                } else {
                                    return null;
                                }
                            }

                            @Override
                            public String getDesc() {
                                if (hint != null) {
                                    return hint.getDesc();
                                } else {
                                    return desc;
                                }
                            }

                            @Override
                            public String getIconID() {
                                if (hint != null) {
                                    return hint.getIconID();
                                } else {
                                    return null;
                                }
                            }

                            @Override
                            public Pattern getPattern() {
                                if (hint != null) {
                                    return hint.compiledAllPattern();
                                } else {
                                    return pattern;
                                }
                            }

                            @Override
                            public String name() {
                                return extension;
                            }

                            @Override
                            public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
                                if (hint != null) {
                                    return hint.isSameExtensionGroup(extension);
                                } else {
                                    return extension != null && extension.getIconID() == null && StringUtils.equals(extension.name(), name());
                                }
                            }

                            @Override
                            public ExtensionsFilterInterface[] listSameGroup() {
                                if (hint != null) {
                                    return hint.listSameGroup();
                                } else {
                                    return new ExtensionsFilterInterface[] { this };
                                }
                            }
                        };
                    } else {
                        extension = new ExtensionsFilterInterface() {

                            @Override
                            public Pattern compiledAllPattern() {
                                return compiled.compiledAllPattern();
                            }

                            @Override
                            public String getDesc() {
                                return compiled.getDesc();
                            }

                            @Override
                            public String getIconID() {
                                return compiled.getIconID();
                            }

                            @Override
                            public Pattern getPattern() {
                                return compiled.getPattern();
                            }

                            @Override
                            public ExtensionsFilterInterface getSource() {
                                return compiled;
                            }

                            @Override
                            public String name() {
                                return fileNameExtension;
                            }

                            @Override
                            public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
                                return compiled.isSameExtensionGroup(extension);
                            }

                            @Override
                            public ExtensionsFilterInterface[] listSameGroup() {
                                return compiled.listSameGroup();
                            }

                        };
                    }
                    ret = new LinkInfo(num, extension, getIcon(fileName, extension));
                    CACHE.put(ID, new WeakReference<LinkInfo>(ret));
                }
                return ret;
            }
        }
        return null;
    }

    public static Icon getIcon(final String name, final ExtensionsFilterInterface extension) {
        Icon newIcon = null;
        final String ext = Files.getExtension(name);
        if (CrossSystem.isWindows() && ext != null) {
            try {
                newIcon = CrossSystem.getMime().getFileIcon(ext, 16, 16);
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (newIcon == null) {
            String iconID = null;
            if (extension != null && extension.getIconID() != null) {
                iconID = extension.getIconID();
            }
            if (StringUtils.isEmpty(iconID)) {
                iconID = IconKey.ICON_FILE;
            }
            newIcon = NewTheme.I().getIcon(iconID, 16);
        }
        return newIcon;
    }

}
