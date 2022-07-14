package com.hcmus.wiberback.entity.dto;

import com.hcmus.wiberback.entity.enums.Role;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CallCenterResponseDto {
  @NotEmpty
  @NotBlank
  private String id;

  @NotEmpty
  @NotBlank
  private String name;

  @NotEmpty(message = "Phone number is required")
  @NotBlank(message = "Phone number is required")
  @Digits(integer = 10, fraction = 0)
  private String phone;

  @NotNull
  private Role role;
}
