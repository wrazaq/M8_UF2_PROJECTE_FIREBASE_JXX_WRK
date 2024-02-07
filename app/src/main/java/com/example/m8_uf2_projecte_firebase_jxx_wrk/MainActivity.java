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
        }
        logOut_button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
        binding.saveDocumentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDocument(v);
            }
        });

    }

    public void saveDocument(View v){
        String title = editTitle.getText().toString();
        String description = editDescription.getText().toString();

        Map<String, Object> document = new HashMap<>();
        document.put(KEY_TITLE, title);
        document.put(KEY_DESCRIPTION, description);

        db.collection("document").document("My first Document").set(document)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "Document saved", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        Log.d(TAG,e.toString());

                    }
                });
    }
}