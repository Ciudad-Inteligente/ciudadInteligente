package com.example.ciudadinteligente;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdjuntarFotoReporteActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE  = 1;
    private static final int REQUEST_IMAGE_PICK     = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;
    private static final int REQUEST_GALLERY_PERMISSION = 4;

    private String areaNombre, tipoId, tipoNombre, asunto, descripcion, direccion;
    private double latitud, longitud;

    private Uri    fotoUri    = null;
    private File   fotoFile   = null;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    private ImageView   imgFotoPrevia;
    private TextView    tvEstadoFoto;
    private Button      btnEnviarReporte;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adjuntar_foto_reporte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth   = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        areaNombre  = getIntent().getStringExtra("AREA_SELECCIONADA");
        tipoId      = getIntent().getStringExtra("TIPO_ID");
        tipoNombre  = getIntent().getStringExtra("TIPO_NOMBRE"); // Este es el que necesitamos guardar
        asunto      = getIntent().getStringExtra("ASUNTO");
        descripcion = getIntent().getStringExtra("DESCRIPCION");
        direccion   = getIntent().getStringExtra("DIRECCION");
        latitud     = getIntent().getDoubleExtra("LATITUD",  3.9009);
        longitud    = getIntent().getDoubleExtra("LONGITUD", -76.2975);

        imgFotoPrevia    = findViewById(R.id.imgFotoPrevia);
        tvEstadoFoto     = findViewById(R.id.tvEstadoFoto);
        btnEnviarReporte = findViewById(R.id.btnEnviarReporte);
        progressBar      = findViewById(R.id.progressBar);
        Button btnTomarFoto       = findViewById(R.id.btnTomarFoto);
        Button btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto);
        ImageView btnPerfil       = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setSelectedItemId(R.id.nav_reportar);
        btnPerfil.setOnClickListener(v -> { });
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reportar) return true;
            if (id == R.id.nav_inicio) {
                irAlInicio();
                return true;
            }
            if (id == R.id.nav_mis_reportes) {
                Intent intent = new Intent(this, MisReportesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_estadisticas) {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        btnTomarFoto.setOnClickListener(v -> verificarPermisosCamara());
        btnSeleccionarFoto.setOnClickListener(v -> verificarPermisosGaleria());
        btnEnviarReporte.setOnClickListener(v -> enviarReporte());
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            tomarFoto();
        }
    }

    private void verificarPermisosGaleria() {
        String permiso;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permiso = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permiso = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permiso)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permiso},
                    REQUEST_GALLERY_PERMISSION);
        } else {
            seleccionarFoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_CAMERA_PERMISSION)  tomarFoto();
            if (requestCode == REQUEST_GALLERY_PERMISSION) seleccionarFoto();
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }

    private void tomarFoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fotoFile = crearArchivoFoto();
            fotoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    fotoFile
            );
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            Toast.makeText(this, "Error al preparar cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String nombre = "FOTO_" + timestamp;
        File dir = getExternalCacheDir();
        return File.createTempFile(nombre, ".jpg", dir);
    }

    private void seleccionarFoto() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (fotoUri != null) {
                imgFotoPrevia.setImageURI(fotoUri);
                imgFotoPrevia.setVisibility(View.VISIBLE);
                mostrarFotoLista();
            }

        } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
            fotoUri  = data.getData();
            fotoFile = null;
            imgFotoPrevia.setImageURI(fotoUri);
            imgFotoPrevia.setVisibility(View.VISIBLE);
            mostrarFotoLista();
        }
    }

    private void mostrarFotoLista() {
        tvEstadoFoto.setText("✅ Foto lista para enviar");
        tvEstadoFoto.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    private void enviarReporte() {
        btnEnviarReporte.setEnabled(false);
        btnEnviarReporte.setText("Guardando...");
        progressBar.setVisibility(View.VISIBLE);

        guardarReporte();
    }

    private void guardarReporte() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("uid_ciudadano",   uid);
        reporte.put("id_tipo",         tipoId);
        reporte.put("tipo_reporte",    tipoNombre); // GUARDAMOS EL NOMBRE TAMBIÉN
        reporte.put("id_area",         areaNombre);
        reporte.put("asunto",          asunto);
        reporte.put("descripcion",     descripcion);
        reporte.put("estado",          "pendiente");
        reporte.put("prioridad",       null);
        reporte.put("latitud",         latitud);
        reporte.put("longitud",        longitud);
        reporte.put("direccion",       direccion != null ? direccion : "");
        reporte.put("zona",            null);
        reporte.put("fecha_reporte",   Timestamp.now());
        reporte.put("uid_funcionario", null);

        db.collection("reportes")
                .add(reporte)
                .addOnSuccessListener(docRef -> {
                    String reporteId = docRef.getId();
                    if (fotoUri != null) {
                        subirFotoAStorage(reporteId);
                    } else {
                        mostrarExito();
                    }
                })
                .addOnFailureListener(e ->
                        mostrarError("Error al guardar reporte: " + e.getMessage())
                );
    }

    private void subirFotoAStorage(String reporteId) {
        String ruta = "fotos/" + reporteId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fotoRef = storage.getReference().child(ruta);

        fotoRef.putFile(fotoUri)
                .addOnSuccessListener(snap ->
                        fotoRef.getDownloadUrl().addOnSuccessListener(uri ->
                                guardarFotoEnFirestore(reporteId, uri.toString())
                        )
                )
                .addOnFailureListener(e ->
                        mostrarError("Error al subir foto: " + e.getMessage())
                );
    }

    private void guardarFotoEnFirestore(String reporteId, String urlFoto) {
        Map<String, Object> foto = new HashMap<>();
        foto.put("id_reporte",   reporteId);
        foto.put("url",          urlFoto);
        foto.put("fecha_subida", Timestamp.now());

        db.collection("fotos")
                .add(foto)
                .addOnSuccessListener(docRef -> mostrarExito())
                .addOnFailureListener(e ->
                        mostrarError("Error al guardar foto en BD: " + e.getMessage())
                );
    }

    private void mostrarExito() {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "¡Reporte enviado exitosamente!", Toast.LENGTH_LONG).show();
        irAlInicio();
    }

    private void irAlInicio() {
        Intent intent = new Intent(this, DashboardCiudadano.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void mostrarError(String mensaje) {
        progressBar.setVisibility(View.GONE);
        btnEnviarReporte.setEnabled(true);
        btnEnviarReporte.setText("Enviar reporte");
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}