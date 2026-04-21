package com.example.ciudadinteligente;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ciudadinteligente.api.ReporteDTO;
import com.example.ciudadinteligente.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MisReportesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private RecyclerView recycler;
    private ProgressBar progressBar;
    private LinearLayout layoutVacio;
    private TextView tvMensajeVacio;

    private List<ReporteDTO> listaCompleta = new ArrayList<>();
    private MisReportesAdapter adaptador;

    private String filtroActivo = "todos";
    private TextView filtroTodos, filtroPendiente, filtroEnRevision,
            filtroEnProceso, filtroResuelto, filtroRechazado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_reportes);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        recycler       = findViewById(R.id.recyclerMisReportes);
        progressBar    = findViewById(R.id.progressBar);
        layoutVacio    = findViewById(R.id.layoutVacio);
        tvMensajeVacio = findViewById(R.id.tvMensajeVacio);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        filtroTodos      = findViewById(R.id.filtroTodos);
        filtroPendiente  = findViewById(R.id.filtroPendiente);
        filtroEnRevision = findViewById(R.id.filtroEnRevision);
        filtroEnProceso  = findViewById(R.id.filtroEnProceso);
        filtroResuelto   = findViewById(R.id.filtroResuelto);
        filtroRechazado  = findViewById(R.id.filtroRechazado);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MisReportesAdapter(new ArrayList<>(), reporteId -> {
            Intent intent = new Intent(this, DetalleReporteActivity.class);
            intent.putExtra("REPORTE_ID", reporteId);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        recycler.setAdapter(adaptador);

        bottomNav.setSelectedItemId(R.id.nav_mis_reportes);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_mis_reportes) return true;

            Intent intent = null;
            if (id == R.id.nav_inicio) {
                intent = new Intent(this, DashboardCiudadano.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else if (id == R.id.nav_reportar) {
                intent = new Intent(this, ReportarActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        configurarFiltros();
        cargarMisReportes();
    }

    private void configurarFiltros() {
        filtroTodos.setOnClickListener(v      -> aplicarFiltro("todos",       filtroTodos));
        filtroPendiente.setOnClickListener(v  -> aplicarFiltro("pendiente",   filtroPendiente));
        filtroEnRevision.setOnClickListener(v -> aplicarFiltro("en_revision", filtroEnRevision));
        filtroEnProceso.setOnClickListener(v  -> aplicarFiltro("en_proceso",  filtroEnProceso));
        filtroResuelto.setOnClickListener(v   -> aplicarFiltro("resuelto",    filtroResuelto));
        filtroRechazado.setOnClickListener(v  -> aplicarFiltro("rechazado",   filtroRechazado));
    }

    private void aplicarFiltro(String estado, TextView chipSeleccionado) {
        filtroActivo = estado;
        TextView[] chips = {filtroTodos, filtroPendiente, filtroEnRevision,
                filtroEnProceso, filtroResuelto, filtroRechazado};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.chip_gris);
            chip.setTextColor(Color.parseColor("#555555"));
        }
        chipSeleccionado.setBackgroundResource(R.drawable.chip_naranja);
        chipSeleccionado.setTextColor(Color.parseColor("#E07A1B"));

        List<ReporteDTO> filtrada = new ArrayList<>();
        for (ReporteDTO item : listaCompleta) {
            if (estado.equals("todos") || estado.equals(item.estado)) {
                filtrada.add(item);
            }
        }
        adaptador.actualizarLista(filtrada);

        if (filtrada.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            tvMensajeVacio.setText(estado.equals("todos")
                    ? "No tienes reportes aún"
                    : "No tienes reportes con estado \"" + chipSeleccionado.getText() + "\"");
        } else {
            layoutVacio.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    private void cargarMisReportes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getApi().misReportes(uid)
                .enqueue(new Callback<List<ReporteDTO>>() {
                    @Override
                    public void onResponse(Call<List<ReporteDTO>> call,
                                           Response<List<ReporteDTO>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            listaCompleta = response.body();
                            aplicarFiltro(filtroActivo,
                                    obtenerChipPorEstado(filtroActivo));
                        } else {
                            mostrarVacio("Error al cargar reportes");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ReporteDTO>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        mostrarVacio("Sin conexión: " + t.getMessage());
                    }
                });
    }

    private void mostrarVacio(String mensaje) {
        layoutVacio.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        tvMensajeVacio.setText(mensaje);
    }

    private TextView obtenerChipPorEstado(String estado) {
        switch (estado) {
            case "pendiente":   return filtroPendiente;
            case "en_revision": return filtroEnRevision;
            case "en_proceso":  return filtroEnProceso;
            case "resuelto":    return filtroResuelto;
            case "rechazado":   return filtroRechazado;
            default:            return filtroTodos;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    static class MisReportesAdapter
            extends RecyclerView.Adapter<MisReportesAdapter.ViewHolder> {

        private List<ReporteDTO> lista;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(Long reporteId);
        }

        MisReportesAdapter(List<ReporteDTO> l, OnItemClickListener listener) {
            this.lista    = l;
            this.listener = listener;
        }

        void actualizarLista(List<ReporteDTO> n) {
            this.lista = n;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAsunto, tvEstado, tvTipo, tvArea, tvFecha, tvDireccion;
            ViewHolder(View v) {
                super(v);
                tvAsunto    = v.findViewById(R.id.tvAsunto);
                tvEstado    = v.findViewById(R.id.tvEstado);
                tvTipo      = v.findViewById(R.id.tvTipo);
                tvArea      = v.findViewById(R.id.tvArea);
                tvFecha     = v.findViewById(R.id.tvFecha);
                tvDireccion = v.findViewById(R.id.tvDireccion);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_mis_reportes, p, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int p) {
            ReporteDTO r = lista.get(p);
            h.tvAsunto.setText(r.asunto);
            h.tvTipo.setText(r.tipoReporte != null ? r.tipoReporte.nombre : "");
            h.tvArea.setText(r.tipoReporte != null && r.tipoReporte.area != null
                    ? r.tipoReporte.area.nombre : "");
            h.tvFecha.setText(r.fechaReporte != null
                    ? r.fechaReporte.substring(0, 10) : "");
            h.tvDireccion.setText(r.direccion == null || r.direccion.isEmpty()
                    ? "📍 Ubicación en mapa" : "📍 " + r.direccion);
            h.tvEstado.setText(formatearEstado(r.estado));
            aplicarColorEstado(h.tvEstado, r.estado);
            h.itemView.setOnClickListener(v -> listener.onItemClick(r.id));
        }

        @Override
        public int getItemCount() { return lista.size(); }

        private String formatearEstado(String e) {
            if (e == null) return "Pendiente";
            switch (e) {
                case "pendiente":   return "Pendiente";
                case "en_revision": return "En revisión";
                case "en_proceso":  return "En proceso";
                case "resuelto":    return "Resuelto";
                case "rechazado":   return "Rechazado";
                default:            return e;
            }
        }

        private void aplicarColorEstado(TextView badge, String estado) {
            String fondo, texto;
            if (estado == null) estado = "pendiente";
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
    }
}