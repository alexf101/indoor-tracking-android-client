package recorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import collector.FingerprintCallback;
import collector.FingerprintCollector;
import Fingerprint.Logger.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import communication.AcknowledgedTask;
import communication.ResponseHandler;
import communication.FingerprintServerClient;
import datatypes.Building;
import datatypes.Fingerprint;
import datatypes.Location;
import datatypes.User;
import messages.FailureMsg;
import tracker.TrackerActivity;
import util.Dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RecorderActivity extends SherlockActivity {

    ExecutorService backgroundTaskQueue = Executors.newSingleThreadExecutor();

    private TextView resultBox;
    public FingerprintServerClient fingerprintServerClient;
    private ArrayAdapter<String> buildingsAdapter;
    private ArrayAdapter<String> locationsAdapter;
    private FingerprintCollector fingerprintCollector;
    List<Fingerprint> fingerprintsJustCollected;
    private ActionBar actionBar;
    private Context context = this;

    // defaults
    int numberOfSamples = 5; // TODO re-implement with preferences
    private Location[] locations;
    private Building[] buildings;
    private volatile Location location;
    private volatile Building building;
    private AutoCompleteTextView locationView;
    private AutoCompleteTextView buildingView;

    /**
     * On system start, we need to:
     *
     *  * set the layout
     *  * other UI elements (listeners, ActionBar)
     *  * find out what the buildings are for AutoComplete
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resultBox = (TextView) findViewById(R.id.readout_values);
        addAutoCompleteAdaptersForBuildingAndLocations();
        this.actionBar = fillActionBar();
        try {
            fingerprintServerClient = new FingerprintServerClient(
                    "science_app_user",
                    "fingerprints_101");
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "Could not instantiate client", e);
        }
        try {
            Dbg.logd(this.getClass().getName(), "buildings are: " + Arrays.toString(buildings));
            getBuildings();
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
            notifyUser("Could not get buildings: " + e.getMessage());
        }
    }

    private ActionBar fillActionBar() {
        ActionBar bar = getSupportActionBar();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                //android.R.layout.simple_spinner_dropdown_item, ALTERNATIVE FOR IF WE REMOVE ActionBarSherlock
                R.layout.sherlock_spinner_item,
                new String[]{
                        "Learn fingerprints",
                        "Track your location",
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
                        Intent i = new Intent(context, TrackerActivity.class);
                        startActivity(i);
                        break;
                }
                return true;
            }
        };
        bar.setListNavigationCallbacks(spinnerAdapter, navListener);
        return bar;
    }

    private void notifyUser(final String s) {
        Dbg.logd(this.getClass().getName(), s);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultBox.append(s+"\n");
            }
        });
    }

    /**
     * Must be run on UI thread
     */
    private void addAutoCompleteAdaptersForBuildingAndLocations() {
        buildingView = (AutoCompleteTextView) findViewById(R.id.building);
        locationView = (AutoCompleteTextView) findViewById(R.id.location);

        buildingsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        locationsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());

        buildingView.setThreshold(1);
        locationView.setThreshold(1);

        buildingView.setAdapter(buildingsAdapter);
        locationView.setAdapter(locationsAdapter);

        // clear the `building' variable whenever the text changes
        buildingView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                building = null;
            }
        });

        // set the `building' variable to the user's selection when they click a drop-down item
        buildingView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Dbg.logv(this.getClass().getName(), "Building view item click");
                building = null;
                String building_name = (String) parent.getItemAtPosition(position);
                building = findBuildingForName(building_name);
            }
        });

        /*
        set or create the `building' variable when the user types out a building name.
        Three alternatives:
          1 The user picked a known building from the drop-down
          2 The user typed out a known building
          3 The user typed out a new building
        Any way, at the end of the day we need to set building and start getLocations().
        */
        buildingView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (!hasFocus) {
                    Dbg.logv(this.getClass().getName(), "Building view lost focus");
                    if (building != null) {
                        // option 1 - user picked a known building
                        getLocations(((AutoCompleteTextView) v).getText().toString());
                    } else {
                        String building_name = ((AutoCompleteTextView) v).getText().toString();
                        Building matchingBuilding = findBuildingForName(building_name);
                        if (matchingBuilding != null) {
                            // option 2 - user typed a known building
                            getLocations(((AutoCompleteTextView) v).getText().toString());
                        } else {
                            newBuilding(building_name);
                        }
                    }
                }
            }
        });

        // clear the `location' variable whenever the text changes
        locationView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                location = null;
                Dbg.logd(this.getClass().getName(), "Setting location to null: text changed");
            }
        });

        // set the `location' variable to the user's selection when they click a drop-down item
        locationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView parent, View view, int position, long rowId) {
                location = null;
                Dbg.logd(this.getClass().getName(), "Setting location to null: item clicked");
                String location_name = (String) parent.getItemAtPosition(position);
                location = findLocationForName(location_name);
                Dbg.logd(this.getClass().getName(), "Location found as " + location);
            }
        });

        /*
        set or create the `location' variable when the user types out a building name.
        Three alternatives:
          1 The user picked a known location from the drop-down
          2 The user typed out a known location
          3 The user typed out a new location
        Any way, at the end of the day we need to set the location (that's all).
        */
        locationView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Dbg.logv(this.getClass().getName(), "Location view lost focus");
                    if (location != null) { // option 1 - location already chosen
                        Dbg.logd(this.getClass().getName(), "Location chosen: " + location);
                    } else {
                        String location_name = ((AutoCompleteTextView) v).getText().toString();
                        Location matchingLocation = findLocationForName(location_name);
                        if (matchingLocation != null) {
                            location = matchingLocation;
                            Dbg.logd(this.getClass().getName(), "Location chosen: " + location);
                        } else {
                            if (building != null) {
                                newLocation(location_name, building);
                            } else {
                                Dbg.logd(this.getClass().getName(), "Couldn't set new location because building was not yet set.");
                            }
                        }
                    }
                }
            }
        });
    }

    private void newBuilding(final String building_name) {
        // option 3 - user typed an unknown building
        try {
            fingerprintServerClient.sendNewBuilding(new Building(building_name), new ResponseHandler<Building>() {
                @Override
                public void onServerResponse(Building msg) {
                    building = msg;
                    getLocations(building_name);
                }

                @Override
                public void onServerDeniedRequest(FailureMsg failureMsg) {
                    notifyUser("Couldn't create building " + building_name+": "+failureMsg.detail);
                }
            });
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "A new building could not be created", e);
            Toast.makeText(context, "Could not create building, error was: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private void newLocation(String location_name, Building building) {
        try {
            Dbg.logd(this.getClass().getName(), "Creating a new location in "+building);
            Location l = new Location(location_name, building);
            fingerprintServerClient.sendNewLocation(l, new ResponseHandler<Location>() {
                @Override
                public void onServerResponse(Location msg) {
                    Dbg.logd(this.getClass().getName(), "Location sent from server: " + location);
                    location = msg;
                }

                @Override
                public void onServerDeniedRequest(FailureMsg failureMsg) {
                    notifyUser(failureMsg.detail);
                }
            });
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), "A new location could not be created", e);
            Toast.makeText(context, "Could not create location, error was: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private Building findBuildingForName(String building_name) {
        if (buildings == null) {
            Toast.makeText(context, "Waiting for building names from server", Toast.LENGTH_SHORT);
        } else {
            for (int i = 0; i < buildings.length; i++) {
                if (buildings[i].getName().equals(building_name)) {
                    return buildings[i];
                }
            }
        }
        return null;
    }

    private Location findLocationForName(String location_name) {
        if (locations == null) {
            Toast.makeText(context, "Waiting for location names from server", Toast.LENGTH_SHORT);
        } else {
            for (int i = 0; i < locations.length; i++) {
                if (locations[i].getName().equals(location_name)) {
                    Dbg.logd(this.getClass().getName(), "Location found: "+locations[i]);
                    return locations[i];
                }
            }
        }
        Dbg.loge(this.getClass().getName(), "No location was found for " + location_name);
        return null;
    }

    private void getBuildings() {
        Dbg.logd(this.getClass().getName(), "Getting buildings");
        try {
            fingerprintServerClient.sendWhatBuildings(new ResponseHandler<Building[]>() {
                @Override
                public void onServerResponse(Building[] msg) {
                    Dbg.logd(this.getClass().getName(), "Found buildings" + Arrays.toString(msg));
                    fillBuildings(msg);
                }

                @Override
                public void onServerDeniedRequest(FailureMsg failureMsg) {
                    notifyUser(failureMsg.detail);
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

    private void getLocations(String building) {
        Dbg.logd(this.getClass().getName(), "Sending what locations message");
        try {
            fingerprintServerClient.sendWhatLocations(building, new ResponseHandler<Location[]>() {
                @Override
                public void onServerResponse(Location[] msg) {
                    locations = msg;
                    fillLocations(msg);
                }

                @Override
                public void onServerDeniedRequest(FailureMsg failureMsg) {
                    notifyUser(failureMsg.detail);
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

    private void fillLocations(Location[] locations) {
        Dbg.logd(this.getClass().getName(), "Adding new locations...");
        String[] location_names = new String[locations.length];
        for (int i=0; i<locations.length; i++) {
            location_names[i] = locations[i].getName();
        }
        this.locationsAdapter.clear();
        this.locationsAdapter.addAll(location_names);
        this.locationsAdapter.notifyDataSetChanged();
    }

    private void fillBuildings(Building[] buildings) {
        Dbg.logd(this.getClass().getName(), "Adding buildings...");
        this.buildings = buildings;
        String[] buildingNames = new String[buildings.length];
        for (int i=0; i< buildings.length; i++) {
            buildingNames[i] = buildings[i].getName();
        }
        this.buildingsAdapter.clear();
        this.buildingsAdapter.addAll(buildingNames);
        this.buildingsAdapter.notifyDataSetChanged();
    }

    public void logXsecs(View logNowButton) {

        clearResults();
        fingerprintsJustCollected = new ArrayList<Fingerprint>(numberOfSamples);

        if (building == null) {
            Dbg.logd(this.getClass().getName(), "Building is null on record");
            final String buildingText = buildingView.getText().toString();
            if (buildingText.isEmpty()) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(RecorderActivity.this);
                alertDialog.setTitle("No Building Named");
                alertDialog.setMessage("Please fill out a Building name");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
                return;
            } else {
                Dbg.logd(this.getClass().getName(), "submitting building request");
                backgroundTaskQueue.submit(new Runnable() {
                    @Override
                    public void run() {
                        building = fingerprintServerClient.sendNewBuilding(building);
                    }
                });
                newBuilding(buildingText);
            }
        }

        if (location == null) {
            Dbg.logd(this.getClass().getName(), "Location is null on record");
            final String locationText = locationView.getText().toString();
            if (locationText.isEmpty()){
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(RecorderActivity.this);
                alertDialog.setTitle("No Location Named");
                alertDialog.setMessage("Please fill out a location name");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
                return;
            } else {
                Dbg.logd(this.getClass().getName(), "submitting location request");
                backgroundTaskQueue.submit(new Runnable() {
                    @Override
                    public void run() {
                        location = fingerprintServerClient.sendNewLocation(new Location(locationText, building));
                        Dbg.logd(this.getClass().getName(), "Location set to " + location);
                    }
                });
            }
        }

        backgroundTaskQueue.submit(new Runnable() {
            @Override
            public void run() {

                fingerprintCollector = new FingerprintCollector(
                        context,
                        new FingerprintCallback() {
                            @Override
                            public void onSamplesCollected(final List<Fingerprint> fingerprints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        beep();
                                        clearResults();
                                        displaySuccessMessage();
                                        displayTotalMeasurements(fingerprints);
                                    }
                                });
                            }

                            @Override
                            public void onSampleCollected(final Fingerprint fingerprint) {
                                Dbg.logd(this.getClass().getName(), "Sending Fingerprint Message: " + fingerprint);
                                notifyUser("Collected a fingerprint");
                                // TODO location = null?
                                if (location == null) {
                                    Dbg.loge(this.getClass().getName(), "No location was found");
                                    notifyUser("Could not record fingerprint");
                                    return;
                                }
                                fingerprint.location = location.getUrl();
                                try {
                                    fingerprintServerClient.sendFingerprint(fingerprint, new ResponseHandler<Fingerprint>() {
                                        @Override
                                        public void onServerResponse(Fingerprint msg) {
                                            fingerprintsJustCollected.add(msg);
                                            notifyUser("Fingerprint saved: " + msg.id());
                                        }

                                        @Override
                                        public void onServerDeniedRequest(FailureMsg failureMsg) {
                                            notifyUser(failureMsg.detail);
                                        }
                                    });
                                } catch (Exception e) {
                                    Dbg.loge(this.getClass().getName(), "Fingerprint not saved.", e);
                                    notifyUser("Fingerprint collection failed: " + e.getMessage());
                                }
                            }
                        }
                );
                fingerprintCollector.collectSamples(numberOfSamples);
                notifyUser("Beginning sample collection");
            }
        });
    }


    public void cancel(View cancelButton) {
        if (fingerprintCollector != null) {
            fingerprintCollector.cancel();
            resultBox.append("\nCanceled.");
        }
        for (final Fingerprint fingerprint : fingerprintsJustCollected) {
            fingerprintServerClient.sendDeleteFingerprint(fingerprint, new AcknowledgedTask() {
                @Override
                public void onServerAcknowledge() {
                    notifyUser("Fingerprint deleted: "+fingerprint.id());
                }
            });
        }
    }

    private Vibrator vibrator = null;
    private void beep() {
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
            vibrator.vibrate(200);
        } catch (Exception e) {
            Dbg.loge(this.getClass().getName(), e.getMessage());
        }
    }

    private void clearResults(){
        resultBox.setText("");
    }

    private void displayTotalMeasurements(List<Fingerprint> fingerprints) {
        resultBox.append(fingerprints.size() + " samples collected, containing ");
        resultBox.append(Fingerprint.countUniqueChannels(fingerprints) + " unique bands.");
    }

    public void displaySuccessMessage(){
        resultBox.append("\nCOMPLETED\n-------------\n");
    }

}
