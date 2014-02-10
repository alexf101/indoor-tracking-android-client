package collector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.Serializable;

public class UnbrokenSensorEvent implements Serializable {
    public long timestamp;
    public float[] values;
    public Sensor sensor;

    private static final long serialVersionUID = 1;

    public UnbrokenSensorEvent(SensorEvent event){
        this.timestamp = event.timestamp;
        this.values = event.values;
        this.sensor = event.sensor;
    }
}
