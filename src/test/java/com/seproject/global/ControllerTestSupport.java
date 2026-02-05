package com.seproject.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.account.application.AccountAppService;
import com.seproject.account.account.application.RegisterAppService;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.email.service.KumohEmailService;
import com.seproject.account.email.service.PasswordChangeEmailService;
import com.seproject.account.email.service.RegisterEmailService;
import com.seproject.account.filter.JwtFilter;
import com.seproject.account.token.service.TokenService;
import com.seproject.admin.account.application.AdminAccountAppService;
import com.seproject.admin.banned.application.AdminBannedIdAppService;
import com.seproject.admin.banned.application.AdminBannedNicknameAppService;
import com.seproject.admin.bulletin.application.AdminBannerAppService;
import com.seproject.admin.bulletin.application.AdminMainPageAppService;
import com.seproject.admin.comment.application.AdminCommentAppService;
import com.seproject.admin.dashboard.application.AdminDashBoardMenuAppService;
import com.seproject.admin.ip.application.AdminIpAppService;
import com.seproject.admin.menu.application.AdminMenuAppService;
import com.seproject.admin.post.application.AdminPostAppService;
import com.seproject.admin.post.application.PostSyncService;
import com.seproject.admin.role.application.AdminRoleAppService;
import com.seproject.admin.settings.application.LoginSettingAppService;
import com.seproject.board.bulletin.application.BannerAppService;
import com.seproject.board.bulletin.application.MainPageAppService;
import com.seproject.board.comment.application.CommentAppService;
import com.seproject.board.comment.application.ReplyAppService;
import com.seproject.board.comment.controller.CommentController;
import com.seproject.board.common.application.ProfileAppService;
import com.seproject.board.common.application.ReportAppService;
import com.seproject.board.menu.application.CategoryAppService;
import com.seproject.board.post.application.BookmarkAppService;
import com.seproject.board.post.application.PostAppService;
import com.seproject.board.post.application.PostSearchAppService;
import com.seproject.board.post.controller.PostController;
import com.seproject.config.SecurityConfig;
import com.seproject.file.application.AdminFileAppService;
import com.seproject.file.application.FileAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;

@WebMvcTest(
        controllers = {
                PostController.class,
                CommentController.class
        },
        excludeFilters ={
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
})
@ExtendWith(RestDocumentationExtension.class)
@ActiveProfiles("test")
public abstract class ControllerTestSupport {
    @Autowired
    protected MockMvc mockMvc;
    protected final ObjectMapper om = new ObjectMapper();

    // account
    @MockBean protected AccountAppService accountAppService;
    @MockBean protected AccountService accountService;
    @MockBean protected RegisterAppService registerAppService;
    @MockBean protected RegisterEmailService registerEmailService;
    @MockBean protected PasswordChangeEmailService passwordChangeEmailService;
    @MockBean protected KumohEmailService kumohEmailService;
    @MockBean protected TokenService tokenService;

    // file
    @MockBean protected FileAppService fileAppService;
    @MockBean protected AdminFileAppService adminFileAppService;

    // board
        // bulletin
    @MockBean protected BannerAppService bannerAppService;
    @MockBean protected MainPageAppService mainPageAppService;
        // comment
    @MockBean protected CommentAppService commentAppService;
    @MockBean protected ReplyAppService replyAppService;
        // common
    @MockBean protected ProfileAppService profileAppService;
    @MockBean protected ReportAppService reportAppService;
        // menu
    @MockBean protected CategoryAppService categoryAppService;
        // post
    @MockBean protected BookmarkAppService bookmarkAppService;
    @MockBean protected PostAppService postAppService;
    @MockBean protected PostSearchAppService postSearchAppService;

    // admin
        // account
    @MockBean protected AdminAccountAppService adminAccountAppService;
        // banned
    @MockBean protected AdminBannedIdAppService adminBannedIdAppService;
    @MockBean protected AdminBannedNicknameAppService adminBannedNicknameAppService;
        // bulletin
    @MockBean protected AdminBannerAppService adminBannerAppService;
    @MockBean protected AdminMainPageAppService adminMainPageAppService;
        // comment
    @MockBean protected AdminCommentAppService adminCommentAppService;
        // dashboard
    @MockBean protected AdminDashBoardMenuAppService adminDashBoardMenuAppService;
        // ip
    @MockBean protected AdminIpAppService adminIpAppService;
        // menu
    @MockBean protected AdminMenuAppService adminMenuAppService;
        // post
    @MockBean protected AdminPostAppService adminPostAppService;
    @MockBean protected PostSyncService postSyncService;
        // role
    @MockBean protected AdminRoleAppService adminRoleAppService;
        // setting
    @MockBean protected LoginSettingAppService loginSettingAppService;



    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext,
               RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }
}

