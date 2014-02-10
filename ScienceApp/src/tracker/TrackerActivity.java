package tracker;

import Fingerprint.Logger.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import collector.FingerprintCallback;
import collector.FingerprintCollector;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import communication.FingerprintServerClient;
import communication.ResponseHandler;
import datatypes.Building;
import datatypes.Location;
import datatypes.Fingerprint;
import messages.ConfirmMsg;
import messages.FailureMsg;
import messages.LocationsFoundMsg;
import recorder.RecorderActivity;
import util.Dbg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class TrackerActivity extends SherlockActivity {

    private TextView scanEvery;
    private TextView locationBox;
    public FingerprintServerClient fingerprintServerClient;
    private int scanInterval;
    private FingerprintCollector fingerprintCollector;
    private AutoCompleteTextView buildingTextView;
    private ArrayList<String> buildings;
    public Building[] buildingObjects;
    private ArrayAdapter<String> buildingsAdapter;
    private ArrayAdapter<String> locationsAdapter;
    private ActionBar actionBar;
    private final Context context = this;
    /**
     * This queue always contains one element - the most up-to-date version available right now of buildings.
     * If the queue is empty, there is no version available right now - you will have to wait.
     */
    private BlockingQueue<Building> blockingQueue = new LinkedBlockingQueue<Building>();
    private Location[] locations; // current locations guesses
    private Long lastFingerprintID;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track);
        actionBar = fillActionBar();
        scanEvery = (TextView) findViewById(R.id.scanEvery);
        locationBox = (TextView) findViewById(R.id.locationBox);
        buildingTextView = (AutoCompleteTextView) findViewById(R.id.building);
        try {
            fingerprintServerClient = new FingerprintServerClient(
                    "science_app_user",
                    "fingerprints_101");
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
        }
        addAutocompleteListener(buildingTextView);
        setupFingerprintCollector();
        setupLocationsSpinner();
        setScanInterval(Integer.parseInt(scanEvery.getText().toString())); // will begin scan
        setupScanEveryListener();
        getBuildings();
    }

    private void setupLocationsSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.locations_spinner);
        locationsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
        locationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationsAdapter.addAll("Not Found, Please Wait");
        spinner.setAdapter(locationsAdapter);
    }

    public void sendConfirmMsg(View v){
        Spinner spinner = (Spinner) findViewById(R.id.locations_spinner);
        String locationSelected = (String) spinner.getSelectedItem();
        if (locationSelected.equals("Not Found, Please Wait")) {
            Toast.makeText(context, "No location to confirm", Toast.LENGTH_SHORT);
        } else {
            for (final Location l : locations) {
                if (l.getName().equals(locationSelected)){
                    try {
                        fingerprintServerClient.sendConfirm(l, lastFingerprintID, new ResponseHandler<ConfirmMsg>() {
                            @Override
                            public void onServerResponse(ConfirmMsg msg) {
                                notifyUser("Confirmed: location " + l.getName() + " is represented by fingerprint " + lastFingerprintID);
                                if (!msg.changed){
                                    notifyUser("However, the server already knew that!");
                                }
                            }

                            @Override
                            public void onServerDeniedRequest(FailureMsg failureMsg) {
                                notifyUser("Could not send confirm msg, "+failureMsg.detail);
                            }
                        });
                    } catch (InterruptedException e) {
                        Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        notifyUser("Could not send confirm msg, unknown cause");
                    } catch (ExecutionException e) {
                        Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        notifyUser("Could not send confirm msg, unknown cause");
                    } catch (TimeoutException e) {
                        Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        notifyUser("Could not send confirm msg, unknown cause");
                    }
                }
            }
        }
    }

    private ActionBar fillActionBar() {
        ActionBar bar = getSupportActionBar();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                R.layout.sherlock_spinner_item,
                new String[]{
                        "Track your location",
                        "Learn fingerprints",
                }
        );
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ActionBar.OnNavigationListener navListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                switch(itemPosition) {
                    case 0:
                        break;
                    case 1:
                        Intent i = new Intent(context, RecorderActivity.class);
                        startActivity(i);
                        break;
                }
                return true;
            }
        };
        bar.setListNavigationCallbacks(spinnerAdapter, navListener);
        return bar;
    }

    private void getBuildings() {
        try {
            notifyUser("Getting building list from server");
            fingerprintServerClient.sendWhatBuildings(new ResponseHandler<Building[]>() {

                @Override
                public void onServerResponse(Building[] msg) {
                    Dbg.logd(this.getClass().getName(), "Server responded to buildings request: " + msg);
                    notifyUser("Server sent list of buildings");
                    fillBuildingAdapter(msg);
                }

                @Override
                public void onServerDeniedRequest(FailureMsg failureMsg) {
                    Dbg.loge(this.getClass().getName(), failureMsg.detail);
                    notifyUser("Server was unable to send buildings: "+failureMsg.detail);
                }
            });
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private void setupFingerprintCollector() {

        fingerprintCollector = new FingerprintCollector(getApplicationContext(),
                new FingerprintCallback() {
                    @Override
                    public void onSamplesCollected(List<Fingerprint> fingerprint) {
                        Dbg.logi(this.getClass().getName(), "Stopped collecting");
                    }

                    @Override
                    public void onSampleCollected(Fingerprint newFingerprint) {
                        Dbg.logd(this.getClass().getName(), "Collected a fingerprint: id = "+newFingerprint.id());
                        notifyUser("Collected a fingerprint - "+newFingerprint.getScans().size()+" channels from "+newFingerprint.uniqueBSSIDs().size()+" access points");
                        try {
                            fingerprintServerClient.sendLocateMe(newFingerprint, getBuilding(), 5, new ResponseHandler<LocationsFoundMsg>() {

                                @Override
                                public void onServerResponse(final LocationsFoundMsg msg) {
                                    lastFingerprintID = msg.fingerprint_id;
                                    final Location[] locations = msg.locations;
                                    fillLocations(msg.locations);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyUser("Server response: location is " + locations[0].getName()+", fingerprint saved with id "+msg.fingerprint_id);
                                        }
                                    });
                                }

                                @Override
                                public void onServerDeniedRequest(FailureMsg failureMsg) {
                                    notifyUser("SERVER ERROR: "+failureMsg.detail);
                                }

                            });
                        } catch (InterruptedException e) {
                            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        } catch (ExecutionException e) {
                            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        } catch (TimeoutException e) {
                            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
                        }
                    }
                });

    }

    private void fillLocations(Location[] locations) {
        this.locations = locations;
        String[] locationNames = new String[locations.length];
        for (int i=0; i< locations.length; i++) {
            locationNames[i] = locations[i].getName();
        }
        this.locationsAdapter.clear();
        this.locationsAdapter.addAll(locationNames);
        this.locationsAdapter.notifyDataSetChanged();
    }

    private void fillBuildingAdapter(Building[] msg) {
        buildingObjects = msg;
        ArrayList<String> building_names = new ArrayList<String>();
        for (Building building : msg) {
            building_names.add(building.getName());
        }
        setBuildings(building_names);
    }

    private void setBuildings(ArrayList<String> buildings){
        this.buildings = buildings;
        this.buildingsAdapter.clear();
        this.buildingsAdapter.addAll(buildings);
        this.buildingsAdapter.notifyDataSetChanged();
    }

    private void addAutocompleteListener(final AutoCompleteTextView buildingView) {
        buildings = new ArrayList<String>();
        buildingsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, buildings);
        buildingView.setThreshold(1);
        buildingView.setAdapter(buildingsAdapter);
        buildingView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (!hasFocus) {
                    AutoCompleteTextView buildingViewInternal = (AutoCompleteTextView) v;
                    final String buildingChosen = buildingViewInternal.getText().toString();
                    if (buildingObjects != null){
                        for (Building buildingObj : buildingObjects) {
                            if (buildingChosen.equals(buildingObj.getName())) {
                                Dbg.logd(this.getClass().getName(), "building chosen: " + buildingObj);
                                setBuilding(buildingObj);
                                startScanning();
                                break;
                            }
                        }
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TrackerActivity.this);
                        alertDialog.setTitle("No Building's Retrieved From Server");
                        alertDialog.setMessage("Please try again in a few seconds.");
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                v.requestFocus();
                            }
                        });
                        //alertDialog.setIcon(R.drawable.icon);
                        alertDialog.show();
                    }
                    if (getBuilding() == null) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TrackerActivity.this);
                        alertDialog.setTitle("Unknown Building Selected");
                        alertDialog.setMessage("For tracking, please select a building from the drop-down menu");
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                v.requestFocus();
                            }
                        });
                        //alertDialog.setIcon(R.drawable.icon);
                        alertDialog.show();
                    }
                }
            }
        });
        buildingView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clearFocus();
            }
        });

    }

    private void clearFocus() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        buildingTextView.clearFocus();
    }

    private void setupScanEveryListener() {
        scanEvery.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            public void onFocusChange(View s, boolean hasFocus) {
                if (!hasFocus) {
                    int scanInterval;
                    String text = ((TextView) s).getText().toString();
                    Dbg.logd(this.getClass().getName(), "Scan Every text: " + text);
                    try {
                        scanInterval = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        Dbg.logw(this.getClass().getName(), "Could not parse integer from " + text);
                        scanInterval = 5;
                    }
                    if (scanInterval > 0) {
                        setScanInterval(scanInterval);
                    } else {
                        setScanInterval(1);
                    }
                }
            }
        });
    }

    private void setScanInterval(int scanEvery) {
        Dbg.logd(this.getClass().getName(), "Setting scan interval: " + scanEvery);
        this.scanInterval = scanEvery;
        stopScanning();
        startScanning();
    }

    private volatile boolean scanStarted= false;
    /**
     * Call this method to enable Fingerprint collection.
     *
     * Runs the scanner on a background thread, as it will wait to start scanning until a building is set
     */
    private void startScanning() {
        if (scanStarted == false){
            scanStarted = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getBuilding();
                    fingerprintCollector.collectSamples(1, scanInterval);
                    notifyUser("Commencing scanning every "+scanInterval+" seconds");
                }
            }).start();
        }
    }

    /**
     * Call this method to stop Fingerprint collection
     */
    private void stopScanning(){
        Dbg.logd(this.getClass().getName(), "Pausing timer");
        fingerprintCollector.cancel();
        scanStarted = false;
        notifyUser("Stopped scanning");
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    public void onResume() {
        super.onResume();
        startScanning();
        Dbg.logd(this.getClass().getName(), "Started Tracker from onResume");
    }

    private Building getBuilding() {
        if (blockingQueue.peek() != null) {
            return blockingQueue.peek();
        } else {
            notifyUser("Waiting for building...");
            Building building = null;
            while (building == null){
                try {
                    building = blockingQueue.take();
                } catch (InterruptedException e) {
                    Dbg.loge(this.getClass().getName(), "Interrupted, retrying...", e);
                }
            }
            notifyUser("Building set.");
            blockingQueue.add(building);
            return building;
        }
    }

    private void notifyUser(final String msgForUser) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                locationBox.append("\n"+msgForUser);

            }
        });
    }

    private void setBuilding(Building building) {
        blockingQueue.poll(); // removes the element if it is present, otherwise no-op returning null
        blockingQueue.add(building);
    }
}