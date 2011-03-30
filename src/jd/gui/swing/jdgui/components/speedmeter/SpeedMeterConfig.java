package jd.gui.swing.jdgui.components.speedmeter;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;

public interface SpeedMeterConfig extends ConfigInterface {
    /**
     * How many seconds the speedmeter shall show/record. Please note that big
     * Timeframes and high fps values may cause high CPU usage
     * 
     * @return
     */
    @DefaultIntValue(30)
    int getTimeFrame();

    /**
     * How many refreshes and datasamples the speedmeter uses. Please note that
     * big Timeframes and high fps values may cause high CPU usage
     * 
     * @return
     */
    @DefaultIntValue(4)
    int getFramesPerSecond();

    /**
     * Hex color for the current speed graph. this graph uses 2 colors. a and b
     * which create a gradient together The first 2 Hexvalues are Alpha
     * (TRansparency)
     * 
     * @return
     */
    @DefaultStringValue("2051F251")
    String getCurrentColorA();

    /**
     * Hex color for the current speed graph. this graph uses 2 colors. a and b
     * which create a gradient together The first 2 Hexvalues are Alpha
     * (TRansparency)
     * 
     * @return
     */
    @DefaultStringValue("CC3DC83D")
    String getCurrentColorB();

    /**
     * Hex Color String for the average graph. The last 2 hexvalues are
     * transparency
     * 
     * @return
     */
    @DefaultStringValue("FF359E35")
    String getAverageGraphColor();

    /**
     * Hex Color String for the limit marker. The last 2 hexvalues are
     * transparency
     * 
     * @return
     */
    @DefaultStringValue("00FF0000")
    String getLimitColorB();

    /**
     * Hex Color String for the limit marker. The last 2 hexvalues are
     * transparency
     * 
     * @return
     */

    @DefaultStringValue("ccFF0000")
    String getLimitColorA();
}
