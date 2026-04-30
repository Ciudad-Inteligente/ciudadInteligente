package com.example.ciudadinteligente;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardCiudadano extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewBienvenida;
    private TextView tvSinReportes;
    private RecyclerView recyclerReportes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard_ciudadano);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewBienvenida = findViewById(R.id.textViewBienvenida);
        tvSinReportes      = findViewById(R.id.tvSinReportes);
        recyclerReportes   = findViewById(R.id.recyclerReportes);
        Button btnLogout   = findViewById(R.id.btnLogout);
        ImageView btnPerfil = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        recyclerReportes.setLayoutManager(new LinearLayoutManager(this));
        bottomNav.setSelectedItemId(R.id.nav_inicio);

        obtenerDatosUsuario();
        cargarUltimosReportes();

        btnLogout.setOnClickListener(v -> cerrarSesion());

        // AHORA EL BOTÓN DESPLIEGA UN MENÚ
        btnPerfil.setOnClickListener(v -> mostrarMenuPerfil(v));

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) return true;
            
            if (id == R.id.nav_reportar) {
                startActivity(new Intent(this, ReportarActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            } else if (id == R.id.nav_mis_reportes) {
                startActivity(new Intent(this, MisReportesActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            } else if (id == R.id.nav_estadisticas) {
                return true;
            }
            return false;
        });
    }

    private void mostrarMenuPerfil(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 1, 0, "Ver perfil");
        popup.getMenu().add(0, 2, 1, "Cerrar sesión");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    startActivity(new Intent(DashboardCiudadano.this, PerfilActivity.class));
                    return true;
                case 2:
                    cerrarSesion();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginUsuario.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void obtenerDatosUsuario() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            String primerNombre = nombre.split(" ")[0];
                            String saludo = getSaludo();
                            textViewBienvenida.setText(saludo + ", " + primerNombre + " 👋");
                        }
                    }
                });
    }

    private String getSaludo() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 6 && hora < 12)  return "Buenos días";
        if (hora >= 12 && hora < 19) return "Buenas tardes";
        return "Buenas noches";
    }

    private void cargarUltimosReportes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("reportes")
                .whereEqualTo("uid_ciudadano", uid)
                .orderBy("fecha_reporte", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        tvSinReportes.setVisibility(View.VISIBLE);
                        recyclerReportes.setVisibility(View.GONE);
                        return;
                    }

                    List<ReporteItem> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String asunto = doc.getString("asunto");
                        String estado = doc.getString("estado");
                        String tipo   = doc.getString("tipo_reporte");
                        com.google.firebase.Timestamp fecha = doc.getTimestamp("fecha_reporte");

                        String fechaTexto = "";
                        if (fecha != null) {
                            fechaTexto = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "CO"))
                                    .format(fecha.toDate());
                        }

                        lista.add(new ReporteItem(
                                asunto  != null ? asunto : "Sin título",
                                estado  != null ? estado : "pendiente",
                                tipo    != null ? tipo   : "",
                                fechaTexto
                        ));
                    }

                    tvSinReportes.setVisibility(View.GONE);
                    recyclerReportes.setVisibility(View.VISIBLE);
                    recyclerReportes.setAdapter(new ReportesAdapter(lista));
                })
                .addOnFailureListener(e -> {
                    tvSinReportes.setVisibility(View.VISIBLE);
                    tvSinReportes.setText("No se pudieron cargar los reportes.");
                    recyclerReportes.setVisibility(View.GONE);
                });
    }

    static class ReporteItem {
        String asunto, estado, tipo, fecha;
        ReporteItem(String asunto, String estado, String tipo, String fecha) {
            this.asunto = asunto; this.estado = estado; this.tipo = tipo; this.fecha = fecha;
        }
    }

    static class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {
        private final List<ReporteItem> lista;
        ReportesAdapter(List<ReporteItem> lista) { this.lista = lista; }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAsunto, tvTipo, tvFecha, tvEstado;
            ViewHolder(View v) {
                super(v);
                tvAsunto = v.findViewById(R.id.tvItemAsunto);
                tvTipo   = v.findViewById(R.id.tvItemTipo);
                tvFecha  = v.findViewById(R.id.tvItemFecha);
                tvEstado = v.findViewById(R.id.tvItemEstado);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reporte, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ReporteItem item = lista.get(position);
            holder.tvAsunto.setText(item.asunto);
            holder.tvTipo.setText(item.tipo);
            holder.tvFecha.setText(item.fecha);
            holder.tvEstado.setText(item.estado);
            aplicarColorEstado(holder.tvEstado, item.estado);
        }

        @Override
        public int getItemCount() { return lista.size(); }

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