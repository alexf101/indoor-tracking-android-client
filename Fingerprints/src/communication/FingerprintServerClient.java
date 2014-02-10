package communication;

import android.util.Base64;
import datatypes.Building;
import datatypes.User;
import messages.*;
import datatypes.Fingerprint;
import datatypes.Location;
import util.Dbg;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static communication.HttpMethod.*;

public class FingerprintServerClient {

    private static String defaultServerAddress = "http://fingerprinttracker.com/api/";
    Sender sender;
    private User user; /** The user to use for authentication. May be null if not yet set.**/


    public FingerprintServerClient() throws Exception {
        this(defaultServerAddress);
    }

    /**
     * If you use this constructor, you will initially only be able to make GET requests since you haven't set a valid user.
     * You can use this constructor, and then call createAndSetUser or createRandomUser to make a new user and use it for authenticated requests.
     * Otherwise, use the constructor that accepts a username and password if you already have an account.
     * @param serverRoot
     * @throws Exception
     */
    public FingerprintServerClient(String serverRoot) throws Exception {
        this(serverRoot, null, null, true);
    }

    public FingerprintServerClient(String username, String password) throws Exception {
        this(defaultServerAddress, username, password, true);
    }
    public FingerprintServerClient(String username, String password, boolean userMayNotExist) throws Exception {
        this(defaultServerAddress, username, password, userMayNotExist);
    }

    public FingerprintServerClient(String serverRoot, String username, String password) throws Exception {
        this(serverRoot, username, password, true);
    }

    public FingerprintServerClient(String serverRoot, String username, String password, boolean userMayNotExist) throws Exception {
        sender = new AndroidHttpSender(serverRoot);
        if (username != null && password != null) {
            setUser(username, password);
            if (userMayNotExist){
                sendNewUser(username, password, new ResponseHandler<User>() {

                    @Override
                    public void onServerResponse(User msg) {
                        Dbg.logd(this.getClass().getName(), "Created user: " + msg);
                    }

                    @Override
                    public void onServerDeniedRequest(FailureMsg failureMsg) {
                        Dbg.logw(this.getClass().getName(), "Could not create user: " + failureMsg.detail);
                    }
                });
            }
        }
    }

    public void setUser(String username, String password) {
        sender.setUser(username, password);
    }

    public void sendNewUser(String username, String password, ResponseHandler<User> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(POST, "users/", new User(username, password), User.class, responseHandler);
    }

    public User sendNewUser(String username, String password) {
        return sender.sync_send(POST, "users/", new User(username, password), User.class);
    }


    public void createRandomUser(ResponseHandler<User> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        User user = new User(randomPassword(8), randomPassword(16));
        sender.async_send(POST, "users/", user, User.class, responseHandler);
    }


    public void useRandomUser() throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException {
        User user = new User(randomPassword(8), randomPassword(16));
        sender.async_send(POST, "users/", user, User.class, new ResponseHandler<User>() {
            @Override
            public void onServerResponse(User msg) {
                setUser(msg.getUsername(), msg.getPassword());
            }

            @Override
            public void onServerDeniedRequest(FailureMsg failureMsg) {
                Dbg.loge(this.getClass().getName(), "Create random user request failed: "+failureMsg.detail);
            }
        });
    }

    private String randomPassword(int minLength) {
        byte[] password_bytes = new byte[minLength];
        new Random().nextBytes(password_bytes);
        return new String(Base64.encode(password_bytes, Base64.DEFAULT));
    }

    /**
     *
     * The Fingerprint must have a valid location url.
     *
     * To get a location url, use sendGetLocation with a location name, or sendWhatLocations(Building).
     *
     * @param fingerprint
     */

    /**
     *
     * The Fingerprint must have a valid location url.
     *
     * To get a location url, use sendGetLocation with a location name, or sendWhatLocations(Building).
     *
     * @param fingerprint
     */

    public void sendFingerprint(Fingerprint fingerprint, ResponseHandler<Fingerprint> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(POST, "fingerprints/", fingerprint, Fingerprint.class, responseHandler);
    }


    public void sendLocateMe(Fingerprint fingerprint, String buildingName, ResponseHandler<LocationsFoundMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sendLocateMe(fingerprint, buildingName, 1, responseHandler);
    }

    public void sendLocateMe(Fingerprint fingerprint, Building building, int limit, ResponseHandler<LocationsFoundMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sendLocateMe(fingerprint, building.getName(), limit, responseHandler);
    }

    public void sendLocateMe(Fingerprint fingerprint, Building building, ResponseHandler<LocationsFoundMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sendLocateMe(fingerprint, building.getName(), 1, responseHandler);
    }

    public void sendLocateMe(Fingerprint fingerprint, String buildingName, int limit, ResponseHandler<LocationsFoundMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        Msg msg = new LocateMeMsg(fingerprint, buildingName);
        sender.async_send(POST, "mylocation/"+limit+"/", msg, LocationsFoundMsg.class, responseHandler);
    }

    public void sendWhatBuildings(ResponseHandler<Building[]> callBack) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(GET, "buildings/", new WhatBuildingsMsg(), Building[].class, callBack);
    }


    public void sendNewBuilding(Building building, ResponseHandler<Building> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(POST, "buildings/", building, Building.class, responseHandler);
    }

    public Building sendNewBuilding(Building building) {
        return sender.sync_send(POST, "buildings/", building, Building.class);
    }

    public void sendDeleteBuilding(Building building, AcknowledgedTask acknowledgedTask) {
        sender.async_send_no_reply(DELETE, building.getUrl(), acknowledgedTask);
    }

    public void sendWhatLocations(String building, ResponseHandler<Location[]> callBack) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            sender.async_send(GET, "locations/?building__name=" + URLEncoder.encode(building, "UTF-8"), null, Location[].class, callBack);
        } catch (UnsupportedEncodingException e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
        }
    }

    /**
     * Location must have a valid, existing building_url
     * @param location
     * @param responseHandler
     */

    public void sendNewLocation(Location location, ResponseHandler<Location> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(POST, "locations/", location, Location.class, responseHandler);
    }

    public Location sendNewLocation(Location location) {
        return sender.sync_send(POST, "locations/", location, Location.class);
    }

    public void sendDeleteLocation(Location location, AcknowledgedTask acknowledgedTask) {
        sender.async_send_no_reply(DELETE, location.getUrl(), acknowledgedTask);
    }

    public void sendDeleteFingerprint(Fingerprint fingerprint, AcknowledgedTask acknowledgedTask) {
        sender.async_send_no_reply(DELETE, "fingerprints/" + fingerprint.id() + "/", acknowledgedTask);
    }

    public void sendGetUserDetails(String user_hyperlink, ResponseHandler<User> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(GET, user_hyperlink, null, User.class, responseHandler);
    }

    public void sendConfirm(Location l, Long lastFingerprintID, ResponseHandler<ConfirmMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(PUT, "confirm/" + l.id + "/" + lastFingerprintID, null, ConfirmMsg.class, responseHandler);
    }

    public void sendGetOrCreateLocation(Location location, ResponseHandler<Location> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        sender.async_send(POST, "custom/location/", location, Location.class, responseHandler);
    }

    public Location sendGetOrCreateLocation(Location location) throws InterruptedException, ExecutionException, TimeoutException {
        return sender.sync_send(POST, "custom/location/", location, Location.class);
    }

    public void close() {
        sender.close();
    }

    public void sendConfirm(String artworkChosen, String building_name, long fingerprintID, ResponseHandler<ConfirmMsg> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        Location l = new Location(artworkChosen, new Building(building_name));
        sender.async_send(PUT, "confirm/", l, ConfirmMsg.class, responseHandler);
    }
}