package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TipoReporteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerTipos;
    private ProgressBar progressBar;
    private TextView tvSinTipos;
    private String areaNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tipo_reporte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Recibir área de la pantalla anterior
        areaNombre = getIntent().getStringExtra("AREA_SELECCIONADA");
        if (areaNombre == null) areaNombre = "";

        // Vincular vistas
        TextView tvNombreArea      = findViewById(R.id.tvNombreArea);
        TextView tvDescripcionArea = findViewById(R.id.tvDescripcionArea);
        recyclerTipos              = findViewById(R.id.recyclerTipos);
        progressBar                = findViewById(R.id.progressBar);
        tvSinTipos                 = findViewById(R.id.tvSinTipos);
        ImageView btnPerfil        = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        tvNombreArea.setText(areaNombre);
        tvDescripcionArea.setText(obtenerDescripcionArea(areaNombre));

        // 2 columnas igual al prototipo
        recyclerTipos.setLayoutManager(new GridLayoutManager(this, 2));

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
                Intent i = new Intent(this, MisReportesActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            }
            if (id == R.id.nav_estadisticas) {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        cargarTiposDeReporte();
    }

    private void cargarTiposDeReporte() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerTipos.setVisibility(View.GONE);
        tvSinTipos.setVisibility(View.GONE);

        db.collection("tipos_reporte")
                .whereEqualTo("area", areaNombre)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (querySnapshot.isEmpty()) {
                        tvSinTipos.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<TipoReporte> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        lista.add(new TipoReporte(
                                doc.getId(),
                                doc.getString("nombre") != null ? doc.getString("nombre") : "Sin nombre",
                                doc.getString("icono")  != null ? doc.getString("icono")  : "ic_default",
                                areaNombre
                        ));
                    }

                    recyclerTipos.setVisibility(View.VISIBLE);
                    recyclerTipos.setAdapter(new TiposAdapter(lista, tipo -> {
                        Intent intent = new Intent(this, CrearReporteActivity.class);
                        intent.putExtra("AREA_SELECCIONADA", areaNombre);
                        intent.putExtra("TIPO_ID",           tipo.id);
                        intent.putExtra("TIPO_NOMBRE",       tipo.nombre);
                        startActivity(intent);
                    }));
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvSinTipos.setVisibility(View.VISIBLE);
                    tvSinTipos.setText("Error al cargar. Revisa tu conexión.");
                });
    }

    private String obtenerDescripcionArea(String area) {
        switch (area) {
            case "Infraestructura vial y espacio público":
                return "Vías, andenes, puentes, parques y señalización.";
            case "Alumbrado público":
                return "Alumbrado de calles, parques y espacios públicos.";
            case "Servicios públicos domiciliarios":
                return "Agua, alcantarillado, energía y gas en hogares.";
            case "Medio ambiente y aseo urbano":
                return "Entorno natural y manejo de residuos en la ciudad.";
            case "Seguridad ciudadana":
                return "Riesgo, delitos y convivencia en el espacio público.";
            case "Salud pública":
                return "Riesgos sanitarios, plagas y salud colectiva.";
            case "Tránsito y movilidad":
                return "Flujo vehicular, señalización y seguridad vial.";
            case "Gobierno y atención ciudadana":
                return "Obras, edificios públicos y servicio de funcionarios.";
            default: return "";
        }
    }

    // ── Modelo ──
    static class TipoReporte {
        String id, nombre, icono, area;
        TipoReporte(String id, String nombre, String icono, String area) {
            this.id = id; this.nombre = nombre;
            this.icono = icono; this.area = area;
        }
    }

    // ── Interfaz de clic ──
    interface OnTipoClickListener {
        void onClick(TipoReporte tipo);
    }

    // ── Adaptador ──
    class TiposAdapter extends RecyclerView.Adapter<TiposAdapter.ViewHolder> {

        private final List<TipoReporte> lista;
        private final OnTipoClickListener listener;

        TiposAdapter(List<TipoReporte> lista, OnTipoClickListener listener) {
            this.lista    = lista;
            this.listener = listener;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIcono;
            TextView tvNombre;
            ViewHolder(View v) {
                super(v);
                imgIcono = v.findViewById(R.id.imgIcono);
                tvNombre = v.findViewById(R.id.tvNombreTipo);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tipo_reporte, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TipoReporte tipo = lista.get(position);
            holder.tvNombre.setText(tipo.nombre);

            /*
             * Aquí está la clave de la imagen local:
             * getIdentifier() busca en R.drawable el recurso
             * cuyo nombre coincida con tipo.icono (el string de Firestore).
             * Si no lo encuentra devuelve 0, y usamos ic_default.
             *
             * Ejemplo: tipo.icono = "ic_semaforo"
             * → busca R.drawable.ic_semaforo
             * → lo carga en el ImageView
             */
            int iconoId = getResources().getIdentifier(
                    tipo.icono,          // nombre del drawable sin extensión
                    "drawable",          // carpeta donde buscar
                    getPackageName()     // paquete de la app
            );
            holder.imgIcono.setImageResource(
                    iconoId != 0 ? iconoId : R.drawable.ic_default
            );

            holder.itemView.setOnClickListener(v -> listener.onClick(tipo));
        }

        @Override
        public int getItemCount() { return lista.size(); }
    }
}