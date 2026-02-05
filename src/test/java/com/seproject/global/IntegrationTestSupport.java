package com.seproject.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.Ip.domain.repository.IpRepository;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.account.service.LogoutService;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.common.authentication.provider.UsernameAuthenticationProvider;
import com.seproject.account.common.domain.repository.LoginHistoryRepository;
import com.seproject.account.common.domain.repository.LoginPreventUserRepository;
import com.seproject.account.email.domain.repository.AccountRegisterConfirmedEmailRepository;
import com.seproject.account.email.domain.repository.KumohConfirmedEmailRepository;
import com.seproject.account.email.domain.repository.PasswordChangeConfirmedEmailRepository;
import com.seproject.account.role.service.RoleService;
import com.seproject.account.social.repository.TemporalUserInfoRepository;
import com.seproject.account.token.domain.repository.LogoutLargeRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.account.token.domain.repository.UserTokenRepository;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.token.utils.JwtProvider;
import com.seproject.admin.account.application.AdminAccountAppService;
import com.seproject.admin.banned.domain.repository.BannedIdRepository;
import com.seproject.admin.banned.domain.repository.BannedNicknameRepository;
import com.seproject.admin.banned.service.BannedIdService;
import com.seproject.admin.banned.service.BannedNicknameService;
import com.seproject.admin.menu.application.AdminMenuAppService;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.bulletin.service.BannerService;
import com.seproject.board.bulletin.service.MainPageService;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.comment.service.CommentService;
import com.seproject.board.comment.service.ReplyService;
import com.seproject.board.common.domain.repository.ReportThresholdRepository;
import com.seproject.board.common.utils.FileUtils;
import com.seproject.board.menu.service.MenuService;
import com.seproject.board.post.domain.repository.BookmarkRepository;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.board.post.service.PostService;
import com.seproject.global.data_setup.*;
import com.seproject.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@SpringBootTest
public abstract class IntegrationTestSupport {


    @Autowired
    protected EntityManager em;
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;

    // Auth관련
    @Autowired protected UsernameAuthenticationProvider usernameAuthenticationProvider;
    @Autowired protected JwtProvider jwtProvider;
    @Autowired protected JwtDecoder jwtDecoder;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Value("${jwt.test}") protected String accessToken;

    // Test data
    @Autowired protected AccountSetup accountSetup;
    @Autowired protected RoleSetup roleSetup;
    @Autowired protected BoardUserSetup boardUserSetup;
    @Autowired protected MenuSetup menuSetup;
    @Autowired protected PostSetup postSetup;
    @Autowired protected CommentSetup commentSetup;
    @Autowired protected BookmarkSetup bookmarkSetup;
    @Autowired protected FileMetaDataSetup fileMetaDataSetup;
    @Autowired protected BannerSetup bannerSetup;
    @Autowired protected MainPageMenuSetup mainPageMenuSetup;
    @Autowired protected TokenSetup tokenSetup;
    @Autowired protected ReplySetup replySetup;

    // Service
    @Autowired protected PostService postService;
    @Autowired protected ReplyService replyService;
    @Autowired protected AdminMenuAppService adminMenuAppService;
    @Autowired protected AdminMenuService adminMenuService;
    @Autowired protected MenuService menuService;
    @Autowired protected CommentService commentService;
    @Autowired protected MainPageService mainPageService;
    @Autowired protected BannerService bannerService;
    @Autowired protected FileUtils fileUtils;
    @Autowired protected RoleService roleService;
    @Autowired protected AuthorizationService authorizationService;
    @Autowired protected TokenService tokenService;
    @Autowired protected MemberService memberService;
    @Autowired protected AccountService accountService;
    @Autowired protected LogoutService logoutService;
    @Autowired protected AdminAccountAppService accountAppService;
    @Autowired protected BannedIdService bannedIdService;
    @Autowired protected BannedNicknameService bannedNicknameService;

    // Repository
    @Autowired protected ReportThresholdRepository reportThresholdRepository;
    @Autowired protected IpRepository ipRepository;
    @Autowired protected BannedNicknameRepository bannedNicknameRepository;
    @Autowired protected BannedIdRepository bannedIdRepository;
    @Autowired protected PostRepository postRepository;
    @Autowired protected CommentRepository commentRepository;
    @Autowired protected BookmarkRepository bookmarkRepository;
    @Autowired protected UserTokenRepository userTokenRepository;
    @Autowired protected TemporalUserInfoRepository temporalUserInfoRepository;
    @Autowired protected LoginHistoryRepository loginHistoryRepository;
    @Autowired protected LoginPreventUserRepository loginPreventUserRepository;
    @Autowired protected OAuthAccountRepository oAuthAccountRepository;
    @Autowired protected LogoutLargeRefreshTokenRepository logoutLargeRefreshTokenRepository;
    @Autowired protected AccountRegisterConfirmedEmailRepository accountRegisterConfirmedEmailRepository;
    @Autowired protected AccountRepository accountRepository;
    @Autowired protected LogoutTokenRepository logoutTokenRepository;
    @Autowired protected LogoutRefreshTokenRepository logoutRefreshTokenRepository;
    @Autowired protected PasswordChangeConfirmedEmailRepository passwordChangeConfirmedEmailRepository;
    @Autowired protected KumohConfirmedEmailRepository kumohConfirmedEmailRepository;
}
