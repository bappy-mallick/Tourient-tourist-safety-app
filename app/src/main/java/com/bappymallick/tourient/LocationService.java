package com.bappymallick.tourient;

import android.Manifest; // Added import
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "location_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private String documentId;
    private static final String TAG = "LocationService";

    @Override
    public void onCreate() {
        super.onCreate();

        db = FirebaseFirestore.getInstance();

        // Load documentId from SharedPreferences first
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        documentId = prefs.getString("documentId", null);
        Log.d(TAG, "Loaded documentId from SharedPreferences in onCreate: " + documentId);

        createNotificationChannel();

        Notification notification = getNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        Log.d(TAG, "Received location update: Lat=" + lat + ", Lng=" + lng);
                        updateLocationToFirestore(lat, lng);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Attempt to load/update documentId if provided in intent
        if (intent != null && intent.hasExtra("documentId")) {
            String newDocumentIdFromIntent = intent.getStringExtra("documentId");
            if (newDocumentIdFromIntent != null && !newDocumentIdFromIntent.equals(this.documentId)) {
                Log.d(TAG, "Received new/updated documentId via Intent: " + newDocumentIdFromIntent);
                this.documentId = newDocumentIdFromIntent;
                // Save the new/updated documentId persistently
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("documentId", this.documentId);
                editor.apply();
                Log.d(TAG, "Saved new/updated documentId to SharedPreferences.");
            }
        }

        // If after all checks, documentId is still not available, stop.
        if (this.documentId == null || this.documentId.isEmpty()) {
            Log.w(TAG, "No documentId available (checked SharedPreferences and Intent). Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.d(TAG, "Starting location updates for documentId: " + this.documentId);
        startLocationUpdates();

        return START_STICKY;
    }

    private void updateLocationToFirestore(double latitude, double longitude) {
        if (documentId == null || documentId.isEmpty()) {
            Log.w(TAG, "documentId is null or empty, skipping Firestore update");
            return;
        }

        Map<String, Object> locData = new HashMap<>();
        locData.put("latitude", latitude);
        locData.put("longitude", longitude);
        locData.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference docRef = db.collection("users").document(documentId);
        docRef.set(locData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore update success for user: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore update failed for user: " + documentId, e));
    }

    public void startSOSService() {
        if (documentId == null || documentId.isEmpty()) {
            Log.w(TAG, "Cannot start SOS: documentId is null or empty");
            return;
        }
        Intent sosIntent = new Intent(this, SOS.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sosIntent);
        } else {
            startService(sosIntent);
        }
        Log.d(TAG, "Attempted to start SOS service from LocationService with documentId: " + documentId);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted. Cannot start location updates.");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); 
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Requested location updates.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while requesting location updates.", e);
            stopSelf();
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tourient Location Service")
                .setContentText("Tracking your location for app features.") 
                .setSmallIcon(R.drawable.ic_location) 
                .setPriority(NotificationCompat.PRIORITY_LOW) 
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW 
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            Log.d(TAG, "Removing location updates in onDestroy.");
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }
}
