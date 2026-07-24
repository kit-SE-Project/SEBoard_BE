--
-- PostgreSQL database dump
--

-- Dumped from database version 15.5 (Ubuntu 15.5-1.pgdg22.04+1)
-- Dumped by pg_dump version 15.5 (Ubuntu 15.5-1.pgdg22.04+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: accounts; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.accounts (
    dtype character varying(31) NOT NULL,
    account_id bigint NOT NULL,
    created_at timestamp without time zone,
    login_id character varying(255),
    name character varying(255),
    password character varying(255),
    status character varying(255)
);


ALTER TABLE public.accounts OWNER TO se;

--
-- Name: accounts_account_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.accounts_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounts_account_id_seq OWNER TO se;

--
-- Name: accounts_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.accounts_account_id_seq OWNED BY public.accounts.account_id;


--
-- Name: anonymous; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.anonymous (
    anonymous_id bigint NOT NULL
);


ALTER TABLE public.anonymous OWNER TO se;

--
-- Name: authorization_metadata; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.authorization_metadata (
    id bigint NOT NULL,
    authorization_id bigint,
    role_id bigint
);


ALTER TABLE public.authorization_metadata OWNER TO se;

--
-- Name: authorization_metadata_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.authorization_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.authorization_metadata_id_seq OWNER TO se;

--
-- Name: authorization_metadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.authorization_metadata_id_seq OWNED BY public.authorization_metadata.id;


--
-- Name: authorizations; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.authorizations (
    dtype character varying(31) NOT NULL,
    id bigint NOT NULL
);


ALTER TABLE public.authorizations OWNER TO se;

--
-- Name: authorizations_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.authorizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.authorizations_id_seq OWNER TO se;

--
-- Name: authorizations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.authorizations_id_seq OWNED BY public.authorizations.id;


--
-- Name: banned_id; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.banned_id (
    id bigint NOT NULL,
    banned_id character varying(255)
);


ALTER TABLE public.banned_id OWNER TO se;

--
-- Name: banned_nickname; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.banned_nickname (
    id bigint NOT NULL,
    banned_nickname character varying(255)
);


ALTER TABLE public.banned_nickname OWNER TO se;

--
-- Name: banner; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.banner (
    banner_id bigint NOT NULL,
    banner_url character varying(255),
    end_date date,
    start_date date,
    file_meta_data_id bigint
);


ALTER TABLE public.banner OWNER TO se;

--
-- Name: board_users; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.board_users (
    board_user_id bigint NOT NULL,
    name character varying(255),
    account_id bigint
);


ALTER TABLE public.board_users OWNER TO se;

--
-- Name: bookmarks; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.bookmarks (
    bookmark_id bigint NOT NULL,
    post_id bigint,
    member_id bigint
);


ALTER TABLE public.bookmarks OWNER TO se;

--
-- Name: comments; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.comments (
    comment_type character varying(31) NOT NULL,
    comment_id bigint NOT NULL,
    created_at timestamp without time zone,
    modified_at timestamp without time zone,
    contents text,
    is_only_read_by_author boolean NOT NULL,
    report_count integer NOT NULL,
    status character varying(255),
    board_user_id bigint,
    post_id bigint,
    super_comment_id bigint,
    tag_comment_id bigint
);


ALTER TABLE public.comments OWNER TO se;

--
-- Name: dash_board_menu; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.dash_board_menu (
    id bigint NOT NULL,
    menu_group character varying(255),
    name character varying(255),
    select_option character varying(255),
    url character varying(255)
);


ALTER TABLE public.dash_board_menu OWNER TO se;

--
-- Name: dash_board_menu_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.dash_board_menu_authorization (
    id bigint NOT NULL,
    dash_board_menu_id bigint,
    role_role_id bigint
);


ALTER TABLE public.dash_board_menu_authorization OWNER TO se;

--
-- Name: dash_board_menu_authorization_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.dash_board_menu_authorization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dash_board_menu_authorization_id_seq OWNER TO se;

--
-- Name: dash_board_menu_authorization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.dash_board_menu_authorization_id_seq OWNED BY public.dash_board_menu_authorization.id;


--
-- Name: dash_board_menu_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.dash_board_menu_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dash_board_menu_id_seq OWNER TO se;

--
-- Name: dash_board_menu_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.dash_board_menu_id_seq OWNED BY public.dash_board_menu.id;


--
-- Name: expose_options; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.expose_options (
    expose_type character varying(31) NOT NULL,
    expose_option_id bigint NOT NULL,
    password character varying(255)
);


ALTER TABLE public.expose_options OWNER TO se;

--
-- Name: file_configuration; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.file_configuration (
    file_configuration_id bigint NOT NULL,
    max_size_per_file bigint,
    max_size_per_post bigint
);


ALTER TABLE public.file_configuration OWNER TO se;

--
-- Name: file_extension; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.file_extension (
    file_extension_id bigint NOT NULL,
    extension_name character varying(255)
);


ALTER TABLE public.file_extension OWNER TO se;

--
-- Name: file_meta_data; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.file_meta_data (
    file_meta_data_id bigint NOT NULL,
    created_at timestamp without time zone,
    modified_at timestamp without time zone,
    file_path character varying(255),
    file_size bigint,
    original_file_name character varying(255),
    stored_file_name character varying(255),
    url_path character varying(255),
    post_id bigint
);


ALTER TABLE public.file_meta_data OWNER TO se;

--
-- Name: form_accounts; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.form_accounts (
    account_id bigint NOT NULL
);


ALTER TABLE public.form_accounts OWNER TO se;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO se;

--
-- Name: ip; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.ip (
    id bigint NOT NULL,
    ip_address character varying(255),
    ip_type character varying(255)
);


ALTER TABLE public.ip OWNER TO se;

--
-- Name: login_histories; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.login_histories (
    id bigint NOT NULL,
    login_id character varying(255),
    "time" timestamp without time zone
);


ALTER TABLE public.login_histories OWNER TO se;

--
-- Name: login_prevent_user; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.login_prevent_user (
    id bigint NOT NULL,
    local_date_time timestamp without time zone,
    login_id character varying(255)
);


ALTER TABLE public.login_prevent_user OWNER TO se;

--
-- Name: login_setting; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.login_setting (
    id bigint NOT NULL,
    login_limit_time bigint,
    login_try_count bigint
);


ALTER TABLE public.login_setting OWNER TO se;

--
-- Name: login_setting_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.login_setting_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.login_setting_id_seq OWNER TO se;

--
-- Name: login_setting_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.login_setting_id_seq OWNED BY public.login_setting.id;


--
-- Name: main_page_menu; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.main_page_menu (
    id bigint NOT NULL,
    menu_id bigint
);


ALTER TABLE public.main_page_menu OWNER TO se;

--
-- Name: members; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.members (
    member_id bigint NOT NULL
);


ALTER TABLE public.members OWNER TO se;

--
-- Name: menu_access_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menu_access_authorization (
    select_option character varying(255),
    id bigint NOT NULL
);


ALTER TABLE public.menu_access_authorization OWNER TO se;

--
-- Name: menu_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menu_authorization (
    id bigint NOT NULL,
    menu_id bigint
);


ALTER TABLE public.menu_authorization OWNER TO se;

--
-- Name: menu_edit_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menu_edit_authorization (
    select_option character varying(255),
    id bigint NOT NULL
);


ALTER TABLE public.menu_edit_authorization OWNER TO se;

--
-- Name: menu_expose_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menu_expose_authorization (
    select_option character varying(255),
    id bigint NOT NULL
);


ALTER TABLE public.menu_expose_authorization OWNER TO se;

--
-- Name: menu_manage_authorization; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menu_manage_authorization (
    select_option character varying(255),
    id bigint NOT NULL
);


ALTER TABLE public.menu_manage_authorization OWNER TO se;

--
-- Name: menus; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.menus (
    menu_type character varying(31) NOT NULL,
    menu_id bigint NOT NULL,
    depth integer NOT NULL,
    description character varying(255),
    name character varying(255),
    url_info character varying(255),
    super_menu_id bigint
);


ALTER TABLE public.menus OWNER TO se;

--
-- Name: oauth_accounts; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.oauth_accounts (
    provider character varying(255),
    sub character varying(255),
    account_id bigint NOT NULL
);


ALTER TABLE public.oauth_accounts OWNER TO se;

--
-- Name: posts; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.posts (
    post_id bigint NOT NULL,
    anonymous_count integer NOT NULL,
    created_at timestamp without time zone,
    modified_at timestamp without time zone,
    contents text,
    pined boolean NOT NULL,
    report_count integer NOT NULL,
    status character varying(255),
    title character varying(255),
    views integer NOT NULL,
    board_user_id bigint,
    category_id bigint,
    expose_option_id bigint
);


ALTER TABLE public.posts OWNER TO se;

--
-- Name: report_thresholds; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.report_thresholds (
    threshold_id bigint NOT NULL,
    threshold integer NOT NULL,
    threshold_type character varying(255)
);


ALTER TABLE public.report_thresholds OWNER TO se;

--
-- Name: reports; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.reports (
    report_id bigint NOT NULL,
    member_id bigint,
    report_type character varying(255),
    target_id bigint
);


ALTER TABLE public.reports OWNER TO se;

--
-- Name: role_account; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.role_account (
    role_account_id bigint NOT NULL,
    account_id bigint,
    role_id bigint
);


ALTER TABLE public.role_account OWNER TO se;

--
-- Name: role_account_role_account_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.role_account_role_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.role_account_role_account_id_seq OWNER TO se;

--
-- Name: role_account_role_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.role_account_role_account_id_seq OWNED BY public.role_account.role_account_id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.roles (
    role_id bigint NOT NULL,
    alias character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.roles OWNER TO se;

--
-- Name: roles_role_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.roles_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.roles_role_id_seq OWNER TO se;

--
-- Name: roles_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.roles_role_id_seq OWNED BY public.roles.role_id;


--
-- Name: spam_word; Type: TABLE; Schema: public; Owner: se
--

CREATE TABLE public.spam_word (
    id bigint NOT NULL,
    word character varying(255)
);


ALTER TABLE public.spam_word OWNER TO se;

--
-- Name: spam_word_id_seq; Type: SEQUENCE; Schema: public; Owner: se
--

CREATE SEQUENCE public.spam_word_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.spam_word_id_seq OWNER TO se;

--
-- Name: spam_word_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: se
--

ALTER SEQUENCE public.spam_word_id_seq OWNED BY public.spam_word.id;


--
-- Name: accounts account_id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.accounts ALTER COLUMN account_id SET DEFAULT nextval('public.accounts_account_id_seq'::regclass);


--
-- Name: authorization_metadata id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorization_metadata ALTER COLUMN id SET DEFAULT nextval('public.authorization_metadata_id_seq'::regclass);


--
-- Name: authorizations id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorizations ALTER COLUMN id SET DEFAULT nextval('public.authorizations_id_seq'::regclass);


--
-- Name: dash_board_menu id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu ALTER COLUMN id SET DEFAULT nextval('public.dash_board_menu_id_seq'::regclass);


--
-- Name: dash_board_menu_authorization id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu_authorization ALTER COLUMN id SET DEFAULT nextval('public.dash_board_menu_authorization_id_seq'::regclass);


--
-- Name: login_setting id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.login_setting ALTER COLUMN id SET DEFAULT nextval('public.login_setting_id_seq'::regclass);


--
-- Name: role_account role_account_id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.role_account ALTER COLUMN role_account_id SET DEFAULT nextval('public.role_account_role_account_id_seq'::regclass);


--
-- Name: roles role_id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.roles ALTER COLUMN role_id SET DEFAULT nextval('public.roles_role_id_seq'::regclass);


--
-- Name: spam_word id; Type: DEFAULT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.spam_word ALTER COLUMN id SET DEFAULT nextval('public.spam_word_id_seq'::regclass);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (account_id);


--
-- Name: anonymous anonymous_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.anonymous
    ADD CONSTRAINT anonymous_pkey PRIMARY KEY (anonymous_id);


--
-- Name: authorization_metadata authorization_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorization_metadata
    ADD CONSTRAINT authorization_metadata_pkey PRIMARY KEY (id);


--
-- Name: authorizations authorizations_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorizations
    ADD CONSTRAINT authorizations_pkey PRIMARY KEY (id);


--
-- Name: banned_id banned_id_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banned_id
    ADD CONSTRAINT banned_id_pkey PRIMARY KEY (id);


--
-- Name: banned_nickname banned_nickname_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banned_nickname
    ADD CONSTRAINT banned_nickname_pkey PRIMARY KEY (id);


--
-- Name: banner banner_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banner
    ADD CONSTRAINT banner_pkey PRIMARY KEY (banner_id);


--
-- Name: board_users board_users_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.board_users
    ADD CONSTRAINT board_users_pkey PRIMARY KEY (board_user_id);


--
-- Name: bookmarks bookmarks_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT bookmarks_pkey PRIMARY KEY (bookmark_id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (comment_id);


--
-- Name: dash_board_menu_authorization dash_board_menu_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu_authorization
    ADD CONSTRAINT dash_board_menu_authorization_pkey PRIMARY KEY (id);


--
-- Name: dash_board_menu dash_board_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu
    ADD CONSTRAINT dash_board_menu_pkey PRIMARY KEY (id);


--
-- Name: expose_options expose_options_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.expose_options
    ADD CONSTRAINT expose_options_pkey PRIMARY KEY (expose_option_id);


--
-- Name: file_configuration file_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.file_configuration
    ADD CONSTRAINT file_configuration_pkey PRIMARY KEY (file_configuration_id);


--
-- Name: file_extension file_extension_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.file_extension
    ADD CONSTRAINT file_extension_pkey PRIMARY KEY (file_extension_id);


--
-- Name: file_meta_data file_meta_data_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.file_meta_data
    ADD CONSTRAINT file_meta_data_pkey PRIMARY KEY (file_meta_data_id);


--
-- Name: form_accounts form_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.form_accounts
    ADD CONSTRAINT form_accounts_pkey PRIMARY KEY (account_id);


--
-- Name: ip ip_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.ip
    ADD CONSTRAINT ip_pkey PRIMARY KEY (id);


--
-- Name: login_histories login_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.login_histories
    ADD CONSTRAINT login_histories_pkey PRIMARY KEY (id);


--
-- Name: login_prevent_user login_prevent_user_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.login_prevent_user
    ADD CONSTRAINT login_prevent_user_pkey PRIMARY KEY (id);


--
-- Name: login_setting login_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.login_setting
    ADD CONSTRAINT login_setting_pkey PRIMARY KEY (id);


--
-- Name: main_page_menu main_page_menu_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.main_page_menu
    ADD CONSTRAINT main_page_menu_pkey PRIMARY KEY (id);


--
-- Name: members members_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.members
    ADD CONSTRAINT members_pkey PRIMARY KEY (member_id);


--
-- Name: menu_access_authorization menu_access_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_access_authorization
    ADD CONSTRAINT menu_access_authorization_pkey PRIMARY KEY (id);


--
-- Name: menu_authorization menu_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_authorization
    ADD CONSTRAINT menu_authorization_pkey PRIMARY KEY (id);


--
-- Name: menu_edit_authorization menu_edit_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_edit_authorization
    ADD CONSTRAINT menu_edit_authorization_pkey PRIMARY KEY (id);


--
-- Name: menu_expose_authorization menu_expose_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_expose_authorization
    ADD CONSTRAINT menu_expose_authorization_pkey PRIMARY KEY (id);


--
-- Name: menu_manage_authorization menu_manage_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_manage_authorization
    ADD CONSTRAINT menu_manage_authorization_pkey PRIMARY KEY (id);


--
-- Name: menus menus_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menus
    ADD CONSTRAINT menus_pkey PRIMARY KEY (menu_id);


--
-- Name: oauth_accounts oauth_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT oauth_accounts_pkey PRIMARY KEY (account_id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (post_id);


--
-- Name: report_thresholds report_thresholds_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.report_thresholds
    ADD CONSTRAINT report_thresholds_pkey PRIMARY KEY (threshold_id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (report_id);


--
-- Name: role_account role_account_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.role_account
    ADD CONSTRAINT role_account_pkey PRIMARY KEY (role_account_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);


--
-- Name: spam_word spam_word_pkey; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.spam_word
    ADD CONSTRAINT spam_word_pkey PRIMARY KEY (id);


--
-- Name: banned_nickname uk_542jhhsg8ohgivioccjau1tl0; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banned_nickname
    ADD CONSTRAINT uk_542jhhsg8ohgivioccjau1tl0 UNIQUE (banned_nickname);


--
-- Name: banned_id uk_63sfnuin0dphl20ud2glgiry0; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banned_id
    ADD CONSTRAINT uk_63sfnuin0dphl20ud2glgiry0 UNIQUE (banned_id);


--
-- Name: login_prevent_user uk_fv2ud2506y71dn28lns7htx0j; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.login_prevent_user
    ADD CONSTRAINT uk_fv2ud2506y71dn28lns7htx0j UNIQUE (login_id);


--
-- Name: oauth_accounts uk_nsr7mmnbrdi1tl124jy4ikuvf; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT uk_nsr7mmnbrdi1tl124jy4ikuvf UNIQUE (sub);


--
-- Name: roles uk_ofx66keruapi6vyqpv6f2or37; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uk_ofx66keruapi6vyqpv6f2or37 UNIQUE (name);


--
-- Name: ip uk_oicmg78n05pjo4dt45d9i0br8; Type: CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.ip
    ADD CONSTRAINT uk_oicmg78n05pjo4dt45d9i0br8 UNIQUE (ip_address);


--
-- Name: posts fk1i0dldfiw7u8n9odry75oimc6; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT fk1i0dldfiw7u8n9odry75oimc6 FOREIGN KEY (expose_option_id) REFERENCES public.expose_options(expose_option_id);


--
-- Name: file_meta_data fk2xvtk6b3v7vpl0yiqqj3y2u9a; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.file_meta_data
    ADD CONSTRAINT fk2xvtk6b3v7vpl0yiqqj3y2u9a FOREIGN KEY (post_id) REFERENCES public.posts(post_id);


--
-- Name: posts fk4c9533uueyn6p6wxrl1npppb2; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT fk4c9533uueyn6p6wxrl1npppb2 FOREIGN KEY (board_user_id) REFERENCES public.board_users(board_user_id);


--
-- Name: role_account fk6bkfjr1ks1mysbwp8knth9gs2; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.role_account
    ADD CONSTRAINT fk6bkfjr1ks1mysbwp8knth9gs2 FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- Name: anonymous fk6x51omp70ah58c3qo6vb3xtbn; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.anonymous
    ADD CONSTRAINT fk6x51omp70ah58c3qo6vb3xtbn FOREIGN KEY (anonymous_id) REFERENCES public.board_users(board_user_id);


--
-- Name: posts fk78k94nuce2fbmrij8eqkpb89y; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT fk78k94nuce2fbmrij8eqkpb89y FOREIGN KEY (category_id) REFERENCES public.menus(menu_id);


--
-- Name: bookmarks fk7nbb4ldgek7ux7y6lu0y4g826; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT fk7nbb4ldgek7ux7y6lu0y4g826 FOREIGN KEY (post_id) REFERENCES public.posts(post_id);


--
-- Name: menu_authorization fk9ge018m8en2gi21xx29qps695; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_authorization
    ADD CONSTRAINT fk9ge018m8en2gi21xx29qps695 FOREIGN KEY (menu_id) REFERENCES public.menus(menu_id);


--
-- Name: dash_board_menu_authorization fkas2813y6je7xkb9c99e6ggbuk; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu_authorization
    ADD CONSTRAINT fkas2813y6je7xkb9c99e6ggbuk FOREIGN KEY (dash_board_menu_id) REFERENCES public.dash_board_menu(id);


--
-- Name: bookmarks fkasvfem60m6iaqonfkge9g2yn9; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT fkasvfem60m6iaqonfkge9g2yn9 FOREIGN KEY (member_id) REFERENCES public.members(member_id);


--
-- Name: comments fkb14mmbagim6vlyuv30re8sw9o; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fkb14mmbagim6vlyuv30re8sw9o FOREIGN KEY (super_comment_id) REFERENCES public.comments(comment_id);


--
-- Name: banner fkd3gqaouwhsk2nm8x66088x6qt; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.banner
    ADD CONSTRAINT fkd3gqaouwhsk2nm8x66088x6qt FOREIGN KEY (file_meta_data_id) REFERENCES public.file_meta_data(file_meta_data_id);


--
-- Name: menu_authorization fkeywn89lvra2m8rl90oon9uo4s; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_authorization
    ADD CONSTRAINT fkeywn89lvra2m8rl90oon9uo4s FOREIGN KEY (id) REFERENCES public.authorizations(id);


--
-- Name: menu_access_authorization fkf0oks4w62hm607sk4akjd1em6; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_access_authorization
    ADD CONSTRAINT fkf0oks4w62hm607sk4akjd1em6 FOREIGN KEY (id) REFERENCES public.menu_authorization(id);


--
-- Name: menus fkfe6j55ll1eigobyq9srgrmyo8; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menus
    ADD CONSTRAINT fkfe6j55ll1eigobyq9srgrmyo8 FOREIGN KEY (super_menu_id) REFERENCES public.menus(menu_id);


--
-- Name: members fkfup4jsy24ecpghgk3m0gq08iq; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.members
    ADD CONSTRAINT fkfup4jsy24ecpghgk3m0gq08iq FOREIGN KEY (member_id) REFERENCES public.board_users(board_user_id);


--
-- Name: comments fkh4c7lvsc298whoyd4w9ta25cr; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fkh4c7lvsc298whoyd4w9ta25cr FOREIGN KEY (post_id) REFERENCES public.posts(post_id);


--
-- Name: board_users fkhwl9ntxs1rma41xjomy4eyygx; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.board_users
    ADD CONSTRAINT fkhwl9ntxs1rma41xjomy4eyygx FOREIGN KEY (account_id) REFERENCES public.accounts(account_id);


--
-- Name: menu_edit_authorization fkhx8tw2wt4ov1owa1j15jwy1jg; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_edit_authorization
    ADD CONSTRAINT fkhx8tw2wt4ov1owa1j15jwy1jg FOREIGN KEY (id) REFERENCES public.menu_authorization(id);


--
-- Name: main_page_menu fkict5o39hxks5lafqm6sb5p9i7; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.main_page_menu
    ADD CONSTRAINT fkict5o39hxks5lafqm6sb5p9i7 FOREIGN KEY (menu_id) REFERENCES public.menus(menu_id);


--
-- Name: comments fkkaj0udkejhn8os8hnffmgx06l; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fkkaj0udkejhn8os8hnffmgx06l FOREIGN KEY (tag_comment_id) REFERENCES public.comments(comment_id);


--
-- Name: comments fkl4vjf3tmdgntsj1sts43pvy1i; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fkl4vjf3tmdgntsj1sts43pvy1i FOREIGN KEY (board_user_id) REFERENCES public.board_users(board_user_id);


--
-- Name: oauth_accounts fklls1h1r6ljq7wrvx0hrhr6arm; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT fklls1h1r6ljq7wrvx0hrhr6arm FOREIGN KEY (account_id) REFERENCES public.accounts(account_id);


--
-- Name: authorization_metadata fkmbfq5ikpah911ork6omkuvmpf; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorization_metadata
    ADD CONSTRAINT fkmbfq5ikpah911ork6omkuvmpf FOREIGN KEY (authorization_id) REFERENCES public.authorizations(id);


--
-- Name: dash_board_menu_authorization fkmqrw2slesfpmh9wbk6si84bpo; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.dash_board_menu_authorization
    ADD CONSTRAINT fkmqrw2slesfpmh9wbk6si84bpo FOREIGN KEY (role_role_id) REFERENCES public.roles(role_id);


--
-- Name: menu_manage_authorization fkn5rciukyn3caj9a9f244et28r; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_manage_authorization
    ADD CONSTRAINT fkn5rciukyn3caj9a9f244et28r FOREIGN KEY (id) REFERENCES public.menu_authorization(id);


--
-- Name: menu_expose_authorization fkoh9aj16ppyr2d9sw7kpywrisb; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.menu_expose_authorization
    ADD CONSTRAINT fkoh9aj16ppyr2d9sw7kpywrisb FOREIGN KEY (id) REFERENCES public.menu_authorization(id);


--
-- Name: role_account fkq7hrp5abxpi17rn6d66f7h2rn; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.role_account
    ADD CONSTRAINT fkq7hrp5abxpi17rn6d66f7h2rn FOREIGN KEY (account_id) REFERENCES public.accounts(account_id);


--
-- Name: authorization_metadata fkqqvcmdu9fmq7h1e6qb3t9l9x1; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.authorization_metadata
    ADD CONSTRAINT fkqqvcmdu9fmq7h1e6qb3t9l9x1 FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- Name: form_accounts fkqum8kksed6bhn4503f4eecyma; Type: FK CONSTRAINT; Schema: public; Owner: se
--

ALTER TABLE ONLY public.form_accounts
    ADD CONSTRAINT fkqum8kksed6bhn4503f4eecyma FOREIGN KEY (account_id) REFERENCES public.accounts(account_id);


--
-- PostgreSQL database dump complete
--

