package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardCiudadano extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewBienvenida;

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

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vincular elementos de UI
        textViewBienvenida = findViewById(R.id.textViewBienvenida);
        Button btnLogout = findViewById(R.id.btnLogout);
        ImageView btnPerfil = findViewById(R.id.btnPerfil);
        RecyclerView recyclerReportes = findViewById(R.id.recyclerReportes);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Obtener información del usuario
        obtenerDatosUsuario();
        cargarUltimosReportes();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(DashboardCiudadano.this, LoginUsuario.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnPerfil.setOnClickListener(v -> {
            // TODO: Navegar a la pantalla de perfil
            // startActivity(new Intent(DashboardCiudadano.this, PerfilActivity.class));
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                // Ya estamos en inicio
                return true;
            } else if (id == R.id.nav_reportar) {
                // TODO: Navegar a pantalla de reportar
                return true;
            } else if (id == R.id.nav_mis_reportes) {
                // TODO: Navegar a pantalla de mis reportes
                return true;
            }
            return id == R.id.nav_estadisticas; // TODO: Navegar a pantalla de estadísticas
        });
    }

    private void obtenerDatosUsuario() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        if (nombre != null) {
                            String saludo = getSaludo();
                            textViewBienvenida.setText(String.format("%s, %s", saludo, nombre));
                        }
                    }
                });
    }

    private String getSaludo() {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hora >= 6 && hora < 12) return "Buenos días";
        else if (hora >= 12 && hora < 19) return "Buenas tardes";
        else return "Buenas noches";
    }

    private void cargarUltimosReportes() {
        // TODO: Implementar la consulta a Firestore para obtener los 2 reportes más recientes del usuario
        // y mostrarlos en el RecyclerView usando un adaptador personalizado.
    }

}
