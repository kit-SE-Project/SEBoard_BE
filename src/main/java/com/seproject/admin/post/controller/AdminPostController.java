package com.seproject.admin.post.controller;


import com.seproject.admin.post.application.AdminPostAppService;
import com.seproject.admin.post.application.PostSyncService;
import com.seproject.admin.post.controller.dto.AdminOldPost;
import com.seproject.admin.post.controller.dto.PostRequest.AdminPostRetrieveCondition;
import com.seproject.admin.post.controller.dto.PostRequest.BulkPostRequest;
import com.seproject.admin.post.controller.dto.PostRequest.MigratePostRequest;
import com.seproject.admin.post.controller.dto.PostResponse.DeletedPostResponse;
import com.seproject.board.common.controller.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.admin.post.controller.dto.PostResponse.PostRetrieveResponse;

@Tag(name = "게시글 관리 API", description = "관리자 시스템의 게시글 관리 API")
@RequestMapping(value = "/admin/posts")
@RestController
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostAppService adminPostAppService;
    private final PostSyncService postSyncService;

    @PostMapping("/old")
    @ResponseStatus(HttpStatus.CREATED)
    public String createOldSePosts(@RequestBody AdminOldPost request){
        return postSyncService.importOldPost(request);
    }

    @Operation(summary = "게시글 목록 조회", description = "등록된 게시글 목록들을 조회한다.")
    @GetMapping
    public ResponseEntity<Page<PostRetrieveResponse>> retrieveAllPost(
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String exposeOption,
                                                @RequestParam(required = false) String searchOption,
                                                @RequestParam(name = "isReported",required = false) Boolean isReported,
                                                @RequestParam(required = false) String query,
                                                @RequestParam(defaultValue = "0",required = true) int page,
                                                @RequestParam(defaultValue = "25",required = true) int perPage) {

        AdminPostRetrieveCondition condition = new AdminPostRetrieveCondition();
        condition.setCategoryId(categoryId);
        condition.setExposeOption(exposeOption);
        condition.setSearchOption(searchOption);
        condition.setIsReported(isReported);
        condition.setQuery(query);

        Page<PostRetrieveResponse> response = adminPostAppService.findPostList(condition, page, perPage);
        return ResponseEntity.ok(response);
    }

    //TODO : 굳이 2개 만들지 말고 밑에꺼 하나로 쓰기
    @Operation(summary = "게시글 복구", description = "휴지통에 있는 게시글을 복구한다.")
    @PostMapping("/{postId}/restore")
    public ResponseEntity<MessageResponse> restorePost(@PathVariable Long postId){
        adminPostAppService.restorePost(postId);
        return new ResponseEntity<>(MessageResponse.of("게시글 복구 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 복구", description = "휴지통에 있는 게시글들을 복구한다.")
    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restoreBulkPost(@RequestBody BulkPostRequest request){
        adminPostAppService.restoreBulkPost(request.getPostIds());
        return new ResponseEntity<>(MessageResponse.of("게시글 복구 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 흎지통 이동", description = "게시글을 흎지통으로 이동한다.")
    @DeleteMapping()
    public ResponseEntity<MessageResponse> deleteBulkPost(@RequestBody BulkPostRequest request){
        adminPostAppService.deleteBulkPost(request.getPostIds(), false);
        return new ResponseEntity<>(MessageResponse.of("게시글 삭제 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 완전 삭제", description = "휴지통에 있는 게시글을 완전 삭제한다.")
    @DeleteMapping("/permanent")
    public ResponseEntity<MessageResponse> deleteBulkPostPermanent(@RequestBody BulkPostRequest request){
        adminPostAppService.deleteBulkPost(request.getPostIds(), true);
        return new ResponseEntity<>(MessageResponse.of("게시글 영구 삭제 성공"), HttpStatus.OK);
    }

    @Operation(summary = "휴지통 게시글 조회", description = "휴지통에 있는 게시글을 조회한다.")
    @GetMapping("/deleted")
    public ResponseEntity<Page<DeletedPostResponse>> retrieveDeletedPostList(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "25") int perPage) {
        Page<DeletedPostResponse> response = adminPostAppService.findDeletedPostList(PageRequest.of(page, perPage));
        return ResponseEntity.ok(response);
    }

    //TODO : Migrate 말고 게시글 카테고리 변경 , category migration이랑 이름 겹침
    @Operation(summary = "게시글 카테고리 이동", description = "from 카테고리의 모든 게시글을 to 카테고리로 변경한다.")
    @PostMapping("/migrate")
    public ResponseEntity<MessageResponse> migratePost(@RequestBody MigratePostRequest request){
        adminPostAppService.migratePost(request.getFromCategoryId(), request.getToCategoryId());
        return ResponseEntity.ok(MessageResponse.of("게시글 전체 이동 성공"));
    }

}
