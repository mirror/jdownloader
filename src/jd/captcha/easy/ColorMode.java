package jd.captcha.easy;
public     class ColorMode {
    public static final ColorMode[] cModes = new ColorMode[] { new ColorMode(CPoint.LAB_DIFFERENCE, "LAB Difference"), new ColorMode(CPoint.RGB_DIFFERENCE1, "RGB1 Difference"), new ColorMode(CPoint.RGB_DIFFERENCE2, "RGB2 Difference"), new ColorMode(CPoint.HUE_DIFFERENCE, "Hue Difference"), new ColorMode(CPoint.SATURATION_DIFFERENCE, "Saturation Difference"), new ColorMode(CPoint.BRIGHTNESS_DIFFERENCE, "Brightness Difference"), new ColorMode(CPoint.RED_DIFFERENCE, "Red Difference"), new ColorMode(CPoint.GREEN_DIFFERENCE, "Green Difference"), new ColorMode(CPoint.BLUE_DIFFERENCE, "Blue Difference") };

    protected byte mode;
    private String modeString;
    public ColorMode(byte mode)
    {
        this.mode = mode;
        for (ColorMode m : cModes) {
            if(mode==m.mode)
            {
                this.modeString=m.modeString;
                break;
            }
        }
    }
    public ColorMode(byte mode, String modestString) {
        this.mode = mode;
        this.modeString = modestString;
    }

    @Override
    public boolean equals(Object arg0) {
        if ((arg0 == null) || !(arg0 instanceof ColorMode)) return false;
        return mode == ((ColorMode) arg0).mode;
    }

    public String toString() {
        return modeString;
    }
}