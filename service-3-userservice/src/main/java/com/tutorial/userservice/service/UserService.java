package com.tutorial.userservice.service;

import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tutorial.userservice.model.User;
import com.tutorial.userservice.repository.UserRepository;




@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("# # # UserService created # # #");
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        User user = optionalUser.orElseThrow(() -> new RuntimeException("Пользователь не найден с id: " + id));
        return user;
    }

    public User createUser(User user) {
        if (userRepository.findByEmail(
            user.getEmail()).isPresent()) {
                throw new RuntimeException("Пользователь с email " + user.getEmail() + " уже существует");
            }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userUpdate) {
        if (userRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Пользователь не найден с id: " + id);
        }
        String emailUpdate = userUpdate.getEmail();
        String nameUpdate = userUpdate.getName();
        Integer ageUpdate = userUpdate.getAge();

        User user = userRepository.findById(id).get();
        if (emailUpdate != null) {
            user.setEmail(emailUpdate);
        }
        if (nameUpdate != null) {
            user.setName(nameUpdate);
        }
        if (ageUpdate != null) {
            user.setAge(ageUpdate);
        }
        userRepository.save(user);
        return user;
    }

    public void deleteUser(Long id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Пользователь не найден с id: " + id);
        }
        userRepository.deleteById(id);
    }

}
