package collector;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.*;
import android.net.wifi.WifiManager;
import android.util.Log;
import datatypes.FingerprintScan;
import datatypes.Fingerprint;
import util.Dbg;

import java.util.*;

public class FingerprintCollector {

    public int MAX_TIME_PER_SAMPLE_MILLIS = 3000;
    Context context;

    WifiManager wifiManager;
    public ArrayList<FingerprintScan[]> wifiResults = new ArrayList<FingerprintScan[]>();

    SensorManager sensorManager;
    Sensor gravity_accel;
    public volatile ArrayList<UnbrokenSensorEvent> gravity_accelResults = new ArrayList<UnbrokenSensorEvent>();
    Sensor magnetometer;
    public volatile ArrayList<UnbrokenSensorEvent> magnetometerResults = new ArrayList<UnbrokenSensorEvent>();
    private ArrayList<Fingerprint> fingerprintsSoFar = new ArrayList<Fingerprint>();

    private List<Sensor> sensorsUsed;
    private GravAndMagHandler gravAndMagHandler;
    private FingerprintCallback callBack;
    private ContinuousScanReceiver broadcastReceiver;
    private volatile boolean collecting;
    private int numberOfSamples;
    private Timer timeoutTimer;
    private Timer collectRepeatedlyTimer;

    public boolean isCollecting(){
        return collecting;
    }

    public void setCallBack(FingerprintCallback callBack) {
        this.callBack = callBack;
    }

    public FingerprintCollector(Context context, FingerprintCallback callBack){
        this.context = context;
        setCallBack(callBack);
        setUpSensors();
        wifiOn();
    }

    /**
     * Some of these sensors are hardware, some are software.
     *
     * See http://developer.android.com/guide/topics/sensors/sensors_motion.html
     */
    private void setUpSensors() {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // magnetic field strength in x, y, z relative to phone coordinate system, microTesla.
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravity_accel = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorsUsed = Arrays.asList(magnetometer, gravity_accel);
        this.gravAndMagHandler = new GravAndMagHandler(this);
        //this.broadcastReceiver = new ContinuousScanReceiver(wifiResults, wifiManager, newWifiScan){
        this.broadcastReceiver = new ContinuousScanReceiver(wifiResults, wifiManager){

            @Override
            protected boolean callBack(FingerprintScan[] scans) {
                // make sure we have at least one measurement.
                if (gravity_accelResults.isEmpty() || magnetometerResults.isEmpty()) {
                    Dbg.logw(this.getClass().getName(), "Scan complete, but discarding result as there are no mag or grav results...");
                    return true;
                }
                ArrayList<UnbrokenSensorEvent> magCopy = (ArrayList<UnbrokenSensorEvent>) magnetometerResults.clone();
                magnetometerResults.clear();
                ArrayList<UnbrokenSensorEvent> gravCopy = (ArrayList<UnbrokenSensorEvent>) gravity_accelResults.clone();
                gravity_accelResults.clear();
                Fingerprint newFingerprint = gatherResults(scans, magCopy, gravCopy);
                callBack.onSampleCollected(newFingerprint);
                if (fingerprintsSoFar.size() >= numberOfSamples) {
                    finishCollecting();
                    return false;
                } else {
                    return true;
                }
            }
        };
    }

    private Fingerprint gatherResults(FingerprintScan[] scans, ArrayList<UnbrokenSensorEvent> magnetometerResults, ArrayList<UnbrokenSensorEvent> gravity_accelResults) {
        Dbg.logd(this.getClass().getName(), "Collecting non-mag results: no. of mag measuremens: " + magnetometerResults.size() + ", no. of grav measurements: " + gravity_accelResults.size());
        Fingerprint fingerprint = new Fingerprint(Arrays.asList(scans));
        fillNonScanParameters(fingerprint, magnetometerResults, gravity_accelResults);
        fingerprintsSoFar.add(fingerprint);
        return fingerprint;
    }

    /**
     * Collects samples, calling onSamplesCollected when done.
     * @param numberOfSamples
     */
    public void collectSamples(int numberOfSamples) {
        if (collecting) return;
        Log.i(this.getClass().getName(), "Collecting "+numberOfSamples+" samples") ;
        this.numberOfSamples = numberOfSamples;
        beginCollecting();
        (timeoutTimer = new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                exitEarlyIfTimeout();
            }
        }, MAX_TIME_PER_SAMPLE_MILLIS * numberOfSamples);
    }

    public void collectSamples(int numberOfSamples, int interval) {
        if (collecting) return;
        this.numberOfSamples = numberOfSamples;
        Log.i(this.getClass().getName(), "Collecting " + numberOfSamples + " samples every " + interval + " seconds");
        (collectRepeatedlyTimer = new Timer()).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                beginCollecting();
            }
        }, 0, interval * 1000);
    }


    private void wifiOn() {
        if (wifiManager.getWifiState() != 3) { // 3 == on
            Log.w("Sensor", "Enabling WiFi - previous status was " + wifiManager.getWifiState());
            if (!wifiManager.setWifiEnabled(true)) {
                Dbg.loge(this.getClass().getName(), "Could not enable WiFi");
            }
        }
    }

    private void exitEarlyIfTimeout() {
        long start = System.currentTimeMillis();
        while (collecting && (System.currentTimeMillis() - start) < (MAX_TIME_PER_SAMPLE_MILLIS * numberOfSamples)){
            try {
                Thread.sleep(MAX_TIME_PER_SAMPLE_MILLIS);
            } catch (InterruptedException e) {
                // usually indicates that collection has finished, but not necessarily, hence while loop
            }
        }
        if (collecting) {
            Dbg.logd(this.getClass().getName(), "Ending early due to timeout. Samples collected so far: "+fingerprintsSoFar.size());
            finishCollecting();
        }
    }

    private synchronized void beginCollecting() {
        if (collecting) return;
        context.registerReceiver(broadcastReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        for (Sensor sensor : sensorsUsed) {
            sensorManager.registerListener(gravAndMagHandler, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        collecting = true;
        if (!wifiManager.startScan()){
            Dbg.logw(this.getClass().getName(), "Could not begin a scan... probably nothing to worry about");
        }
    }

    private synchronized void finishCollecting() {
        if (!collecting){
            Log.w(this.getClass().getName(), "Already stopped collecting!");
        } else {
            collecting = false;
            if (timeoutTimer != null) timeoutTimer.cancel();
            Log.d(this.getClass().getName(), "Unregistering receivers...");
            Dbg.logd(this.getClass().getName(), "Number of fingerprints collected: " + fingerprintsSoFar.size());
            try {
                sensorManager.unregisterListener(gravAndMagHandler);
            } catch (IllegalArgumentException e) {
                Dbg.loge(this.getClass().getName(), "Failed to unregister gravAndMagHandler", e);
            }
            try {
                context.unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException e) {
                Dbg.loge(this.getClass().getName(), "Failed to unregister broadcastReceiver...", e);
            }
            callBack.onSamplesCollected(fingerprintsSoFar);
            wifiResults.clear();
            magnetometerResults.clear();
            gravity_accelResults.clear();
            fingerprintsSoFar.clear();
        }
    }

    /**
     * Fills the parameters of a fingerprint other than WiFi scan
     * with average values.
     * @param fingerprint
     * @param magResults
     * @param gravResults
     *
     */
    private void fillNonScanParameters(Fingerprint fingerprint,
                                       ArrayList<UnbrokenSensorEvent> magResults,
                                       ArrayList<UnbrokenSensorEvent> gravResults) {
        float sumOfZMag = 0;
        float sumOfTotalMag = 0;
        float sumOfDirection = 0;
        int numberOfMeasurements = 0;
        UnbrokenSensorEvent mag = null;
        UnbrokenSensorEvent grav = null;
        if (magResults.isEmpty() || gravResults.isEmpty()) {
            Dbg.logw(this.getClass().getName(), "No mag or gravity measurements were taken");
            return;
        }
        int magIndex = 0;
        int gravIndex = 0;
        while (true) {
            boolean changed = false;
            if (magIndex < magResults.size()) {
                mag = magResults.get(magIndex);
                changed = true;
            }
            if (gravIndex < gravResults.size()) {
                grav = gravResults.get(gravIndex);
                changed = true;
            }
            if (!changed) {
                break;
            }
            Dbg.logv(this.getClass().getName(), "MAGt: " + mag.timestamp + " - GRAVt: " + grav.timestamp);
            if (Math.abs(mag.timestamp - grav.timestamp) > 200000000) { // one fifth of a second in nanoseconds
                Dbg.logd(this.getClass().getName(), "Grav and Mag results are out of sync");
                Dbg.logd(this.getClass().getName(), "Difference (seconds): " + ((float) Math.abs(mag.timestamp - grav.timestamp) / 1000000000));
                if (mag.timestamp < grav.timestamp) {
                    if (magIndex < magResults.size()) {
                        mag = magResults.get(++magIndex);
                    }
                } else {
                    if (magIndex < gravResults.size()) {
                        grav = gravResults.get(++gravIndex);
                    }
                }
                Dbg.logd(this.getClass().getName(), "After correction: ");
                Dbg.logd(this.getClass().getName(), "Difference (seconds): " + ((float) Math.abs(mag.timestamp - grav.timestamp) / 1000000000));
            }
            numberOfMeasurements++;
            magIndex++;
            gravIndex++;
            extractToFingerprint(fingerprint, grav.values, mag.values);
            sumOfZMag += fingerprint.zaxis;
            sumOfTotalMag += fingerprint.magnitude;
            sumOfDirection += fingerprint.direction;
        }
        if (numberOfMeasurements >= 1){
            Dbg.logv(this.getClass().getName(), "Number of measurements: " + numberOfMeasurements);
            fingerprint.magnitude = sumOfTotalMag / numberOfMeasurements;
            fingerprint.zaxis = sumOfZMag / numberOfMeasurements;
            fingerprint.direction = sumOfDirection / numberOfMeasurements;
            Dbg.logv(this.getClass().getName(), "Total mag: " + fingerprint.magnitude);
            Dbg.logv(this.getClass().getName(), "Total z: " + fingerprint.zaxis);
            Dbg.logv(this.getClass().getName(), "Direction: " +fingerprint.direction);
        } else {
            Dbg.logw(this.getClass().getName(), "No mag or gravity measurements between last scan and this one");
        }
    }

    // No need to reallocate these two matrices every time we call extractToFingerprint
    private float[] rotationMatrix = new float[9]; // output argument
    private float[] inclinationMatrix = new float[9]; // output argument

    /**
     * We aim to detect a local fluctuation in magnetic field strength.
     *
     * There are two components of magnetic field that we are able to measure without an independent knowledge of phone orientation..
     *
     * One is the scalar magnitude, the other is the z-component relative to Earth.
     *
     * The X and Y component aren't measurable, since X = Y cross Z and Y is determined from the compass.
     *
     * Also see http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix%28float[],%20float[],%20float[],%20float[]%29
     *
     * @param gravity
     * acceleration [x, y, z] relative to phone - output from accelerometer (Sensor.TYPE_ACCELEROMETER) or gravity (Sensor.TYPE_GRAVITY)
     * @param geoMagnetic
     * magnetic field [x, y, z] relative to phone - output from magnetic sensor (Sensor.TYPE_ACCELEROMETER)
     * @return
     * float[2] - first element is magnetic field strength in Z direction, second component is sum of magnetic field components
     */
    private void extractToFingerprint(Fingerprint fingerprint, float[] gravity, float[] geoMagnetic){
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geoMagnetic) ;
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        fingerprint.direction = orientation[0];
        // The rotation matrix will convert a vector from device coordinate system to world coordinate system as R * v(device) = v(world)
        fingerprint.magnitude = (float) Math.sqrt(geoMagnetic[0]*geoMagnetic[0]+geoMagnetic[1]*geoMagnetic[1]+geoMagnetic[2]*geoMagnetic[2]);
        fingerprint.zaxis = rotationMatrix[6] * geoMagnetic[0] + rotationMatrix[7] * geoMagnetic[1] + rotationMatrix[8] * geoMagnetic[2]; // z-component of magnetism in Earth frame
    }

    /**
     * Cancels current and scheduled data collections.
     */
    public void cancel() {
        if (collecting){
            finishCollecting();
        }
        if (collectRepeatedlyTimer != null) collectRepeatedlyTimer.cancel();
    }

}