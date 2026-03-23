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

public class MemoService {



  private final MemoRepository memoRepository;



  public MemoService(MemoRepository memoRepository) {
    this.memoRepository = memoRepository;
  }


  public List<MemoResponseDto> getMemos() {

    return memoRepository.findAll();


  }

  public MemoResponseDto createMemo(MemoRequestDto requestDto) {
    Memo memo = new Memo(requestDto);
    Memo saveMemo = memoRepository.save(memo);

    // Entity -> ResponseDto
    MemoResponseDto memoResponseDto = new MemoResponseDto(saveMemo);

    return memoResponseDto;
  }

  public Long updateMemo(Long id, MemoRequestDto requestDto) {
    // 해당 메모가 DB에 존재하는지 확인
    Memo memo = memoRepository.findById(id);
    if (memo != null) {
      memoRepository.update(id,requestDto);


      return id;
    } else {
      throw new IllegalArgumentException("선택한 메모는 존재하지 않습니다.");
    }

  }


  public Long deleteMemo(Long id) {
    // 해당 메모가 DB에 존재하는지 확인
    Memo memo = memoRepository.findById(id);
    ;
    if (memo != null) {
      memoRepository.delete(id);


      return id;
    } else {
      throw new IllegalArgumentException("선택한 메모는 존재하지 않습니다.");
    }
  }






}
