package com.seproject.board.post.controller;

import com.seproject.board.comment.application.CommentAppService;
import com.seproject.board.post.application.PostAppService;
import com.seproject.board.post.application.PostSearchAppService;
import com.seproject.board.post.controller.dto.PostRequest.CreatePostRequest;
import com.seproject.board.post.controller.dto.PostRequest.UpdatePostRequest;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostDetailResponse;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.board.comment.application.dto.CommentCommand.CommentListFindCommand;
import static com.seproject.board.comment.controller.dto.CommentResponse.CommentListResponse;
import static com.seproject.board.common.controller.dto.MessageResponse.CreateAndUpdateMessage;
import static com.seproject.board.post.controller.dto.PostRequest.RetrievePrivacyPostRequest;

@Slf4j
@Tag(name = "게시글 API", description = "게시글(posts) 관련 API")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostAppService postAppService;
    private final PostSearchAppService postSearchAppService;
    private final CommentAppService commentAppService;


    @Operation(summary = "게시글 목록 조회", description = "카테고리, 페이징 정보를 전달하여 게시글 목록 조회한다")
    @GetMapping
    public ResponseEntity<Page<RetrievePostListResponseElement>> retrievePostList(
            @RequestParam Long categoryId, //TODO : 이게 왜 query param 인지
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer perPage
    ) {
        Page<RetrievePostListResponseElement> postList = postSearchAppService.findPostList(categoryId, page, perPage);

        return ResponseEntity.ok(postList);
    }

    @Parameter(name = "postId", description = "상세 조회를 할 게시물의 pk")
    @Operation(summary = "게시글 상세 조회", description = "게시글을 클릭하면 게시글의 상세 내역을 조회한다")
    @GetMapping("/{postId}")
    public ResponseEntity<RetrievePostDetailResponse> retrievePost(
            @PathVariable("postId") Long postId) {
        RetrievePostDetailResponse postDetailRes = postSearchAppService.findPostDetail(postId);
        return ResponseEntity.ok(postDetailRes);
    }

    @Operation(summary = "비밀 게시글 상세 조회", description = "비밀 게시글의 비밀번호가 일치하는지 확인하고 게시글을 조회한다.")
    @PostMapping("/{postId}/auth")
    public ResponseEntity<RetrievePostDetailResponse> retrievePrivacyPost(
            @RequestBody RetrievePrivacyPostRequest request,
            @PathVariable Long postId) {

        String password = request.getPassword();
        RetrievePostDetailResponse response =
                postSearchAppService.findPrivacyPostDetail(postId, password);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "공지 게시글 조회", description = "공지 게시글을 조회한다.")
    @GetMapping("/pined")
    public ResponseEntity<List<RetrievePostListResponseElement>> retrievePinedPost(
            @RequestParam Long categoryId) {//TODO : 이게 왜 query param 인지
        List<RetrievePostListResponseElement> pinedPostList =
                postSearchAppService.findPinedPostList(categoryId);
        return ResponseEntity.ok(pinedPostList);
    }

    @Parameter(name = "request", description = "게시물 생성에 필요한 제목, 본문, 공개여부, 익명 여부, 첨부파일, 카테고리 pk, 상단 고정 여부 정보")
    @Operation(summary = "게시글 작성", description = "사용자는 게시글을 작성한다")
    @PostMapping
    public ResponseEntity<CreateAndUpdateMessage> createPost(
            @Validated @RequestBody CreatePostRequest request) {
        Long postId = postAppService.writePost(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateAndUpdateMessage.of(postId, "게시글 작성 성공"));
    }




    @Operation(summary = "게시글 수정", description = "사용자는 본인이 작성한 게시물을 수정한다")
    @PutMapping("/{postId}")
    public ResponseEntity<CreateAndUpdateMessage> updatePost(
            @PathVariable Long postId,
            @Validated @RequestBody UpdatePostRequest request) { //TODO : 첨부파일 필드 추가
        Long id = postAppService.editPost(
                request.toCommand(postId)
        );

        return ResponseEntity.ok(CreateAndUpdateMessage.of(id, "게시글 수정 성공"));
    }

    @Operation(summary = "게시글 삭제", description = "사용자는 본인이 작성한 게시물을 삭제한다")
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId) {

        postAppService.removePost(postId);

        return ResponseEntity.ok(CreateAndUpdateMessage.of(postId, "게시글 삭제 성공"));
    }

    @Operation(summary = "게시물에 달린 댓글 조회", description = "게시물에 달린 댓글을 조회한다")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<CommentListResponse> retrievePostComments(
            @PathVariable Long postId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer perPage) {

        CommentListResponse commentListResponse = commentAppService.retrieveCommentList(
                CommentListFindCommand.builder()
                        .postId(postId)
                        .password(password)
                        .page(page)
                        .perPage(perPage)
                        .build()
        );

        return new ResponseEntity<>(commentListResponse, HttpStatus.OK);
    }

}
