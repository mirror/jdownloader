package org.jdownloader.gui.views.components.packagetable.columns;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.images.NewTheme;

public class HasCaptchaColumn extends ExtIconColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ImageIcon         iconNo;
    private ImageIcon         iconYes;

    public HasCaptchaColumn() {
        super(_GUI._.HasCaptchaColumn_HasCaptchaColumn_());

        iconYes = NewTheme.I().getIcon("true", 16);

        iconNo = NewTheme.I().getIcon("error", 16);
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return false;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        if (obj instanceof FilePackage) {
            return ((FilePackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    private boolean hasCaptcha(DownloadLink dlink) {
        PluginForHost plg = dlink.getDefaultPlugin();

        boolean hasCaptcha = true;
        if (plg != null) {
            hasCaptcha = plg.hasCaptcha(dlink, null);
            if (hasCaptcha) {
                List<Account> accs = AccountController.getInstance().getMultiHostAccounts(plg.getHost());
                if (accs != null) {
                    for (Account acc : accs) {

                        if (acc.isValid() && !acc.isTempDisabled() && acc.isEnabled()) {
                            if (acc.getPlugin() == null) {
                                hasCaptcha = plg.hasCaptcha(dlink, acc);
                            } else {
                                hasCaptcha = acc.getPlugin().hasCaptcha(dlink, acc);

                            }
                        }
                        if (!hasCaptcha) {
                            break;
                        }
                    }

                }
            }
            if (hasCaptcha) {
                ArrayList<Account> accs = AccountController.getInstance().list(plg.getHost());
                if (accs != null) {
                    for (Account acc : accs) {
                        if (acc.isValid() && !acc.isTempDisabled() && acc.isEnabled()) {
                            if (acc.getPlugin() == null) {
                                hasCaptcha = plg.hasCaptcha(dlink, acc);
                            } else {
                                hasCaptcha = acc.getPlugin().hasCaptcha(dlink, acc);

                            }
                        }
                        if (!hasCaptcha) {
                            break;
                        }
                    }

                }
            }

        }

        return hasCaptcha;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {

        if (value instanceof DownloadLink) {
            DownloadLink dlink = ((DownloadLink) value);
            if (hasCaptcha(dlink)) {
                return iconYes;
            } else {
                return iconNo;
            }

        } else if (value instanceof CrawledLink) {
            DownloadLink dlink = ((CrawledLink) value).getDownloadLink();

            if (hasCaptcha(dlink)) {
                return iconYes;
            } else {
                return iconNo;
            }
        } else {
            return null;
        }

    }

    @Override
    protected String getTooltipText(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dlink = ((DownloadLink) value);
            if (hasCaptcha(dlink)) {
                return _GUI._.HasCaptchaColumn_getTooltipText_yes();
            } else {
                return _GUI._.HasCaptchaColumn_getTooltipText_no();
            }

        } else if (value instanceof CrawledLink) {
            DownloadLink dlink = ((CrawledLink) value).getDownloadLink();

            if (hasCaptcha(dlink)) {
                return _GUI._.HasCaptchaColumn_getTooltipText_yes();
            } else {
                return _GUI._.HasCaptchaColumn_getTooltipText_no();
            }
        }
        return null;
    }
}