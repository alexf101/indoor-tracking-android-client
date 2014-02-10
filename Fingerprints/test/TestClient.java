import android.util.Log;
import communication.AcknowledgedTask;
import communication.FingerprintServerClient;
import datatypes.Building;
import datatypes.Location;
import datatypes.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import util.Dbg;

@RunWith(RobolectricTestRunner.class)
public class TestClient {

    FingerprintServerClient fingerprintServerClient;

    public TestClient() throws Exception {
        Dbg.onPhone = false;
        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        fingerprintServerClient = new FingerprintServerClient("http://localhost:8000/api/",
                "TestUser",
                "TestUser'sPassword");
    }

    @Test
    public void syncLocations() throws InterruptedException {
        Building building = fingerprintServerClient.sendNewBuilding(new Building("test_building_1"));
        Location location = fingerprintServerClient.sendNewLocation(new Location("test_location_1", building));
        fingerprintServerClient.sendDeleteBuilding(building, new AcknowledgedTask() {
            @Override
            public void onServerAcknowledge() {
                System.out.println("Building deleted");
            }
        });
        fingerprintServerClient.sendDeleteLocation(location, new AcknowledgedTask() {
            @Override
            public void onServerAcknowledge() {
                System.out.println("Location deleted");
            }
        });
        System.out.println(building);
        System.out.println(location);
        Thread.sleep(1000);
    }

}
