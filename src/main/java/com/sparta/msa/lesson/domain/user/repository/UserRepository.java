package com.sparta.msa.lesson.domain.user.repository;

import com.sparta.msa.lesson.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // Fetch Join: users와 orders를 한 번의 쿼리로 함께 조회
  @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.orders")
  List<User> findAllWithOrders();
}
