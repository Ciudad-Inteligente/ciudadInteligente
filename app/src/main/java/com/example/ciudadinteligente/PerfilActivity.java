package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etIdentificacion, etCorreo, etTelefono, etDepartamento, etCiudad, etDireccion;
    private TextView tvNombrePerfil;
    private Button btnEditarPerfil;
    private CheckBox cbCambiarPassword;
    private CardView cardPassword;
    private TextInputEditText etPasswordActual, etPasswordNueva, etPasswordConfirmar;
    private Button btnActualizarPassword;

    private boolean modoEdicion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vincular vistas del Perfil
        tvNombrePerfil = findViewById(R.id.tvNombrePerfil);
        etIdentificacion = findViewById(R.id.etPerfilIdentificacion);
        etCorreo = findViewById(R.id.etPerfilCorreo);
        etTelefono = findViewById(R.id.etPerfilTelefono);
        etDepartamento = findViewById(R.id.etPerfilDepartamento);
        etCiudad = findViewById(R.id.etPerfilCiudad);
        etDireccion = findViewById(R.id.etPerfilDireccion);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        
        cbCambiarPassword = findViewById(R.id.cbCambiarPassword);
        cardPassword = findViewById(R.id.cardPassword);
        etPasswordActual = findViewById(R.id.etPasswordActual);
        etPasswordNueva = findViewById(R.id.etPasswordNueva);
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar);
        btnActualizarPassword = findViewById(R.id.btnActualizarPassword);

        // Lógica del Header (Menú de perfil)
        ImageView btnHeaderPerfil = findViewById(R.id.btnPerfil);
        if (btnHeaderPerfil != null) {
            btnHeaderPerfil.setOnClickListener(v -> mostrarMenuPerfil(v));
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        cargarDatosUsuario();

        btnEditarPerfil.setOnClickListener(v -> toggleModoEdicion());
        
        cbCambiarPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cardPassword.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnActualizarPassword.setOnClickListener(v -> cambiarContrasena());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardCiudadano.class));
                finish();
                return true;
            }
            if (id == R.id.nav_reportar) {
                startActivity(new Intent(this, ReportarActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_mis_reportes) {
                startActivity(new Intent(this, MisReportesActivity.class));
                finish();
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
                    Toast.makeText(this, "Ya estás viendo tu perfil", Toast.LENGTH_SHORT).show();
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

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        tvNombrePerfil.setText(nombre);
                        etIdentificacion.setText(documentSnapshot.getString("id"));
                        etCorreo.setText(documentSnapshot.getString("correo"));
                        etTelefono.setText(documentSnapshot.getString("telefono"));
                        etDepartamento.setText(documentSnapshot.getString("departamento"));
                        etCiudad.setText(documentSnapshot.getString("ciudad"));
                        etDireccion.setText(documentSnapshot.getString("direccion"));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show());
    }

    private void toggleModoEdicion() {
        if (!modoEdicion) {
            modoEdicion = true;
            btnEditarPerfil.setText("Guardar Cambios");
            
            etTelefono.setEnabled(true);
            etDepartamento.setEnabled(true);
            etCiudad.setEnabled(true);
            etDireccion.setEnabled(true);
            
            etTelefono.requestFocus();
        } else {
            guardarCambiosFirestore();
        }
    }

    private void guardarCambiosFirestore() {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("telefono", etTelefono.getText().toString().trim());
        updates.put("departamento", etDepartamento.getText().toString().trim());
        updates.put("ciudad", etCiudad.getText().toString().trim());
        updates.put("direccion", etDireccion.getText().toString().trim());

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                    modoEdicion = false;
                    btnEditarPerfil.setText("Editar Datos");
                    
                    etTelefono.setEnabled(false);
                    etDepartamento.setEnabled(false);
                    etCiudad.setEnabled(false);
                    etDireccion.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }

    private void cambiarContrasena() {
        String actual = etPasswordActual.getText().toString();
        String nueva = etPasswordNueva.getText().toString();
        String confirmar = etPasswordConfirmar.getText().toString();

        if (TextUtils.isEmpty(actual) || TextUtils.isEmpty(nueva) || TextUtils.isEmpty(confirmar)) {
            Toast.makeText(this, "Completa todos los campos de contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nueva.equals(confirmar)) {
            Toast.makeText(this, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), actual);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(nueva).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                            etPasswordActual.setText("");
                            etPasswordNueva.setText("");
                            etPasswordConfirmar.setText("");
                            cbCambiarPassword.setChecked(false);
                        } else {
                            Toast.makeText(this, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}