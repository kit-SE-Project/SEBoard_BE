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
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.common.Status;
import com.seproject.board.common.domain.ReportThreshold;
import com.seproject.board.common.domain.ReportType;
import com.seproject.board.common.domain.repository.ReportThresholdRepository;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.RecruitMenu;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.board.menu.domain.repository.RecruitMenuRepository;
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
import com.seproject.skill.domain.SkillCategory;
import com.seproject.skill.domain.SkillTag;
import com.seproject.skill.repository.SkillTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        private final AdminDashBoardServiceImpl adminDashBoardService;
        private final FileExtensionRepository fileExtensionRepository;
        private final ReportThresholdRepository reportThresholdRepository;
        private final FrameRepository frameRepository;
        private final MemberFrameRepository memberFrameRepository;
        private final TierBatchService tierBatchService;
        private final RecruitMenuRepository recruitMenuRepository;
        private final SkillTagRepository skillTagRepository;

        @Value("${system_account.password}")
        private String systemPassword;

        public void init() {
            log.info("==================== required data init start ===================");
            initRole();
            initRoleBadge();
            initBoardMenu();
            initRecruitMenu();
            initSkillTags();
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
            initAdminDashBoard(
                    "학과 게시판 API 다운로드",
                    DashBoardMenu.DEPARTMENT_BOARD_API_DOWNLOAD_URL,
                    DashBoardMenuGroup.CONTENT_GROUP,
                    "학과 게시판 수집",
                    "/admin/department-board"
            );
            initAdminDashBoard("첨부파일 관리", DashBoardMenu.FILE_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("휴지통", DashBoardMenu.TRASH_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("메인 페이지 설정", DashBoardMenu.MAIN_PAGE_MENU_MANAGE_URL, DashBoardMenuGroup.SETTING_GROUP);
            initAdminDashBoard("일반", DashBoardMenu.GENERAL_URL, DashBoardMenuGroup.SETTING_GROUP);
            initAdminDashBoard("스킬 태그 관리", DashBoardMenu.SKILL_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            initAdminDashBoard("구인구직 관리", DashBoardMenu.RECRUIT_MANAGE_URL, DashBoardMenuGroup.CONTENT_GROUP);
            adminDashBoardService.refreshCache();
        }

        private void initAdminDashBoard(String name, String url, DashBoardMenuGroup menuGroup){
            initAdminDashBoard(name, url, menuGroup, null, null);
        }

        private void initAdminDashBoard(String name, String url, DashBoardMenuGroup menuGroup, String legacyName, String legacyUrl){
            Optional<DashBoardMenu> existingMenu = dashBoardMenuRepository.findByName(name);

            if(existingMenu.isEmpty() && legacyName != null) {
                existingMenu = dashBoardMenuRepository.findByName(legacyName);
            }

            if(existingMenu.isEmpty()) {
                existingMenu = dashBoardMenuRepository.findOneByUrl(url);
            }

            if(existingMenu.isEmpty() && legacyUrl != null) {
                existingMenu = dashBoardMenuRepository.findOneByUrl(legacyUrl);
            }

            Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN).get();

            if(existingMenu.isPresent()){
                DashBoardMenu dashboardMenu = existingMenu.get();

                if(!name.equals(dashboardMenu.getName()) || !url.equals(dashboardMenu.getUrl()) || !menuGroup.equals(dashboardMenu.getMenuGroup())){
                    dashboardMenu.updateMenu(name, url, menuGroup);
                    dashBoardMenuRepository.save(dashboardMenu);
                    log.info("DashboardMenu {} is updated", name);
                }else{
                    log.info("DashBoardMenu {} already exists", name);
                }

                ensureAdminDashboardAuthorization(dashboardMenu, adminRole);
            }else{
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

        private void ensureAdminDashboardAuthorization(DashBoardMenu dashboardMenu, Role adminRole) {
            boolean hasAdminAuthorization = dashboardMenu.getDashBoardMenuAuthorizations()
                    .stream()
                    .anyMatch(authorization -> authorization.authorize(List.of(adminRole)));

            if(hasAdminAuthorization) {
                return;
            }

            DashBoardMenuAuthorization dashBoardMenuAuthorization = new DashBoardMenuAuthorization(dashboardMenu, adminRole);
            dashBoardMenuAuthorizationRepository.save(dashBoardMenuAuthorization);

            List<DashBoardMenuAuthorization> authorizations = new ArrayList<>(dashboardMenu.getDashBoardMenuAuthorizations());
            authorizations.add(dashBoardMenuAuthorization);

            SelectOption selectOption = dashboardMenu.getSelectOption() == null
                    ? SelectOption.ONLY_ADMIN
                    : dashboardMenu.getSelectOption();

            dashboardMenu.update(selectOption, authorizations);
            dashBoardMenuRepository.save(dashboardMenu);
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

        private void initRecruitMenu(){
            if(recruitMenuRepository.count()>0){
                log.info("Recruit Menu already exists");
            }else{
                List<Role> adminRole = roleRepository.findByNameIn(List.of(Role.ROLE_ADMIN));

                RecruitMenu menu = new RecruitMenu(null, "Project & Job", "구인/구직 메뉴");

                menu.addAuthorization(new MenuAccessAuthorization(menu));
                menu.addAuthorization(new MenuExposeAuthorization(menu));
                menu.addAuthorization(new MenuEditAuthorization(menu));
                MenuManageAuthorization menuManageAuthorization = new MenuManageAuthorization(menu);
                menuManageAuthorization.update(adminRole);
                menu.addAuthorization(menuManageAuthorization);

                recruitMenuRepository.save(menu);

                log.info("Recruit Menu created");
            }

        }

        private void initSkillTags() {
            // LANGUAGE
            s("Java",           SkillCategory.LANGUAGE, "java");
            s("Python",         SkillCategory.LANGUAGE, "python");
            s("JavaScript",     SkillCategory.LANGUAGE, "javascript");
            s("TypeScript",     SkillCategory.LANGUAGE, "typescript");
            s("Go",             SkillCategory.LANGUAGE, "go");
            s("Kotlin",         SkillCategory.LANGUAGE, "kotlin");
            s("C++",            SkillCategory.LANGUAGE, "cplusplus");
            s("C#",             SkillCategory.LANGUAGE, "csharp");
            s("Swift",          SkillCategory.LANGUAGE, "swift");
            s("Rust",           SkillCategory.LANGUAGE, "rust");
            s("Ruby",           SkillCategory.LANGUAGE, "ruby");
            s("PHP",            SkillCategory.LANGUAGE, "php");
            s("Scala",          SkillCategory.LANGUAGE, "scala");
            s("Dart",           SkillCategory.LANGUAGE, "dart");

            // FRONTEND
            s("React",          SkillCategory.FRONTEND, "react");
            s("Vue.js",         SkillCategory.FRONTEND, "vuedotjs");
            s("Angular",        SkillCategory.FRONTEND, "angular");
            s("Next.js",        SkillCategory.FRONTEND, "nextdotjs");
            s("Nuxt.js",        SkillCategory.FRONTEND, "nuxtdotjs");
            s("Svelte",         SkillCategory.FRONTEND, "svelte");
            s("Flutter",        SkillCategory.FRONTEND, "flutter");
            s("React Native",   SkillCategory.FRONTEND, "react");
            s("Tailwind CSS",   SkillCategory.FRONTEND, "tailwindcss");
            s("Bootstrap",      SkillCategory.FRONTEND, "bootstrap");
            s("Sass",           SkillCategory.FRONTEND, "sass");
            s("Vite",           SkillCategory.FRONTEND, "vite");
            s("Webpack",        SkillCategory.FRONTEND, "webpack");
            s("Redux",          SkillCategory.FRONTEND, "redux");
            s("Three.js",       SkillCategory.FRONTEND, "threedotjs");
            s("Storybook",      SkillCategory.FRONTEND, "storybook");

            // BACKEND
            s("Spring Boot",    SkillCategory.BACKEND, "springboot");
            s("Spring",         SkillCategory.BACKEND, "spring");
            s("Node.js",        SkillCategory.BACKEND, "nodedotjs");
            s("Express",        SkillCategory.BACKEND, "express");
            s("NestJS",         SkillCategory.BACKEND, "nestjs");
            s("FastAPI",        SkillCategory.BACKEND, "fastapi");
            s("Django",         SkillCategory.BACKEND, "django");
            s("Flask",          SkillCategory.BACKEND, "flask");
            s("Laravel",        SkillCategory.BACKEND, "laravel");
            s("Ruby on Rails",  SkillCategory.BACKEND, "rubyonrails");
            s("GraphQL",        SkillCategory.BACKEND, "graphql");
            s("Quarkus",        SkillCategory.BACKEND, "quarkus");
            s("gRPC",           SkillCategory.BACKEND, null);

            // DB
            s("MySQL",          SkillCategory.DB, "mysql");
            s("PostgreSQL",     SkillCategory.DB, "postgresql");
            s("MongoDB",        SkillCategory.DB, "mongodb");
            s("Redis",          SkillCategory.DB, "redis");
            s("SQLite",         SkillCategory.DB, "sqlite");
            s("Oracle",         SkillCategory.DB, "oracle");
            s("SQL Server",     SkillCategory.DB, "microsoftsqlserver");
            s("Elasticsearch",  SkillCategory.DB, "elasticsearch");
            s("MariaDB",        SkillCategory.DB, "mariadb");
            s("Firebase",       SkillCategory.DB, "firebase");
            s("Supabase",       SkillCategory.DB, "supabase");
            s("Neo4j",          SkillCategory.DB, "neo4j");
            s("Cassandra",      SkillCategory.DB, "apachecassandra");
            s("DynamoDB",       SkillCategory.DB, "amazondynamodb");

            // INFRA
            s("Docker",         SkillCategory.INFRA, "docker");
            s("Kubernetes",     SkillCategory.INFRA, "kubernetes");
            s("AWS",            SkillCategory.INFRA, "amazonaws");
            s("Google Cloud",   SkillCategory.INFRA, "googlecloud");
            s("Azure",          SkillCategory.INFRA, "microsoftazure");
            s("Terraform",      SkillCategory.INFRA, "terraform");
            s("Ansible",        SkillCategory.INFRA, "ansible");
            s("Jenkins",        SkillCategory.INFRA, "jenkins");
            s("GitHub Actions", SkillCategory.INFRA, "githubactions");
            s("GitLab CI",      SkillCategory.INFRA, "gitlab");
            s("Nginx",          SkillCategory.INFRA, "nginx");
            s("Linux",          SkillCategory.INFRA, "linux");
            s("Ubuntu",         SkillCategory.INFRA, "ubuntu");
            s("Prometheus",     SkillCategory.INFRA, "prometheus");
            s("Grafana",        SkillCategory.INFRA, "grafana");
            s("Kafka",          SkillCategory.INFRA, "apachekafka");
            s("RabbitMQ",       SkillCategory.INFRA, "rabbitmq");
            s("Vercel",         SkillCategory.INFRA, "vercel");
            s("Cloudflare",     SkillCategory.INFRA, "cloudflare");

            // GAME
            s("Unity",          SkillCategory.GAME, "unity");
            s("Unreal Engine",  SkillCategory.GAME, "unrealengine");
            s("Godot",          SkillCategory.GAME, "godotengine");
            s("Pygame",         SkillCategory.GAME, null);
            s("LibGDX",         SkillCategory.GAME, null);
            s("MonoGame",       SkillCategory.GAME, null);
            s("OpenGL",         SkillCategory.GAME, "opengl");
            s("Vulkan",         SkillCategory.GAME, null);
            s("DirectX",        SkillCategory.GAME, null);
            s("WebGL",          SkillCategory.GAME, "webgl");
            s("Blender",        SkillCategory.GAME, "blender");

            // EMBEDDED
            s("Arduino",        SkillCategory.EMBEDDED, "arduino");
            s("Raspberry Pi",   SkillCategory.EMBEDDED, "raspberrypi");
            s("FreeRTOS",       SkillCategory.EMBEDDED, null);
            s("STM32",          SkillCategory.EMBEDDED, null);
            s("AVR",            SkillCategory.EMBEDDED, null);
            s("ESP32",          SkillCategory.EMBEDDED, "espressif");
            s("MQTT",           SkillCategory.EMBEDDED, null);
            s("ROS",            SkillCategory.EMBEDDED, "ros");
            s("CUDA",           SkillCategory.EMBEDDED, "nvidia");
            s("OpenCV",         SkillCategory.EMBEDDED, "opencv");
            s("TensorFlow Lite",SkillCategory.EMBEDDED, "tensorflow");
            s("Verilog",        SkillCategory.EMBEDDED, null);
            s("VHDL",           SkillCategory.EMBEDDED, null);

            // AI
            s("TensorFlow",     SkillCategory.AI, "tensorflow");
            s("PyTorch",        SkillCategory.AI, "pytorch");
            s("scikit-learn",   SkillCategory.AI, "scikitlearn");
            s("Keras",          SkillCategory.AI, "keras");
            s("Hugging Face",   SkillCategory.AI, "huggingface");
            s("LangChain",      SkillCategory.AI, null);
            s("OpenAI API",     SkillCategory.AI, "openai");
            s("Pandas",         SkillCategory.AI, "pandas");
            s("NumPy",          SkillCategory.AI, "numpy");
            s("Jupyter",        SkillCategory.AI, "jupyter");
            s("ONNX",           SkillCategory.AI, "onnx");
            s("MLflow",         SkillCategory.AI, null);
            s("Weights & Biases", SkillCategory.AI, "weightsandbiases");
            s("YOLO",           SkillCategory.AI, null);

            // OTHER
            s("Git",            SkillCategory.OTHER, "git");
            s("GitHub",         SkillCategory.OTHER, "github");
            s("GitLab",         SkillCategory.OTHER, "gitlab");
            s("Figma",          SkillCategory.OTHER, "figma");
            s("Postman",        SkillCategory.OTHER, "postman");
            s("Swagger",        SkillCategory.OTHER, "swagger");
            s("Jira",           SkillCategory.OTHER, "jira");
            s("Slack",          SkillCategory.OTHER, "slack");
            s("Notion",         SkillCategory.OTHER, "notion");
            s("VS Code",        SkillCategory.OTHER, "visualstudiocode");

            log.info("SkillTags initialized");
        }

        private void s(String name, SkillCategory category, String iconSlug) {
            if (!skillTagRepository.existsByName(name)) {
                skillTagRepository.save(SkillTag.builder()
                        .name(name)
                        .category(category)
                        .iconSlug(iconSlug)
                        .build());
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
