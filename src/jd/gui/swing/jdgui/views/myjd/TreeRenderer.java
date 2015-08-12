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

package jd.gui.swing.jdgui.views.myjd;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.ColorUtils;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class TreeRenderer extends JPanel implements ListCellRenderer {

    private static final double   _1_15            = 1.15;

    private static final long     serialVersionUID = -3927390875702401200L;

    public static final Dimension SMALL_DIMENSION  = new Dimension(0, 10);

    public static final Dimension DIMENSION        = new Dimension(0, 35);

    private final Font            orgFont;
    private final Font            boldFont;

    private RenderLabel           lbl;

    private Color                 f;

    private Color                 alternateHighlight;

    private Color                 selectedBackground;

    private MatteBorder           matteBorder;

    public TreeRenderer() {
        super(new MigLayout("ins 0 ,wrap 1", "[grow,fill]", "[]"));

        lbl = new RenderLabel();
        add(lbl, "");

        lbl.setVerticalTextPosition(JLabel.BOTTOM);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        orgFont = lbl.getFont();
        boldFont = lbl.getFont().deriveFont(Font.BOLD);
        lbl.setFont(boldFont);
        lbl.setText("DUMMY");
        // we need to update the height here - if font scale faktor is over 100% we need higher rows
        TreeRenderer.DIMENSION.height += lbl.getPreferredSize().getHeight();

        TreeRenderer.SMALL_DIMENSION.height += lbl.getPreferredSize().getHeight();
        // this.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        f = lbl.getForeground();
        selectedBackground = ColorUtils.getAlphaInstance(f, 60);

        Color c = (LAFOptions.getInstance().getColorForPanelHeaderBackground());
        if (c != null) {
            selectedBackground = ColorUtils.getAlphaInstance(c, 230);
        }

        alternateHighlight = ColorUtils.getAlphaInstance(lbl.getForeground(), 4);
        // a=NewTheme.I().getColor(ColorUtils.getAlphaInstance(lbl.getForeground(),
        // 4));

        lbl.setOpaque(false);
        setOpaque(false);
        setBackground(null);
        lbl.setBackground(null);

        matteBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, selectedBackground);

        // this.setPreferredSize(new Dimension(200, 60));
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JComponent ret = null;

        if (value == null) {
            ret = this;
        } else {
            AbstractConfigPanel te = (AbstractConfigPanel) value;
            setText(te.getTitle());
            setIcon(te.getIcon());
            lbl.setVerticalTextPosition(JLabel.BOTTOM);
            lbl.setHorizontalTextPosition(JLabel.CENTER);
            lbl.setHorizontalAlignment(JLabel.CENTER);
            ret = this;
            this.setBorder(null);
            if (isSelected) {
                lbl.setFont(boldFont);
            } else {
                lbl.setFont(orgFont);
            }
            lbl.setEnabled(te.isEnabled());
            ret.setPreferredSize(null);
            TreeRenderer.DIMENSION.width = (int) Math.max(TreeRenderer.DIMENSION.width, ret.getPreferredSize().width * _1_15);
            ret.setPreferredSize(TreeRenderer.DIMENSION);

        }
        if (isSelected) {
            // lbl.setFont(boldFont);
            // lbl.setBorder(brd);
            setBackground(selectedBackground);
            // lbl.setForeground(b);
            setOpaque(true);
            // lbl.setForeground(b);
        } else {
            // lbl.setFont(orgFont);
            if (index % 2 == 0) {
                setBackground(alternateHighlight);
                setOpaque(true);
            } else {
                setOpaque(false);
                setBackground(null);
            }
        }

        return ret;
    }

    private void setIcon(Icon icon) {
        lbl.setIcon(icon);
    }

    private void setText(String name) {
        lbl.setText(name);
    }

}
