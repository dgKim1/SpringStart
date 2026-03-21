package com.sparta.memo.controller;

import com.sparta.memo.dto.MemoRequestDto;
import com.sparta.memo.dto.MemoResponseDto;
import com.sparta.memo.service.MemoService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MemoController {

  private final JdbcTemplate jdbcTemplate;

  public MemoController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostMapping("/memos")
  public MemoResponseDto createMemo(@RequestBody MemoRequestDto requestDto) {
    // RequestDto -> Entity
    MemoService memoService = new MemoService(jdbcTemplate);
    return memoService.createMemo(requestDto);


  }

  @GetMapping("/memos")
  public List<MemoResponseDto> getMemos() {

    MemoService memoService = new MemoService(jdbcTemplate);
    return memoService.getMemos();

  }

  @PutMapping("/memos/{id}")
  public Long updateMemo(@PathVariable Long id, @RequestBody MemoRequestDto requestDto) {

    MemoService memoService = new MemoService(jdbcTemplate);
    return memoService.updateMemo(id,requestDto);


  }

  @DeleteMapping("/memos/{id}")
  public Long deleteMemo(@PathVariable Long id) {


    MemoService memoService = new MemoService(jdbcTemplate);
    return memoService.deleteMemo(id);

  }


}
