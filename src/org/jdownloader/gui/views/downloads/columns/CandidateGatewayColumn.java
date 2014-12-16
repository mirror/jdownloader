package org.jdownloader.gui.views.downloads.columns;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateHistory;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.NoProxySelector;
import jd.controlling.proxy.SingleDirectGatewaySelector;
import jd.plugins.DownloadLink;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltip;
import org.jdownloader.images.AbstractIcon;

public class CandidateGatewayColumn extends ExtTextColumn<AbstractNode> {

    private Icon icon;
    private Icon iconProxy = new AbstractIcon(IconKey.ICON_PROXY, 18);

    public CandidateGatewayColumn() {
        super(_GUI._.CandidateGatewayColumn());
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode obj) {
        return CandidateTooltip.create(position, obj);
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.prepareColumn(value);

        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            DownloadLinkCandidate candidate = dl.getLatestCandidate();

            if (candidate != null) {

                configureForCandidate(rendererField, rendererIcon, getTableColumn(), candidate);
                return;
            }

        }

        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
    }

    public void configureForCandidate(JLabel rendererField, JLabel rendererIcon, TableColumn tableColumn, DownloadLinkCandidate candidate) {
        icon = null;
        String str = null;
        AbstractProxySelectorImpl proxySel = candidate.getProxySelector();
        SingleDownloadController controller = candidate.getLink().getDownloadLinkController();
        if (controller != null) {
            HTTPProxy proxy = controller.getUsedProxy();
            if (proxy != null) {
                str = proxy.toString();
                switch (proxy.getType()) {
                case DIRECT:
                case NONE:
                    break;
                case HTTP:
                    icon = iconProxy;
                    break;
                case SOCKS4:
                    icon = iconProxy;
                    break;
                case SOCKS5:
                    icon = iconProxy;
                    break;
                }

            }
        }
        if (StringUtils.isEmpty(str)) {
            if (proxySel instanceof NoProxySelector) {
                str = HTTPProxy.NONE.toString();
            } else if (proxySel instanceof SingleDirectGatewaySelector) {
                str = ((SingleDirectGatewaySelector) proxySel).getProxy().toString();
            } else {
                icon = iconProxy;
                str = proxySel.toString();
            }

        }

        rendererIcon.setIcon(icon);

        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }

        if (tableColumn != null) {
            try {
                rendererField.setText(org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, tableColumn.getWidth() - this.rendererIcon.getPreferredSize().width - 5));
            } catch (Throwable e) {
                // fallback if org.appwork.swing.sunwrapper.SwingUtilities2 disappears someday
                e.printStackTrace();
                rendererField.setText(str);
            }
        } else {
            rendererField.setText(str);
        }
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                if (obj instanceof DownloadLink) {
                    DownloadLinkCandidateHistory history = DownloadWatchDog.getInstance().getSession().getHistory((DownloadLink) obj);
                    if (history != null) {
                        ToolTipController.getInstance().show(CandidateTooltip.create(e.getPoint(), obj));
                    }
                }

            }

        });

        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        // unused, because #configureRendererComponent is implemented
        return null;
    }

}
