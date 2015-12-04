package com.example.dell_15.final_call;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.GetCallback;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    MapView mMapView;
    Marker startPerc;
    GoogleApiClient mGoogleApiClient;
    List<LatLng> markerPoints;
    Geocoder gCoder = null;
    List<Address> addrList = new ArrayList<Address>();
    String srcDest[] = new String[2];
    ArrayList<LatLng> placeLatLng = new ArrayList<LatLng>();
//    String placeId[] = new String[2];
//    List<Address> endAddrList = new ArrayList<Address>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        markerPoints = new ArrayList<LatLng>();

        Intent i = getIntent();
        Bundle b = i.getBundleExtra("LocBundle");
        if (b != null) {
            placeLatLng = b.getParcelableArrayList("SrcDest");
        }
//        placeId = b.getStringArray("SrcDest");

        Button updateBtn = (Button)findViewById(R.id.btn_update);

        initializemap();

        gCoder = new Geocoder(this);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            int i = 0;

            @Override
            public void onMapClick(LatLng latLng) {
                // Already two locations
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                markerPoints.add(latLng);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(latLng);

                /**
                 * For the start location, the color of marker is GREEN and
                 * for the end location, the color of marker is RED.
                 */
                if (markerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    try {
                        addrList = gCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        srcDest[0] = addrList.get(0).getAddressLine(0) +", "+ addrList.get(0).getAddressLine(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    try {
                        addrList = gCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        srcDest[1] = addrList.get(0).getAddressLine(0) +", "+ addrList.get(0).getAddressLine(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options).setTitle(addrList.get(0).getAddressLine(1));

                // Checks, whether start and end locations are captured
                if (markerPoints.size() >= 2) {
                    LatLng origin = markerPoints.get(0);
                    LatLng dest = markerPoints.get(1);

                    // Getting URL to the Google Directions API
                    String url = getDirectionsUrl(origin, dest);

                    DownloadTask downloadTask = new DownloadTask();

                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);
                }
            }

        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseQuery<ParseObject> query1 = ParseQuery.getQuery("PathFence");
                query1.whereEqualTo("Source", "Lal Bagh Rd, Mavalli").whereEqualTo("Destination", "2/1, Infantry Rd, Vasanth Nagar");
                query1.getFirstInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject objects, com.parse.ParseException e) {
                        ParseGeoPoint pgPt = objects.getParseGeoPoint("DriverLocation");
                        Log.d("Point", pgPt.toString());
//                        MarkerOptions mopt = new MarkerOptions();
//                        mopt.position(new LatLng(pgPt.getLatitude(),pgPt.getLongitude()));
//
//                        mMap.addMarker(mopt);
                    }
                });
            }
        });
    }

    private void initializemap() {
        setUpMapIfNeeded();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            TextView txt = (TextView) findViewById(R.id.err);
            txt.setText("Cannot do this");
        }
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        // Create a criteria object to retrieve provider
//        Criteria criteria = new Criteria();
//
//        // Get the name of the best provider
//        String provider = locationManager.getBestProvider(criteria, true);
//
//        // Get Current Location
//        Location myLocation = locationManager.getLastKnownLocation(provider);
//
//        //set map type
//        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//
//        // Get latitude of the current location
//        double latitude = myLocation.getLatitude();
//
//        // Get longitude of the current location
//        double longitude = myLocation.getLongitude();
//
//        // Create a LatLng object for the current location
//        LatLng latLng = new LatLng(latitude, longitude);
//
//        // Show the current location in Google Map
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//
//        // Zoom in the Google Map
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You are here!"));

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(placeLatLng.get(0)).title("Start"));
        mMap.addMarker(new MarkerOptions().position(placeLatLng.get(placeLatLng.size() - 1)).title("End"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(placeLatLng.get(0)));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        /*
        *
        //Change from Here
        */
            LatLng origin = placeLatLng.get(0);
            LatLng dest = placeLatLng.get(1);

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // if (requestCode == MY_LOCATION_REQUEST_CODE) {
        if (permissions.length == 1 &&
                permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            onLocationChanged(mLastLocation);
        } else {
            // Permission was denied. Display an error message.
        }
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        // UiSettings.setMyLocationButtonEnabled(false);
    }

    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        startPerc.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher));
        LatLng origin = new LatLng(3.214732, 101.747047);
        LatLng dest = new LatLng(3.214507, 101.749697);

// Getting URL to the Google Directions API
//        String url = getDirectionsUrl(origin, dest);
//
//        DownloadTask downloadTask = new DownloadTask();
//
//// Start downloading json data from Google Directions API
//        downloadTask.execute(url);

    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Enable bicycling mode
        String mode = "mode=driving";

        String output = "json";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+ parameters;

        return url;
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    public class DirectionsJSONParser {

        /** Receives a JSONObject and returns a list of lists containing latitude and longitude */
        public List<List<HashMap<String,String>>> parse(JSONObject jObject){

            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String,String>>>() ;
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for(int i=0;i<jRoutes.length();i++){
                    jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<HashMap<String, String>>();

                    /** Traversing all legs */
                    for(int j=0;j<jLegs.length();j++){
                        jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for(int k=0;k<jSteps.length();k++){
                            String polyline = "";
                            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for(int l=0;l<list.size();l++){
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                                hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
            }

            return routes;
        }
        /**
         * Method to decode polyline points
         * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
         * */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
//        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            //Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            ArrayList<ParseGeoPoint> parsePtList = null;
            LinkedList<LatLng> leftFencePts = null;
            LinkedList<LatLng> rightFencePts = null;
            LinkedList<ParseGeoPoint> parseLeftFenceList = null;
            LinkedList<ParseGeoPoint> parseRightFenceList = null;
            PolylineOptions lineOptions = null;
            PolygonOptions polyOpt = null;
//            JSONArray geoFence = null;
            ParseGeoPoint parsePos = null;



            MarkerOptions markerOptions = new MarkerOptions();


            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                parsePtList = new ArrayList<ParseGeoPoint>();
                parseLeftFenceList = new LinkedList<ParseGeoPoint>();
                parseRightFenceList = new LinkedList<ParseGeoPoint>();
//                geoFence = new JSONArray();
                leftFencePts = new LinkedList<LatLng>();
                rightFencePts = new LinkedList<LatLng>();


                lineOptions = new PolylineOptions();
                polyOpt = new PolygonOptions();
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    parsePos = new ParseGeoPoint(lat, lng);
                    LatLng position = new LatLng(lat, lng);

                    Location strtLoc = new Location("StartLoc");
                    strtLoc.setLatitude(lat);
                    strtLoc.setLongitude(lng);

                    Location polyLocLeft = getDestinationPoint(strtLoc, strtLoc.getBearing() + 90, 2);
                    Location polyLocRight = getDestinationPoint(strtLoc, strtLoc.getBearing() - 90, 2);


//                        if (j == 0) {
//                        Location strtLoc = new Location("StartLoc");
//                        strtLoc.setLatitude(lat);
//                        strtLoc.setLongitude(lng);
//                        if (strtLoc.getBearing() == 0) {
//                            polyLoc[0] = getDestinationPoint(strtLoc, 90, 1);
//                            polyLoc[3] = getDestinationPoint(strtLoc, -90, 1);
//                        }else{
//                            polyLoc[0] = getDestinationPoint(strtLoc, 0, 1);
//                            polyLoc[3] = getDestinationPoint(strtLoc, 180, 1);
//                        }
//
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(polyLoc[0].getLatitude(), polyLoc[0].getLongitude())).title( polyLoc[0].toString()));
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(polyLoc[3].getLatitude(), polyLoc[3].getLongitude())).title( polyLoc[3].toString()));


//                        } else if (j == path.size() - 1) {
//                        Location strtLoc = new Location("StartLoc");
//                        strtLoc.setLatitude(lat);
//                        strtLoc.setLongitude(lng);
//
//                        if (strtLoc.getBearing() == 0) {
//                            polyLoc[1] = getDestinationPoint(strtLoc, 90, 1);
//                            polyLoc[2] = getDestinationPoint(strtLoc, -90, 1);
//                        }else{
//                            polyLoc[1] = getDestinationPoint(strtLoc, 0, 1);
//                            polyLoc[2] = getDestinationPoint(strtLoc, 180, 1);
//                        }
//
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(polyLoc[1].getLatitude(), polyLoc[1].getLongitude())).title(polyLoc[1].toString()));
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(polyLoc[2].getLatitude(), polyLoc[2].getLongitude())).title(polyLoc[2].toString()));
//
//                        // Instantiates a new Polygon object and adds points to define a rectangle
//                        PolygonOptions rectOptions = new PolygonOptions()
//                                .add(new LatLng(polyLoc[0].getLatitude(), polyLoc[0].getLongitude()),
//                                        new LatLng(polyLoc[1].getLatitude(), polyLoc[1].getLongitude()),
//                                        new LatLng(polyLoc[2].getLatitude(), polyLoc[2].getLongitude()),
//                                        new LatLng(polyLoc[3].getLatitude(), polyLoc[3].getLongitude())).strokeWidth(3).strokeColor(Color.BLUE).fillColor(Color.parseColor("#f06e6e"));
//
//                        // Get back the mutable Polygon
//                        Polygon polygon = mMap.addPolygon(rectOptions);


//                        }

                    points.add(position);
                    parsePtList.add(parsePos);

                    leftFencePts.add(new LatLng(polyLocLeft.getLatitude(), polyLocLeft.getLongitude()));
                    parseLeftFenceList.add(new ParseGeoPoint(polyLocLeft.getLatitude(), polyLocLeft.getLongitude()));

                    rightFencePts.add(new LatLng(polyLocRight.getLatitude(), polyLocRight.getLongitude()));
                    parseRightFenceList.add(new ParseGeoPoint(polyLocRight.getLatitude(), polyLocRight.getLongitude()));
                }

//                geoFence.put(parsePtList);
//                geoFence.put(parseLeftFenceList);
//                geoFence.put(parseRightFenceList);

                ParseObject testObject = new ParseObject("PathFence");
                testObject.put("Source", srcDest[0]);
                testObject.put("Destination", srcDest[1]);
                testObject.put("ParsePoints", parsePtList);
                testObject.put("ParseLeftPoints", parseLeftFenceList);
                testObject.put("ParseRightPoints", parseRightFenceList);
                testObject.saveInBackground();

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                polyOpt.addAll(leftFencePts).strokeWidth(3).strokeColor(Color.BLUE);
                Collections.reverse(rightFencePts);
                polyOpt.addAll(rightFencePts).strokeWidth(3).strokeColor(Color.BLUE);;


            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
            mMap.addPolygon(polyOpt);
        }
    }

    public static Location getDestinationPoint(Location startLoc, float bearing, float depth)
    {
        Location newLocation = new Location("newLocation");

        double radius = 6371.0; // earth's mean radius in km
        double lat1 = Math.toRadians(startLoc.getLatitude());
        double lng1 = Math.toRadians(startLoc.getLongitude());
        double brng = Math.toRadians(bearing);
        double lat2 = Math.asin( Math.sin(lat1)*Math.cos(depth/radius) + Math.cos(lat1)*Math.sin(depth/radius)*Math.cos(brng) );
        double lng2 = lng1 + Math.atan2(Math.sin(brng)*Math.sin(depth/radius)*Math.cos(lat1), Math.cos(depth/radius)-Math.sin(lat1)*Math.sin(lat2));
        lng2 = (lng2+Math.PI)%(2*Math.PI) - Math.PI;

        // normalize to -180...+180
        if (lat2 == 0 || lng2 == 0)
        {
            newLocation.setLatitude(0.0);
            newLocation.setLongitude(0.0);
        }
        else
        {
            newLocation.setLatitude(Math.toDegrees(lat2));
            newLocation.setLongitude(Math.toDegrees(lng2));
        }

        return newLocation;
    };
}


