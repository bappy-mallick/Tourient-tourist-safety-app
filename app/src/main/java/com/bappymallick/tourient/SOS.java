package com.bappymallick.tourient;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SOS extends Service {

    private String documentId;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isSmsSent = false;

    private static final String PREF_EMERGENCY_SMS_NUMBERS = "emergency_sms_numbers";
    private static final String PREF_EMERGENCY_CALL_NUMBERS = "emergency_call_numbers";

    private String[] smsNumbers = new String[]{"+91 8107272366","1000"};
    private String[] callNumbers = new String[]{"+91 8107272366","1000"};

    private int callAttemptIndex = 0;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isCallListenerActive = false;
    private Handler callHandler = new Handler(Looper.getMainLooper());
    private static final int CALL_RETRY_DELAY_MS = 2000; // 2 seconds delay

    // BLE
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isAdvertising = false;
    private static final ParcelUuid SERVICE_UUID = new ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"));
    private static final int MANUFACTURER_ID = 0xFFFF;

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d("SOS_BLE", "BLE Advertising started successfully.");
            isAdvertising = true;
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e("SOS_BLE", "BLE Advertising onStartFailure: " + errorCode);
            isAdvertising = false;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w("SOS_BLE", "Bluetooth is not enabled or not available.");
        } else {
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (bluetoothLeAdvertiser == null) {
                Log.w("SOS_BLE", "BLE Advertising not supported on this device.");
            }
        }

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        documentId = prefs.getString("documentId", null);
        if (documentId == null || documentId.isEmpty()) {
            Log.w("SOS", "No documentId found in SharedPreferences. Stopping service.");
            stopSelf();
            return;
        }

        Set<String> smsSet = prefs.getStringSet(PREF_EMERGENCY_SMS_NUMBERS, null);
        if (smsSet != null && !smsSet.isEmpty()) {
            smsNumbers = smsSet.toArray(new String[0]);
        }

        Set<String> callSet = prefs.getStringSet(PREF_EMERGENCY_CALL_NUMBERS, null);
        if (callSet != null && !callSet.isEmpty()) {
            callNumbers = callSet.toArray(new String[0]);
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w("SOS", "Location update is null");
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    uploadToFirestore(location);
                    startBleAdvertising(location);

                    if (!isSmsSent) {
                        if (ContextCompat.checkSelfPermission(SOS.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                            sendSMSAlert(SOS.this, location);
                        } else {
                            Log.w("SOS", "SEND_SMS permission not granted. Skipping SMS alert.");
                        }
                        isSmsSent = true;

                        if (callNumbers.length > 0) {
                            if (ContextCompat.checkSelfPermission(SOS.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                callAttemptIndex = 0;
                                callAuthorities(SOS.this, callNumbers);
                            } else {
                                Log.w("SOS", "CALL_PHONE permission not granted. Skipping emergency calls.");
                            }
                        }
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        triggerSOS(this);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callHandler.removeCallbacksAndMessages(null); // Remove any pending delayed calls
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (telephonyManager != null && phoneStateListener != null && isCallListenerActive) {
            Log.d("SOS", "onDestroy: Unregistering PhoneStateListener.");
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            isCallListenerActive = false;
        }
        stopBleAdvertising();
    }

    private void triggerSOS(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SOS", "ACCESS_FINE_LOCATION permission not granted. SOS service cannot get location.");
            stopSelf();
            return;
        }
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void uploadToFirestore(Location location) {
        DocumentReference sosRef = db.collection("SOS").document(documentId);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("latitude", location.getLatitude());
        dataMap.put("longitude", location.getLongitude());
        dataMap.put("timestamp", FieldValue.serverTimestamp());
        sosRef.set(dataMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("SOS", "Successfully updated SOS data in Firestore"))
                .addOnFailureListener(e -> Log.e("SOS", "Failed to update SOS data in Firestore", e));
    }

    private void sendSMSAlert(Context context, Location location) {
        String message = "SOS! Need help at location: http://maps.google.com/maps?q="
                + location.getLatitude() + "," + location.getLongitude();
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (String number : smsNumbers) {
                smsManager.sendTextMessage(number, null, message, null, null);
            }
            Log.d("SOS", "Sent SMS alert to emergency contacts.");
        } catch (Exception e) {
            Log.e("SOS", "Error sending SMS", e);
        }
    }

    private void callAuthorities(Context context, String[] emergencyNumbers) {
        if (callAttemptIndex >= emergencyNumbers.length) {
            Log.d("SOS", "All emergency numbers have been attempted.");
            if (telephonyManager != null && phoneStateListener != null && isCallListenerActive) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                isCallListenerActive = false;
            }
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SOS", "CALL_PHONE permission not granted for " + emergencyNumbers[callAttemptIndex] + ". Trying next.");
            callAttemptIndex++;
            callAuthorities(context, emergencyNumbers);
            return;
        }

        String currentNumber = emergencyNumbers[callAttemptIndex];
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(android.net.Uri.parse("tel:" + currentNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.d("SOS", "Attempting to call: " + currentNumber + " (Attempt " + (callAttemptIndex + 1) + ")");

        if (phoneStateListener == null) {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    super.onCallStateChanged(state, incomingNumber);
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE:
                            Log.d("SOS", "PhoneStateListener: Call state IDLE. Last called index: " + callAttemptIndex);
                            if (telephonyManager != null && isCallListenerActive) {
                                Log.d("SOS", "PhoneStateListener: Unregistering self due to IDLE state.");
                                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                                isCallListenerActive = false;
                            }
                            callAttemptIndex++;
                            if (callAttemptIndex < emergencyNumbers.length) {
                                Log.d("SOS", "PhoneStateListener: Scheduling next call attempt to index " + callAttemptIndex + " after delay.");
                                callHandler.postDelayed(() -> {
                                    if (ContextCompat.checkSelfPermission(SOS.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        callAuthorities(SOS.this, emergencyNumbers);
                                    } else {
                                        Log.w("SOS", "CALL_PHONE permission revoked before next attempt. Stopping call sequence.");
                                    }
                                }, CALL_RETRY_DELAY_MS);
                            } else {
                                Log.d("SOS", "PhoneStateListener: All numbers attempted.");
                            }
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                            Log.d("SOS", "PhoneStateListener: Call state RINGING for number: " + incomingNumber);
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            Log.d("SOS", "PhoneStateListener: Call state OFFHOOK (call active or dialing) for number: " + (callAttemptIndex < emergencyNumbers.length ? emergencyNumbers[callAttemptIndex] : "Unknown"));
                            break;
                    }
                }
            };
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (telephonyManager != null) {
                if (isCallListenerActive) { // Unregister if a previous listener was somehow still active for a different call leg
                    Log.w("SOS", "Call listener was unexpectedly active before new registration. Unregistering first.");
                    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                    isCallListenerActive = false;
                }
                Log.d("SOS", "Registering PhoneStateListener for number: " + currentNumber);
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                isCallListenerActive = true;
            } else {
                 Log.e("SOS", "TelephonyManager is null, cannot register listener.");
            }
        } else {
            Log.w("SOS", "READ_PHONE_STATE permission not granted. Automatic call retry on disconnect disabled for: " + currentNumber);
            // If no READ_PHONE_STATE, we make the call but cannot automatically retry the *next* number on disconnect.
            // The current call is made, and the sequence (for this specific call chain) effectively stops here in terms of auto-retry.
        }

        try {
            context.startActivity(callIntent);
        } catch (SecurityException e) {
            Log.e("SOS", "SecurityException making call to " + currentNumber + ". This usually means CALL_PHONE was revoked.", e);
            if (telephonyManager != null && phoneStateListener != null && isCallListenerActive) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                isCallListenerActive = false;
            }
            callAttemptIndex++; // Try next number if this one failed due to security exception
            callAuthorities(context, emergencyNumbers);
        }
    }

    private void startBleAdvertising(Location location) {
        if (bluetoothLeAdvertiser == null) {
            Log.e("SOS_BLE", "BluetoothLeAdvertiser not initialized.");
            return;
        }
        if (isAdvertising) {
            Log.d("SOS_BLE", "Already advertising.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SOS_BLE", "BLUETOOTH_ADVERTISE permission not granted (Android 12+).");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
        ByteBuffer manufacturerData = ByteBuffer.allocate(20);
        manufacturerData.put("HELP".getBytes(StandardCharsets.UTF_8));
        manufacturerData.putDouble(location.getLatitude());
        manufacturerData.putDouble(location.getLongitude());
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(MANUFACTURER_ID, manufacturerData.array())
                .build();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                 Log.w("SOS_BLE", "BLUETOOTH_CONNECT permission not granted (Android 12+). Advertising might fail or have issues.");
            }
            bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
        } catch (SecurityException se) {
            Log.e("SOS_BLE", "SecurityException starting BLE advertising.", se);
        }
    }

    private void stopBleAdvertising() {
        if (bluetoothLeAdvertiser == null || !isAdvertising) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SOS_BLE", "BLUETOOTH_ADVERTISE permission not granted (Android 12+). Cannot stop advertising cleanly.");
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                 Log.w("SOS_BLE", "BLUETOOTH_CONNECT permission not granted (Android 12+). Stopping advertising might fail or have issues.");
            }
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        } catch (SecurityException se) {
            Log.e("SOS_BLE", "SecurityException stopping BLE advertising.", se);
        }
        isAdvertising = false;
        Log.d("SOS_BLE", "Attempted to stop BLE Advertising.");
    }
}
