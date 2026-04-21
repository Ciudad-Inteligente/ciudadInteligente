package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginUsuario extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button btnEntrar;
    private CheckBox checkboxRecordar;
    private TextView textViewMensaje, textViewRegistrarse, textViewOlvideContrasena;
    private ProgressBar progressBarLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_usuario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        checkboxRecordar = findViewById(R.id.checkboxRecordar);
        textViewMensaje = findViewById(R.id.textViewMensaje);
        textViewRegistrarse = findViewById(R.id.textViewRegistrarse);
        textViewOlvideContrasena = findViewById(R.id.textViewOlvideContrasena);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        btnEntrar.setOnClickListener(v -> iniciarSesion());

        textViewRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(LoginUsuario.this, RegistroCiudadano.class);
            startActivity(intent);
        });

        textViewOlvideContrasena.setOnClickListener(v -> {
            RecuperarContraseñaDialogFragment dialogFragment = new RecuperarContraseñaDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "RecuperarContraseña");
        });
    }

    private void iniciarSesion() {
        textViewMensaje.setVisibility(View.GONE);
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            mostrarError("Por favor ingresa tu correo electrónico");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Correo electrónico inválido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            mostrarError("Por favor ingresa tu contraseña");
            return;
        }

        // Mostrar carga
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        verificarEstadoUsuario();
                    } else {
                        setLoading(false);
                        String errorMessage = "Error al iniciar sesión";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        mostrarError(errorMessage);
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressBarLogin.setVisibility(View.VISIBLE);
            btnEntrar.setEnabled(false);
            btnEntrar.setAlpha(0.5f);
        } else {
            progressBarLogin.setVisibility(View.GONE);
            btnEntrar.setEnabled(true);
            btnEntrar.setAlpha(1.0f);
        }
    }

    private void mostrarError(String mensaje) {
        textViewMensaje.setText(mensaje);
        textViewMensaje.setVisibility(View.VISIBLE);
    }

    private void verificarEstadoUsuario() {
        if (mAuth.getCurrentUser() == null) {
            setLoading(false);
            return;
        }
        
        String uid = mAuth.getCurrentUser().getUid();
        boolean emailVerified = mAuth.getCurrentUser().isEmailVerified();

        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String estado = document.getString("estado");
                            String rol = document.getString("rol");

                            if (!emailVerified) {
                                mAuth.signOut();
                                mostrarError("Por favor, verifica tu correo electrónico antes de continuar.");
                                return;
                            }

                            if ("PENDIENTE".equals(estado)) {
                                db.collection("users").document(uid).update("estado", "ACTIVO");
                                estado = "ACTIVO";
                            }

                            if ("ACTIVO".equals(estado)) {
                                if ("Ciudadano".equals(rol)) {
                                    startActivity(new Intent(LoginUsuario.this, DashboardCiudadano.class));
                                    finish();
                                } else {
                                    mAuth.signOut();
                                    mostrarError("Acceso denegado. Rol no autorizado.");
                                }
                            } else {
                                mAuth.signOut();
                                mostrarError("Cuenta " + estado.toLowerCase() + ". Contacta a soporte.");
                            }
                        } else {
                            mAuth.signOut();
                            mostrarError("Usuario no encontrado.");
                        }
                    } else {
                        mAuth.signOut();
                        mostrarError("Error al validar cuenta.");
                    }
                });
    }
}