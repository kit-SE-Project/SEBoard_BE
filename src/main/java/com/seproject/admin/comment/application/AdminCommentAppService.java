package com.seproject.admin.comment.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.comment.controller.condition.AdminCommentRetrieveCondition;
import com.seproject.admin.comment.controller.dto.CommentDTO;
import com.seproject.admin.comment.controller.dto.CommentDTO.AdminDeletedCommentResponse;
import com.seproject.admin.comment.persistence.AdminCommentSearchJpaRepository;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.comment.service.CommentService;
import com.seproject.board.common.Status;
import com.seproject.board.common.domain.repository.ReportRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentAppService {

    private final CommentRepository commentRepository;
    private final AdminCommentSearchJpaRepository adminCommentSearchRepository;
    private final ReportRepository reportRepository;

    private final CommentService commentService;

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


    public Page<AdminDeletedCommentResponse> retrieveDeletedCommentList(Pageable pageable){
        checkAuthorization(DashBoardMenu.TRASH_URL);

        return adminCommentSearchRepository.findDeletedCommentList(pageable);
    }

    public Page<CommentDTO.AdminCommentListResponse> retrieveCommentList(AdminCommentRetrieveCondition condition, Pageable pageable){
        checkAuthorization(DashBoardMenu.COMMENT_MANAGE_URL);

        return adminCommentSearchRepository.findCommentListByCondition(condition, pageable);
    }

    @Transactional
    public void restoreComment(Long commentId){
        checkAuthorization(DashBoardMenu.TRASH_URL);

        Comment comment = commentService.findById(commentId);

        comment.restore();
        reportRepository.deleteAllByCommentId(comment.getCommentId());
    }

    @Transactional
    public void restoreBulkComment(List<Long> commentIds) {
        checkAuthorization(DashBoardMenu.TRASH_URL);

        List<Comment> comments = commentService.findAllByIds(commentIds);

        if (comments.size() != commentIds.size())
            throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_COMMENT,null);

        reportRepository.deleteAllByCommentId(commentIds);
        commentService.updateStatus(commentIds, Status.NORMAL);
    }

    @Transactional
    public void deleteBulkComment(List<Long> commentIds, boolean isPermanent){
        if(isPermanent){
            checkAuthorization(DashBoardMenu.TRASH_URL);
        }else{
            checkAuthorization(DashBoardMenu.COMMENT_MANAGE_URL);
        }

        List<Comment> comments = commentService.findAllByIds(commentIds);
        if (comments.size() != commentIds.size())
            throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_COMMENT,null);

        Status status = isPermanent ? Status.PERMANENT_DELETED : Status.TEMP_DELETED;
        commentService.updateStatus(commentIds, status);
    }

}
