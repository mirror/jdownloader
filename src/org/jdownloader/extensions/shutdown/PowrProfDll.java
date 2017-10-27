package org.jdownloader.extensions.shutdown;

import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public interface PowrProfDll extends StdCallLibrary {
    PowrProfDll INSTANCE = (PowrProfDll) com.sun.jna.Native.loadLibrary("PowrProf", PowrProfDll.class);

    public static class BATTERY_REPORTING_SCALE extends Structure {
        public long Granularity;
        public long Capacity;

        @Override
        protected List getFieldOrder() {
            return null;
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
            return null;
        }
    }

    void GetPwrCapabilities(SYSTEM_POWER_CAPABILITIES result);

    public static void main(String[] args) {
        PowrProfDll lib2 = PowrProfDll.INSTANCE;
        PowrProfDll.SYSTEM_POWER_CAPABILITIES systemPOWERCAPABILITIES = new PowrProfDll.SYSTEM_POWER_CAPABILITIES();
        lib2.GetPwrCapabilities(systemPOWERCAPABILITIES);
        System.out.println("HibernateFile:" + systemPOWERCAPABILITIES.HiberFilePresent);
        System.out.println("Hibernate:" + systemPOWERCAPABILITIES.SystemS4);
    }
}
