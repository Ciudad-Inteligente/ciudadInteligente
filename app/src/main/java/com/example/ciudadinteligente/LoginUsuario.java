package com.example.ciudadinteligente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vincular elementos de UI
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        checkboxRecordar = findViewById(R.id.checkboxRecordar);
        textViewMensaje = findViewById(R.id.textViewMensaje);
        textViewRegistrarse = findViewById(R.id.textViewRegistrarse);
        textViewOlvideContrasena = findViewById(R.id.textViewOlvideContrasena);

        // Listeners
        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });

        textViewRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginUsuario.this, RegistroCiudadano.class);
                startActivity(intent);
            }
        });

        textViewOlvideContrasena.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar diálogo de recuperación de contraseña
                RecuperarContraseñaDialogFragment dialogFragment = new RecuperarContraseñaDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "RecuperarContraseña");
            }
        });
    }

    private void iniciarSesion() {
        textViewMensaje.setText("");
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        // Validaciones
        if (TextUtils.isEmpty(email)) {
            textViewMensaje.setText("Por favor ingresa tu correo electrónico");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textViewMensaje.setText("Correo electrónico inválido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            textViewMensaje.setText("Por favor ingresa tu contraseña");
            return;
        }

        if (password.length() < 6) {
            textViewMensaje.setText("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        // Iniciar sesión con Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Autenticación exitosa, verificar estado del usuario
                            verificarEstadoUsuario();
                        } else {
                            // Error en la autenticación
                            String errorMessage = "Error al iniciar sesión";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            textViewMensaje.setText(errorMessage);
                        }
                    }
                });
    }

    private void verificarEstadoUsuario() {
        String uid = mAuth.getCurrentUser().getUid();

        // Verificar si el correo está verificado en Firebase Auth
        boolean emailVerified = mAuth.getCurrentUser().isEmailVerified();

        // Obtener el estado del usuario en Firestore
        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String estado = document.getString("estado");
                                String rol = document.getString("rol");

                                // Verificar si el correo está verificado
                                if (!emailVerified) {
                                    // Correo no verificado
                                    mAuth.signOut(); // Cerrar sesión
                                    textViewMensaje.setText("Por favor, verifica tu correo electrónico antes de continuar.");
                                    return;
                                }

                                // Si el correo está verificado pero el estado sigue siendo PENDIENTE, actualizar a ACTIVO
                                if ("PENDIENTE".equals(estado) && emailVerified) {
                                    actualizarEstadoUsuario(uid, "ACTIVO");
                                    return;
                                }

                                // Verificar el estado del usuario
                                if ("ACTIVO".equals(estado)) {
                                    if (rol != null && rol.equals("Ciudadano")) {
                                        // Usuario activo, redirigir a DashboardCiudadano
                                        Intent intent = new Intent(LoginUsuario.this, DashboardCiudadano.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        mAuth.signOut();
                                        textViewMensaje.setText("Rol no reconocido. Contacta con soporte.");
                                    }
                                } else if ("INACTIVO".equals(estado)) {
                                    mAuth.signOut();
                                    textViewMensaje.setText("Tu cuenta está inactiva. Contacta con soporte.");
                                } else if ("SUSPENDIDO".equals(estado)) {
                                    mAuth.signOut();
                                    textViewMensaje.setText("Tu cuenta ha sido suspendida. Contacta con soporte.");
                                } else {
                                    mAuth.signOut();
                                    textViewMensaje.setText("Estado de cuenta no reconocido. Contacta con soporte.");
                                }
                            } else {
                                mAuth.signOut();
                                textViewMensaje.setText("No se encontró información del usuario en la base de datos.");
                            }
                        } else {
                            mAuth.signOut();
                            textViewMensaje.setText("Error al obtener datos del usuario");
                        }
                    }
                });
    }

    private void actualizarEstadoUsuario(String uid, String nuevoEstado) {
        db.collection("users")
                .document(uid)
                .update("estado", nuevoEstado)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Estado actualizado, obtener datos y redirigir
                            obtenerRolUsuario(uid);
                        } else {
                            mAuth.signOut();
                            textViewMensaje.setText("Error al actualizar el estado del usuario.");
                        }
                    }
                });
    }

    private void obtenerRolUsuario(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String rol = document.getString("rol");
                                if (rol != null && rol.equals("Ciudadano")) {
                                    // Redirigir a DashboardCiudadano
                                    Intent intent = new Intent(LoginUsuario.this, DashboardCiudadano.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    textViewMensaje.setText("Rol no reconocido. Contacta con soporte.");
                                }
                            } else {
                                textViewMensaje.setText("No se encontró información del usuario en la base de datos.");
                            }
                        } else {
                            textViewMensaje.setText("Error al obtener datos del usuario");
                        }
                    }
                });
    }
}

