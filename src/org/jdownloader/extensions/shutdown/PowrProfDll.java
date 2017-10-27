package org.jdownloader.extensions.shutdown;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

//https://stackoverflow.com/questions/2413973/jna-with-complicated-struct
public interface PowrProfDll extends StdCallLibrary {
    public static final PowrProfDll INSTANCE = (PowrProfDll) com.sun.jna.Native.loadLibrary("PowrProf", PowrProfDll.class);

    public static class BATTERY_REPORTING_SCALE extends Structure {
        public long Granularity;
        public long Capacity;

        @Override
        protected List getFieldOrder() {
            final List<String> ret = new ArrayList<String>();
            for (final Field f : SYSTEM_POWER_CAPABILITIES.class.getDeclaredFields()) {
                ret.add(f.getName());
            }
            return ret;
        }
    }

    public static class SYSTEM_POWER_CAPABILITIES extends Structure {
        public boolean                 PowerButtonPresent;
        public boolean                 SleepButtonPresent;
        public boolean                 LidPresent;
        public boolean                 SystemS1;
        public boolean                 SystemS2;
        public boolean                 SystemS3;
        public boolean                 SystemS4;
        public boolean                 SystemS5;
        public boolean                 HiberFilePresent;
        public boolean                 FullWake;
        public boolean                 VideoDimPresent;
        public boolean                 ApmPresent;
        public boolean                 UpsPresent;
        public boolean                 ThermalControl;
        public boolean                 ProcessorThrottle;
        public int                     ProcessorMinThrottle;
        public int                     ProcessorMaxThrottle;
        public boolean                 FastSystemS4;
        public int                     spare2[]       = new int[3];
        public boolean                 DiskSpinDown;
        public int                     spare3[]       = new int[8];
        public boolean                 SystemBatteriesPresent;
        public boolean                 BatteriesAreShortTerm;
        public BATTERY_REPORTING_SCALE BatteryScale[] = new BATTERY_REPORTING_SCALE[3];
        public int                     AcOnLineWake;
        public int                     SoftLidWake;
        public int                     RtcWake;
        public int                     MinDeviceWakeState;
        public int                     DefaultLowLatencyWake;

        @Override
        protected List getFieldOrder() {
            final List<String> ret = new ArrayList<String>();
            for (final Field f : SYSTEM_POWER_CAPABILITIES.class.getDeclaredFields()) {
                ret.add(f.getName());
            }
            return ret;
        }
    }

    void GetPwrCapabilities(SYSTEM_POWER_CAPABILITIES result);
}
