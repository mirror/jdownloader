//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.dialog;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.nutils.JDFlags;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

/**
 * Dieser Dialog wird angezeigt, wenn ein Download mit einem Plugin getätigt
 * wird, dessen Agbs noch nicht akzeptiert wurden
 * 
 * @author JD-Team
 */
public class AgbDialog extends AbstractDialog<Integer> {

    private static final long serialVersionUID = -1466993330568207945L;

    /**
     * Zeigt einen Dialog, in dem man die Hoster AGB akzeptieren kann
     * 
     * @param downloadLink
     *            abzuarbeitender Link
     */
    public static void showDialog(final DownloadLink downloadLink) {
        if (downloadLink.getDefaultPlugin() == null) { return; }
        final AgbDialog dialog = new AgbDialog(downloadLink.getDefaultPlugin());

        if (JDFlags.hasAllFlags(Dialog.getInstance().showDialog(dialog), UserIO.RETURN_OK) && dialog.isAccepted()) {
            downloadLink.getDefaultPlugin().setAGBChecked(true);
            downloadLink.getLinkStatus().reset();
        }
    }

    /**
     * Zeigt einen Dialog, in dem man die Hoster AGB akzeptieren kann
     * 
     * @param plugin
     *            Hoster-Plugin
     */
    public static void showDialog(final PluginForHost plugin) {
        final AgbDialog dialog = new AgbDialog(plugin);
        if (JDFlags.hasAllFlags(Dialog.getInstance().showDialog(dialog), UserIO.RETURN_OK) && dialog.isAccepted()) {

            plugin.setAGBChecked(true);
        }
    }

    private JLink               linkAgb;

    private JCheckBox           checkAgbAccepted;

    private final PluginForHost plugin;

    private AgbDialog(final PluginForHost plugin) {
        super(0, JDL.L("gui.dialogs.agb_tos.title", "Terms of Service are not accepted"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);
        this.plugin = plugin;

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.linkAgb || e.getSource() == this.checkAgbAccepted) {
            this.cancel();
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    private boolean isAccepted() {
        return this.checkAgbAccepted.isSelected();
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("wrap 1", "[center]"));

        final JLabel labelInfo = new JLabel(JDL.LF("gui.dialogs.agb_tos.description", "The TOSs of %s have not been read and accepted.", this.plugin.getHost()));

        this.linkAgb = new JLink(JDL.LF("gui.dialogs.agb_tos.readagb", "Read %s's TOSs", this.plugin.getHost()), this.plugin.getAGBLink());
        this.linkAgb.getBroadcaster().addListener(this);
        this.linkAgb.setFocusable(false);

        this.checkAgbAccepted = new JCheckBox(JDL.L("gui.dialogs.agb_tos.agbaccepted", "I accept the terms of service"));
        this.checkAgbAccepted.addActionListener(this);
        this.checkAgbAccepted.setFocusable(false);

        panel.add(labelInfo);
        panel.add(this.linkAgb);
        panel.add(this.checkAgbAccepted, "left");

        return panel;
    }

}
