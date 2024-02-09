package com.example.m8_uf2_projecte_firebase_jxx_wrk;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.example.m8_uf2_projecte_firebase_jxx_wrk.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    private TextView user_details;
    FirebaseUser user;
    private static final String TAG = "MainActivity";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private EditText editTitle;
    private EditText editDescription;
    private Button saveDocumentBtn;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        user_details = binding.userDetails;
        user = auth.getCurrentUser();
        saveDocumentBtn = binding.saveDocumentBtn;

        editTitle = binding.editTextTitle;
        editDescription = binding.editTextDescription;

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            user_details.setText(user.getEmail());
            updateToken();

        }
        binding.saveDocumentBtn.setOnClickListener(this::saveDocument);
    }

    private void updateToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        Log.d(TAG, "Token: " + token);
                        updateTokenInFirebase(token);
                    } else {
                        Log.w(TAG, "FCM token is null");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get FCM token", e);
                });
    }

    private void updateTokenInFirebase(String token) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("userToken", token);

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(tokenMap, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "FCM token updated in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token in Firestore", e));
    }

    public void saveDocument(View v) {
        String title = editTitle.getText().toString();
        String description = editDescription.getText().toString();

        checkAndSetEditingStatus(title, description);
    }

    public void logoutAndResetEditingStatus(View v) {

        db.collection("editingStatus").document("MyEditingStatus")
                .update("currentUserId", FieldValue.delete())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Current user ID removed from editingStatus"))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing current user ID from editingStatus", e));

        updateEditingStatus(false);

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }

    private void updateEditingStatus(boolean isBeingEdited) {
        DocumentReference editingStatusRef = db.collection("editingStatus").document("MyEditingStatus");

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("isBeingEdited", isBeingEdited);
        if (isBeingEdited) {
            updateData.put("currentUserId", user.getUid());
        }
        editingStatusRef.update(updateData)
                .addOnSuccessListener(unused -> Log.d(TAG, "Editing status updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating editing status", e));
    }

    private void checkAndSetEditingStatus(String title, String description) {
        DocumentReference editingStatusRef = db.collection("editingStatus").document("MyEditingStatus");

        editingStatusRef.get().addOnSuccessListener(documentSnapshot -> {
            boolean isCurrentlyBeingEdited = documentSnapshot.exists() && documentSnapshot.getBoolean("isBeingEdited");
            String currentUserId = documentSnapshot.getString("currentUserId");

            if (!isCurrentlyBeingEdited || (currentUserId != null && currentUserId.equals(user.getUid()))) {
                updateEditingStatus(true);
                updateDocument(title, description);
            } else {
                sendNotification();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Error checking editing status", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error checking editing status", e);
        });
    }

    private void updateDocument(String title, String description) {
        Map<String, Object> document = new HashMap<>();
        document.put(KEY_TITLE, title);
        document.put(KEY_DESCRIPTION, description);

        db.collection("document").document("My first Document").set(document)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(MainActivity.this, "Document saved", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving document", e);
                });
    }

    private void sendNotification() {

        String notificationMessage = "Another user is editing this document, try again later";

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Document Editing Not Allowed")
                        .setContentText(notificationMessage)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}

