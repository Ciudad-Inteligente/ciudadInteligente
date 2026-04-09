package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ReportarActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reportar);
        
        // CORRECCIÓN PARA EL NAVBAR (Hueco en blanco)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_reportar);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reportar) return true;
            
            Intent intent = null;
            if (id == R.id.nav_inicio) {
                intent = new Intent(this, DashboardCiudadano.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else if (id == R.id.nav_mis_reportes) {
                intent = new Intent(this, MisReportesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else if (id == R.id.nav_estadisticas) {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0); // Quitar parpadeo
                finish();
                return true;
            }
            return false;
        });

        configurarCards();
    }

    private void configurarCards() {
        int[] cards = {
                R.id.cardInfraestructura, R.id.cardAlumbrado, R.id.cardServicios,
                R.id.cardMedioAmbiente, R.id.cardSeguridad, R.id.cardSalud,
                R.id.cardTransito, R.id.cardGobierno
        };

        String[] areas = {
                "Infraestructura vial y espacio público", "Alumbrado público",
                "Servicios públicos domiciliarios", "Medio ambiente y aseo urbano",
                "Seguridad ciudadana", "Salud pública", "Tránsito y movilidad",
                "Gobierno y atención ciudadana"
        };

        for (int i = 0; i < cards.length; i++) {
            final String area = areas[i];
            findViewById(cards[i]).setOnClickListener(v -> {
                Intent intent = new Intent(this, TipoReporteActivity.class);
                intent.putExtra("AREA_SELECCIONADA", area);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
    }
}