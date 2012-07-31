/*
 * Copyright (C) 2012 Shashank Tulsyan
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
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import neembuu.rangearray.ModificationType;
import neembuu.rangearray.Range;
import neembuu.rangearray.RangeArrayListener;
import neembuu.rangearray.RangeUtils;
import neembuu.rangearray.UnsyncRangeArrayCopy;
import neembuu.swing.RangeArrayComponent;
import neembuu.swing.RangeArrayElementColorProvider;
import neembuu.swing.RangeArrayElementColorProvider.SelectionState;
import neembuu.swing.RangeArrayElementToolTipTextProvider;
import neembuu.swing.RangeSelectedListener;
import neembuu.util.logging.LoggerUtil;
import neembuu.vfs.file.MonitoredHttpFile;
import neembuu.vfs.readmanager.RegionHandler;

import org.appwork.utils.logging.Log;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdownloader.extensions.neembuu.translate._NT;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class HttpFilePanel extends javax.swing.JPanel implements RangeSelectedListener {

    private final MonitoredHttpFile                 file;
    private SpeedGraphJFluid                        graphJFluid                     = null;
    private volatile String                         virtualPathOfFile               = null;
    private static final Logger                     LOGGER                          = LoggerUtil.getLogger();
    private volatile Range                          lastRegionSelected              = null;
    private volatile RegionHandler                  lastRegionHandler               = null;
    // private final Object rangeSelectedLock = new Object();
    RegionHandler                                   previousRegionOfInterest;
    // Map<Long,Color> rm = new
    private final RequestedRegionColorProvider      requestedRegionColorProvider    = new RequestedRegionColorProvider();
    private final JLabel                            activateLable                   = new JLabel(_NT._.filePanel_graph_noRegionSelected());
    // ,new
    // ImageIcon(HttpFilePanel.class.getResource("activate.png")),SwingConstants.CENTER
    private final RangeArrayElementColorProvider    downloadedRegionColorProvider   = new DownloadedRegionColorProvider();
    private final DownloadedRegionToolTipProvider   downloadedRegionToolTipProvider = new DownloadedRegionToolTipProvider();
    
    private final class DownloadedRegionColorProvider implements RangeArrayElementColorProvider {

        // @Override
        public Color getColor(Color defaultColor, Range element, SelectionState selectionState) {
            RegionHandler rh = (RegionHandler) element.getProperty();
            Color base = defaultColor;            
            try {
                if (rh.isAlive()) {
                    base = new Color(102, 93, 149);
                    // base=new
                    // Color(126,193,160);//RangeArrayComponent.lightenColor(Color.GREEN,0.56f);
                }
            } catch (NullPointerException npe) {
                return defaultColor;
            }
            if (rh.isMainDirectionOfDownload()) {
                if (!rh.isAlive()) {
                    base = new Color(168, 122, 92);
                } else {
                    // base=new Color(30,50,200);//
                    // RangeArrayComponent.lightenColor(Color.BLUE,0.56f);
                    base = new Color(138, 170, 231);
                }
            }
            if(file.getDownloadConstrainHandler().isComplete()){
                base = new Color(185,198,222); // download completed
            }
            switch (selectionState) {
            case LIST:
                base = RangeArrayComponent.lightenColor(base, 0.9f);
                break;
            case MOUSE_OVER:
                base = RangeArrayComponent.lightenColor(base, 0.8f);
                break;
            case SELECTED:
                base = RangeArrayComponent.lightenColor(base, 0.7f);
                break;
            case NONE:
                break;
            }

            return base;
        }
    };

    private static final class RequestedRegionColorProvider implements RangeArrayElementColorProvider, RangeArrayListener {

        long lastModStart, lastModEnd;

        // @Override
        public void rangeArrayModified(long modificationResultStart, long modificationResultEnd, Range elementOperated, ModificationType modificationType, boolean removed, long modCount) {
            lastModEnd = modificationResultEnd;
            lastModStart = modificationResultStart;
        }

        // @Override
        public Color getColor(Color defaultColor, Range element, SelectionState selectionState) {

            Color base = defaultColor;
            if (RangeUtils.contains(element, lastModStart, lastModEnd)) {
                base = new Color(138, 170, 231);
            }

            switch (selectionState) {
            case LIST:
                base = RangeArrayComponent.lightenColor(base, 0.9f);
                break;
            case MOUSE_OVER:
                base = RangeArrayComponent.lightenColor(base, 0.8f);
                break;
            case SELECTED:
                break;
            case NONE:
                break;
            }

            return base;
        }
    };
    
    private final class DownloadedRegionToolTipProvider implements RangeArrayElementToolTipTextProvider{

        public String getToolTipText(Range element, SelectionState selectionState) {
            if(!(element.getProperty() instanceof RegionHandler))return "";
            RegionHandler regionHandler = (RegionHandler)element.getProperty();
            String toRet = "";
            if(file.getDownloadConstrainHandler().isComplete()){
                toRet = "File completely downloaded \n";
            }
            return toRet + NumberFormat.getInstance().format( regionHandler.getThrottleStatistics().getDownloadSpeed_KiBps())+" KBps \n"+
                    " RequestSpeed = " + NumberFormat.getInstance().format( regionHandler.getThrottleStatistics().getRequestSpeed_KiBps())+" KBps \n"+
                    (regionHandler.isAlive()?"alive":"dead")+" "+(regionHandler.isMainDirectionOfDownload()?"main":"notmain")  ;
        }
        
    }

    final Timer updateGraphTimer = new Timer(500, new ActionListener() {
                                     int x = 0;
                                     // @Override
                                     public void actionPerformed(ActionEvent e) {
                                         double d = file.getTotalFileReadStatistics().getTotalAverageDownloadSpeedProvider().getDownloadSpeed_KiBps(),r=file.getTotalFileReadStatistics().getTotalAverageRequestSpeedProvider().getRequestSpeed_KiBps();
                                         downloadSpeedVal.setText(new DecimalFormat("###,###,###.###").format(d) + " KiBps");
                                         requestRateVal.setText(new DecimalFormat("###,###,###.###").format(r) + " KiBps");

                                         RegionHandler currentlySelectedRegion = lastRegionHandler;
                                         if (currentlySelectedRegion == null) {
                                             throttleStateLable.setText("");
                                             return;
                                         }
                                         if (graphJFluid != null) {
                                             graphJFluid.speedChanged(currentlySelectedRegion.getThrottleStatistics().getDownloadSpeed_KiBps(), currentlySelectedRegion.getThrottleStatistics().getRequestSpeed_KiBps());
                                         }
                                         throttleStateLable.setText(currentlySelectedRegion.getThrottleStatistics().getThrottleState().toString());
                                         if(x%8==0){
                                            regionDownloadedBar.repaint();
                                            regionRequestedBar.repaint();
                                         }x++;
                                     }
                                 });

    /**
     * Creates new form MonitoredHttpFilePanel
     */
    public HttpFilePanel(MonitoredHttpFile par) {
        this.file = par;
        initComponents();
        ((JXCollapsiblePane) graphAndAdvancedPanel).setCollapsed(true);
        printStateButton.setVisible(false);
        this.speedGraphPanel.setLayout(new BorderLayout());
        activateLable.setFont(new Font(activateLable.getFont().getName(), activateLable.getFont().getStyle(), (int) (activateLable.getFont().getSize() * 1.5)));
        this.speedGraphPanel.add(activateLable);

        if (par != null) {
            fileNameValue.setText(par.getName());
            fileSizeValue.setText(formatSize(par.getFileSize()));
            connectionDescText.setText(par.getSourceDescription());
        }
        ((RangeArrayComponent) regionDownloadedBar).addRangeSelectedListener(this);
        updateGraphTimer.start();

        file.getRequestedRegion().addRangeArrayListener(requestedRegionColorProvider);
        translate();
    }

    private String formatSize(long fsz) {
        int dig = (int) (Math.log10(fsz)) + 1;

        if (dig > 3 && dig <= 6) { return fsz / 1024d + " KiB"; }
        if (dig > 6 && dig <= 9) { return fsz / (1024*1024d) + " MiB"; }
        if (dig > 9 && dig <= 12) { return fsz /(1024*1024*1024d) + " GiB"; }
        if (dig > 12) { return fsz / (1024*1024*1024*1024d) + " TiB"; }
        return fsz + "Bytes";
    }

    private void translate() {
        // /regionDownloadedLbl.setText("Region\nDownloaded");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        normalControls = new javax.swing.JPanel();
        downloadSpeedVal = new javax.swing.JLabel();
        filenameLabel = new javax.swing.JLabel();
        fileSizeValue = new javax.swing.JLabel();
        urlLabel = new javax.swing.JLabel();
        requestRateVal = new javax.swing.JLabel();
        totalRequestRateLabel = new javax.swing.JLabel();
        totalDownloadSpeedLabel = new javax.swing.JLabel();
        toggleAdvancedView = new javax.swing.JToggleButton();
        filesizeLabel = new javax.swing.JLabel();
        fileNameValue = new javax.swing.JLabel();
        connectionDescText = new javax.swing.JTextField();
        openFile = new javax.swing.JButton();
        autoCompleteEnabledButton = new javax.swing.JToggleButton();
        jPanel1 = new javax.swing.JPanel();
        regionDownloadedBar = new neembuu.swing.RangeArrayComponent(file.getRegionHandlers(),downloadedRegionColorProvider,true,downloadedRegionToolTipProvider);
        jPanel2 = new javax.swing.JPanel();
        regionRequestedBar = new neembuu.swing.RangeArrayComponent(file.getRequestedRegion(),requestedRegionColorProvider,true);
        graphAndAdvancedPanel = new JXCollapsiblePane();
        connectionStatusLabel = new javax.swing.JLabel();
        previousButton = new javax.swing.JButton();
        throttleStateLable = new javax.swing.JLabel();
        nextButton = new javax.swing.JButton();
        killConnection = new javax.swing.JButton();
        speedGraphPanel = new javax.swing.JPanel();
        printStateButton = new javax.swing.JButton();

        setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setMaximumSize(new java.awt.Dimension(628, 10000));
        setVerifyInputWhenFocusTarget(false);
        setLayout(new java.awt.BorderLayout());

        normalControls.setMinimumSize(new java.awt.Dimension(619, 250));
        normalControls.setName("");
        normalControls.setPreferredSize(new java.awt.Dimension(619, 250));
        normalControls.setLayout(null);

        downloadSpeedVal.setText("KiBps");
        normalControls.add(downloadSpeedVal);
        downloadSpeedVal.setBounds(220, 210, 170, 16);

        filenameLabel.setText(_NT._.filePanel_linkFileName());
        normalControls.add(filenameLabel);
        filenameLabel.setBounds(3, 55, 84, 16);

        fileSizeValue.setText("filesize");
        normalControls.add(fileSizeValue);
        fileSizeValue.setBounds(115, 32, 230, 16);

        urlLabel.setText(_NT._.filePanel_link());
        normalControls.add(urlLabel);
        urlLabel.setBounds(3, 6, 57, 16);

        requestRateVal.setText("KiBps");
        normalControls.add(requestRateVal);
        requestRateVal.setBounds(220, 230, 170, 16);

        totalRequestRateLabel.setText(_NT._.filePanel_totalRequestRate());
        normalControls.add(totalRequestRateLabel);
        totalRequestRateLabel.setBounds(10, 230, 190, 16);

        totalDownloadSpeedLabel.setText(_NT._.filePanel_totalDownloadSpeed());
        normalControls.add(totalDownloadSpeedLabel);
        totalDownloadSpeedLabel.setBounds(10, 210, 200, 16);

        toggleAdvancedView.setText(_NT._.filePanel_advancedViewButton());
        toggleAdvancedView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleAdvancedViewActionPerformed(evt);
            }
        });
        normalControls.add(toggleAdvancedView);
        toggleAdvancedView.setBounds(402, 210, 190, 25);

        filesizeLabel.setText(_NT._.filePanel_linkFileSize());
        normalControls.add(filesizeLabel);
        filesizeLabel.setBounds(3, 32, 74, 16);

        fileNameValue.setText("filename_");
        normalControls.add(fileNameValue);
        fileNameValue.setBounds(115, 55, 290, 16);

        connectionDescText.setEditable(false);
        normalControls.add(connectionDescText);
        connectionDescText.setBounds(115, 3, 483, 22);

        openFile.setText(_NT._.filePanel_openFile());
        openFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileActionPerformed(evt);
            }
        });
        normalControls.add(openFile);
        openFile.setBounds(440, 60, 160, 25);

        autoCompleteEnabledButton.setText(_NT._.filePanel_autoCompleteButtonEnabled());
        autoCompleteEnabledButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoCompleteEnabledButtonActionPerformed(evt);
            }
        });
        normalControls.add(autoCompleteEnabledButton);
        autoCompleteEnabledButton.setBounds(360, 30, 240, 25);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, _NT._.filePanel_regionDownloadedTitle(), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel1.setLayout(new java.awt.BorderLayout());

        regionDownloadedBar.setMaximumSize(new java.awt.Dimension(32767, 10));
        regionDownloadedBar.setMinimumSize(new java.awt.Dimension(10, 10));
        regionDownloadedBar.setPreferredSize(new java.awt.Dimension(146, 10));
        jPanel1.add(regionDownloadedBar, java.awt.BorderLayout.CENTER);

        normalControls.add(jPanel1);
        jPanel1.setBounds(10, 80, 590, 65);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, _NT._.filePanel_regionRequestedTitle(), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.add(regionRequestedBar, java.awt.BorderLayout.CENTER);

        normalControls.add(jPanel2);
        jPanel2.setBounds(10, 145, 590, 65);

        add(normalControls, java.awt.BorderLayout.CENTER);

        graphAndAdvancedPanel.setMinimumSize(new java.awt.Dimension(619, 300));
        graphAndAdvancedPanel.setName("");
        graphAndAdvancedPanel.setPreferredSize(new java.awt.Dimension(619, 300));
        graphAndAdvancedPanel.setLayout(null);

        connectionStatusLabel.setText("No connection selected");
        graphAndAdvancedPanel.add(connectionStatusLabel);
        connectionStatusLabel.setBounds(0, 32, 590, 16);

        previousButton.setText(_NT._.filePanel_selectPreviousRegionButton());
        previousButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousButtonActionPerformed(evt);
            }
        });
        graphAndAdvancedPanel.add(previousButton);
        previousButton.setBounds(396, 0, 190, 25);

        throttleStateLable.setText("Throttle state");
        graphAndAdvancedPanel.add(throttleStateLable);
        throttleStateLable.setBounds(0, 55, 379, 16);

        nextButton.setText(_NT._.filePanel_selectNextRegionButton());
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });
        graphAndAdvancedPanel.add(nextButton);
        nextButton.setBounds(122, 0, 260, 25);

        killConnection.setText(_NT._.filePanel_killConnection());
        killConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                killConnectionActionPerformed(evt);
            }
        });
        graphAndAdvancedPanel.add(killConnection);
        killConnection.setBounds(430, 50, 163, 25);

        speedGraphPanel.setMaximumSize(new java.awt.Dimension(412, 350));
        speedGraphPanel.setName(""); // NOI18N
        speedGraphPanel.setPreferredSize(new java.awt.Dimension(412, 200));
        speedGraphPanel.setLayout(new java.awt.BorderLayout());
        graphAndAdvancedPanel.add(speedGraphPanel);
        speedGraphPanel.setBounds(0, 78, 600, 213);

        printStateButton.setText("Print State");
        printStateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printStateButtonActionPerformed(evt);
            }
        });
        graphAndAdvancedPanel.add(printStateButton);
        printStateButton.setBounds(10, 10, 93, 19);

        add(graphAndAdvancedPanel, java.awt.BorderLayout.SOUTH);

        getAccessibleContext().setAccessibleDescription("");
    }// </editor-fold>//GEN-END:initComponents

    public void enableOpenButton(boolean enable) {
        openFile.setEnabled(enable);
    }

    private void printStateButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_printStateButtonActionPerformed
        System.out.println("+++++++" + file.getName() + file.getFileDescriptor().getFileId() + "++++++");
        System.out.println("Requested Region");
        System.out.println(file.getRequestedRegion());
        System.out.println("-------------------------------");
        System.out.println("Downloaded Region");
        System.out.println(file.getRegionHandlers());
        System.out.println("-------" + file.getName() + "------");
        System.out.println();
        System.out.println("Pending regions in each connection");

        try {
            UnsyncRangeArrayCopy<RegionHandler> downloadedRegions = file.getRegionHandlers().tryToGetUnsynchronizedCopy();
            Range<RegionHandler> downloadedRegion;
            for (int j = 0; j < downloadedRegions.size(); j++) {
                downloadedRegion = downloadedRegions.get(j);
                System.out.println("In Channel=" + downloadedRegion);

                String[] pendingRqs = downloadedRegion.getProperty().getPendingOperationsAsString();
                System.out.println("++++++++++++++++++");
                for (int i = 0; i < pendingRqs.length; i++) {
                    System.out.println(pendingRqs[i]);
                }
                System.out.println("------------------");
                // ((neembuu.vfs.readmanager.impl.BasicRegionHandler)downloadedRegion.getProperty()).printPendingOps(System.out);
            }
        } catch (Exception l) {
            System.out.println("could not print");
        }
        System.out.println("-------------------------------");
    }// GEN-LAST:event_printStateButtonActionPerformed

    final Object regionTraversalLock = new Object();

    @SuppressWarnings(value = "unchecked")
    private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_previousButtonActionPerformed
        // TODO add your handling code here:
        synchronized (regionTraversalLock) {
            if (lastRegionSelected == null) {
                if (file.getRequestedRegion().isEmpty()) { return; }
                ((RangeArrayComponent) regionDownloadedBar).selectRange(file.getRegionHandlers().getFirst());
            }
            try {
                Range previouselement = file.getRegionHandlers().getPrevious(lastRegionSelected);
                ((RangeArrayComponent) regionDownloadedBar).selectRange(previouselement);
                lastRegionSelected = previouselement;
            } catch (ArrayIndexOutOfBoundsException exception) {
                // ignore
            } catch (Exception anyother) {
                LOGGER.log(Level.SEVERE, "problem in region traversing", anyother);
            }
        }

    }// GEN-LAST:event_previousButtonActionPerformed

    @SuppressWarnings(value = "unchecked")
    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_nextButtonActionPerformed
        synchronized (regionTraversalLock) {
            if (lastRegionSelected == null) {
                if (file.getRequestedRegion().isEmpty()) { return; }
                ((RangeArrayComponent) regionDownloadedBar).selectRange(file.getRegionHandlers().getFirst());
            }
            try {
                System.out.println("previous=" + lastRegionSelected);
                // System.out.println("(previousindex)="+file.getDownloadedRegion().get(file.getRequestedRegion().getIndexOf(lastRegionSelected)));
                Range nextElement = file.getRegionHandlers().getNext(lastRegionSelected);
                System.out.println("next=" + nextElement);
                System.out.println("last=" + lastRegionSelected);
                lastRegionSelected = nextElement;
                ((RangeArrayComponent) regionDownloadedBar).selectRange(nextElement);
            } catch (ArrayIndexOutOfBoundsException exception) {
                // ignore
            } catch (Exception anyother) {
                LOGGER.log(Level.SEVERE, "problem in region traversing", anyother);
            }
        }
    }// GEN-LAST:event_nextButtonActionPerformed

    private void killConnectionActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_killConnectionActionPerformed
        System.err.println("kill button pressed for region=" + lastRegionSelected);
        try {
            if(file.getParams().getConnectionProvider().estimateCreationTime(0)>=Integer.MAX_VALUE){
                JOptionPane.showMessageDialog(this, "This connection cannot be resumed,\nwhich is why this should not be killed.","Cannot kill connection",JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            neembuu.vfs.readmanager.impl.BasicRegionHandler regionHandler = ((neembuu.vfs.readmanager.impl.BasicRegionHandler) lastRegionSelected.getProperty());
            regionHandler.getConnection().abort();
        } catch (Exception any) {
            LOGGER.log(Level.SEVERE, "Connection killing exception", any);
        }
    }// GEN-LAST:event_killConnectionActionPerformed

    private void toggleAdvancedViewActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleAdvancedViewActionPerformed
        // TODO add your handling code here:
        graphAndAdvancedPanel.getActionMap().get("toggle").actionPerformed(evt);
    }// GEN-LAST:event_toggleAdvancedViewActionPerformed

    private void autoCompleteEnabledButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_autoCompleteEnabledButtonActionPerformed
        // TODO add your handling code here:
        file.setAutoCompleteEnabled(!file.isAutoCompleteEnabled());
        if (file.isAutoCompleteEnabled()) {
            autoCompleteEnabledButton.setText(_NT._.filePanel_autoCompleteButtonEnabled());
        } else {
            autoCompleteEnabledButton.setText(_NT._.filePanel_autoCompleteButtonDisabled());
        }
    }// GEN-LAST:event_autoCompleteEnabledButtonActionPerformed

    private void openFileActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_openFileActionPerformed
        // TODO add your handling code here:
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(virtualPathOfFile));
        } catch (Exception a) {
            JOptionPane.showMessageDialog(this, "Could not show the file");
            Log.L.log(Level.SEVERE,"Could not show the file",a);
        }
    }// GEN-LAST:event_openFileActionPerformed

    public void setVirtualPathOfFile(String virtualPathOfFile) {
        this.virtualPathOfFile = virtualPathOfFile;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton autoCompleteEnabledButton;
    private javax.swing.JTextField connectionDescText;
    private javax.swing.JLabel connectionStatusLabel;
    public javax.swing.JLabel downloadSpeedVal;
    private javax.swing.JLabel fileNameValue;
    private javax.swing.JLabel fileSizeValue;
    private javax.swing.JLabel filenameLabel;
    private javax.swing.JLabel filesizeLabel;
    private javax.swing.JPanel graphAndAdvancedPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton killConnection;
    private javax.swing.JButton nextButton;
    private javax.swing.JPanel normalControls;
    private javax.swing.JButton openFile;
    private javax.swing.JButton previousButton;
    private javax.swing.JButton printStateButton;
    public javax.swing.JProgressBar regionDownloadedBar;
    public javax.swing.JProgressBar regionRequestedBar;
    private javax.swing.JLabel requestRateVal;
    private javax.swing.JPanel speedGraphPanel;
    private javax.swing.JLabel throttleStateLable;
    private javax.swing.JToggleButton toggleAdvancedView;
    private javax.swing.JLabel totalDownloadSpeedLabel;
    private javax.swing.JLabel totalRequestRateLabel;
    private javax.swing.JLabel urlLabel;
    // End of variables declaration//GEN-END:variables
    // @Override
    public void rangeSelected(Range arrayElement) {
        LOGGER.log(Level.INFO, "region selected {0}", arrayElement);
        lastRegionSelected = arrayElement;

        RegionHandler regionOfInterest;

        if (arrayElement == null) {
            connectionStatusLabel.setText("No connection selected");
            this.speedGraphPanel.removeAll();
            this.speedGraphPanel.add(activateLable, BorderLayout.CENTER);
            lastRegionHandler = null;
            graphJFluid = null;
            this.repaint();
            return;
        }

        regionOfInterest = (RegionHandler) arrayElement.getProperty();
        lastRegionHandler = regionOfInterest;

        LOGGER.info("Channel of interest=" + regionOfInterest);

        connectionStatusLabel.setText("Connection " + arrayElement.starting() + " selected. This connection is " + (regionOfInterest.isAlive() ? "alive." : "dead."));
        throttleStateLable.setText(regionOfInterest.getThrottleStatistics().getThrottleState().toString());

        graphJFluid = new SpeedGraphJFluid();
        this.speedGraphPanel.removeAll();
        this.speedGraphPanel.add(graphJFluid, BorderLayout.CENTER);
        this.repaint();
    }
}