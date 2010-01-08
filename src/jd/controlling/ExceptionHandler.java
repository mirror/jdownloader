//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.util.logging.Level;
import java.util.logging.Logger;

class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Logger logger;

    public ExceptionHandler(final Logger logger) {
        super();
        this.logger = logger;
    }

    public void uncaughtException(final Thread t, final Throwable e) {
        handle(e);
    }

    public void handle(final Throwable throwable) {
        try {
            logger.log(Level.SEVERE, "Uncaught Exception occurred", throwable);
        } catch (Throwable t) {
            // don't let the exception get thrown out, will cause infinite
            // looping!
        }
    }

    public static void register(final Logger logger) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(logger));
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
    }
}
