package com.sparta.memo.service;

import com.sparta.memo.dto.MemoRequestDto;
import com.sparta.memo.dto.MemoResponseDto;
import com.sparta.memo.entity.Memo;
import com.sparta.memo.repository.MemoRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class MemoService {



  private final MemoRepository memoRepository;



  public MemoService(MemoRepository memoRepository) {
    this.memoRepository = memoRepository;
  }


  public List<MemoResponseDto> getMemos() {

    return memoRepository.findAllByOrderByModifiedAtDesc().stream().map(MemoResponseDto::new).toList();


  }

  public MemoResponseDto createMemo(MemoRequestDto requestDto) {
    Memo memo = new Memo(requestDto);
    Memo saveMemo = memoRepository.save(memo);

    // Entity -> ResponseDto
    MemoResponseDto memoResponseDto = new MemoResponseDto(saveMemo);

    return memoResponseDto;
  }

  @Transactional
  public Long updateMemo(Long id, MemoRequestDto requestDto) {
    // 해당 메모가 DB에 존재하는지 확인
    Memo memo = findMemo(id);
    memo.update(requestDto);
    return id;

  }


  public Long deleteMemo(Long id) {
    // 해당 메모가 DB에 존재하는지 확인
    Memo memo = findMemo(id);

    //메모 삭제
    memoRepository.delete(memo);
    return id;
  }


  private Memo findMemo(Long id) {
    return memoRepository.findById(id).orElseThrow(()->
            new IllegalArgumentException("선택한 메모는 존재하지 않습니다.")
        );
  }







}
