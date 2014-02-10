package collector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import datatypes.FingerprintScan;
import util.Dbg;

import java.util.ArrayList;
import java.util.List;

/**
 * Continuously scans and receives WiFi, parses results into Scan objects.
 *
 * Executes the callBack each scan.
 */
public abstract class ContinuousScanReceiver extends BroadcastReceiver {

    private final ArrayList<FingerprintScan[]> wifiResults;
    private final WifiManager wifiManager;

    public ContinuousScanReceiver(ArrayList<FingerprintScan[]> wifiResults, WifiManager wifiManager) {
        this.wifiResults = wifiResults;
        this.wifiManager = wifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long scanCompletedAt = System.nanoTime();
        Dbg.logd(this.getClass().getName(), "Completed scan at "+scanCompletedAt);
        FingerprintScan[] parsedResult = wifiParse(wifiManager.getScanResults());
        wifiResults.add(parsedResult);
        if (callBack(parsedResult)){
            wifiManager.startScan(); // scan continuously unless callBack returns false
        }
    }

    /**
     * Override this method to obtain a callback after each scan result has been parsed
     * @param parsedResult
     * @return True if the receiver should keep starting new scans
     */
    protected abstract boolean callBack(FingerprintScan[] parsedResult);

    private FingerprintScan[] wifiParse(List<ScanResult> scanResults) {
        ArrayList<FingerprintScan> parsedResult = new ArrayList<FingerprintScan>();
        for (ScanResult scanResult : scanResults){
            parsedResult.add(baseStationFromScanResult(scanResult));
        }
        return parsedResult.toArray(new FingerprintScan[parsedResult.size()]);
    }

    private FingerprintScan baseStationFromScanResult(ScanResult scanResult) {
        return new FingerprintScan(scanResult.BSSID, scanResult.SSID, scanResult.frequency, scanResult.level);
    }

}
