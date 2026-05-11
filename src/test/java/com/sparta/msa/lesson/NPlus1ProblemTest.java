package com.sparta.msa.lesson;

import com.sparta.msa.lesson.domain.order.entity.Order;
import com.sparta.msa.lesson.domain.order.entity.OrderStatus;
import com.sparta.msa.lesson.domain.order.repository.OrderRepository;
import com.sparta.msa.lesson.domain.user.entity.User;
import com.sparta.msa.lesson.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NPlus1ProblemTest {

    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @PersistenceContext EntityManager em;

    @BeforeEach
    void setUp() {
        // 유저 3명, 각 유저당 주문 2건 생성
        for (int i = 1; i <= 3; i++) {
            User user = User.builder()
                .name("유저" + i)
                .email("user" + i + "@test.com")
                .password("password")
                .build();
            userRepository.save(user);

            for (int j = 1; j <= 2; j++) {
                Order order = Order.builder()
                    .user(user)
                    .totalPrice(BigDecimal.valueOf(10000L * j))
                    .status(OrderStatus.PENDING)
                    .build();
                orderRepository.save(order);
            }
        }

        em.flush(); // INSERT 쿼리 실행
        em.clear(); // 1차 캐시 초기화 → 이후 조회 시 실제 DB에서 가져옴
    }

    @Test
    @DisplayName("N+1 문제 발생: 유저 조회 1번 + 유저마다 주문 조회 N번")
    void nPlus1_문제_발생() {
        // 쿼리 1번: SELECT * FROM users
        List<User> users = userRepository.findAll();
        System.out.println("===== N+1 문제 시작 =====");

        for (User user : users) {
            // 유저마다 쿼리 1번씩 추가 발생: SELECT * FROM orders WHERE user_id = ?
            int orderCount = user.getOrders().size();
            System.out.println(user.getName() + " 의 주문 수: " + orderCount);
        }
        // 총 쿼리 수: 1(유저 조회) + 3(주문 조회) = 4번
        System.out.println("===== N+1 문제 종료 =====");
    }

    @Test
    @DisplayName("Fetch Join 해결: JOIN FETCH로 한 번의 쿼리에 모두 조회")
    void fetchJoin_해결() {
        // 쿼리 1번: SELECT DISTINCT u FROM User u JOIN FETCH u.orders
        List<User> users = userRepository.findAllWithOrders();
        System.out.println("===== Fetch Join 시작 =====");

        for (User user : users) {
            // 추가 쿼리 없음 - 이미 메모리에 로딩됨
            int orderCount = user.getOrders().size();
            System.out.println(user.getName() + " 의 주문 수: " + orderCount);
        }
        // 총 쿼리 수: 1번
        System.out.println("===== Fetch Join 종료 =====");
    }

    @Test
    @DisplayName("EAGER 로딩의 문제: 필요 없는 상황에서도 항상 즉시 조회")
    void eager_로딩의_문제점() {
        // EAGER로 설정하면 단순히 유저 1명만 조회해도
        // 연관된 orders가 무조건 함께 조회됨
        // → 주문 목록이 필요 없는 API에서도 불필요한 쿼리가 항상 발생
        // → 연관관계가 많아질수록 하나의 조회가 수십 개의 쿼리로 폭발할 수 있음

        userRepository.findByEmail("user1@test.com")
            .ifPresent(user -> System.out.println("조회된 유저: " + user.getName()));
    }
}
