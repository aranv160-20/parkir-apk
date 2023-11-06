package com.example.parkirfirebase;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class AddActivity extends AppCompatActivity {

    private Spinner lokasi;
    private FirebaseStorage firebaseStorage;
    private StorageReference mStorageRef;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ImageView gambar;
    private Button cameraBtn;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> pilok;
    private ProgressDialog progressDialog;
    private QuerySnapshot cities;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        firebaseStorage = FirebaseStorage.getInstance();
        mStorageRef = firebaseStorage.getReference();

        cameraBtn = findViewById(R.id.cameraBtn);
        lokasi = findViewById(R.id.lokasi);
        gambar = findViewById(R.id.gambar);
        progressDialog = new ProgressDialog(AddActivity.this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please");

        pilok = new ArrayList<>();
        pilok.add("Gedung Kuliah Baru");
        pilok.add("Gedung KHD");
        pilok.add("Pos Jembatan");
        pilok.add("Pos Gedung S");
        pilok.add("Gedung Seroja");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, pilok);
        lokasi.setAdapter(adapter);
        lokasi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lokasi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                Toast.makeText(getApplicationContext(),adapter.getItem(i),Toast.LENGTH_SHORT).show();
                Log.e("ID CITY", cities.getDocuments().get(i).getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        getData();



        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(AddActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AddActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
                } else {
                    startCamera();
                }
            }
        });
    }
    private void getData(){
        progressDialog.show();
        db.collection("city").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                progressDialog.hide();
                cities = queryDocumentSnapshots;
                if (queryDocumentSnapshots.size() > 0) {
                    pilok.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        pilok.add(doc.getString("name"));
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplicationContext(), "Data tidak tersedia", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.hide();
                Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk mengambil gambar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {
            if (data != null && data.getExtras() != null) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                if (thumbnail != null) {
                    gambar.setImageBitmap(thumbnail);
                    uploadToFirebase(thumbnail);
                } else {
                    Toast.makeText(this, "Gagal mengambil gambar, coba lagi.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        String timestamp = String.valueOf(System.currentTimeMillis());
        StorageReference imageRef = mStorageRef.child("images/" + timestamp + ".jpg");

        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(AddActivity.this, "Sukses Upload", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddActivity.this, "Gagal upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseStorage", "Gagal upload: " + e.getMessage());
                });
    }
}