package org.jdownloader.donate;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.Icon;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public enum DONATE_EVENT {
    NEWYEARSEVE(IconKey.ICON_CHAMPAGNE) {
        @Override
        public boolean isNow() {
            // last day of the year and first day of the year
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            if ((month == 11 && day == lastDay) || (month == 0 && day == 1)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getID() {
            // NEWYEARSEVE.CurrentYear-NextYear
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int year = calendar.get(Calendar.YEAR);
            if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
                return name() + "." + (year - 1) + "-" + year;
            } else {
                return name() + "." + year + "-" + (year + 1);
            }
        }
    },
    XMAS(IconKey.ICON_XMAS_GIFT) {
        @Override
        public boolean isNow() {
            // 7 days, from 20.12 - 26.12
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            if (month == 11 && day >= 20 && day <= 26) {
                return true;
            } else {
                return false;
            }
        }
    },
    BLACK_FRIDAY(IconKey.ICON_BLACK_FRIDAY) {
        @Override
        public boolean isNow() {
            // Black Friday, 24.12.2017
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int year = calendar.get(Calendar.YEAR);
            if (month == 10 && day == 24 && year == 2017) {
                return true;
            } else {
                return false;
            }
        }
    },
    DEFAULT(IconKey.ICON_HEART) {
        @Override
        public boolean isNow() {
            return true;
        }

        @Override
        public boolean matchesID(String id) {
            return true;
        }
    };
    private static final long timeStamp = System.currentTimeMillis();
    private final String      iconKey;

    private DONATE_EVENT(final String iconKey) {
        this.iconKey = iconKey;
    }

    public final String getIconKey() {
        return iconKey;
    }

    public final Icon getIcon() {
        return new AbstractIcon(getIconKey(), 16);
    }

    public abstract boolean isNow();

    public String getID() {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timeStamp);
        return name() + "." + calendar.get(Calendar.YEAR);
    }

    public static DONATE_EVENT getNow() {
        for (final DONATE_EVENT donateEvent : values()) {
            if (donateEvent.isNow()) {
                return donateEvent;
            }
        }
        return DONATE_EVENT.DEFAULT;
    }

    public boolean matchesID(String id) {
        return StringUtils.equals(getID(), id);
    }
}
