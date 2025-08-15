package com.example.ourenbus2.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.User;
import com.example.ourenbus2.repository.UserRepository;

/**
 * Pantalla de registro de usuario. Realiza validaciones básicas y guarda el usuario
 * en la base de datos para futuras validaciones, dejando el email pre-rellenado.
 */
public class RegisterActivity extends AppCompatActivity {

	private EditText etName;
	private EditText etEmail;
	private EditText etPassword;
	private Button btnCreate;

	private UserRepository userRepository;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		userRepository = new UserRepository(getApplication());

		etName = findViewById(R.id.et_name);
		etEmail = findViewById(R.id.et_email);
		etPassword = findViewById(R.id.et_password);
		btnCreate = findViewById(R.id.btn_create_account);

		btnCreate.setOnClickListener(v -> doRegister());
	}

	private void doRegister() {
		String name = etName.getText().toString().trim();
		String email = etEmail.getText().toString().trim();
		String pass = etPassword.getText().toString();
		if (name.isEmpty()) { etName.setError(getString(R.string.error_required_field)); return; }
		if (email.isEmpty()) { etEmail.setError(getString(R.string.error_required_field)); return; }
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError(getString(R.string.error_invalid_email)); return; }
		if (pass.isEmpty()) { etPassword.setError(getString(R.string.error_required_field)); return; }

		// Guardar usuario en DB para validaciones futuras y pre-rellenar email
		new Thread(() -> {
			User u = new User();
			u.setName(name);
			u.setEmail(email);
			userRepository.insert(u);
			runOnUiThread(() -> {
				Toast.makeText(this, R.string.register, Toast.LENGTH_SHORT).show();
				// Recordar email y volver a Login (sin mantener sesión)
				userRepository.saveUser(u);
				userRepository.deleteUser();
				startActivity(new Intent(this, LoginActivity.class));
				finish();
			});
		}).start();
	}
}


