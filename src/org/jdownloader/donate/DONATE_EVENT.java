package org.jdownloader.donate;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.Icon;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public enum DONATE_EVENT {
    MUNICH_OKTOBERFEST(IconKey.ICON_BEER) {
        @Override
        public String getToolTipText() {
            return "Munich Oktoberfest! O'zapft is!";
        }

        @Override
        public boolean isNow() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int year = calendar.get(Calendar.YEAR);
            if (year == 2019) {
                if ((month == Calendar.SEPTEMBER && day >= 21) || (month == Calendar.OCTOBER && day <= 6)) {
                    return true;
                }
            }
            return false;
        }
    },
    NEWYEARSEVE(IconKey.ICON_CHAMPAGNE) {
        @Override
        public boolean isNow() {
            // last day of the year and first day of the year
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            if ((month == Calendar.DECEMBER && day == lastDay) || (month == Calendar.JANUARY && day == 1)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getToolTipText() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int year = calendar.get(Calendar.YEAR);
            final int happyNewYear;
            if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
                happyNewYear = year;
                return name() + "." + (year - 1) + "-" + year;
            } else {
                happyNewYear = year + 1;
            }
            return "Happy New Year " + happyNewYear + "!";
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
        public String getToolTipText() {
            return "Ho! Ho! Ho! Merry Christmas!";
        }

        @Override
        public boolean isNow() {
            // 7 days, from 20.12 - 26.12
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            if (month == Calendar.DECEMBER && day >= 20 && day <= 26) {
                return true;
            } else {
                return false;
            }
        }
    },
    HALLOWEEN(IconKey.ICON_HALLOWEEN) {
        @Override
        public String getToolTipText() {
            return "Happy Halloween!";
        }

        @Override
        public boolean isNow() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            if (month == Calendar.OCTOBER && day == 31) {
                return true;
            } else {
                return false;
            }
        }
    },
    VALENTINE(IconKey.ICON_VALENTINE) {
        @Override
        public String getToolTipText() {
            return "Happy Valentines Day!";
        }

        @Override
        public boolean isNow() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            if (month == Calendar.FEBRUARY && day == 14) {
                return true;
            } else {
                return false;
            }
        }
    },
    EASTER(IconKey.ICON_EASTER_EGG) {
        @Override
        public String getToolTipText() {
            return "We wish you a happy Easter!";
        }

        @Override
        public boolean isNow() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int year = calendar.get(Calendar.YEAR);
            if (year == 2018 && month == Calendar.APRIL && day == 1) {
                return true;
            } else if (year == 2019 && month == Calendar.APRIL && day == 21) {
                return true;
            } else if (year == 2020 && month == Calendar.APRIL && day == 12) {
                return true;
            } else if (year == 2021 && month == Calendar.APRIL && day == 4) {
                return true;
            } else {
                return false;
            }
        }
    },
    BLACK_FRIDAY(IconKey.ICON_BLACK_FRIDAY) {
        @Override
        public String getToolTipText() {
            return "Black Friday! Shopping! Deals! Donations!";
        }

        @Override
        public boolean isNow() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(timeStamp);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int month = calendar.get(Calendar.MONTH);
            final int year = calendar.get(Calendar.YEAR);
            if (month == Calendar.NOVEMBER && day == 23 && year == 2018) {
                return true;
            } else if (month == Calendar.NOVEMBER && day == 29 && year == 2019) {
                return true;
            } else if (month == Calendar.NOVEMBER && day == 27 && year == 2020) {
                return true;
            } else if (month == Calendar.NOVEMBER && day == 26 && year == 2021) {
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
        public String getTitleText() {
            return _GUI.T.DonationDialog_DonationDialog_title_();
        }

        @Override
        public String getToolTipText() {
            return null;
        }

        @Override
        public boolean matchesID(String id) {
            return true;
        }
    },
    DEBUG(IconKey.ICON_CONSOLE) {
        @Override
        public boolean isNow() {
            return false;
        }

        final long timeStamp = System.currentTimeMillis();

        @Override
        public String getToolTipText() {
            return "Testing";
        }

        @Override
        public String getID() {
            return name() + "." + timeStamp;
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

    public String getTitleText() {
        return getToolTipText();
    }

    public String getText() {
        return _GUI.T.DonationDialog_layoutDialogContent_top_text();
    }

    public abstract String getToolTipText();

    public String getID() {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timeStamp);
        return name() + "." + calendar.get(Calendar.YEAR);
    }

    public static DONATE_EVENT getNow() {
        if (true && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return DONATE_EVENT.DEBUG;
        } else {
            for (final DONATE_EVENT donateEvent : values()) {
                if (donateEvent.isNow()) {
                    return donateEvent;
                }
            }
            return DONATE_EVENT.DEFAULT;
        }
    }

    public boolean matchesID(String id) {
        return StringUtils.equals(getID(), id);
    }
}
