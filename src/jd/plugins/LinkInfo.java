package jd.plugins;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.parser.Regex;

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class LinkInfo {

    private final int       partNum;

    private final ImageIcon icon;

    public ImageIcon getIcon() {
        return icon;
    }

    public int getPartNum() {
        return partNum;
    }

    private final ExtensionsFilterInterface extension;

    public ExtensionsFilterInterface getExtension() {
        return extension;
    }

    private LinkInfo(final int partNum, final ExtensionsFilterInterface extension, ImageIcon icon) {
        this.partNum = partNum;
        this.icon = icon;
        this.extension = extension;
    }

    private static final HashMap<String, WeakReference<LinkInfo>> CACHE = new HashMap<String, WeakReference<LinkInfo>>();

    public static LinkInfo getLinkInfo(AbstractPackageChildrenNode abstractChildrenNode) {
        if (abstractChildrenNode != null) {
            final String fileName;
            if (abstractChildrenNode instanceof DownloadLink) {
                fileName = ((DownloadLink) abstractChildrenNode).getView().getDisplayName();
            } else {
                fileName = abstractChildrenNode.getName();
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
            final String ID = fileNameExtension + "_" + num;
            synchronized (CACHE) {
                LinkInfo ret = null;
                WeakReference<LinkInfo> linkInfo = CACHE.get(ID);
                if (linkInfo == null || (ret = linkInfo.get()) == null) {
                    ExtensionsFilterInterface extension = CompiledFiletypeFilter.getExtensionsFilterInterface(fileNameExtension);
                    if (extension == null) {
                        extension = new ExtensionsFilterInterface() {

                            @Override
                            public Pattern compiledAllPattern() {
                                return null;
                            }

                            @Override
                            public String getDesc() {
                                return fileNameExtension;
                            }

                            @Override
                            public String getIconID() {
                                return null;
                            }

                            @Override
                            public Pattern getPattern() {
                                return null;
                            }

                            @Override
                            public String name() {
                                return fileNameExtension;
                            }

                            @Override
                            public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
                                return extension != null && extension.getPattern() == null && extension.getIconID() == null && StringUtils.equals(extension.name(), name());
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

    public static ImageIcon getIcon(final String name, final ExtensionsFilterInterface extension) {
        ImageIcon newIcon = null;
        final String ext = Files.getExtension(name);
        if (CrossSystem.isWindows() && ext != null) {
            try {
                Icon ico = CrossSystem.getMime().getFileIcon(ext, 16, 16);
                newIcon = IconIO.toImageIcon(ico);
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
                iconID = "file";
            }
            newIcon = NewTheme.I().getIcon(iconID, 16);
        }
        return newIcon;
    }

}
