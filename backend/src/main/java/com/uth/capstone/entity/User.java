package com.uth.capstone.entity;

import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity ánh xạ bảng User trong cơ sở dữ liệu MySQL (uth_find_db).
 */
@Entity
@Table(name = "User")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "Email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "StudentID", unique = true, length = 50)
    private String studentId;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, columnDefinition = "enum('STUDENT','STAFF','ADMIN','SECURITY','SAC')")
    @Builder.Default
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, columnDefinition = "enum('ACTIVE','LOCKED')")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
}
