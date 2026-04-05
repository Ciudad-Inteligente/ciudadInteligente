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
                // TODO: Implementar recuperación de contraseña
                Toast.makeText(LoginUsuario.this, "Recuperación de contraseña", Toast.LENGTH_SHORT).show();
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
                            // Autenticación exitosa, ahora obtener rol de Firestore
                            String uid = mAuth.getCurrentUser().getUid();
                            obtenerRolUsuario(uid);
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