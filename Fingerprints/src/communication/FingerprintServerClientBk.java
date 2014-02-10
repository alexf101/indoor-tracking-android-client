//package communication;
//
//import datatypes.Building;
//import datatypes.Fingerprint;
//import datatypes.Location;
//import datatypes.User;
//import messages.LocationsAreMsg;
//
//import java.net.URISyntaxException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeoutException;
//
//public interface FingerprintServerClientBk {
//    void setUser(String username, String password) throws URISyntaxException;
//
//    void createRandomUser(AsyncHttpResponseHandler<User> asyncHttpResponseHandler) throws InterruptedException, ExecutionException, TimeoutException;
//
//    User useRandomUser() throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException;
//
//    void sendFingerprint(Fingerprint fingerprint) throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException;
//
//    void sendFingerprint(Fingerprint fingerprint, AsyncHttpResponseHandler<Fingerprint> asyncHttpResponseHandler) throws InterruptedException, ExecutionException, TimeoutException;
//
//    void sendLocateMe(Fingerprint fingerprint, AsyncHttpResponseHandler<LocationsAreMsg> asyncHttpResponseHandler);
//
//    void sendWhatBuildings(AsyncHttpResponseHandler<Building[]> callBack) throws InterruptedException, ExecutionException, TimeoutException;
//
//    Building[] sendWhatBuildings() throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException;
//
//    void sendNewBuilding(Building building, AsyncHttpResponseHandler<Building> asyncHttpResponseHandler) throws InterruptedException, ExecutionException, TimeoutException;
//
//    Building sendNewBuilding(Building building) throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException;
//
//    Location[] sendWhatLocations(String building) throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException;
//
//    void sendWhatLocations(String building, AsyncHttpResponseHandler<Location[]> callBack) throws InterruptedException, ExecutionException, TimeoutException;
//
//    void sendNewLocation(Location location, AsyncHttpResponseHandler<Location> asyncHttpResponseHandler) throws InterruptedException, ExecutionException, TimeoutException;
//
//    void sendDeleteFingerprint(String fingerprint_id, AcknowledgedTask acknowledgedTask);
//}
