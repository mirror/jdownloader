/*
 * Copyright (C) 2011 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.jdownloader.extensions.neembuu.translate._NT;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChart;

import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;

/**
 * Comments by Jiri Sedlacek
 * 
 * <pre>
 * JFreeCharts are great for creating any kind of static graphs (typically for reports). 
 * They provide support for all types of existing chart types. 
 * The benefit of using JFreeChart is fully customizable appearance and 
 * export to various formats. The only problem of this library is that 
 * it's not primarily designed for displaying live data. You can hack it to 
 * display data in real time, but the performance is poor.
 * 
 * That's why I've created the VisualVM charts. The primary (and so far only) 
 * goal is to provide charts optimized for displaying live data with minimal 
 * performance and memory overhead. You can easily display a fullscreen graph 
 * and it will still scroll smoothly while running and adding new values 
 * (when running on physical hardware, virtualized environment may give 
 * slightly worse results). There's a real rendering engine behind the 
 * charts which ensures that only the changed areas of the chart are repainted 
 * (no full-repaints because of a 1px change). Scrolling the chart means moving 
 * the already rendered image and only painting the newly displayed area. Last 
 * but not least, the charts are optimized for displaying over a remote X session - 
 * rendering is automatically switched to low-quality ensuring good response 
 * times and interactivity.
 * 
 * The Tracer engine introduced in VisualVM 1.3 further improves 
 * performance of the charts. I've intensively profiled and optimized 
 * the charts to minimize the cpu cycles/memory allocations for each repaint. 
 * As of now, I believe that the VisualVM charts are the fastest real time 
 * Java charts with the lowest cpu/memory footprint.
 * </pre>
 * 
 * @author Shashank Tulsyan
 * @author Geertjan
 */
public final class SpeedGraphJFluid extends JPanel {

    private static final int           VALUES_LIMIT = 10;
    private final SimpleXYChartSupport support;

    private final SynchronousXYChart   actual_chart;

    public SpeedGraphJFluid() {
        SimpleXYChartDescriptor descriptor =
        // SimpleXYChartDescriptor.decimal(0,true,VALUES_LIMIT);
        SimpleXYChartDescriptor.decimal(0, 10/* 24*8 */, 10, 1d, true, VALUES_LIMIT);

        descriptor.addLineFillItems(_NT._.filePanel_graph_downloadSpeed());
        descriptor.addLineFillItems(_NT._.filePanel_graph_requestSpeed());

        // descriptor.setDetailsItems(new String[]{"Download Speed(KiB/s)",
        // "Request Speed(KiB/s)"});
        // descriptor.setChartTitle("<html><font size='+1'><b>SpeedGraph</b></font></html>");
        // descriptor.setXAxisDescription("<html>Time</html>");
        descriptor.setYAxisDescription(_NT._.filePanel_graph_yaxis());

        support = ChartFactory.createSimpleXYChart(descriptor);

        // new Generator(support).start();
        setLayout(new BorderLayout());
        add(support.getChart(), BorderLayout.CENTER);

        actual_chart = findSynchronousXYChart(support.getChart(), 0);
        if (actual_chart == null) throw new RuntimeException("Could not find actual chart");
    }

    private static SynchronousXYChart findSynchronousXYChart(Container k, int depth) {
        /*
         * for (int i = 0; i < depth; i++) { System.out.print("\t"); } System.out.println("+++++"+k+"+++++++");
         */
        for (Component c : k.getComponents()) {
            /*
             * for (int i = 0; i < depth; i++) { System.out.print("\t"); } System.out.println(c);
             */

            if (c instanceof SynchronousXYChart) return (SynchronousXYChart) c;

            if (c instanceof Container) {
                SynchronousXYChart chart = findSynchronousXYChart((Container) c, depth + 1);
                if (chart != null) return chart;
            }
        }
        return null;
        // System.out.println("-----"+k+"-------");
    }

    // @Override
    public void speedChanged(double downloadSpeedInKiBps, double requestSpeedInKiBps) {
        recentDownloadSpeedObservation = downloadSpeedInKiBps;
        recentRequestSpeedObservation = requestSpeedInKiBps;
        support.addValues(System.currentTimeMillis(), new long[] { (long) (recentDownloadSpeedObservation), (long) (recentRequestSpeedObservation) });

        maxValues[index % VALUES_LIMIT] = Math.max(recentRequestSpeedObservation, recentDownloadSpeedObservation);
        index++;

        double max = 0;
        for (int i = 0; i < maxValues.length; i++) {
            max = Math.max(maxValues[i], max);
        }
        if (max < previousHeight / 2 || previousHeight == -1) {
            previousHeight = max * 1.1;
            // System.out.println("contracting to "+previousHeight);
            changeHeight(previousHeight);
        }
        if (previousHeight < max) {
            previousHeight = max * 1.1;
            // System.out.println("expanding to "+previousHeight);
            changeHeight(previousHeight);
        }
    }

    private void changeHeight(double h) {
        /*
         * try{ System.out.println("before="+actual_chart.getDataHeight()); Field f =
         * TransformableCanvasComponent.class.getDeclaredField("dataHeight"); f.setAccessible(true); f.setLong(actual_chart, (long)h);
         * 
         * Method m = ChartComponent.class.getDeclaredMethod("dataBoundsChanged", long.class,long.class,long.class,long.class
         * ,long.class,long.class,long.class,long.class); m.setAccessible(true); m.invoke(actual_chart,
         * actual_chart.getOffsetX(),actual_chart.getOffsetY(), actual_chart.getDataWidth(),(long)h,
         * actual_chart.getOffsetX(),actual_chart.getOffsetY(), actual_chart.getDataWidth(),actual_chart.getDataHeight()); f =
         * TransformableCanvasComponent.class.getDeclaredField("maxOffsetY"); f.setAccessible(true); f.setLong(actual_chart, (long)h);
         * //pendingDataHeight f = TransformableCanvasComponent.class.getDeclaredField ("pendingDataHeight"); f.setAccessible(true);
         * f.setLong(actual_chart, (long)h);
         * 
         * System.out.println("after="+actual_chart.getDataHeight()); }catch(Exception a){ a.printStackTrace(System.err); }
         * actual_chart.setDataBounds(actual_chart.getDataOffsetX(), actual_chart.getDataOffsetY(), actual_chart.getDataWidth(), (long)(h));
         */
    }

    private volatile double recentDownloadSpeedObservation = 0;
    private volatile double recentRequestSpeedObservation  = 0;

    double[]                maxValues                      = new double[VALUES_LIMIT];
    double                  previousHeight                 = -1;
    int                     index                          = 0;

    public static void main(String[] args) {
        JFrame fr = new JFrame();
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(600, 400);
        fr.getContentPane().add(new SpeedGraphJFluid());
        WindowManager.getInstance().setVisible(fr, true, FrameState.FOCUS);
    }

}
