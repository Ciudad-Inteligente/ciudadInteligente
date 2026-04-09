package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;

public class CrearReporteActivity extends AppCompatActivity {

    // Datos que vienen de las pantallas anteriores
    private String areaNombre;
    private String tipoId;
    private String tipoNombre;

    private EditText etAsunto, etDescripcion;
    private TextView tvMensaje, tvContador;

    // Límite de caracteres para la descripción
    private static final int MAX_CARACTERES = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_reporte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recibir datos de TipoReporteActivity
        areaNombre = getIntent().getStringExtra("AREA_SELECCIONADA");
        tipoId     = getIntent().getStringExtra("TIPO_ID");
        tipoNombre = getIntent().getStringExtra("TIPO_NOMBRE");

        // Vincular vistas
        TextView tvChipArea  = findViewById(R.id.tvChipArea);
        TextView tvChipTipo  = findViewById(R.id.tvChipTipo);
        etAsunto             = findViewById(R.id.etAsunto);
        etDescripcion        = findViewById(R.id.etDescripcion);
        tvMensaje            = findViewById(R.id.tvMensaje);
        tvContador           = findViewById(R.id.tvContador);
        Button btnSiguiente  = findViewById(R.id.btnSiguiente);
        ImageView btnPerfil  = findViewById(R.id.btnPerfil);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Mostrar el área y tipo en los chips
        // Si por alguna razón vienen nulos, mostramos texto genérico
        tvChipArea.setText(areaNombre != null ? areaNombre : "Sin área");
        tvChipTipo.setText(tipoNombre != null ? tipoNombre : "Sin tipo");

        // Contador de caracteres en la descripción
        // TextWatcher escucha cada vez que el usuario escribe algo
        etDescripcion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int largo = s.length();
                tvContador.setText(largo + " / " + MAX_CARACTERES);

                // Cambiar color del contador cuando se acerca al límite
                if (largo >= MAX_CARACTERES) {
                    tvContador.setTextColor(getColor(android.R.color.holo_red_dark));
                } else if (largo >= MAX_CARACTERES * 0.8) {
                    // 80% del límite — aviso naranja
                    tvContador.setTextColor(getColor(android.R.color.holo_orange_dark));
                } else {
                    tvContador.setTextColor(0xFFAAAAAA);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Si supera el límite, cortar el texto automáticamente
                if (s.length() > MAX_CARACTERES) {
                    s.delete(MAX_CARACTERES, s.length());
                }
            }
        });

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
            if (id == R.id.nav_mis_reportes || id == R.id.nav_estadisticas) {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        btnSiguiente.setOnClickListener(v -> validarYContinuar());
    }

    private void validarYContinuar() {
        // Limpiar mensaje anterior
        tvMensaje.setVisibility(View.GONE);
        tvMensaje.setText("");

        String asunto      = etAsunto.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        // Validaciones en orden — mostramos el primer error que encontremos
        if (asunto.isEmpty()) {
            mostrarError("El asunto es obligatorio.");
            etAsunto.requestFocus();
            return;
        }
        if (asunto.length() < 5) {
            mostrarError("El asunto debe tener al menos 5 caracteres.");
            etAsunto.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            mostrarError("La descripción es obligatoria.");
            etDescripcion.requestFocus();
            return;
        }
        if (descripcion.length() < 10) {
            mostrarError("La descripción debe tener al menos 10 caracteres.");
            etDescripcion.requestFocus();
            return;
        }

        /*
         * Todo válido — ir a la pantalla de ubicación.
         * Pasamos todos los datos acumulados hasta ahora:
         * área + tipo (vienen de atrás) + asunto + descripción (de esta pantalla)
         */
        Intent intent = new Intent(this, UbicacionReporteActivity.class);
        intent.putExtra("AREA_SELECCIONADA", areaNombre);
        intent.putExtra("TIPO_ID",           tipoId);
        intent.putExtra("TIPO_NOMBRE",       tipoNombre);
        intent.putExtra("ASUNTO",            asunto);
        intent.putExtra("DESCRIPCION",       descripcion);
        startActivity(intent);
    }

    private void mostrarError(String mensaje) {
        tvMensaje.setText(mensaje);
        tvMensaje.setVisibility(View.VISIBLE);
    }
}