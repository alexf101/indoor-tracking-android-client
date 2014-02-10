package collector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import util.Dbg;

public class GravAndMagHandler implements SensorEventListener {

    FingerprintCollector fingerprintCollector;

    public GravAndMagHandler(FingerprintCollector f){
        this.fingerprintCollector = f;
    }

    public void onSensorChanged(SensorEvent event1) {
        UnbrokenSensorEvent unbrokenSensorEvent = new UnbrokenSensorEvent(event1);
        Dbg.logd(this.getClass().getName(), unbrokenSensorEvent.sensor.getName() + " at " + Long.valueOf(unbrokenSensorEvent.timestamp));
        switch (unbrokenSensorEvent.sensor.getType()) {
            case (Sensor.TYPE_GRAVITY):
                fingerprintCollector.gravity_accelResults.add(unbrokenSensorEvent);
                Dbg.logv(this.getClass().getName(), "gravity results size: " + fingerprintCollector.gravity_accelResults.size());
                break;
            case (Sensor.TYPE_MAGNETIC_FIELD):
                fingerprintCollector.magnetometerResults.add(unbrokenSensorEvent);
                Dbg.logv(this.getClass().getName(), "magneto results size: " + fingerprintCollector.magnetometerResults.size());
                break;
            default:
                throw new Error("NO HANDLING FOR SENSOR: " + unbrokenSensorEvent.sensor.getName() + ", type " + unbrokenSensorEvent.sensor.getType());
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Dbg.logd(this.getClass().getName(), sensor.getType() + ": " + accuracy);
    }

}
