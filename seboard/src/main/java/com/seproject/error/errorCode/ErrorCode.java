package com.seproject.error.errorCode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_JWT(HttpStatus.UNAUTHORIZED,100, "유효하지 않은 Json Web Token입니다."),
    PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST,101, "사용자 정보가 일치하지 않습니다."),
    NOT_LOGIN(HttpStatus.UNAUTHORIZED,102, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN,103, "접근 권한이 존재하지 않습니다."),
    BANNED_IP(HttpStatus.FORBIDDEN,104, "금지된 ip입니다."),
    NOT_REGISTERED_USER(HttpStatus.TEMPORARY_REDIRECT,105, "추가 정보를 입력하지 않은 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,106, "사용자 정보가 일치하지 않습니다."),
//    USER_NOT_FOUND(HttpStatus.NOT_FOUND,106, "존재하지 않는 아이디입니다."),
    DISABLE_REFRESH_TOKEN(HttpStatus.NOT_FOUND,107,"사용할 수 없는 refreshToken 입니다."),
    TOKEN_EXPIRED(HttpStatus.NOT_FOUND,108,"만료된 Token 입니다."),
    ROLE_NOT_FOUND(HttpStatus.BAD_REQUEST,109,"존재하지 않는 권한을 요청하였습니다."),
    USER_ALREADY_EXIST(HttpStatus.BAD_REQUEST,110,"이미 존재하는 id입니다."),
    INVALID_MAIL(HttpStatus.BAD_REQUEST,111,"잘못된 이메일 형식입니다."),
    INCORRECT_AUTH_CODE(HttpStatus.BAD_REQUEST,112,"인증 코드가 일치하지 않습니다."),
    NOT_POST_AUTHOR(HttpStatus.BAD_REQUEST,113,"비밀 게시글 작성자가 아닙니다."),
    INCORRECT_POST_PASSWORD(HttpStatus.BAD_REQUEST,114,"게시글 비밀번호가 일치하지 않습니다."),
    EXCEED_WRITING_COUNT(HttpStatus.BAD_REQUEST,115,"게시글 및 댓글 작성 횟수 초과입니다."),
    EMAIL_NOT_FOUNT(HttpStatus.NOT_FOUND,116,"일치하는 이메일 인증 정보가 없습니다."),
    ALREADY_EXIST_ROLE(HttpStatus.BAD_REQUEST,117,"이미 존재하는 권한 정보입니다."),
    IMMUTABLE_ROLE(HttpStatus.BAD_REQUEST,118,"등록,수정,삭제할 수 없는 권한입니다."),
    LOGIN_PREVENT_USER(HttpStatus.BAD_REQUEST,119,"일시적으로 로그인이 금지되었습니다."),
    BANNED_ID(HttpStatus.BAD_REQUEST,120,"금지된 단어가 포함된 아이디입니다."),
    BANNED_NICKNAME(HttpStatus.BAD_REQUEST,121,"금지된 닉네임 입니다."),
    ILLEGAL_MENU_TYPE(HttpStatus.BAD_REQUEST,122,"일치하는 Menu 타입이 없습니다."),
    INVALID_MENU_REQUEST(HttpStatus.BAD_REQUEST,123,"Menu가 필요로하는 정보가 전달되지 않았습니다."),
    INVALID_MAIN_PAGE_MENU(HttpStatus.BAD_REQUEST,124,"메인 페이지 메뉴는 InternalSiteMenu만 가능합니다."),
    CANNOT_DELETE_MENU(HttpStatus.BAD_REQUEST,125,"삭제할 수 없는 메뉴입니다."),
    ALREADY_KUMOH(HttpStatus.BAD_REQUEST,126,"이미 금오인 권한을 갖고있습니다."),
    ALREADY_EXIST_NICKNAME(HttpStatus.BAD_REQUEST,127,"이미 존재하는 닉네임입니다."),
    ALREADY_EXIST_BANNED_ID(HttpStatus.BAD_REQUEST,128,"이미 존재하는 금지아이디 입니다."),
    ALREADY_EXIST_BANNED_NICKNAME(HttpStatus.BAD_REQUEST,129,"이미 존재하는 금지닉네임 입니다."),
    MENU_NAME_NULL(HttpStatus.BAD_REQUEST,130,"name값이 null입니다. "),
    NOT_EXIST_BANNED_NICKNAME(HttpStatus.NOT_FOUND,131,"존재하지 않는 금지 닉네임입니다."),
    NOT_EXIST_BANNED_ID(HttpStatus.NOT_FOUND,132,"존재하지 않는 금지아이디 입니다."),
    NOT_EXIST_IP(HttpStatus.NOT_FOUND,133,"ip를 찾을수 없습니다."),
    CATEGORY_CREATE_ERROR(HttpStatus.BAD_REQUEST,134,"카테고리는 Board Menu에만 생성할 수 있습니다."),
    MAX_DEPTH(HttpStatus.BAD_REQUEST,135,"더 이상 하위 메뉴를 만들 수 없습니다."),
    NOT_EXIST_MAIN_PAGE_MENU(HttpStatus.BAD_REQUEST,136,"존재하지 않는 메인 페이지 메뉴입니다."),
    UNAUTHORIZATION(HttpStatus.UNAUTHORIZED,137, "등록되지 않은 사용자입니다."),
    DIFFERENT_POST_COMMENT(HttpStatus.BAD_REQUEST,138, "서로 다른 게시글에 작성된 댓글입니다."),
    DIFFERENT_SUPER_COMMENT(HttpStatus.BAD_REQUEST,139, "부모 댓글과 태그 댓글의 부모가 서로 다릅니다."),
    SELECT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND,140, "존재하지 않는 선택 옵션입니다."),
    CONTAIN_SPAM_KEYWORD(HttpStatus.BAD_REQUEST,141, "스팸 키워드가 포함되어 있습니다."),
    NOT_EXIST_DASHBOARDMENU(HttpStatus.BAD_REQUEST,142, "일치하는 URL을 가진 DashBoardMenu가 없습니다."),
    INVALID_POST_TYPE(HttpStatus.BAD_REQUEST,143, "요청한 POST Type과 다릅니다."),
    INVALID_URL_INFO(HttpStatus.BAD_REQUEST, 144, "올바르지 않은 URL INFO 입니다."),
    ALREADY_EXIST_IP(HttpStatus.BAD_REQUEST, 145, "이미 등록된 IP 입니다."),
    CANNOT_ACCESS_ADMIN_IP(HttpStatus.BAD_REQUEST, 146, "관리자 모드에 접속할 수 없는 IP입니다."),


    INVALID_PAGINATION(HttpStatus.BAD_REQUEST,200, "올바르지 않은 페이지 정보를 전송하였습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 201, "올바르지 않은 형식의 요청입니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, 202, "올바르지 않은 날짜 정보를 전송하였습니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, 203, "파일 크기가 너무 큽니다."),
    INVALID_SEARCH_OPTION(HttpStatus.BAD_REQUEST, 204, "올바르지 않은 검색 옵션입니다."),


    NOT_EXIST_POST(HttpStatus.NOT_FOUND, 300, "존재하지 않는 게시글입니다."),
    NOT_EXIST_CATEGORY(HttpStatus.NOT_FOUND, 301, "존재하지 않는 카테고리입니다."),
    NOT_EXIST_ATTACHMENT(HttpStatus.NOT_FOUND, 302, "존재하지 않는 첨부파일입니다."),
    NOT_EXIST_COMMENT(HttpStatus.NOT_FOUND, 303, "존재하지 않는 댓글/답글입니다."),
    NOT_EXIST_MEMBER(HttpStatus.NOT_FOUND, 304, "존재하지 않는 회원입니다."),
    NOT_ENROLLED_ROLE(HttpStatus.NOT_FOUND, 304, "등록되지 않은 권한 설정입니다."),
    NOT_EXIST_EXTENSION(HttpStatus.NOT_FOUND, 305, "사용가능하지 않은 확장자입니다."),
    CATEGORY_NOT_EXIST_EXPOSE_OPTION(HttpStatus.NOT_FOUND, 306, "소분류 카테고리에는 메뉴 노출 설정이 존재하지 않습니다."),
    CANNOT_FOUND_OPTION(HttpStatus.NOT_FOUND, 307, "찾을 수 없는 카테고리 접근 제한 옵션입니다."),
    NOT_EXIST_FILE(HttpStatus.NOT_FOUND, 308, "존재하지 않는 파일입니다."),
    NOT_EXIST_BANNER(HttpStatus.NOT_FOUND, 309, "존재하지 않는 배너입니다."),
    NOT_EXIST_MENU(HttpStatus.NOT_FOUND, 310, "존재하지 않는 메뉴입니다."),
    NOT_EXIST_ANONYMOUS(HttpStatus.NOT_FOUND, 311, "존재하지 않는 익명 유저 입니다."),
    DUPLICATED_REPORT(HttpStatus.BAD_REQUEST, 400, "이미 신고하였습니다.");

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    private final HttpStatus httpStatus;
    private final String message;
    private final int code;

}
