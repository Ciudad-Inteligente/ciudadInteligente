package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class UbicacionReporteActivity extends AppCompatActivity {

    private String areaNombre, tipoId, tipoNombre, asunto, descripcion;

    private double latitud  = 3.9009;
    private double longitud = -76.2975;
    private boolean ubicacionSeleccionada = false;

    private MapView mapView;
    private PointAnnotationManager pointAnnotationManager;

    // CORRECCIÓN: declarar etDireccion como campo de la clase
    // antes estaba declarada dentro del método onCreate como variable local,
    // por eso no se podía acceder desde el listener del botón
    private EditText etDireccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ubicacion_reporte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        areaNombre  = getIntent().getStringExtra("AREA_SELECCIONADA");
        tipoId      = getIntent().getStringExtra("TIPO_ID");
        tipoNombre  = getIntent().getStringExtra("TIPO_NOMBRE");
        asunto      = getIntent().getStringExtra("ASUNTO");
        descripcion = getIntent().getStringExtra("DESCRIPCION");

        // Vincular vistas
        TextView tvTitulo      = findViewById(R.id.tvTitulo);
        TextView tvInstruccion = findViewById(R.id.tvInstruccion);
        TextView tvCoordenadas = findViewById(R.id.tvCoordenadas);
        Button btnSiguiente    = findViewById(R.id.btnSiguiente);
        ImageView btnPerfil    = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // CORRECCIÓN: vincular etDireccion al campo de la clase
        etDireccion = findViewById(R.id.etDireccion);

        tvTitulo.setText("2. Ubicación");
        tvInstruccion.setText("Toca en el mapa para marcar la ubicación del problema");

        bottomNav.setSelectedItemId(R.id.nav_reportar);
        btnPerfil.setOnClickListener(v -> { });
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reportar) return true;
            if (id == R.id.nav_inicio) {
                Intent i = new Intent(this, DashboardCiudadano.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
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

        // Mapbox
        mapView = findViewById(R.id.mapView);
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {

            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(longitud, latitud))
                            .zoom(14.0)
                            .build()
            );

            AnnotationPlugin annotationPlugin =
                    AnnotationPluginImplKt.getAnnotations(mapView);
            pointAnnotationManager =
                    PointAnnotationManagerKt.createPointAnnotationManager(
                            annotationPlugin, mapView
                    );

            GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
                latitud  = point.latitude();
                longitud = point.longitude();
                ubicacionSeleccionada = true;

                tvCoordenadas.setText(
                        String.format("📍 %.5f, %.5f", latitud, longitud)
                );

                colocarPin(point);
                return true;
            });
        });

        btnSiguiente.setOnClickListener(v -> {
            // CORRECCIÓN: leer el texto del EditText aquí,
            // justo antes de enviarlo — así siempre tiene el valor actual
            String direccion = etDireccion.getText().toString().trim();

            Intent intent = new Intent(this, AdjuntarFotoReporteActivity.class);
            intent.putExtra("AREA_SELECCIONADA", areaNombre);
            intent.putExtra("TIPO_ID",           tipoId);
            intent.putExtra("TIPO_NOMBRE",       tipoNombre);
            intent.putExtra("ASUNTO",            asunto);
            intent.putExtra("DESCRIPCION",       descripcion);
            intent.putExtra("LATITUD",           latitud);
            intent.putExtra("LONGITUD",          longitud);
            intent.putExtra("DIRECCION",         direccion); // ← ya funciona
            startActivity(intent);
        });
    }

    private void colocarPin(Point punto) {
        if (pointAnnotationManager == null) return;

        pointAnnotationManager.deleteAll();

        Bitmap bitmap = drawableToBitmap(R.drawable.ic_pin_mapa);
        if (bitmap == null) return;

        PointAnnotationOptions opciones = new PointAnnotationOptions()
                .withPoint(punto)
                .withIconImage(bitmap)
                .withIconSize(1.5);

        pointAnnotationManager.create(opciones);
    }

    private Bitmap drawableToBitmap(int drawableRes) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override protected void onStart()   { super.onStart();   mapView.onStart(); }
    @Override protected void onStop()    { super.onStop();    mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}