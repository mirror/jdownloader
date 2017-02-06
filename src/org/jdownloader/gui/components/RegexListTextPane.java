package org.jdownloader.gui.components;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.components.TextPane;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.IconLabelToolTip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.ToolTipHandler;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class RegexListTextPane extends TextPane implements ToolTipHandler {
    private UnderlineHighlightPainter lastPainter;

    @Override
    public void setEnabled(boolean enabled) {
        if (txt != null) {
            txt.setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return txt != null && txt.isEnabled();
    }

    public RegexListTextPane() {
        addStateUpdateListener(new StateUpdateListener() {

            @Override
            public void onStateUpdated() {
                updateHighlights();
            }
        });
        ToolTipController.getInstance().register(this);
        ToolTipManager.sharedInstance().unregisterComponent(this);
        ToolTipManager.sharedInstance().unregisterComponent(txt);
        txt.setMargin(new Insets(0, 30, 0, 0));
        txt.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {

                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
                if (CrossSystem.isContextMenuTrigger(e)) {
                    ToolTipController.getInstance().show(RegexListTextPane.this);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }
        });

        txt.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                RegexListTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(txt, e, RegexListTextPane.this));
            }
        });
    }

    public static class UnderlineHighlightPainter extends LayeredHighlighter.LayerPainter {

        private boolean      ok;
        private AbstractIcon warning;
        private Rectangle    bounds;
        private String       pattern;
        private int          line;
        private int          min = Integer.MAX_VALUE;

        public String getPattern() {
            return pattern;
        }

        public UnderlineHighlightPainter(int line, String pattern, boolean ok) {
            this.ok = ok;
            color = ok ? Color.GREEN : Color.red;
            warning = new AbstractIcon(IconKey.ICON_WARNING, 14);
            this.line = line;
            this.pattern = pattern;
        }

        public boolean isOk() {
            return ok;
        }

        @Override
        public void paint(final Graphics g, final int offs0, final int offs1, final Shape bounds, final JTextComponent c) {
            // Do nothing: this method will never be called
        }

        @Override
        public Shape paintLayer(final Graphics g, final int offs0, final int offs1, final Shape bounds, final JTextComponent c, final View view) {
            g.setColor(color == null ? c.getSelectionColor() : color);
            Rectangle alloc = null;
            if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
                if (bounds instanceof Rectangle) {
                    alloc = (Rectangle) bounds;
                } else {
                    alloc = bounds.getBounds();
                }
            } else {
                try {
                    final Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                    alloc = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
                } catch (final BadLocationException e) {
                    return null;
                }
            }
            final FontMetrics fm = c.getFontMetrics(c.getFont());
            final int baseline = alloc.y + alloc.height - fm.getDescent() + 1;
            if (!ok) {
                g.drawLine(alloc.x, baseline, alloc.x + alloc.width + warning.getIconWidth(), baseline);
                g.drawLine(alloc.x, baseline + 1, alloc.x + alloc.width + warning.getIconWidth(), baseline + 1);

                warning.paintIcon(c, g, alloc.x + alloc.width, baseline - 14 + 1);
                this.bounds = new Rectangle(alloc.x, alloc.y, alloc.width + warning.getIconWidth(), alloc.height);
            } else {
                g.drawLine(alloc.x, baseline, alloc.x + alloc.width, baseline);
                g.drawLine(alloc.x, baseline + 1, alloc.x + alloc.width, baseline + 1);

                this.bounds = new Rectangle(alloc.x, alloc.y, alloc.width, alloc.height);
            }
            g.setColor(Color.GRAY);
            g.drawLine(alloc.x - 2, alloc.y, alloc.x - 2, alloc.y + alloc.height);
            this.min = Math.min(min, offs0);
            if (offs0 == min) {
                // only first line
                g.drawString((line + 1) + "", 0, baseline);
            }
            // System.out.println(offs0 + " - " + offs1 + " " + view.getStartOffset() + " - " + view.getEndOffset() + "- " + view);

            // new AbstractIcon(IconKey.ICON_RIGHT, 14).paintIcon(c, g, alloc.x - 5, baseline - 14 + 1);
            // g.drawOval(-10, baseline, 10, 10);

            return alloc;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        protected Color color; // The color for the underline
    }

    protected void updateHighlights() {

        final Highlighter highlighter = txt.getHighlighter();
        // Remove any existing highlights for last word
        final Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {

            final Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlightPainter) {
                highlighter.removeHighlight(h);

            }
        }
        try {
            // gettext does returned modified text. we have to work on the document to avoid index problems
            Document doc = txt.getDocument();
            final String text = doc.getText(0, doc.getLength());
            final Matcher matcher = Pattern.compile("([^\r\n]+)").matcher(text);
            int line = 0;
            while (matcher.find()) {
                final MatchResult mr = matcher.toMatchResult();

                final int start = mr.start();
                final String g2 = mr.group(1);
                final int end = start + g2.length();

                if (validateLine(g2)) {

                    try {
                        txt.getHighlighter().addHighlight(start, end, new UnderlineHighlightPainter(line++, g2, true));
                    } catch (final BadLocationException e1) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);

                    }
                } else {
                    try {
                        txt.getHighlighter().addHighlight(start, end, new UnderlineHighlightPainter(line++, g2, false));
                    } catch (final BadLocationException e1) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);

                    }
                }

            }
        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
    }

    protected boolean validateLine(String g2) {
        try {
            Pattern.compile(g2);
            return true;
        } catch (Throwable e) {

        }
        return false;
    }

    @Override
    public ExtTooltip createExtTooltip(Point position) {
        if (position == null) {
            position = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(position, this);
        }
        // final int row = this.getRowIndexByPoint(position);
        // final ExtColumn<E> col = this.getExtColumnAtPoint(position);
        // this.lastTooltipCol = col;
        // this.lastTooltipRow = row;
        UnderlineHighlightPainter p = null;
        final Highlighter highlighter = txt.getHighlighter();
        // Remove any existing highlights for last word
        final Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            final Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlightPainter) {
                Rectangle bounds = ((UnderlineHighlightPainter) h.getPainter()).getBounds();
                if (bounds != null && bounds.contains(position)) {
                    p = ((UnderlineHighlightPainter) h.getPainter());
                }
            }
        }
        lastPainter = p;
        if (p == null) {
            return null;
        }

        if (p.isOk()) {
            return createOkTooltip(p.getPattern());
        } else {
            return createFailTooltip(p.getPattern());
        }

    }

    public ExtTooltip createFailTooltip(String p) {
        IconLabelToolTip ret = new IconLabelToolTip(_GUI.T.RegexListTextPane_createExtTooltip_bad(p), new AbstractIcon(IconKey.ICON_WARNING, 24));
        return ret;
    }

    public ExtTooltip createOkTooltip(String p) {
        IconLabelToolTip ret = new IconLabelToolTip(_GUI.T.RegexListTextPane_createExtTooltip_ok(p), new AbstractIcon(IconKey.ICON_OK, 24));
        return ret;
    }

    @Override
    public boolean isTooltipDisabledUntilNextRefocus() {
        return false;
    }

    @Override
    public boolean updateTooltip(ExtTooltip activeToolTip, MouseEvent e) {
        UnderlineHighlightPainter p = null;
        final Highlighter highlighter = txt.getHighlighter();
        // Remove any existing highlights for last word
        final Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            final Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlightPainter) {
                Rectangle bounds = ((UnderlineHighlightPainter) h.getPainter()).getBounds();
                if (bounds != null && bounds.contains(e.getPoint())) {
                    p = ((UnderlineHighlightPainter) h.getPainter());
                }
            }
        }
        if (p != lastPainter) {
            return true;
        }

        return false;
    }

    // override required for tooltip
    @Override
    public boolean isFocusable() {
        return txt != null && txt.isFocusable();
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        updateHighlights();
    }

    // override required for tooltip
    @Override
    public boolean hasFocus() {
        return txt != null && txt.hasFocus();
    }

    @Override
    public boolean isTooltipWithoutFocusEnabled() {
        return false;
    }

    @Override
    public int getTooltipDelay(Point mousePositionOnScreen) {
        SwingUtilities.convertPointFromScreen(mousePositionOnScreen, txt);
        UnderlineHighlightPainter p = null;
        final Highlighter highlighter = txt.getHighlighter();
        // Remove any existing highlights for last word
        final Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            final Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlightPainter) {
                Rectangle bounds = ((UnderlineHighlightPainter) h.getPainter()).getBounds();
                if (bounds != null && bounds.contains(mousePositionOnScreen)) {
                    p = ((UnderlineHighlightPainter) h.getPainter());
                }
            }
        }

        if (p == null) {
            return 0;
        }
        if (p.isOk()) {
            return 0;
        }
        return 100;
    }
}
