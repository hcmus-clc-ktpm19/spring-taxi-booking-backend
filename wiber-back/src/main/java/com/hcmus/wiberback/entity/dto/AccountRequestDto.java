package com.hcmus.wiberback.entity.dto;

import com.hcmus.wiberback.entity.enums.Role;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountRequestDto {
  @NotEmpty(message = "Phone number is required")
  @NotBlank(message = "Phone number must not be blank")
  @Digits(integer = 10, fraction = 0)
  private String phone;

  @NotEmpty(message = "Password is required")
  @NotBlank(message = "Password must not be blank")
  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
      message = "Password must be at least 8 characters and contain at least one letter, one number and must not contain spaces")
  private String password;

  @NotNull(message = "Role is required")
  private Role role;
}
