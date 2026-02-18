package com.tutorial.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tutorial.userservice.model.User;
import java.util.Optional;

// TODO: интерфейс, наследующий JpaRepository<User, Long>

public interface UserRepository extends JpaRepository<User, Long> {
    private void myInit() {
        System.out.println("# # # UserRepository created # # #");
    }
}
