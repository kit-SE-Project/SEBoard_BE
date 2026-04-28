package com.seproject;

import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuEditAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.domain.MenuManageAuthorization;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.account.role.domain.repository.RoleRepository;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.domain.DashBoardMenuAuthorization;
import com.seproject.admin.dashboard.domain.DashBoardMenuGroup;
import com.seproject.admin.dashboard.domain.repository.DashBoardMenuAuthorizationRepository;
import com.seproject.admin.dashboard.domain.repository.DashBoardMenuRepository;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.common.Status;
import com.seproject.board.common.domain.ReportThreshold;
import com.seproject.board.common.domain.ReportType;
import com.seproject.board.common.domain.repository.ReportThresholdRepository;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.file.domain.model.FileExtension;
import com.seproject.file.domain.repository.FileExtensionRepository;
import com.seproject.member.application.TierBatchService;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.FrameType;
import com.seproject.member.domain.model.MemberFrame;
import com.seproject.member.domain.model.Tier;
import com.seproject.member.domain.repository.FrameRepository;
import com.seproject.member.domain.repository.MemberFrameRepository;
import com.seproject.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Profile({"local", "dev", "prod"})
public class InitRequiredData {

    private final InitService initService;

    @PostConstruct
    public void init() throws Exception {
        initService.init();
    }

    @Component
    @RequiredArgsConstructor
    @Transactional
    @Slf4j
    static class InitService {
        private final RoleRepository roleRepository;
        private final BoardMenuRepository boardMenuRepository;
        private final CategoryRepository categoryRepository;
        private final AccountRepository accountRepository;
        private final MemberRepository memberRepository;
        private final PasswordEncoder passwordEncoder;
        private final DashBoardMenuRepository dashBoardMenuRepository;
        private final DashBoardMenuAuthorizationRepository dashBoardMenuAuthorizationRepository;
        private final FileExtensionRepository fileExtensionRepository;
        private final ReportThresholdRepository reportThresholdRepository;
        private final FrameRepository frameRepository;
        private final MemberFrameRepository memberFrameRepository;
        private final TierBatchService tierBatchService;

        @Value("${system_account.password}")
        private String systemPassword;

        public void init() {
            log.info("==================== required data init start ===================");
            initRole();
            initRoleBadge();
            initBoardMenu();
            initSystemAccount();
            initAdminDashBoard();
            initFileExtension();
            initReportThreshold();
            initTierFrames();
            tierBatchService.updateTiersAndGrantFrames();
            initSystemMemberTier();
            log.info("==================== required data init end ===================");
        }

        private void initRoleBadge() {
            // 서버 기동 시마다 기본 역할의 배지 설정을 보장 (컬럼이 null이면 덮어씀)
            roleRepository.findByName(Role.ROLE_ADMIN)
                    .ifPresent(r -> r.updateBadge("CHECK", 1));
            roleRepository.findByName(Role.ROLE_KUMOH)
                    .ifPresent(r -> r.updateBadge("KUMOH_CROW", 2));
            roleRepository.findByName(Role.ROLE_USER)
                    .ifPresent(r -> r.updateBadge(null, 3));
            log.info("Role badge settings initialized");
        }

        private void initSystemMemberTier() {
            memberRepository.findByLoginId("system").ifPresent(member -> {
                member.updateTierAndScore(1500L);   // 다이아몬드 기준 점수
                for (Tier tier : Tier.values()) {
                    String frameName = tierFrameName(tier);
                    frameRepository.findByName(frameName).ifPresent(frame -> {
                        if (!memberFrameRepository.existsByMemberAndFrame(member, frame)) {
                            memberFrameRepository.save(MemberFrame.builder()
                                    .member(member)
                                    .frame(frame)
                                    .build());
                        }
                    });
                }
                log.info("System account set to Diamond with all tier frames");
            });
        }

        private String tierFrameName(Tier tier) {
            switch (tier) {
                case BRONZE:   return "브론즈 프레임";
                case SILVER:   return "실버 프레임";
                case GOLD:     return "골드 프레임";
                case PLATINUM: return "플래티넘 프레임";
                case DIAMOND:  return "다이아몬드 프레임";
                default:       return "";
            }
        }

        private void initTierFrames() {
            createFrameIfAbsent("브론즈 프레임", "브론즈 등급 달성 보상", "#CD7F32", "#8B4513", FrameType.TIER);
            createFrameIfAbsent("실버 프레임",   "실버 등급 달성 보상",   "#C0C0C0", "#808080", FrameType.TIER);
            createFrameIfAbsent("골드 프레임",   "골드 등급 달성 보상",   "#FFD700", "#FFA500", FrameType.TIER);
            createFrameIfAbsent("플래티넘 프레임","플래티넘 등급 달성 보상","#85C1E9", "#5DADE2", FrameType.TIER);
            createFrameIfAbsent("다이아몬드 프레임","다이아몬드 등급 달성 보상","#B9F2FF","#C9B1FF", FrameType.TIER);
        }

        private void createFrameIfAbsent(String name, String description, String gradientStart, String gradientEnd, FrameType type) {
            if (!frameRepository.existsByName(name)) {
                frameRepository.save(Frame.builder()
                        .name(name)
                        .description(description)
                        .gradientStart(gradientStart)
                        .gradientEnd(gradientEnd)
                        .frameType(type)
                        .build());
            }
        }

        private void initFileExtension() {
            List.of("jpg", "jpeg", "png", "gif", "svg", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "hwp")
                    .forEach(this::initFileExtension);
        }

        private void initFileExtension(String name){
            if(fileExtensionRepository.existsExtensionName(name)){
                log.info("File Extension {} already exists");
            }else{
                fileExtensionRepository.save(
                        new FileExtension(name)
                );

                log.info("File Extension {} is created");
            }
        }

        private void initReportThreshold(){
            initReportThreshold(ReportType.POST, 5);
            initReportThreshold(ReportType.COMMENT, 5);
        }

        private void initReportThreshold(ReportType type, int threshold){
            if(reportThresholdRepository.existsByThresholdType(type)){
                log.info("Threshold {} already exists", type);
            }else{
                reportThresholdRepository.save(
                        ReportThreshold.of(threshold, type)
                );
            }
        }

        private void initAdminDashBoard() {
            initAdminDashBoard("SE 메뉴 편집", DashBoardMenu.MENU_EDIT_URL, DashBoardMenuGroup.MENU_GROUP);
            initAdminDashBoard("관리자 메뉴 편집", DashBoardMenu.MENU_ADMIN_DASHBOARD_MENU_URL, DashBoardMenuGroup.MENU_GROUP);
            initAdminDashBoard("회원 목록", DashBoardMenu.ACCOUNT_MANAGE_URL, DashBoardMenuGroup.PERSON_GROUP);
            initAdminDashBoard("회원 정책", DashBoardMenu.ACCOUNT_POLICY_URL, DashBoardMenuGroup.PERSON_GROUP);
            initAdminDashBoard("회원 그룹", DashBoardMenu.ROLE_MANAGE_URL, DashBoardMenuGroup.PERSON_GROUP);
            initAdminDashBoard("게시글 관리", DashBoardMenu.POST_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("댓글 관리", DashBoardMenu.COMMENT_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("첨부파일 관리", DashBoardMenu.FILE_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("휴지통", DashBoardMenu.TRASH_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("메인 페이지 설정", DashBoardMenu.MAIN_PAGE_MENU_MANAGE_URL, DashBoardMenuGroup.SETTING_GROUP);
            initAdminDashBoard("일반", DashBoardMenu.GENERAL_URL, DashBoardMenuGroup.SETTING_GROUP);
        }

        private void initAdminDashBoard(String name, String url, DashBoardMenuGroup menuGroup){
            if(dashBoardMenuRepository.existsByName(name)){
                log.info("DashBoardMenu {} already exists", name);
            }else{
                Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN).get();

                DashBoardMenu dashboardMenu = dashBoardMenuRepository.save(
                        DashBoardMenu.builder()
                                .name(name)
                                .url(url)
                                .menuGroup(menuGroup)
                                .build()
                );

                DashBoardMenuAuthorization dashBoardMenuAuthorization = new DashBoardMenuAuthorization(dashboardMenu, adminRole);
                dashBoardMenuAuthorizationRepository.save(dashBoardMenuAuthorization);
                dashboardMenu.update(SelectOption.ONLY_ADMIN, List.of(dashBoardMenuAuthorization));
                dashBoardMenuRepository.save(dashboardMenu);

                log.info("DashboardMenu {} is created", name);
            }


        }

        private void initSystemAccount(){
            String systemName = "system";
            if(accountRepository.existsByLoginId(systemName)){
                log.info("System account already exists");
            }else{
                List<Role> roles = roleRepository.findByNameIn(List.of(Role.ROLE_ADMIN, Role.ROLE_KUMOH, Role.ROLE_USER));

                FormAccount account = accountRepository.save(
                        FormAccount.builder()
                                .loginId(systemName)
                                .name(systemName)
                                .password(passwordEncoder.encode(systemPassword))
                                .roleAccounts(roles.stream().map(role -> new RoleAccount(null, role)).collect(Collectors.toList()))
                                .createdAt(LocalDateTime.now())
                                .status(Status.NORMAL)
                                .build()
                );

                memberRepository.save(
                        Member.builder()
                                .name(systemName)
                                .account(account)
                                .build()
                );
                log.info("System account is created");
            }

        }

        private void initBoardMenu(){
            initBoardMenu("공지사항", "공지사항입니다.", "notice");
            initBoardMenu("자유게시판", "자유게시판입니다.", "freeboard");
        }

        private void initBoardMenu(String name, String description, String urlInfo){
            if(boardMenuRepository.existsByUrlInfo(urlInfo)){
                log.info("Board Menu {} already exists", name);
            }else{
                List<Role> adminRole = roleRepository.findByNameIn(List.of(Role.ROLE_ADMIN));

                BoardMenu menu = BoardMenu.builder()
                        .name(name)
                        .description(description)
                        .categoryPathId(urlInfo)
                        .build();
                menu.addAuthorization(new MenuAccessAuthorization(menu));
                menu.addAuthorization(new MenuExposeAuthorization(menu));
                menu.addAuthorization(new MenuEditAuthorization(menu));
                MenuManageAuthorization menuManageAuthorization = new MenuManageAuthorization(menu);
                menuManageAuthorization.update(adminRole);
                menu.addAuthorization(menuManageAuthorization);

                BoardMenu boardMenu = boardMenuRepository.save(menu);

                Category category = new Category(null, boardMenu, "일반", "일반", UUID.randomUUID().toString().substring(0, 8));

                category.changePopularPostEnabled(true);
                category.addAuthorization(new MenuAccessAuthorization(category));
                category.addAuthorization(new MenuExposeAuthorization(category));
                category.addAuthorization(new MenuEditAuthorization(category));
                MenuManageAuthorization categoryMenuAuthorization = new MenuManageAuthorization(category);
                categoryMenuAuthorization.update(adminRole);
                category.addAuthorization(categoryMenuAuthorization);

                categoryRepository.save(category);

                log.info("Board Menu {} created", name);
            }
        }

        private void initRole() {
            initRole(Role.ROLE_ADMIN, "시스템 최고 관리자", "관리자");
            initRole(Role.ROLE_KUMOH, "금오공대 구성원 인증된 사용자", "금오인");
            initRole(Role.ROLE_USER, "금오공대 구성원 인증안된 일반 사용자", "준회원");
        }

        private void initRole(String roleName, String description, String alias) {
            if (roleRepository.existsByName(roleName)) {
                log.info("Role {} already exists", roleName);
            } else {
                roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .description(description)
                                .alias(alias)
                                .build()
                );
                log.info("Role {} created", roleName);
            }
        }


    }
}