//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.skins.jdgui.components;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.gui.skins.jdgui.InfoPanelHandler;
import jd.gui.skins.jdgui.interfaces.DroppedPanel;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;
/**
 * class for an infopanel with close button.
 * @author Coalado
 *
 */
public class JDCollapser extends DroppedPanel {

    private static final long serialVersionUID = 6864885344815243560L;
    private static JDCollapser INSTANCE = null;

    public static JDCollapser getInstance() {
        if (INSTANCE == null) INSTANCE = new JDCollapser();
        return INSTANCE;
    }

    private SwitchPanel panel;
    private JLabel title;
    private JPanel content;

    private JDCollapser() {
        super();
        this.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[]5[fill,grow]"));
        add(title = new JLabel(""), "split 3,gapleft 5,gapbottom 0,gaptop 0");
        title.setIcon(JDTheme.II("gui.images.sort", 24, 24));
        title.setIconTextGap(15);
        add(new JSeparator(), "growx,pushx,gapright 5");
        JButton bt;
        add(bt = new JButton(JDTheme.II("gui.images.close", 16, 16)), "gapright 10");
        bt.setContentAreaFilled(false);
        bt.setBorder(null);
        bt.setOpaque(false);
        bt.setBorderPainted(false);
        bt.setToolTipText(JDL.L("gui.tooltips.infocollapser", "Click to close and save"));
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                InfoPanelHandler.setPanel(null);
                
            }

        });
        content = new JPanel();
        add(content);
        this.setVisible(true);

    }

    public void paint(Graphics g) {
        super.paint(g);

    }



    public void setContentPanel(SwitchPanel panel2) {
        if (panel2 == this.panel) return;     
        content.removeAll();
        this.panel = panel2;
        if (panel == null) return;
        content.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));      

        content.add(panel);     
        revalidate();
        content.revalidate();
    }

    public SwitchPanel getContentPanel() {
        return panel;
    }

    public void setTitle(String l) {
        title.setText(l);

    }

    public void setIcon(ImageIcon ii) {
        title.setIcon(ii);

    }

    @Override
    /**
     * deligates the onHidevenet to the contentpanel
     */
    public void onHide() {
        if(panel!=null)panel.hide();
    }

    @Override
    /**
     * deligates the onShow event to the contentpanel
     */
    public void onShow() {
     if(panel!=null)panel.show();
        
    }
}
