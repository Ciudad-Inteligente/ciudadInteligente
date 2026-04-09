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

    private String areaNombre;
    private String tipoId;
    private String tipoNombre;

    private EditText etAsunto, etDescripcion;
    private TextView tvMensaje, tvContador;

    private static final int MAX_CARACTERES = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_reporte);
        
        // CORRECCIÓN PARA EL NAVBAR (Hueco en blanco)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        areaNombre = getIntent().getStringExtra("AREA_SELECCIONADA");
        tipoId     = getIntent().getStringExtra("TIPO_ID");
        tipoNombre = getIntent().getStringExtra("TIPO_NOMBRE");

        TextView tvChipArea  = findViewById(R.id.tvChipArea);
        TextView tvChipTipo  = findViewById(R.id.tvChipTipo);
        etAsunto             = findViewById(R.id.etAsunto);
        etDescripcion        = findViewById(R.id.etDescripcion);
        tvMensaje            = findViewById(R.id.tvMensaje);
        tvContador           = findViewById(R.id.tvContador);
        Button btnSiguiente  = findViewById(R.id.btnSiguiente);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        tvChipArea.setText(areaNombre != null ? areaNombre : "Sin área");
        tvChipTipo.setText(tipoNombre != null ? tipoNombre : "Sin tipo");

        etDescripcion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int largo = s.length();
                tvContador.setText(largo + " / " + MAX_CARACTERES);
                if (largo >= MAX_CARACTERES) tvContador.setTextColor(getColor(android.R.color.holo_red_dark));
                else if (largo >= MAX_CARACTERES * 0.8) tvContador.setTextColor(getColor(android.R.color.holo_orange_dark));
                else tvContador.setTextColor(0xFFAAAAAA);
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > MAX_CARACTERES) s.delete(MAX_CARACTERES, s.length());
            }
        });

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
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        btnSiguiente.setOnClickListener(v -> validarYContinuar());
    }

    private void validarYContinuar() {
        tvMensaje.setVisibility(View.GONE);
        String asunto      = etAsunto.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        if (asunto.isEmpty()) { mostrarError("El asunto es obligatorio."); return; }
        if (descripcion.isEmpty()) { mostrarError("La descripción es obligatoria."); return; }

        Intent intent = new Intent(this, UbicacionReporteActivity.class);
        intent.putExtra("AREA_SELECCIONADA", areaNombre);
        intent.putExtra("TIPO_ID",           tipoId);
        intent.putExtra("TIPO_NOMBRE",       tipoNombre);
        intent.putExtra("ASUNTO",            asunto);
        intent.putExtra("DESCRIPCION",       descripcion);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void mostrarError(String mensaje) {
        tvMensaje.setText(mensaje);
        tvMensaje.setVisibility(View.VISIBLE);
    }
}