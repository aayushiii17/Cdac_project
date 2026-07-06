package com.collabcode.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User extends BaseEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name= "full_name", nullable=false, length=100)
	private String fullName;
	
	@Column(nullable=false, unique=true, length=255)
	private String email;
	
	@Column(nullable=false, length= 255)
	private String password;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable=false)
	private Role role;
}
	
//	@Column(name= "created_at", nullable= false, updatable= false)
//	private LocalDateTime createdAt;
//	
//	@Column(name= "updated_at", nullable= false)
//	private LocalDateTime updatedAt;
//	
//	@PrePersist
//	public void onCreate() {
//		createdAt = LocalDateTime.now();
//		updatedAt= LocalDateTime.now();
//	}
//	
//	@PreUpdate
//	public void onUpdate() {
//		updatedAt= LocalDateTime.now();
//}
	
	
	
	
	
	
	


