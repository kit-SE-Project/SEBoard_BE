package com.seproject.admin.post.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.post.controller.dto.PostRequest.AdminPostRetrieveCondition;
import com.seproject.admin.post.controller.dto.PostResponse.DeletedPostResponse;
import com.seproject.admin.post.controller.dto.PostResponse.PostRetrieveResponse;
import com.seproject.admin.post.service.AdminPostService;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.repository.AdminPostSearchRepository;
import com.seproject.board.post.service.PostService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.NoSuchResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostAppService {

    private final CategoryRepository categoryRepository;

    private final AdminPostService adminPostService;
    private final PostService postService;
    private final AdminPostSearchRepository adminPostSearchRepository;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(String url){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(url);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    public Page<PostRetrieveResponse> findPostList(AdminPostRetrieveCondition condition, int page, int perPage) {
        checkAuthorization(DashBoardMenu.POST_MANAGE_URL);

        return adminPostSearchRepository.findPostListByCondition(condition, PageRequest.of(page, perPage));
    }

    public Page<DeletedPostResponse> findDeletedPostList(Pageable pageable){
        checkAuthorization(DashBoardMenu.TRASH_URL);

        return adminPostSearchRepository.findDeletedPostList(pageable);
    }

    @Transactional
    public void enrollPin(Long postId) {
        changePostPinState(postId,true);
    }

    @Transactional
    public void cancelPin(Long postId) {
        changePostPinState(postId,false);
    }

    private void changePostPinState(Long postId, boolean state) {
        checkAuthorization(DashBoardMenu.POST_MANAGE_URL);

        Post post = postService.findById(postId);

        post.changePin(state);
    }

    @Transactional
    public void restorePost(Long postId){
        checkAuthorization(DashBoardMenu.POST_MANAGE_URL);

        Post post = postService.findById(postId);
        adminPostService.restore(post);
    }

    @Transactional
    public void restoreBulkPost(List<Long> postIds) {
        checkAuthorization(DashBoardMenu.TRASH_URL);

        adminPostService.restore(postIds);
    }

    @Transactional
    public void deleteBulkPost(List<Long> postIds, boolean isPermanent) {
        if(isPermanent){
            checkAuthorization(DashBoardMenu.TRASH_URL);
        }else{
            checkAuthorization(DashBoardMenu.POST_MANAGE_URL);
        }

        adminPostService.deleteAllByIds(postIds,isPermanent);
    }

    @Transactional
    public void migratePost(Long fromCategoryId, Long toCategoryId){
        checkAuthorization(DashBoardMenu.MENU_EDIT_URL);

        Category from = categoryRepository.findById(fromCategoryId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_CATEGORY));
        Category to = categoryRepository.findById(toCategoryId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_CATEGORY));

        adminPostService.changeCategory(from, to);
    }

}
