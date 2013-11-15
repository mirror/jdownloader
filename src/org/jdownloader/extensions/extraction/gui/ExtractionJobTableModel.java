package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;

import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.IconPainter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCircleProgressColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.translate._GUI;

public class ExtractionJobTableModel extends ExtTableModel<ExtractionController> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7585295834599426087L;
    private Color             textColor;
    private Color             back;

    public ExtractionJobTableModel(Color c) {
        super("ExtractionJobTableModel4");
        setColor(c);
    }

    public void setColor(Color c) {
        textColor = c;
        back = ColorUtils.getAlphaInstance(c, 50);
    }

    @Override
    protected void initColumns() {

        addColumn(new ExtTextColumn<ExtractionController>(T._.tooltip_NameColumn()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -7294960809807602558L;

            @Override
            protected Color getDefaultForeground() {
                return textColor;
            }

            @Override
            public boolean isSortable(final ExtractionController obj) {
                return false;
            }

            @Override
            protected String getTooltipText(ExtractionController value) {
                return value.getArchiv().getName();
            }

            @Override
            public int getDefaultWidth() {
                return 200;
            }

            @Override
            protected Icon getIcon(ExtractionController value) {
                try {
                    return (CrossSystem.getMime().getFileIcon(Files.getExtension(value.getArchiv().getFirstArchiveFile().getFilePath()), 16, 16));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getStringValue(ExtractionController value) {

                return value.getArchiv().getName();
            }
        });

        addColumn(new ExtTextColumn<ExtractionController>(_GUI._.lit_status()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -5325290576078384961L;

            {

            }

            @Override
            public boolean isSortable(final ExtractionController obj) {
                return false;
            }

            @Override
            protected Color getDefaultForeground() {
                return textColor;
            }

            @Override
            public int getDefaultWidth() {
                return 150;
            }

            protected int getMaxWidth() {
                return getDefaultWidth();
            }

            /**
             * @return
             */
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            public String getStringValue(ExtractionController value) {
                Type event = value.getLatestEvent();
                if (event == null) return "";
                switch (event) {
                case START:
                    return T._.plugins_optional_extraction_status_openingarchive();

                case START_CRACK_PASSWORD:
                    return "Start password finding";

                case PASSWORT_CRACKING:
                    try {
                        return T._.plugins_optional_extraction_status_crackingpass_progress(((10000 * value.getCrackProgress()) / value.getPasswordListSize()) / 100.00);
                    } catch (Throwable e) {
                        return T._.plugins_optional_extraction_status_crackingpass_progress(0.00d);

                    }
                case PASSWORD_FOUND:
                    return T._.plugins_optional_extraction_status_passfound();
                case EXTRACTING:
                    if (value.getArchiv().getExtractedFiles().size() > 0) {
                        return T._.plugins_optional_extraction_status_extracting_filename(value.getArchiv().getExtractedFiles().get(value.getArchiv().getExtractedFiles().size() - 1).getName());
                    } else {
                        return T._.plugins_optional_extraction_status_extracting2();
                    }
                case FINISHED:
                    return T._.plugins_optional_extraction_status_extractok();

                case NOT_ENOUGH_SPACE:
                    return T._.plugins_optional_extraction_status_notenoughspace();

                case FILE_NOT_FOUND:
                    return T._.plugins_optional_extraction_filenotfound();

                }
                return null;

            }
        });
        ExtCircleProgressColumn<ExtractionController> sorter;
        addColumn(sorter = new ExtCircleProgressColumn<ExtractionController>(_GUI._.lit_progress()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -7238552518783596726L;
            private RendererMigPanel  panel;
            private RenderLabel       label;
            private DecimalFormat     format;

            {
                determinatedRenderer = new CircledProgressBar();
                renderer = determinatedRenderer;
                determinatedRenderer.setForeground(textColor);
                determinatedRenderer.setValueClipPainter(new IconPainter() {

                    public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(textColor);
                        // g2.fillRect(0, 0, 10, 10);
                        final Area a = new Area(shape);
                        a.intersect(new Area(new Ellipse2D.Float(-(diameter) / 2, -(diameter) / 2, diameter, diameter)));

                        g2.fill(a);

                    }

                    private Dimension dimension;
                    {
                        dimension = new Dimension(20, 20);
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        return dimension;
                    }
                });

                determinatedRenderer.setNonvalueClipPainter(new IconPainter() {

                    public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(back);
                        final Area a = new Area(shape);
                        a.intersect(new Area(new Ellipse2D.Float(-(diameter) / 2, -(diameter) / 2, diameter, diameter)));

                        g2.fill(a);
                    }

                    private Dimension dimension;
                    {
                        dimension = new Dimension(20, 20);
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        return dimension;
                    }
                });

                panel = new RendererMigPanel("ins 0", "[][grow,fill]", "[]");
                panel.add(determinatedRenderer, "width 20!,height 20!");
                label = new RenderLabel();
                format = new DecimalFormat("00.00 %");
                panel.add(label);
            }

            @Override
            protected String getString(ExtractionController value) {

                return "fdfd";
            }

            @Override
            protected Color getDefaultForeground() {
                return textColor;
            }

            @Override
            public JComponent getRendererComponent(ExtractionController value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getRendererComponent(value, isSelected, hasFocus, row, column);

                return panel;
            }

            @Override
            public void configureRendererComponent(ExtractionController value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                label.setText(format.format(value.getProgress()));
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();

                determinatedRenderer.setForeground(textColor);
                this.determinatedRenderer.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 1));

            }

            @Override
            public int getDefaultWidth() {
                return 80;
            }

            protected int getMaxWidth() {
                return getDefaultWidth();
            }

            /**
             * @return
             */
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            protected long getMax(ExtractionController value) {
                return 10000;
            }

            @Override
            protected long getValue(ExtractionController value) {
                return (long) (value.getProgress() * 10000);
            }
        });

        setSortColumn(sorter);
    }
}
