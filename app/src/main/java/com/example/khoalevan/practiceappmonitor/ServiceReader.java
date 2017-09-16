package com.example.khoalevan.practiceappmonitor;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by khoalevan on 6/10/17.
 */

public class ServiceReader extends Service {

    private boolean firstRead = true;
    private int memTotal, intervalRead, intervalUpdate, intervalWidth, pId, maxSample = 200;
    private List<Float> cpuTotal, cpuAM;
    private List<Integer> memoryAM;
    private String s;
    private long total,totalT, work, workT, totalBefore, workBefore, workAM, workAMT, workAMBefore;
    private List<Map<String, Object>> mListSelected;

    private List<String> memUsed, memAvailable, memFree, cached, threshold;
    private ActivityManager am;
    private Debug.MemoryInfo[] amMI;
    private String[] sa;
    private ActivityManager.MemoryInfo mi;
    private SharedPreferences mPrefs;
    private BufferedReader reader;

    private Runnable readRunnable = new Runnable() {
        @Override
        public void run() {
            Thread thisThread = Thread.currentThread();
            while (readThread == thisThread) {
                read();
                try {
                    Thread.sleep(intervalRead);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    };

    private volatile Thread readThread = new Thread(readRunnable, C.readThread);

    @Override
    public void onCreate() {
        cpuTotal = new ArrayList<Float>(maxSample);
        cpuAM = new ArrayList<Float>(maxSample);
        memoryAM = new ArrayList<Integer>(maxSample);
        memUsed = new ArrayList<String>(maxSample);
        memAvailable = new ArrayList<String>(maxSample);
        memFree = new ArrayList<String>(maxSample);
        cached = new ArrayList<String>(maxSample);
        threshold = new ArrayList<String>(maxSample);

        pId = Process.myPid();

        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        amMI = am.getProcessMemoryInfo(new int[]{pId});
        mi = new ActivityManager.MemoryInfo();

        mPrefs = getSharedPreferences("PracticeAppMonitor" + C.prefs, MODE_PRIVATE);
        intervalRead = mPrefs.getInt(C.intervalRead, C.defaultIntervalRead);
        intervalUpdate = mPrefs.getInt(C.intervalUpdate, C.defaultIntervalUpdate);
        intervalWidth = mPrefs.getInt(C.intervalWidth, C.defaultIntervalWidth);

        readThread.start();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceReaderDataBinder();
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("unchecked")
    private void read() {
        try {
            reader = new BufferedReader(new FileReader("/proc/meminfo"));
            s = reader.readLine();

            while (s != null) {
                while (memFree.size() >= maxSample) {
                    cpuTotal.remove(cpuTotal.size() - 1);
                    cpuAM.remove(cpuAM.size() - 1);
                    memoryAM.remove(memoryAM.size() - 1);

                    memUsed.remove(memUsed.size() - 1);
                    memAvailable.remove(memAvailable.size() - 1);
                    memFree.remove(memFree.size() - 1);
                    cached.remove(cached.size() - 1);
                    threshold.remove(threshold.size() - 1);
                }

                if (mListSelected != null && !mListSelected.isEmpty()) {
                    List<Integer> l = (List<Integer>) mListSelected.get(0).get(C.pFinalValue);
                    if (l != null && l.size() >= maxSample) {
                        for (Map<String, Object> m : mListSelected) {
                            ((List<Integer>)m.get(C.pFinalValue)).remove(l.size()-1);
                            ((List<Integer>) m.get(C.pTPD)).remove(((List<Integer>) m.get(C.pTPD)).size() - 1);
                        }
                    }
                    for (Map<String, Object> m : mListSelected) {
                        l = (List<Integer>) m.get(C.pFinalValue);
                        if (l == null)
                            break;
                        while (l.size() >= maxSample)
                            l.remove(l.size() - 1);
                        l = (List<Integer>) m.get(C.pTPD);
                        while (l.size() >= maxSample)
                            l.remove(l.size() - 1);
                    }
                }

                if (firstRead && s.startsWith("MemTotal:")) {
                    memTotal = Integer.parseInt(s.split("[ ]+", 3)[1]);
                    firstRead = false;
                } else if (s.startsWith("MemFree:"))
                    memFree.add(0, s.split("[ ]+", 3)[1]);
                else if (s.startsWith("Cached:"))
                    cached.add(0, s.split("[ ]+", 3)[1]);

                s = reader.readLine();
            }

            reader.close();

            am.getMemoryInfo(mi);

            if (mi == null) {
                memUsed.add(0, String.valueOf(0));
                memAvailable.add(0, String.valueOf(0));
                threshold.add(0, String.valueOf(0));
            } else {
                memUsed.add(0, String.valueOf(memTotal - mi.availMem/1024));
                memAvailable.add(0, String.valueOf(mi.availMem/1024));
                threshold.add(0, String.valueOf(mi.threshold/1024));
            }

            memoryAM.add(amMI[0].getTotalPrivateDirty());



            ////////////////////


            reader = new BufferedReader(new FileReader("/proc/stat"));
            sa = reader.readLine().split("[ ]+", 9);

            work = Long.parseLong(sa[1]) + Long.parseLong(sa[2]) + Long.parseLong(sa[3]);
            total = work + Long.parseLong(sa[4]) + Long.parseLong(sa[5]) + Long.parseLong(sa[6]) + Long.parseLong(sa[7]);
            reader.close();

            reader = new BufferedReader(new FileReader("/proc/" + pId + "/stat"));
            sa = reader.readLine().split("[ ]+", 18);
            workAM = Long.parseLong(sa[13]) + Long.parseLong(sa[14]) + Long.parseLong(sa[15]) + Long.parseLong(sa[16]);
            reader.close();

            if (totalBefore != 0) {
                totalT = total - totalBefore;
                workT= work - workBefore;
                workAMT = workAM -workAMBefore;

                cpuTotal.add(0, restrictPercentage(workT * 100 / (float) totalT));
                cpuAM.add(0, restrictPercentage(workAMT*100/(float)totalT));
            }

            totalBefore = total;
            workBefore = work;
            workAMBefore = workAM;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getIntervalWidth() {
        return intervalWidth;
    }

    public int getIntervalRead() {
        return intervalRead;
    }

    class ServiceReaderDataBinder extends Binder {
        ServiceReader getService() { return ServiceReader.this; }
    }

    private float restrictPercentage(float percentage) {
        if (percentage > 100) {
            return 100;
        } else if (percentage < 0) {
            return 0;
        }
        return percentage;
    }

    public List<Float> getCPUTotalP() {
        return cpuTotal;
    }

    public List<Float> getCPUAMP() {
        return cpuAM;
    }
}
