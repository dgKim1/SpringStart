package com.sparta.msa.lesson.domain.user.service;

import com.sparta.msa.lesson.domain.user.dto.UserCreateRequest;
import com.sparta.msa.lesson.domain.user.dto.UserResponse;
import com.sparta.msa.lesson.domain.user.dto.UserUpdateRequest;
import com.sparta.msa.lesson.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public UserResponse create(UserCreateRequest request) {
    return null; // TODO
  }

  public UserResponse getById(Long id) {
    return null; // TODO
  }

  public UserResponse update(Long id, UserUpdateRequest request) {
    return null; // TODO
  }
}