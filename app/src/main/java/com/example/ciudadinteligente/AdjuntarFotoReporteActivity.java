package com.example.ciudadinteligente;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.core.widget.ImageViewCompat;

import com.example.ciudadinteligente.api.ReporteDTO;
import com.example.ciudadinteligente.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdjuntarFotoReporteActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE      = 1;
    private static final int REQUEST_IMAGE_PICK         = 2;
    private static final int REQUEST_CAMERA_PERMISSION  = 3;
    private static final int REQUEST_GALLERY_PERMISSION = 4;

    private String areaNombre, tipoId, tipoNombre, asunto, descripcion, direccion;
    private double latitud, longitud;

    private Uri  fotoUri  = null;
    private File fotoFile = null;

    private FirebaseAuth mAuth;

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        areaNombre  = getIntent().getStringExtra("AREA_SELECCIONADA");
        tipoId      = getIntent().getStringExtra("TIPO_ID");
        tipoNombre  = getIntent().getStringExtra("TIPO_NOMBRE");
        asunto      = getIntent().getStringExtra("ASUNTO");
        descripcion = getIntent().getStringExtra("DESCRIPCION");
        direccion   = getIntent().getStringExtra("DIRECCION");
        latitud     = getIntent().getDoubleExtra("LATITUD",   3.9009);
        longitud    = getIntent().getDoubleExtra("LONGITUD", -76.2975);

        imgFotoPrevia    = findViewById(R.id.imgFotoPrevia);
        tvEstadoFoto     = findViewById(R.id.tvEstadoFoto);
        btnEnviarReporte = findViewById(R.id.btnEnviarReporte);
        progressBar      = findViewById(R.id.progressBar);

        Button btnTomarFoto       = findViewById(R.id.btnTomarFoto);
        Button btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setSelectedItemId(R.id.nav_reportar);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reportar) return true;
            if (id == R.id.nav_inicio) { irAlInicio(); return true; }
            if (id == R.id.nav_mis_reportes) {
                startActivity(new Intent(this, MisReportesActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(0, 0);
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
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            tomarFoto();
        }
    }

    private void verificarPermisosGaleria() {
        String permiso = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permiso)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permiso}, REQUEST_GALLERY_PERMISSION);
        } else {
            seleccionarFoto();
        }
    }

    private void tomarFoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            fotoFile = crearArchivoFoto();
            fotoUri  = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", fotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            Toast.makeText(this, "Error al preparar cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile("FOTO_" + ts, ".jpg", getExternalCacheDir());
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

        ImageViewCompat.setImageTintList(imgFotoPrevia, null);
        imgFotoPrevia.setPadding(0, 0, 0, 0);
        imgFotoPrevia.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            imgFotoPrevia.setImageURI(fotoUri);
            mostrarFotoLista();
        } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
            fotoUri = data.getData();
            imgFotoPrevia.setImageURI(fotoUri);
            mostrarFotoLista();
        }
    }

    private void mostrarFotoLista() {
        tvEstadoFoto.setText("✅ Foto lista para enviar");
        tvEstadoFoto.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    private void enviarReporte() {
        btnEnviarReporte.setEnabled(false);
        btnEnviarReporte.setText("Enviando...");
        progressBar.setVisibility(View.VISIBLE);
        guardarReporte();
    }

    private void guardarReporte() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        ReporteDTO reporte = new ReporteDTO();
        reporte.uidCiudadano = uid;
        reporte.asunto       = asunto;
        reporte.descripcion  = descripcion;
        reporte.latitud      = latitud;
        reporte.longitud     = longitud;
        reporte.direccion    = direccion != null ? direccion : "";
        reporte.estado       = "pendiente";

        ReporteDTO.TipoReporteDTO tipo = new ReporteDTO.TipoReporteDTO();
        tipo.id = Long.parseLong(tipoId);
        reporte.tipoReporte = tipo;

        RetrofitClient.getApi().crearReporte(reporte)
                .enqueue(new Callback<ReporteDTO>() {
                    @Override
                    public void onResponse(Call<ReporteDTO> call,
                                           Response<ReporteDTO> response) {
                        if (response.isSuccessful()) {
                            mostrarExito("¡Reporte enviado exitosamente!");
                        } else {
                            mostrarError("Error del servidor: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ReporteDTO> call, Throwable t) {
                        mostrarError("Sin conexión: " + t.getMessage());
                    }
                });
    }

    private void mostrarExito(String mensaje) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        irAlInicio();
    }

    private void irAlInicio() {
        Intent intent = new Intent(this, DashboardCiudadano.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void mostrarError(String mensaje) {
        progressBar.setVisibility(View.GONE);
        btnEnviarReporte.setEnabled(true);
        btnEnviarReporte.setText("Enviar reporte");
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}