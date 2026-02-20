package com.tutorial.userservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;


// TODO: Реализуй JPA-сущность User
// Смотри TASKS.md → Шаг 4

// TODO: аннотации @Entity, @Table, @Id, @GeneratedValue
// TODO: валидация: @NotBlank, @Email, @Size

@Entity
@Table(name = "users")
public class User {
    // TODO: поля: id (Long), name (String), email (String), age (Integer)
    // TODO: конструкторы, геттеры, сеттеры

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    private String name;

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email не может быть пустым")
    private String email;

    @Min(value = 0, message = "Возраст не может быть отрицательным")
    @Max(value = 150, message = "Возраст не может быть больше 150")
    private Integer age;    

    public Long getId() { return id; }
    public String getName() { return name;}
    public String getEmail() { return email; }
    public Integer getAge() { return age; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name;} 
    public void setEmail(String email) { this.email = email; }
    public void setAge(Integer age) { this.age = age; }
    
    public User() {
    }

    public User(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }
}
