package com.example.ourenbus2.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ourenbus2.MainActivity;
import com.example.ourenbus2.R;
import com.example.ourenbus2.model.User;
import com.example.ourenbus2.repository.UserRepository;

/**
 * Pantalla de inicio de sesión. Valida credenciales básicas, comprueba la existencia del
 * usuario y persiste un perfil ligero en preferencias para mantener la sesión iniciada.
 */
public class LoginActivity extends AppCompatActivity {

	private EditText etEmail;
	private EditText etPassword;
	private Button btnLogin;
	private Button btnRegister;

	private UserRepository userRepository;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		userRepository = new UserRepository(getApplication());

		etEmail = findViewById(R.id.et_email);
		etPassword = findViewById(R.id.et_password);
		btnLogin = findViewById(R.id.btn_login);
		btnRegister = findViewById(R.id.btn_register);

		// Prefill last email
		String last = userRepository.getLastEmail();
		if (last != null && !last.isEmpty()) {
			etEmail.setText(last);
		}

		btnLogin.setOnClickListener(v -> doLogin());
		btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
	}

	private void doLogin() {
		String email = etEmail.getText().toString().trim();
		String pass = etPassword.getText().toString();
		if (email.isEmpty()) { etEmail.setError(getString(R.string.error_required_field)); return; }
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError(getString(R.string.error_invalid_email)); return; }
		if (pass.isEmpty()) { etPassword.setError(getString(R.string.error_required_field)); return; }

		// Validar que el email está registrado en DB
		new Thread(() -> {
			boolean exists = userRepository.existsByEmail(email);
			runOnUiThread(() -> {
				if (!exists) {
					Toast.makeText(this, R.string.user_not_found, Toast.LENGTH_SHORT).show();
					etEmail.setError(getString(R.string.register));
					return;
				}
				if (pass.isEmpty()) {
					Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
					return;
				}
				// Iniciar sesión: guardar usuario ligero en preferencias
				User u = new User();
				u.setName(email);
				u.setEmail(email);
				userRepository.saveUser(u);
				Toast.makeText(this, R.string.session_started, Toast.LENGTH_SHORT).show();
				startActivity(new Intent(this, MainActivity.class));
				finish();
			});
		}).start();
	}
}
