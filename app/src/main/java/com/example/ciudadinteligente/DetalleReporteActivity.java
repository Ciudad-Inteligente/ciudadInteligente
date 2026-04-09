package com.example.ciudadinteligente;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetalleReporteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String reporteId;

    private TextView tvAsunto, tvEstado, tvArea, tvTipo, tvFecha, tvDireccion, tvDescripcion, tvSinHistorial;
    private ImageView ivFoto;
    private RecyclerView rvHistorial;
    private HistorialAdapter adapter;
    private Button btnEliminar, btnEditar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_reporte);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        reporteId = getIntent().getStringExtra("REPORTE_ID");

        // Vincular vistas
        tvAsunto = findViewById(R.id.tvDetalleAsunto);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvArea = findViewById(R.id.tvDetalleArea);
        tvTipo = findViewById(R.id.tvDetalleTipo);
        tvFecha = findViewById(R.id.tvDetalleFecha);
        tvDireccion = findViewById(R.id.tvDetalleDireccion);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvSinHistorial = findViewById(R.id.tvSinHistorial);
        ivFoto = findViewById(R.id.ivDetalleFoto);
        rvHistorial = findViewById(R.id.rvHistorial);
        btnEliminar = findViewById(R.id.btnEliminarReporte);
        btnEditar = findViewById(R.id.btnEditarReporte);

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistorialAdapter(new ArrayList<>());
        rvHistorial.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_mis_reportes);

        // Configurar botones
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
        
        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleReporteActivity.this, EditarReporteActivity.class);
            intent.putExtra("REPORTE_ID", reporteId);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        if (reporteId != null) {
            cargarDatosReporte();
            cargarHistorial();
        }
    }

    private void cargarDatosReporte() {
        db.collection("reportes").document(reporteId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvAsunto.setText(doc.getString("asunto"));
                        String estado = doc.getString("estado");
                        tvEstado.setText(estado != null ? estado.toUpperCase() : "PENDIENTE");
                        aplicarColorEstado(tvEstado, estado);

                        tvArea.setText(doc.getString("id_area"));
                        String tipo = doc.getString("tipo_reporte");
                        if (tipo == null) tipo = doc.getString("id_tipo");
                        tvTipo.setText(tipo);

                        tvDireccion.setText(doc.getString("direccion"));
                        tvDescripcion.setText(doc.getString("descripcion"));

                        com.google.firebase.Timestamp ts = doc.getTimestamp("fecha_reporte");
                        if (ts != null) {
                            tvFecha.setText(new SimpleDateFormat("dd 'de' MMMM yyyy", new Locale("es", "CO")).format(ts.toDate()));
                        }
                    }
                });
    }

    private void cargarHistorial() {
        db.collection("historial_estado")
                .whereEqualTo("id_reporte", reporteId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        tvSinHistorial.setVisibility(View.VISIBLE);
                        return;
                    }
                    List<HistorialItem> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        lista.add(new HistorialItem(
                                doc.getString("estado_nuevo"),
                                doc.getString("mensaje"),
                                doc.getTimestamp("fecha")
                        ));
                    }
                    adapter.updateList(lista);
                    tvSinHistorial.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> tvSinHistorial.setVisibility(View.VISIBLE));
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar reporte")
                .setMessage("¿Estás seguro de que deseas eliminar este reporte? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarReporteLogico())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarReporteLogico() {
        db.collection("reportes").document(reporteId)
                .update("visible", false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Reporte eliminado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar el reporte", Toast.LENGTH_SHORT).show();
                });
    }

    private void aplicarColorEstado(TextView badge, String estado) {
        String fondo, texto;
        if (estado == null) estado = "pendiente";
        switch (estado) {
            case "resuelto":    fondo="#E8F5E9"; texto="#2E7D32"; break;
            case "en_proceso":  fondo="#E3F2FD"; texto="#1565C0"; break;
            case "en_revision": fondo="#FFF8E1"; texto="#F9A825"; break;
            case "rechazado":   fondo="#FFEBEE"; texto="#C62828"; break;
            default:            fondo="#FFF3E0"; texto="#E65100"; break;
        }
        badge.setBackgroundColor(Color.parseColor(fondo));
        badge.setTextColor(Color.parseColor(texto));
    }

    static class HistorialItem {
        String estado, mensaje;
        com.google.firebase.Timestamp fecha;
        HistorialItem(String e, String m, com.google.firebase.Timestamp f) {
            this.estado = e; this.mensaje = m; this.fecha = f;
        }
    }

    static class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private List<HistorialItem> lista;
        HistorialAdapter(List<HistorialItem> l) { this.lista = l; }
        void updateList(List<HistorialItem> n) { this.lista = n; notifyDataSetChanged(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistorialItem item = lista.get(position);
            holder.tvEstado.setText("Estado: " + (item.estado != null ? item.estado : "Actualizado"));
            holder.tvMensaje.setText(item.mensaje);
            if (item.fecha != null) {
                holder.tvFecha.setText(new SimpleDateFormat("dd MMM yyyy · HH:mm", new Locale("es", "CO")).format(item.fecha.toDate()));
            }
        }

        @Override
        public int getItemCount() { return lista.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEstado, tvFecha, tvMensaje;
            ViewHolder(View v) {
                super(v);
                tvEstado = v.findViewById(R.id.tvHistorialEstado);
                tvFecha = v.findViewById(R.id.tvHistorialFecha);
                tvMensaje = v.findViewById(R.id.tvHistorialMensaje);
            }
        }
    }
}