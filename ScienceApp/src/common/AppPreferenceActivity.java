package common;

import Fingerprint.Logger.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class AppPreferenceActivity extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}