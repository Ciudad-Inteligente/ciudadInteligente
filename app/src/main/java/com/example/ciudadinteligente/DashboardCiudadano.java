package com.example.ciudadinteligente;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vincular vistas
        textViewBienvenida = findViewById(R.id.textViewBienvenida);
        tvSinReportes      = findViewById(R.id.tvSinReportes);
        recyclerReportes   = findViewById(R.id.recyclerReportes);
        Button btnLogout   = findViewById(R.id.btnLogout);
        ImageView btnPerfil = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // CORRECCIÓN: sin LayoutManager el RecyclerView crashea
        recyclerReportes.setLayoutManager(new LinearLayoutManager(this));

        // Marcar qué tab está activo visualmente
        bottomNav.setSelectedItemId(R.id.nav_inicio);

        // Cargar datos de Firebase
        obtenerDatosUsuario();
        cargarUltimosReportes();

        // Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginUsuario.class);
            // Limpia toda la pila de actividades — el usuario no puede
            // volver atrás con el botón físico después de hacer logout
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Perfil (pendiente)
        btnPerfil.setOnClickListener(v -> {
            // startActivity(new Intent(this, PerfilActivity.class));
        });

        // Navegación inferior
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                // Ya estamos aquí, no hacer nada
                return true;
            } else if (id == R.id.nav_reportar) {
                // CORRECCIÓN: FLAG_ACTIVITY_SINGLE_TOP evita crear
                // una nueva instancia si ya existe una en la pila
                Intent intent = new Intent(this, ReportarActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_mis_reportes) {
                // TODO: MisReportesActivity
                return true;
            } else if (id == R.id.nav_estadisticas) {
                // TODO: EstadisticasActivity
                return true;
            }
            return false;
        });
    }

    private void obtenerDatosUsuario() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            // Muestra solo el primer nombre
                            String primerNombre = nombre.split(" ")[0];
                            String saludo = getSaludo();
                            textViewBienvenida.setText(saludo + ", " + primerNombre + " 👋");
                        }
                    }
                });
    }

    // Saludo según la hora del día
    private String getSaludo() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 6 && hora < 12)  return "Buenos días";
        if (hora >= 12 && hora < 19) return "Buenas tardes";
        return "Buenas noches";
    }

    private void cargarUltimosReportes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Trae los 3 reportes más recientes del usuario
        // NOTA: la primera vez que ejecute esto, Firestore pedirá
        // crear un índice. Aparece un link en el Logcat — solo haz clic
        db.collection("reportes")
                .whereEqualTo("uid_ciudadano", uid)
                .orderBy("fecha_reporte", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        // No tiene reportes — mostrar mensaje vacío
                        tvSinReportes.setVisibility(View.VISIBLE);
                        recyclerReportes.setVisibility(View.GONE);
                        return;
                    }

                    // Construir lista de mapas simples para el adaptador
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

                    // Conectar lista al RecyclerView mediante el adaptador
                    tvSinReportes.setVisibility(View.GONE);
                    recyclerReportes.setVisibility(View.VISIBLE);
                    recyclerReportes.setAdapter(new ReportesAdapter(lista));
                })
                .addOnFailureListener(e -> {
                    // Si falla (ej: sin internet), mostrar mensaje vacío
                    tvSinReportes.setVisibility(View.VISIBLE);
                    tvSinReportes.setText("No se pudieron cargar los reportes.");
                    recyclerReportes.setVisibility(View.GONE);
                });
    }

    // ── Clase interna: modelo simple para cada fila del RecyclerView ──
    // En un proyecto más grande iría en su propio archivo, pero aquí
    // es más fácil tenerla junto al adaptador
    static class ReporteItem {
        String asunto, estado, tipo, fecha;
        ReporteItem(String asunto, String estado, String tipo, String fecha) {
            this.asunto = asunto;
            this.estado = estado;
            this.tipo   = tipo;
            this.fecha  = fecha;
        }
    }

    // ── Clase interna: adaptador del RecyclerView ──
    // El adaptador es el "puente" entre tu lista de datos y las filas
    // visuales. Android llama a sus métodos automáticamente.
    static class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

        private final List<ReporteItem> lista;

        ReportesAdapter(List<ReporteItem> lista) {
            this.lista = lista;
        }

        // ViewHolder guarda referencias a las vistas de UNA fila
        // para no tener que buscarlas cada vez (mejora rendimiento)
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

        // Android llama esto para crear la vista de una fila nueva
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reporte, parent, false);
            return new ViewHolder(v);
        }

        // Android llama esto para llenar los datos en cada fila
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ReporteItem item = lista.get(position);
            holder.tvAsunto.setText(item.asunto);
            holder.tvTipo.setText(item.tipo);
            holder.tvFecha.setText(item.fecha);
            holder.tvEstado.setText(formatearEstado(item.estado));
            aplicarColorEstado(holder.tvEstado, item.estado);
        }

        @Override
        public int getItemCount() { return lista.size(); }

        private String formatearEstado(String e) {
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