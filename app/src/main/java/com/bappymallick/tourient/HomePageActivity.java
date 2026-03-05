package com.bappymallick.tourient;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class HomePageActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 2000;
    private static final int REQUEST_CHECK_SETTINGS = 123;

    private static final String TAG = "HomePageActivity";

    private LinearLayout btnAccommodation, btnMedical, btnLocation, btnDashboard;
    private AppCompatButton btnEmergencyHelp;
    private ImageView menuIcon, notificationIcon, assistantIcon; // assistantIcon moved here and type changed

    private CountDownTimer sosCountDownTimer;
    private boolean isSosTimerRunning = false;

    private AlertDialog sosDialog;
    private long powerButtonPressStartTime = 0;
    private BroadcastReceiver screenReceiver;

    private boolean awaitingPermissionsForSosAction = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        initializeViews();
        setupClickListeners();

        checkAndRequestPermissionsIfNeeded(false); // Initial permission check on app startup
        registerScreenReceiver();
    }

    private void initializeViews() {
        btnAccommodation = findViewById(R.id.btn_accommodation);
        btnMedical = findViewById(R.id.btn_medical);
        btnLocation = findViewById(R.id.btn_location);
        btnDashboard = findViewById(R.id.btn_dashboard);
        btnEmergencyHelp = findViewById(R.id.btn_emergency_help);
        notificationIcon = findViewById(R.id.notificationIcon); // Corrected ID
        assistantIcon = findViewById(R.id.assistantIcon);
    }

    private void setupClickListeners() {
        btnAccommodation.setOnClickListener(v -> startActivity(new Intent(HomePageActivity.this, AccommodationActivity.class)));
        btnMedical.setOnClickListener(v -> startActivity(new Intent(HomePageActivity.this, MedicalActivity.class)));
        btnLocation.setOnClickListener(v -> startActivity(new Intent(HomePageActivity.this, LocationActivity.class)));
        btnDashboard.setOnClickListener(v -> startActivity(new Intent(HomePageActivity.this, DashboardActivity.class)));
        // Permission check specifically for SOS action
        btnEmergencyHelp.setOnClickListener(v -> {checkAndRequestPermissionsIfNeeded(true);});

        notificationIcon.setOnClickListener(v -> Toast.makeText(this, "Opening notifications...", Toast.LENGTH_SHORT).show());
        assistantIcon.setOnClickListener(v -> Toast.makeText(this, "Opening AI Assistant...", Toast.LENGTH_SHORT).show());
    }

    private List<String> getListOfNeededPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        String[] basePermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
        };

        for (String permission : basePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        return neededPermissions;
    }

    private void checkAndRequestPermissionsIfNeeded(boolean isForSos) {
        List<String> listPermissionsNeeded = getListOfNeededPermissions();
        if (listPermissionsNeeded.isEmpty()) {
            handlePermissionSuccess(isForSos);
        } else {
            this.awaitingPermissionsForSosAction = isForSos;
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }

    private void handlePermissionSuccess(boolean isForSos) {
        if (isForSos) {
            startSosTimerWithPopup();
        } else {
            checkLocationSettings();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allCurrentlyRequestedPermissionsGranted = true;
            if (grantResults.length == 0 && permissions.length > 0) { // Request cancelled by user potentially
                allCurrentlyRequestedPermissionsGranted = false;
            } else {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allCurrentlyRequestedPermissionsGranted = false;
                        break;
                    }
                }
            }

            if (allCurrentlyRequestedPermissionsGranted) {
                handlePermissionSuccess(this.awaitingPermissionsForSosAction);
            } else {
                // Some permissions were denied.
                if (this.awaitingPermissionsForSosAction) {
                    // If it was for an SOS action, proceed with SOS anyway.
                    // The SOS service will handle doing what it can with granted permissions.
                    Log.d(TAG, "Proceeding with SOS activation despite some denied permissions. User not explicitly notified by HomePageActivity.");
                    handlePermissionSuccess(true); // Call with 'true' because it's for SOS
                } else {
                    // Not an SOS action (e.g., initial app load), and not all permissions granted.
                    // Do nothing further here; user not explicitly notified by HomePageActivity.
                    Log.d(TAG, "Initial permission check had some denials. User not explicitly notified by HomePageActivity.");
                     // We still call checkLocationSettings as it was the original next step for non-SOS permission success.
                    checkLocationSettings();
                }
            }
            this.awaitingPermissionsForSosAction = false; // Reset flag
        }
    }

    // getPermissionUserFriendlyName and showSettingsRedirectDialog are no longer called from onRequestPermissionsResult
    // They can be kept if they are used elsewhere or removed if not.
    private String getPermissionUserFriendlyName(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Precise Location";
            case Manifest.permission.SEND_SMS:
                return "Send SMS";
            case Manifest.permission.CALL_PHONE:
                return "Make Phone Calls";
            case Manifest.permission.READ_PHONE_STATE:
                return "Read Phone State (for call retry)";
            case Manifest.permission.BLUETOOTH_ADVERTISE:
                return "Bluetooth Advertising (for BLE SOS)";
            case Manifest.permission.BLUETOOTH_CONNECT:
                return "Bluetooth Connect (for BLE SOS)";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Post Notifications (for service status)";
            default:
                return permission.substring(permission.lastIndexOf('.') + 1);
        }
    }

    private void showSettingsRedirectDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Information")
                .setMessage(message + "\n\nYou can grant these permissions in app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startSosTimerWithPopup() {
        if (isSosTimerRunning) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_sos_countdown, null);
        builder.setView(dialogView);
        builder.setTitle("SOS Countdown");

        TextView tvCountdown = dialogView.findViewById(R.id.tv_countdown);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        sosDialog = builder.create();
        sosDialog.setCancelable(false);
        sosDialog.show();

        Toast.makeText(this, "SOS countdown started!", Toast.LENGTH_SHORT).show();

        sosCountDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvCountdown.setText("SOS will activate in " + secondsLeft + " second" + (secondsLeft > 1 ? "s" : ""));
                Log.d(TAG, "SOS timer tick: " + secondsLeft);
            }

            public void onFinish() {
                isSosTimerRunning = false;
                if (sosDialog != null && sosDialog.isShowing()) {
                    sosDialog.dismiss();
                }
                Toast.makeText(HomePageActivity.this, "SOS Activated! Service will use granted permissions.", Toast.LENGTH_LONG).show();

                Intent sosIntent = new Intent(HomePageActivity.this, SOS.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(sosIntent);
                } else {
                    startService(sosIntent);
                }
            }
        }.start();

        isSosTimerRunning = true;

        btnCancel.setOnClickListener(v -> {
            cancelSosTimer();
            if (sosDialog != null && sosDialog.isShowing()) {
                sosDialog.dismiss();
            }
        });
    }

    private void cancelSosTimer() {
        if (sosCountDownTimer != null) {
            sosCountDownTimer.cancel();
            isSosTimerRunning = false;
            Toast.makeText(this, "SOS cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_OFF:
                        powerButtonPressStartTime = System.currentTimeMillis();
                        new Handler().postDelayed(() -> {
                            long elapsed = System.currentTimeMillis() - powerButtonPressStartTime;
                            if (elapsed >= 5000 && !isSosTimerRunning && powerButtonPressStartTime != 0) {
                                Log.d(TAG, "Power button long pressed for 5 seconds. Checking permissions for SOS timer.");
                                runOnUiThread(() -> checkAndRequestPermissionsIfNeeded(true));
                            }
                        }, 5000);
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        powerButtonPressStartTime = 0; 
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        cancelSosTimer();
        if (sosDialog != null && sosDialog.isShowing()) {
            sosDialog.dismiss();
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startLocationService();
            }
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(HomePageActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (Exception sendEx) {
                    // Silently ignore if cannot prompt, or log only
                    Log.e(TAG, "Error showing location settings resolution", sendEx);
                }
            } else {
                // Silently ignore or log if location services are not enabled
                Log.w(TAG, "Location services are not enabled.");
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        try {
            startForegroundService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting LocationService in foreground", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startLocationService();
                }
            } else {
                // Silently ignore if user doesn't enable location settings or log
                Log.w(TAG, "User did not enable location settings when prompted.");
            }
        }
    }
}
