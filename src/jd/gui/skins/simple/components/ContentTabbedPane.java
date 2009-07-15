package jd.gui.skins.simple.components;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JTabbedPane;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.acryl.AcrylTabbedPaneUI;

public class ContentTabbedPane extends JTabbedPane {
    public ContentTabbedPane() {

        this.setFocusable(false);

        this.setTabPlacement(JTabbedPane.TOP);
  
        if (getUI() instanceof AcrylTabbedPaneUI) {

            setUI(new AcrylTabbedPaneUI() {
                

                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
                    // super.paintContentBorder(arg0, arg1, arg2, arg3, arg4,
                    // arg5, arg6)
                    int sepHeight = tabAreaInsets.bottom;
                    if (sepHeight > 0) {
                        switch (tabPlacement) {
                       
                        case TOP: {
                            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                            if (sepHeight > 1) {
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    g.drawLine(x, y + tabAreaHeight - sepHeight + i + 1, x + w, y + tabAreaHeight - sepHeight + i + 1);
                                }
                            } else {
                                g.setColor(getContentBorderColors(tabPlacement)[0]);
                                g.drawLine(x, y + tabAreaHeight, w, y + tabAreaHeight);
                            }
//                            g.setColor(AbstractLookAndFeel.getControlDarkShadow());
//                            g.drawLine(x, y + tabAreaHeight - 1, x, h);
                            break;
                        }
                        
                        }

                    }

                }
            });

        }
    }
}