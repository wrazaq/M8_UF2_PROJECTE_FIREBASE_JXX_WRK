package com.example.m8_uf2_projecte_firebase_jxx_wrk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.m8_uf2_projecte_firebase_jxx_wrk.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.nio.file.FileStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    private ImageButton logOut_button;
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
        logOut_button = binding.logoutBtn;
        user_details = binding.userDetails;
        user = auth.getCurrentUser();
        saveDocumentBtn = binding.saveDocumentBtn;

        editTitle = binding.editTextTitle;
        editDescription = binding.editTextDescription;

        if (user == null){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        else {
            user_details.setText(user.getEmail());
            updateToken();

        }
        logOut_button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
        binding.saveDocumentBtn.setOnClickListener(v -> saveDocument(v));
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


    public void saveDocument(View v){
        String title = editTitle.getText().toString();
        String description = editDescription.getText().toString();

        Map<String, Object> document = new HashMap<>();
        document.put(KEY_TITLE, title);
        document.put(KEY_DESCRIPTION, description);

        db.collection("document").document("My first Document").set(document)
                .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this, "Document saved", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    Log.d(TAG,e.toString());
                });
    }
}