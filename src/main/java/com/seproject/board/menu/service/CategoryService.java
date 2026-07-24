package com.seproject.board.menu.service;

import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_CATEGORY, null));

        return category;
    }
}
