package ru.mpstat.api.auth;

import lombok.Data;

@Data
public class AuthResponse {
  private String message;
  private String status;
}
