package com.sparta.memo.controller;


import com.sparta.memo.dto.MemoRequestDto;
import com.sparta.memo.dto.MemoResponseDto;
import com.sparta.memo.entitty.Memo;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private final Map<Long,Memo> memoList = new HashMap<>();

  @PostMapping("/memos")
  public MemoResponseDto createMemo(@RequestBody MemoRequestDto requestDto) {
    Memo memo = new Memo(requestDto);

    Long maxId= memoList.size() > 0 ? Collections.max(memoList.keySet())+1:1;
    memo.setId(maxId);

    memoList.put(memo.getId(),memo);


    MemoResponseDto memoResponseDto = new MemoResponseDto(memo);

    return memoResponseDto;
  }

  @GetMapping("/memos")
  public List<MemoResponseDto> getMemos(){

    List<MemoResponseDto> responseList = memoList.values().stream().map(MemoResponseDto::new).toList();
    return responseList;

  }

  @PutMapping("/memos/{id}")
  public Long updateMemo(@PathVariable Long id, @RequestBody MemoRequestDto requestDto) {
    if(memoList.containsKey(id)){
      //해당 메모 가져오기
      Memo memo = memoList.get(id);

      //메모 수정
      memo.update(requestDto);
      return memo.getId();
    }else {
      throw new IllegalArgumentException("선택한 메모는 존재하지 않습니다.");
    }
  }


  @DeleteMapping("/memos/{id}")
  public Long deleteMemo(@PathVariable Long id){
    if(memoList.containsKey(id)){
      memoList.remove(id);
      return id;
    } else {
      throw new IllegalArgumentException("선택한 메모는 존재하지 않습니다.");
    }
  }

}


