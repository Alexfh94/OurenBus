package com.example.ourenbus2.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.model.User;
import com.example.ourenbus2.repository.UserRepository;

/**
 * ViewModel para gestionar la información del usuario
 */
public class UserViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        
        // Cargar usuario actual
        loadUser();
    }
    
    /**
     * Carga el usuario desde el repositorio
     */
    private void loadUser() {
        userRepository.getCurrentUser(user -> {
            if (user != null) {
                currentUser.postValue(user);
            } else {
                // Si no hay usuario, crear uno predeterminado
                User defaultUser = new User();
                defaultUser.setName("Usuario");
                defaultUser.setEmail("usuario@ejemplo.com");
                currentUser.postValue(defaultUser);
            }
        });
    }
    
    /**
     * Actualiza el usuario actual
     */
    public void updateUser(User user) {
        if (user != null) {
            userRepository.saveUser(user);
            currentUser.setValue(user);
        }
    }
    
    /**
     * Obtiene el usuario actual como LiveData
     */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
} 