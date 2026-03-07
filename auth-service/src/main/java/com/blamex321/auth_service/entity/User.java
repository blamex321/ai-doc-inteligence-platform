package com.blamex321.auth_service.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
	@Id
	private String id;
	@Indexed(collation = "users")
	private String email;
	private String password;
	private Role role;
	private LocalDateTime createdAt;
}
