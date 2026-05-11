package com.sparta.msa.lesson.domain.user.controller;


import com.sparta.msa.lesson.domain.user.dto.UserCreateRequest;
import com.sparta.msa.lesson.domain.user.dto.UserResponse;
import com.sparta.msa.lesson.domain.user.dto.UserUpdateRequest;
import com.sparta.msa.lesson.domain.user.service.UserService;
import com.sparta.msa.lesson.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class UserController {

  private final UserService userService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
    return ApiResponse.ok(userService.create(request));
  }


  @GetMapping("/{id}")
  public ApiResponse<UserResponse> getById(@PathVariable Long id) {
    return ApiResponse.ok(userService.getById(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<UserResponse> upadate(@PathVariable Long id,
      @RequestBody @Valid UserUpdateRequest request) {
    return ApiResponse.ok(userService.update(id, request));
  }

}
