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

/**
 * Dieser Dialog wird angezeigt, wenn ein Download mit einem Plugin getätigt
 * wird, dessen Agbs noch nicht akzeptiert wurden
 * 
 * @author JD-Team
 */
public class AgbDialog extends AbstractDialog {

    /**
     * Zeigt einen Dialog, in dem man die Hoster AGB akzeptieren kann
     * 
     * @param downloadLink
     *            abzuarbeitender Link
     */
    public static void showDialog(DownloadLink downloadLink) {
        AgbDialog dialog = new AgbDialog(downloadLink.getPlugin());
        if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK) && dialog.isAccepted()) {
            downloadLink.getPlugin().setAGBChecked(true);
            downloadLink.getLinkStatus().reset();
        }
    }

    /**
     * Zeigt einen Dialog, in dem man die Hoster AGB akzeptieren kann
     * 
     * @param plugin
     *            Hoster-Plugin
     */
    public static void showDialog(PluginForHost plugin) {
        AgbDialog dialog = new AgbDialog(plugin);
        if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK) && dialog.isAccepted()) {
            plugin.setAGBChecked(true);
        }
    }

    private static final long serialVersionUID = -1466993330568207945L;

    private JLink linkAgb;

    private JCheckBox checkAgbAccepted;

    private PluginForHost plugin;

    private AgbDialog(PluginForHost plugin) {
        super(0, JDL.L("gui.dialogs.agb_tos.title", "Terms of Service are not accepted"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);
        this.plugin = plugin;
        init();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == linkAgb || e.getSource() == checkAgbAccepted) {
            interrupt();
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("wrap 1", "[center]"));

        JLabel labelInfo = new JLabel(JDL.LF("gui.dialogs.agb_tos.description", "The TOSs of %s have not been read and accepted.", plugin.getHost()));

        linkAgb = new JLink(JDL.LF("gui.dialogs.agb_tos.readagb", "Read %s's TOSs", plugin.getHost()), plugin.getAGBLink());
        linkAgb.getBroadcaster().addListener(this);
        linkAgb.setFocusable(false);

        checkAgbAccepted = new JCheckBox(JDL.L("gui.dialogs.agb_tos.agbaccepted", "I accept the terms of service"));
        checkAgbAccepted.addActionListener(this);
        checkAgbAccepted.setFocusable(false);

        panel.add(labelInfo);
        panel.add(linkAgb);
        panel.add(checkAgbAccepted, "left");

        return panel;
    }

    private boolean isAccepted() {
        return checkAgbAccepted.isSelected();
    }

}
