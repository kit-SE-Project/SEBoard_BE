package com.seproject.board.menu.service;

import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class BoardMenuService {

    private final BoardMenuRepository boardMenuRepository;

    public BoardMenu findById(Long id) {
        BoardMenu boardMenu = boardMenuRepository.findById(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU, null));
        return boardMenu;
    }
}
