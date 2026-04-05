package com.example.ciudadinteligente;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class RegistroCiudadano extends AppCompatActivity {
    private EditText editTextIdentificacion, editTextNombre, editTextCorreo, editTextPassword, editTextConfirmarPassword;
    private Button btnRegistrar;
    private TextView textViewMensaje;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.registrociudadano);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextIdentificacion = findViewById(R.id.editTextIdentificacion);
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmarPassword = findViewById(R.id.editTextConfirmarPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        textViewMensaje = findViewById(R.id.textViewMensaje);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        textViewMensaje.setText("");
        final String identificacion = editTextIdentificacion.getText().toString().trim();
        final String nombre = editTextNombre.getText().toString().trim();
        final String correo = editTextCorreo.getText().toString().trim();
        final String password = editTextPassword.getText().toString();
        final String confirmarPassword = editTextConfirmarPassword.getText().toString();

        // Validaciones frontend
        if (TextUtils.isEmpty(identificacion) || TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmarPassword)) {
            textViewMensaje.setText("Todos los campos son obligatorios.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            textViewMensaje.setText("Correo electrónico inválido.");
            return;
        }
        if (password.length() < 8) {
            textViewMensaje.setText("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        if (!password.equals(confirmarPassword)) {
            textViewMensaje.setText("Las contraseñas no coinciden.");
            return;
        }

        // Validar unicidad de identificación en Firestore
        db.collection("users")
                .whereEqualTo("id", identificacion)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                textViewMensaje.setText("La identificación ya está registrada.");
                            } else {
                                // Intentar registrar en Firebase Auth
                                crearUsuarioFirebaseAuth(identificacion, nombre, correo, password);
                            }
                        } else {
                            textViewMensaje.setText("Error al validar identificación. Intenta de nuevo.");
                        }
                    }
                });
    }

    private void crearUsuarioFirebaseAuth(final String identificacion, final String nombre, final String correo, final String password) {
        mAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            guardarUsuarioFirestore(identificacion, nombre, correo);
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                textViewMensaje.setText("El correo ya está registrado.");
                            } else {
                                textViewMensaje.setText("Error al registrar usuario: " + task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    private void guardarUsuarioFirestore(String identificacion, String nombre, String correo) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", identificacion);
        usuario.put("nombre", nombre);
        usuario.put("correo", correo);
        usuario.put("rol", "Ciudadano");
        usuario.put("area", null);
        usuario.put("cargo", null);
        // La contraseña ya está hasheada por Firebase Auth, no se almacena aquí

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .set(usuario)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            textViewMensaje.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            textViewMensaje.setText("¡Registro exitoso! Ahora puedes iniciar sesión.");
                            limpiarCampos();
                            // Volver a LoginUsuario
                            finish();
                        } else {
                            textViewMensaje.setText("Error al guardar usuario en la base de datos.");
                        }
                    }
                });
    }

    private void limpiarCampos() {
        editTextIdentificacion.setText("");
        editTextNombre.setText("");
        editTextCorreo.setText("");
        editTextPassword.setText("");
        editTextConfirmarPassword.setText("");
    }
}