package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.UrlDisplayType;

@ScriptAPI(description = "The context download list link")
public class CrawledLinkSandbox {
    private final CrawledLink                                              link;
    private final static WeakHashMap<CrawledLink, HashMap<String, Object>> SESSIONPROPERTIES = new WeakHashMap<CrawledLink, HashMap<String, Object>>();

    public CrawledLinkSandbox(CrawledLink link) {
        this.link = link;
    }

    public String getAvailableState() {
        if (link != null) {
            return link.getLinkState().name();
        } else {
            return AvailableLinkState.UNKNOWN.name();
        }
    }

    public String getPriority() {
        if (link != null) {
            return link.getPriority().name();
        } else {
            return Priority.DEFAULT.name();
        }
    }

    public void setPriority(final String priority) {
        if (link != null) {
            try {
                link.setPriority(Priority.valueOf(priority));
            } catch (final Throwable e) {
                link.setPriority(Priority.DEFAULT);
            }
        }
    }

    public long getAddedDate() {
        if (link != null) {
            return link.getCreated();
        }
        return -1;
    }

    public CrawledLinkSandbox() {
        link = null;
    }

    @Override
    public int hashCode() {
        if (link != null) {
            return link.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CrawledLinkSandbox) {
            return ((CrawledLinkSandbox) obj).link == link;
        } else {
            return super.equals(obj);
        }
    }

    public Object getProperty(String key) {
        if (link != null) {
            return link.getDownloadLink().getProperty(key);
        }
        return null;
    }

    public Object getSessionProperty(final String key) {
        if (this.link != null) {
            synchronized (SESSIONPROPERTIES) {
                final HashMap<String, Object> properties = SESSIONPROPERTIES.get(this.link);
                if (properties != null) {
                    return properties.get(key);
                }
            }
        }
        return null;
    }

    public String getContentURL() {
        if (link != null) {
            return LinkTreeUtils.getUrlByType(UrlDisplayType.CONTENT, link);
        }
        return null;
    }

    public String getContainerURL() {
        if (link != null) {
            return LinkTreeUtils.getUrlByType(UrlDisplayType.CONTAINER, link);
        }
        return null;
    }

    public String getOriginURL() {
        if (link != null) {
            return LinkTreeUtils.getUrlByType(UrlDisplayType.ORIGIN, link);
        }
        return null;
    }

    public String getReferrerURL() {
        if (link != null) {
            return LinkTreeUtils.getUrlByType(UrlDisplayType.REFERRER, link);
        }
        return null;
    }

    public boolean remove() {
        if (this.link != null) {
            final CrawledPackage pkg = this.link.getParentNode();
            if (pkg != null) {
                final PackageController<CrawledPackage, CrawledLink> controller = pkg.getControlledBy();
                if (controller != null) {
                    final ArrayList<CrawledLink> children = new ArrayList<CrawledLink>();
                    children.add(link);
                    controller.removeChildren(children);
                    return true;
                }
            }
        }
        return false;
    }

    public void setSessionProperty(final String key, final Object value) {
        if (link != null) {
            if (value != null) {
                if (!canStore(value)) {
                    throw new WTFException("Type " + value.getClass().getSimpleName() + " is not supported");
                }
            }
            synchronized (SESSIONPROPERTIES) {
                HashMap<String, Object> properties = SESSIONPROPERTIES.get(link);
                if (properties == null) {
                    properties = new HashMap<String, Object>();
                    SESSIONPROPERTIES.put(link, properties);
                }
                properties.put(key, value);
            }
        }
    }

    public String getUUID() {
        if (link != null) {
            return link.getUniqueID().toString();
        }
        return null;
    }

    public void setProperty(String key, Object value) {
        if (link != null) {
            if (value != null) {
                if (!canStore(value)) {
                    throw new WTFException("Type " + value.getClass().getSimpleName() + " is not supported");
                }
            }
            link.getDownloadLink().setProperty(key, value);
        }
    }

    private boolean canStore(final Object value) {
        return value == null || Clazz.isPrimitive(value.getClass()) || JsonKeyValueStorage.isWrapperType(value.getClass()) || value instanceof Storable;
    }

    public ArchiveSandbox getArchive() {
        if (link == null || ArchiveValidator.EXTENSION == null) {
            return null;
        }
        final Archive archive = ArchiveValidator.EXTENSION.getArchiveByFactory(new CrawledLinkFactory(link));
        if (archive != null) {
            return new ArchiveSandbox(archive);
        }
        final ArrayList<Object> list = new ArrayList<Object>();
        list.add(link);
        final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(list);
        return (archives == null || archives.size() == 0) ? null : new ArchiveSandbox(archives.get(0));
    }

    public String getComment() {
        if (link != null) {
            return link.getComment();
        }
        return null;
    }

    public void setComment(String comment) {
        if (link != null) {
            link.setComment(comment);
        }
    }

    public void setEnabled(boolean b) {
        if (link != null) {
            link.setEnabled(b);
        }
    }

    public String getDownloadPath() {
        if (link == null) {
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                return "c:\\I am a dummy folder\\Test.txt";
            default:
                return "/mnt/Text.txt";
            }
        }
        return link.getDownloadLink().getFileOutput();
    }

    @ScriptAPI(description = "Sets a new filename", parameters = { "new Name" })
    public void setName(String name) {
        if (link != null) {
            link.setName(name);
        }
    }

    public String getUrl() {
        if (link != null) {
            return link.getURL();
        }
        return null;
    }

    public long getBytesTotal() {
        if (link != null) {
            return link.getSize();
        }
        return -1;
    }

    public String getName() {
        if (link == null) {
            return "Test.txt";
        }
        return link.getName();
    }

    public CrawledPackageSandbox getPackage() {
        if (link == null) {
            return new CrawledPackageSandbox();
        }
        return new CrawledPackageSandbox(link.getParentNode());
    }

    public String getHost() {
        if (link != null) {
            return link.getHost();
        } else {
            return null;
        }
    }

    public boolean isEnabled() {
        if (link != null) {
            return link.isEnabled();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "CrawledLink Instance: " + getName();
    }
}
