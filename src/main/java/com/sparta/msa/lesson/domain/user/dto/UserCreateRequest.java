package com.sparta.msa.lesson.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {

  @NotBlank(message = "이름은 필수입니다.")
  @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
  String name;
  @NotBlank(message = "비밀번호는 필수입니다.")
  String password;

}
