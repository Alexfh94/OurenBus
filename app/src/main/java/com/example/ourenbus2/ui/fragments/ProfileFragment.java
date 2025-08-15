package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.User;
import com.example.ourenbus2.ui.viewmodel.UserViewModel;

/**
 * Fragmento para mostrar y editar el perfil de usuario
 */
public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private EditText etName;
    private EditText etEmail;
    private Button btnSave;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Referencias a vistas
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        btnSave = view.findViewById(R.id.btn_save_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
        // Observar cambios en el usuario
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUI);
        
        // Configurar listener del botón guardar
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> doLogout());
    }
    
    /**
     * Actualiza la interfaz con los datos del usuario
     */
    private void updateUI(User user) {
        if (user != null) {
            etName.setText(user.getName());
            etEmail.setText(user.getEmail());
        }
    }
    
    /**
     * Guarda los cambios en el perfil
     */
    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        
        // Validaciones básicas
        if (name.isEmpty()) {
            etName.setError(getString(R.string.error_required_field));
            return;
        }
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        
        // Crear objeto user con los datos actualizados
        User updatedUser = new User();
        updatedUser.setName(name);
        updatedUser.setEmail(email);
        
        // Guardar cambios
        viewModel.updateUser(updatedUser);
        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
        
        // Permanecer en la misma pantalla (no navegar atrás)
    }

    private void doLogout() {
        viewModel.logout();
        // Ir a LoginActivity y limpiar back stack
        android.content.Intent i = new android.content.Intent(requireContext(), com.example.ourenbus2.auth.LoginActivity.class);
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
} 