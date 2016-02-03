/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.jdownloader.images;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.io.InputStream;

import javax.swing.Icon;
import javax.swing.JButton;

import org.appwork.exceptions.WTFException;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

/**
 * TestClass to use a custom font instead of icons
 *
 * @date 02.02.2016
 *
 */
public class FontIcon implements Icon {
    public static void main(String[] args) throws DialogClosedException, DialogCanceledException {
        MigPanel panel = new MigPanel("ins 5", "[]", "[]");
        panel.add(new JButton(new FontIcon("\uf101", Color.RED, 8)));
        panel.add(new JButton(new FontIcon("\uf101", Color.BLUE, 12)));
        panel.add(new JButton(new FontIcon("\uf101", Color.GREEN, 16)));
        panel.add(new JButton(new FontIcon("\uf100", Color.GRAY, 32)));
        panel.add(new JButton(new FontIcon("\uf100", Color.BLACK, 48)));
        panel.add(new JButton(new FontIcon("\uf100", Color.BLACK, 256)));
        Dialog.getInstance().showDialog(new ContainerDialog(0, "FontIcon Test", panel, null, null, null));
    }

    private Font   font;
    private Color  color;
    private String string;
    private float  size;

    /**
     * @param string
     * @param i
     */
    public FontIcon(String string, Color color, float size) {
        this.color = color;
        this.size = size;
        InputStream is;
        this.string = string;
        try {
            is = getClass().getResource("IconFontTest.ttf").openStream();

            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(Font.PLAIN, size);

        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        // ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(color);
        g.drawString(string, x, y + getIconHeight() - 1);

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.Icon#getIconWidth()
     */
    @Override
    public int getIconWidth() {
        int ret = new Canvas().getFontMetrics(font).stringWidth(string);
        return ret;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.Icon#getIconHeight()
     */
    @Override
    public int getIconHeight() {
        FontRenderContext context = new Canvas().getFontMetrics(font).getFontRenderContext();

        int ret = font.createGlyphVector(context, string).getPixelBounds(null, 0, 0).height;
        return ret;
    }

}
