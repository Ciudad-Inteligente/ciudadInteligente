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

import com.example.ciudadinteligente.api.HistorialCambioDTO;
import com.example.ciudadinteligente.api.ReporteDTO;
import com.example.ciudadinteligente.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleReporteActivity extends AppCompatActivity {

    private long reporteId;
    private String uidCiudadano;

    private TextView tvAsunto, tvEstado, tvArea, tvTipo, tvFecha,
            tvDireccion, tvDescripcion, tvSinHistorial;
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

        reporteId = getIntent().getLongExtra("REPORTE_ID", -1);

        // Obtener UID del ciudadano desde Firebase
        uidCiudadano = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "desconocido";

        tvAsunto      = findViewById(R.id.tvDetalleAsunto);
        tvEstado      = findViewById(R.id.tvDetalleEstado);
        tvArea        = findViewById(R.id.tvDetalleArea);
        tvTipo        = findViewById(R.id.tvDetalleTipo);
        tvFecha       = findViewById(R.id.tvDetalleFecha);
        tvDireccion   = findViewById(R.id.tvDetalleDireccion);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvSinHistorial = findViewById(R.id.tvSinHistorial);
        rvHistorial   = findViewById(R.id.rvHistorial);
        btnEliminar   = findViewById(R.id.btnEliminarReporte);
        btnEditar     = findViewById(R.id.btnEditarReporte);

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistorialAdapter(new ArrayList<>());
        rvHistorial.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_mis_reportes);

        btnEliminar.setOnClickListener(v -> confirmarEliminacion());

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditarReporteActivity.class);
            intent.putExtra("REPORTE_ID", reporteId);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        if (reporteId != -1) {
            cargarDatosReporte();
            cargarHistorial();
        }
    }

    private void cargarDatosReporte() {
        RetrofitClient.getApi().detalleReporte(reporteId)
                .enqueue(new Callback<ReporteDTO>() {
                    @Override
                    public void onResponse(Call<ReporteDTO> call,
                                           Response<ReporteDTO> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ReporteDTO r = response.body();

                            tvAsunto.setText(r.asunto);
                            tvDescripcion.setText(r.descripcion);
                            tvDireccion.setText(r.direccion != null
                                    ? r.direccion : "Ubicación en mapa");

                            String estado = r.estado != null ? r.estado : "pendiente";
                            tvEstado.setText(estado.toUpperCase());
                            aplicarColorEstado(tvEstado, estado);

                            tvTipo.setText(r.tipoReporte != null
                                    ? r.tipoReporte.nombre : "");
                            tvArea.setText(r.tipoReporte != null
                                    && r.tipoReporte.area != null
                                    ? r.tipoReporte.area.nombre : "");

                            // Fecha — viene como String "2026-04-18T..."
                            if (r.fechaReporte != null && r.fechaReporte.length() >= 10) {
                                tvFecha.setText(r.fechaReporte.substring(0, 10));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ReporteDTO> call, Throwable t) {
                        Toast.makeText(DetalleReporteActivity.this,
                                "Error al cargar: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 🆕 Cargar historial de cambios desde el backend
     */
    private void cargarHistorial() {
        RetrofitClient.getApi().obtenerHistorial(reporteId, uidCiudadano)
                .enqueue(new Callback<List<HistorialCambioDTO>>() {
                    @Override
                    public void onResponse(Call<List<HistorialCambioDTO>> call,
                                           Response<List<HistorialCambioDTO>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<HistorialCambioDTO> historialCompleto = response.body();

                            // Filtrar solo items visibles para el ciudadano
                            List<HistorialCambioDTO> historialVisible = new ArrayList<>();
                            for (HistorialCambioDTO item : historialCompleto) {
                                if (item.visibleCiudadano != null && item.visibleCiudadano) {
                                    historialVisible.add(item);
                                }
                            }

                            if (historialVisible.isEmpty()) {
                                tvSinHistorial.setVisibility(View.VISIBLE);
                                rvHistorial.setVisibility(View.GONE);
                            } else {
                                tvSinHistorial.setVisibility(View.GONE);
                                rvHistorial.setVisibility(View.VISIBLE);
                                adapter.updateList(historialVisible);
                            }
                        } else {
                            tvSinHistorial.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<HistorialCambioDTO>> call, Throwable t) {
                        // No mostramos error, solo el mensaje vacío
                        tvSinHistorial.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar reporte")
                .setMessage("¿Estás seguro de que deseas eliminar este reporte?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarReporte())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarReporte() {
        RetrofitClient.getApi().eliminarReporte(reporteId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(DetalleReporteActivity.this,
                                    "Reporte eliminado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(DetalleReporteActivity.this,
                                    "Error al eliminar",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(DetalleReporteActivity.this,
                                "Sin conexión: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void aplicarColorEstado(TextView badge, String estado) {
        String fondo, texto;
        switch (estado) {
            case "resuelto":    fondo = "#E8F5E9"; texto = "#2E7D32"; break;
            case "en_proceso":  fondo = "#E3F2FD"; texto = "#1565C0"; break;
            case "en_revision": fondo = "#FFF8E1"; texto = "#F9A825"; break;
            case "rechazado":   fondo = "#FFEBEE"; texto = "#C62828"; break;
            default:            fondo = "#FFF3E0"; texto = "#E65100"; break;
        }
        badge.setBackgroundColor(Color.parseColor(fondo));
        badge.setTextColor(Color.parseColor(texto));
    }

    // ── Adapter para mostrar historial con HistorialCambioDTO ────────────────────

    static class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private List<HistorialCambioDTO> lista;
        private static final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd MMM yyyy · HH:mm", new Locale("es", "ES"));

        HistorialAdapter(List<HistorialCambioDTO> l) {
            this.lista = l;
        }

        void updateList(List<HistorialCambioDTO> n) {
            this.lista = n;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_historial, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistorialCambioDTO item = lista.get(position);

            // Mostrar descripción o tipo
            String titulo = item.descripcion != null ? item.descripcion
                    : (item.tipo != null ? "Actualización: " + item.tipo : "Actualizado");
            holder.tvEstado.setText(titulo);

            // Mostrar comentario si existe
            if (item.comentario != null && !item.comentario.isEmpty()) {
                holder.tvMensaje.setText(item.comentario);
                holder.tvMensaje.setVisibility(View.VISIBLE);
            } else {
                holder.tvMensaje.setVisibility(View.GONE);
            }

            // Formatear fecha
            if (item.fechaCambio != null) {
                try {
                    // La fecha viene como "2024-04-29T21:00:00"
                    String fechaFormato = item.fechaCambio.replace("T", " ").substring(0, 16);
                    holder.tvFecha.setText(fechaFormato);
                } catch (Exception e) {
                    holder.tvFecha.setText(item.fechaCambio);
                }
            }
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEstado, tvFecha, tvMensaje;

            ViewHolder(View v) {
                super(v);
                tvEstado  = v.findViewById(R.id.tvHistorialEstado);
                tvFecha   = v.findViewById(R.id.tvHistorialFecha);
                tvMensaje = v.findViewById(R.id.tvHistorialMensaje);
            }
        }
    }
}
