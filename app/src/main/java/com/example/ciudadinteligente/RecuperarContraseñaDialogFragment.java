package com.example.ciudadinteligente;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

/**
 * DialogFragment para la recuperación de contraseña mediante Firebase.
 * Presenta un diálogo modal centrado superpuesto sobre la pantalla de login.
 */
public class RecuperarContraseñaDialogFragment extends DialogFragment {

    private EditText editTextEmail;
    private Button btnRecuperar, btnCancelar;
    private TextView textViewMensaje;
    private FirebaseAuth mAuth;

    public RecuperarContraseñaDialogFragment() {
        // Constructor público requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Usar estilo de diálogo transparent
        setStyle(DialogFragment.STYLE_NORMAL, R.style.TransparentDialog);
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_recuperar_contrasena, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vincular elementos de UI
        editTextEmail = view.findViewById(R.id.editTextEmailRecuperar);
        btnRecuperar = view.findViewById(R.id.btnRecuperarContrasena);
        btnCancelar = view.findViewById(R.id.btnCancelarRecuperacion);
        textViewMensaje = view.findViewById(R.id.textViewMensajeRecuperacion);

        // Configurar listeners
        btnRecuperar.setOnClickListener(v -> procesarRecuperacion());
        btnCancelar.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Hacer el diálogo centrado y modal
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void procesarRecuperacion() {
        textViewMensaje.setText("");
        String email = editTextEmail.getText().toString().trim();

        // Validar que el campo no esté vacío
        if (TextUtils.isEmpty(email)) {
            textViewMensaje.setText(getString(R.string.error_email_requerido));
            return;
        }

        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textViewMensaje.setText(getString(R.string.error_email_invalido));
            return;
        }

        // Enviar correo de recuperación
        btnRecuperar.setEnabled(false);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        btnRecuperar.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Mostrar mensaje genérico de éxito
                            mostrarMensajeExito();
                        } else {
                            // Mostrar mensaje de error genérico
                            mostrarMensajeError();
                        }
                    }
                });
    }

    private void mostrarMensajeExito() {
        Toast.makeText(
                getContext(),
                getString(R.string.mensaje_recuperacion_exitosa),
                Toast.LENGTH_LONG
        ).show();
        dismiss();
    }

    private void mostrarMensajeError() {
        textViewMensaje.setText(getString(R.string.error_servicio_recuperacion));
    }
}

