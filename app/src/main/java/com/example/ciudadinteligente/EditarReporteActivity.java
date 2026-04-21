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

import com.example.ciudadinteligente.api.ReporteDTO;
import com.example.ciudadinteligente.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditarReporteActivity extends AppCompatActivity {

    private long reporteId;
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

        reporteId = getIntent().getLongExtra("REPORTE_ID", -1);

        etAsunto      = findViewById(R.id.etEditarAsunto);
        etDescripcion = findViewById(R.id.etEditarDescripcion);
        btnGuardar    = findViewById(R.id.btnGuardarCambios);

        if (reporteId != -1) {
            cargarDatosOriginales();
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosOriginales() {
        RetrofitClient.getApi().detalleReporte(reporteId)
                .enqueue(new Callback<ReporteDTO>() {
                    @Override
                    public void onResponse(Call<ReporteDTO> call,
                                           Response<ReporteDTO> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ReporteDTO r = response.body();
                            etAsunto.setText(r.asunto);
                            etDescripcion.setText(r.descripcion);
                        }
                    }

                    @Override
                    public void onFailure(Call<ReporteDTO> call, Throwable t) {
                        Toast.makeText(EditarReporteActivity.this,
                                "Error al cargar datos: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarCambios() {
        String nuevoAsunto = etAsunto.getText().toString().trim();
        String nuevaDesc   = etDescripcion.getText().toString().trim();

        if (nuevoAsunto.isEmpty() || nuevaDesc.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ReporteDTO datos = new ReporteDTO();
        datos.asunto      = nuevoAsunto;
        datos.descripcion = nuevaDesc;

        RetrofitClient.getApi().editarReporte(reporteId, datos)
                .enqueue(new Callback<ReporteDTO>() {
                    @Override
                    public void onResponse(Call<ReporteDTO> call,
                                           Response<ReporteDTO> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(EditarReporteActivity.this,
                                    "Reporte actualizado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditarReporteActivity.this,
                                    "Error al actualizar: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ReporteDTO> call, Throwable t) {
                        Toast.makeText(EditarReporteActivity.this,
                                "Sin conexión: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}