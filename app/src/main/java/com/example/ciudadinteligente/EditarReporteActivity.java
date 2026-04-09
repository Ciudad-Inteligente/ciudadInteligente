package com.example.ciudadinteligente;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditarReporteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String reporteId;
    private EditText etAsunto, etDescripcion;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_reporte);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        reporteId = getIntent().getStringExtra("REPORTE_ID");

        etAsunto = findViewById(R.id.etEditarAsunto);
        etDescripcion = findViewById(R.id.etEditarDescripcion);
        btnGuardar = findViewById(R.id.btnGuardarCambios);

        if (reporteId != null) {
            cargarDatosOriginales();
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosOriginales() {
        db.collection("reportes").document(reporteId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etAsunto.setText(doc.getString("asunto"));
                        etDescripcion.setText(doc.getString("descripcion"));
                    }
                });
    }

    private void guardarCambios() {
        String nuevoAsunto = etAsunto.getText().toString().trim();
        String nuevaDesc = etDescripcion.getText().toString().trim();

        if (nuevoAsunto.isEmpty() || nuevaDesc.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> cambios = new HashMap<>();
        cambios.put("asunto", nuevoAsunto);
        cambios.put("descripcion", nuevaDesc);

        db.collection("reportes").document(reporteId)
                .update(cambios)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Reporte actualizado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }
}