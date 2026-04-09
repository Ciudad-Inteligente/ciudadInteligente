package com.example.ciudadinteligente;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MisReportesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private RecyclerView recycler;
    private ProgressBar progressBar;
    private LinearLayout layoutVacio;
    private TextView tvMensajeVacio;

    private List<ReporteItem> listaCompleta = new ArrayList<>();
    private MisReportesAdapter adaptador;
    
    private Map<String, String> mapaNombresTipos = new HashMap<>();

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
        db    = FirebaseFirestore.getInstance();

        recycler        = findViewById(R.id.recyclerMisReportes);
        progressBar     = findViewById(R.id.progressBar);
        layoutVacio     = findViewById(R.id.layoutVacio);
        tvMensajeVacio  = findViewById(R.id.tvMensajeVacio);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        filtroTodos       = findViewById(R.id.filtroTodos);
        filtroPendiente   = findViewById(R.id.filtroPendiente);
        filtroEnRevision  = findViewById(R.id.filtroEnRevision);
        filtroEnProceso   = findViewById(R.id.filtroEnProceso);
        filtroResuelto    = findViewById(R.id.filtroResuelto);
        filtroRechazado   = findViewById(R.id.filtroRechazado);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        
        // CORRECCIÓN: Pasar el listener al adaptador
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
        cargarDiccionarioYReportes();
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
        TextView[] chips = {filtroTodos, filtroPendiente, filtroEnRevision, filtroEnProceso, filtroResuelto, filtroRechazado};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.chip_gris);
            chip.setTextColor(Color.parseColor("#555555"));
        }
        chipSeleccionado.setBackgroundResource(R.drawable.chip_naranja);
        chipSeleccionado.setTextColor(Color.parseColor("#E07A1B"));

        List<ReporteItem> filtrada = new ArrayList<>();
        for (ReporteItem item : listaCompleta) {
            if (estado.equals("todos") || estado.equals(item.estado)) {
                filtrada.add(item);
            }
        }
        adaptador.actualizarLista(filtrada);
        if (filtrada.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            tvMensajeVacio.setText(estado.equals("todos") ? "No tienes reportes aún" : "No tienes reportes con estado \"" + chipSeleccionado.getText() + "\"");
        } else {
            layoutVacio.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    private void cargarDiccionarioYReportes() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tipos_reporte").get().addOnSuccessListener(querySnapshot -> {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                mapaNombresTipos.put(doc.getId(), doc.getString("nombre"));
            }
            cargarMisReportes();
        }).addOnFailureListener(e -> cargarMisReportes());
    }

    private void cargarMisReportes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("reportes")
                .whereEqualTo("uid_ciudadano", uid)
                .orderBy("fecha_reporte", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::procesarResultados)
                .addOnFailureListener(e -> {
                    db.collection("reportes").whereEqualTo("uid_ciudadano", uid).get()
                            .addOnSuccessListener(this::procesarResultados)
                            .addOnFailureListener(this::mostrarErrorFinal);
                });
    }

    private void procesarResultados(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        progressBar.setVisibility(View.GONE);
        listaCompleta.clear();
        if (querySnapshot.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            return;
        }
        for (QueryDocumentSnapshot doc : querySnapshot) {
            String fechaTexto = "";
            com.google.firebase.Timestamp ts = doc.getTimestamp("fecha_reporte");
            if (ts != null) {
                fechaTexto = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "CO")).format(ts.toDate());
            }
            String idTipo = doc.getString("id_tipo");
            String nombreTipo = doc.getString("tipo_reporte");
            if (nombreTipo == null || nombreTipo.isEmpty()) {
                nombreTipo = mapaNombresTipos.getOrDefault(idTipo, "Reporte");
            }
            listaCompleta.add(new ReporteItem(
                    doc.getId(),
                    doc.getString("asunto"),
                    doc.getString("estado"),
                    nombreTipo,
                    doc.getString("id_area"),
                    doc.getString("direccion"),
                    fechaTexto
            ));
        }
        aplicarFiltro(filtroActivo, obtenerChipPorEstado(filtroActivo));
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

    private void mostrarErrorFinal(Exception e) {
        progressBar.setVisibility(View.GONE);
        layoutVacio.setVisibility(View.VISIBLE);
        tvMensajeVacio.setText("Error al cargar: " + e.getLocalizedMessage());
    }

    static class ReporteItem {
        String id, asunto, estado, tipo, area, direccion, fecha;
        ReporteItem(String id, String as, String es, String ti, String ar, String dir, String fe) {
            this.id=id; this.asunto=as; this.estado=es; this.tipo=ti; this.area=ar; this.direccion=dir; this.fecha=fe;
        }
    }

    // --- Adaptador con soporte para clic ---
    static class MisReportesAdapter extends RecyclerView.Adapter<MisReportesAdapter.ViewHolder> {
        private List<ReporteItem> lista;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(String reporteId);
        }

        MisReportesAdapter(List<ReporteItem> l, OnItemClickListener listener) { 
            this.lista = l; 
            this.listener = listener;
        }
        
        void actualizarLista(List<ReporteItem> n) { this.lista = n; notifyDataSetChanged(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAsunto, tvEstado, tvTipo, tvArea, tvFecha, tvDireccion;
            ViewHolder(View v) {
                super(v);
                tvAsunto=v.findViewById(R.id.tvAsunto); tvEstado=v.findViewById(R.id.tvEstado);
                tvTipo=v.findViewById(R.id.tvTipo); tvArea=v.findViewById(R.id.tvArea);
                tvFecha=v.findViewById(R.id.tvFecha); tvDireccion=v.findViewById(R.id.tvDireccion);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_mis_reportes, p, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int p) {
            ReporteItem i = lista.get(p);
            h.tvAsunto.setText(i.asunto);
            h.tvTipo.setText(i.tipo);
            h.tvArea.setText(i.area);
            h.tvFecha.setText(i.fecha);
            h.tvDireccion.setText(i.direccion == null || i.direccion.isEmpty() ? "📍 Ubicación en mapa" : "📍 " + i.direccion);
            
            h.tvEstado.setText(formatearEstado(i.estado));
            aplicarColorEstado(h.tvEstado, i.estado);

            // CORRECCIÓN: Al hacer clic, abrir detalle
            h.itemView.setOnClickListener(v -> listener.onItemClick(i.id));
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
                case "resuelto":    fondo="#E8F5E9"; texto="#2E7D32"; break;
                case "en_proceso":  fondo="#E3F2FD"; texto="#1565C0"; break;
                case "en_revision": fondo="#FFF8E1"; texto="#F9A825"; break;
                case "rechazado":   fondo="#FFEBEE"; texto="#C62828"; break;
                default:            fondo="#FFF3E0"; texto="#E65100"; break;
            }
            badge.setBackgroundColor(Color.parseColor(fondo));
            badge.setTextColor(Color.parseColor(texto));
        }
    }
}