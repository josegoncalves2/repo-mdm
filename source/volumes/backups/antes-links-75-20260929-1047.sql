--
-- PostgreSQL database dump
--

-- Dumped from database version 12.22
-- Dumped by pg_dump version 12.22

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
-- Name: applicationversions; Type: TABLE; Schema: public; Owner: hmdm
--

CREATE TABLE public.applicationversions (
    id integer NOT NULL,
    applicationid integer NOT NULL,
    version character varying(50) NOT NULL,
    url character varying(500),
    apkhash character varying(100),
    split boolean DEFAULT false NOT NULL,
    urlarmeabi text,
    urlarm64 text,
    versioncode integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.applicationversions OWNER TO hmdm;

--
-- Name: applicationversions_id_seq; Type: SEQUENCE; Schema: public; Owner: hmdm
--

CREATE SEQUENCE public.applicationversions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.applicationversions_id_seq OWNER TO hmdm;

--
-- Name: applicationversions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hmdm
--

ALTER SEQUENCE public.applicationversions_id_seq OWNED BY public.applicationversions.id;


--
-- Name: settings; Type: TABLE; Schema: public; Owner: hmdm
--

CREATE TABLE public.settings (
    id integer NOT NULL,
    backgroundcolor character varying(20),
    textcolor character varying(20),
    backgroundimageurl character varying(500),
    iconsize text DEFAULT 'SMALL'::text NOT NULL,
    desktopheader text DEFAULT 'NO_HEADER'::text NOT NULL,
    customerid bigint,
    usedefaultlanguage boolean DEFAULT true NOT NULL,
    language character varying(20),
    createnewdevices boolean DEFAULT false NOT NULL,
    newdevicegroupid integer,
    newdeviceconfigurationid integer,
    phonenumberformat character varying(50) DEFAULT '+9 (999) 999-99-99'::character varying,
    custompropertyname1 character varying(200),
    custompropertyname2 character varying(200),
    custompropertyname3 character varying(200),
    custommultiline1 boolean DEFAULT false NOT NULL,
    custommultiline2 boolean DEFAULT false NOT NULL,
    custommultiline3 boolean DEFAULT false NOT NULL,
    customsend1 boolean DEFAULT false NOT NULL,
    customsend2 boolean DEFAULT false NOT NULL,
    customsend3 boolean DEFAULT false NOT NULL,
    desktopheadertemplate text,
    senddescription boolean DEFAULT false NOT NULL,
    passwordreset boolean DEFAULT false NOT NULL,
    passwordlength integer DEFAULT 0 NOT NULL,
    passwordstrength integer DEFAULT 0 NOT NULL,
    twofactor boolean DEFAULT false NOT NULL,
    idlelogout integer,
    webprimarycolor character varying(20),
    websidebarcolor character varying(20),
    webtextcolor character varying(20),
    weblogourl character varying(500)
);


ALTER TABLE public.settings OWNER TO hmdm;

--
-- Name: settings_id_seq; Type: SEQUENCE; Schema: public; Owner: hmdm
--

CREATE SEQUENCE public.settings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.settings_id_seq OWNER TO hmdm;

--
-- Name: settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hmdm
--

ALTER SEQUENCE public.settings_id_seq OWNED BY public.settings.id;


--
-- Name: applicationversions id; Type: DEFAULT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.applicationversions ALTER COLUMN id SET DEFAULT nextval('public.applicationversions_id_seq'::regclass);


--
-- Name: settings id; Type: DEFAULT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.settings ALTER COLUMN id SET DEFAULT nextval('public.settings_id_seq'::regclass);


--
-- Data for Name: applicationversions; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.applicationversions (id, applicationid, version, url, apkhash, split, urlarmeabi, urlarm64, versioncode) FROM stdin;
10000	1	0	\N	\N	f	\N	\N	0
10001	2	0	\N	\N	f	\N	\N	0
10002	3	0	\N	\N	f	\N	\N	0
10003	4	0	\N	\N	f	\N	\N	0
10004	5	0	\N	\N	f	\N	\N	0
10005	6	0	\N	\N	f	\N	\N	0
10007	8	0	\N	\N	f	\N	\N	0
10008	9	0	\N	\N	f	\N	\N	0
10009	10	0	\N	\N	f	\N	\N	0
10010	11	0	\N	\N	f	\N	\N	0
10011	12	0	\N	\N	f	\N	\N	0
10012	13	0	\N	\N	f	\N	\N	0
10013	14	0	\N	\N	f	\N	\N	0
10014	15	0	\N	\N	f	\N	\N	0
10015	16	0	\N	\N	f	\N	\N	0
10016	17	0	\N	\N	f	\N	\N	0
10017	18	0	\N	\N	f	\N	\N	0
10018	19	0	\N	\N	f	\N	\N	0
10019	20	0	\N	\N	f	\N	\N	0
10020	21	0	\N	\N	f	\N	\N	0
10021	22	0	\N	\N	f	\N	\N	0
10022	23	0	\N	\N	f	\N	\N	0
10023	24	0	\N	\N	f	\N	\N	0
10024	25	0	\N	\N	f	\N	\N	0
10025	26	0	\N	\N	f	\N	\N	0
10026	27	0	\N	\N	f	\N	\N	0
10027	28	0	\N	\N	f	\N	\N	0
10028	29	0	\N	\N	f	\N	\N	0
10029	30	0	\N	\N	f	\N	\N	0
10030	31	0	\N	\N	f	\N	\N	0
10031	32	0	\N	\N	f	\N	\N	0
10032	33	0	\N	\N	f	\N	\N	0
10033	34	0	\N	\N	f	\N	\N	0
10034	35	0	\N	\N	f	\N	\N	0
10035	36	0	\N	\N	f	\N	\N	0
10036	37	0	\N	\N	f	\N	\N	0
10037	38	0	\N	\N	f	\N	\N	0
10038	39	0	\N	\N	f	\N	\N	0
10039	40	0	\N	\N	f	\N	\N	0
10040	41	0	\N	\N	f	\N	\N	0
10041	42	0	\N	\N	f	\N	\N	0
10042	43	0	\N	\N	f	\N	\N	0
10046	47	0	\N	\N	f	\N	\N	0
10050	51	0	\N	\N	f	\N	\N	0
10051	52	0	\N	\N	f	\N	\N	0
10052	53	0	\N	\N	f	\N	\N	0
10053	54	0	\N	\N	f	\N	\N	0
10054	55	0	\N	\N	f	\N	\N	0
10055	56	0	\N	\N	f	\N	\N	0
10056	57	0	\N	\N	f	\N	\N	0
10057	58	0	\N	\N	f	\N	\N	0
10058	59	0	\N	\N	f	\N	\N	0
10059	60	0	\N	\N	f	\N	\N	0
10060	61	0	\N	\N	f	\N	\N	0
10061	62	0	\N	\N	f	\N	\N	0
10062	63	0	\N	\N	f	\N	\N	0
10063	64	0	\N	\N	f	\N	\N	0
10064	65	0	\N	\N	f	\N	\N	0
10065	66	0	\N	\N	f	\N	\N	0
10066	67	0	\N	\N	f	\N	\N	0
10067	68	0	\N	\N	f	\N	\N	0
10068	69	0	\N	\N	f	\N	\N	0
10069	70	0	\N	\N	f	\N	\N	0
10070	71	0	\N	\N	f	\N	\N	0
10071	72	0	\N	\N	f	\N	\N	0
10072	73	0	\N	\N	f	\N	\N	0
10073	74	0	\N	\N	f	\N	\N	0
10074	75	0	\N	\N	f	\N	\N	0
10075	76	0	\N	\N	f	\N	\N	0
10076	77	0	\N	\N	f	\N	\N	0
10045	46	6.36	http://192.168.1.75:8080/files/hmdm-6.36-os.apk	aNlmR0NqQ2Dl4W7VXXuQWTSnzg40svKKpz2Ii4y433s=	f	\N	\N	0
10114	87	1.15	http://192.168.1.75:8080/files/hwmdm-remote-1.15.apk	ejZ+YF+LbkppofUjVBSlyKNwSOL42Bb/i+ZSC49OhKk	f	\N	\N	16
10115	87	1.17	http://192.168.1.75:8080/files/hwmdm-remote-1.17.apk	XM/cpp5JJOKzVB0NkaH9AP77X+ceH/4XMcXiTAYOveQ=	f	\N	\N	18
10047	48	1.02	http://192.168.1.75:8080/files/pager-1.02.apk	\N	f	\N	\N	0
10048	49	1.02	http://192.168.1.75:8080/files/phoneproxy-1.02.apk	\N	f	\N	\N	0
10049	50	1.04	http://192.168.1.75:8080/files/LauncherRestarter-1.04.apk	\N	f	\N	\N	0
10102	87	1.3	http://192.168.1.75:8080/files/hwmdm-remote-1.3.apk	fTOWfYkSzgBndhfyav8mLU6eJyg19T0DOTuOfHrUG4U	f	\N	\N	0
10103	87	1.4	http://192.168.1.75:8080/files/hwmdm-remote-1.4.apk	w8F42l7BvCeWw1t9F++oA8lFisKvnNovwwGjssc34Mw	f	\N	\N	5
10104	87	1.5	http://192.168.1.75:8080/files/hwmdm-remote-1.5.apk	iLED381c+YCo5UIajQPlx+kQRPelU0CET29Oj49AyBM	f	\N	\N	6
10092	86	2.26.29.71	http://192.168.1.75:8080/files/WhatsApp.apk	\N	f	\N	\N	262907130
10105	87	1.6	http://192.168.1.75:8080/files/hwmdm-remote-1.6.apk	VulxLaiyWWhCEM7rm2tdB9d0NpDyUHPKul0FaKdQohU	f	\N	\N	7
10106	87	1.7	http://192.168.1.75:8080/files/hwmdm-remote-1.7.apk	pvp4UTk/0py5P+/JsWIRlv05iw7VgESmIYxXCkc+Kmk	f	\N	\N	8
10107	87	1.8	http://192.168.1.75:8080/files/hwmdm-remote-1.8.apk	s+gHfyLxTAMzqnthya/PLLxQ4AsCOp2DSXHmixiplAI	f	\N	\N	9
10108	87	1.9	http://192.168.1.75:8080/files/hwmdm-remote-1.9.apk	tQ8z/EAavdYpInOeGTuB3Ac/y5geUWBUdUXHds2Fda4	f	\N	\N	10
10109	87	1.10	http://192.168.1.75:8080/files/hwmdm-remote-1.10.apk	SMXyxa12zTB9IMdaXnO71JWMOpLNDJofchcK90mPe1Q	f	\N	\N	11
10110	87	1.11	http://192.168.1.75:8080/files/hwmdm-remote-1.11.apk	jXzRAAwavptrVlxFHDrY1s2+iq6y0cokd4gu3LJWyYg	f	\N	\N	12
10111	87	1.12	http://192.168.1.75:8080/files/hwmdm-remote-1.12.apk	tAJysfJ3DYSdwP2OrEa9Yd6RTiGvpO4xjbqhYC+P+MI	f	\N	\N	13
10112	87	1.13	http://192.168.1.75:8080/files/hwmdm-remote-1.13.apk	FpgS+aeKQgdGDyXTUflEr76YyvJvKsEhcA/cdVp5Mug	f	\N	\N	14
10113	87	1.14	http://192.168.1.75:8080/files/hwmdm-remote-1.14.apk	IgxFToQDmDGSzv8CQJG7tGLFuyu5y9Ds4gsiQ3+kfeI	f	\N	\N	15
10116	87	1.22	http://192.168.1.65:8080/files/hwmdm-remote-1.22.apk	yWTmXkwDJQoWvLB/WmmWxmV2JdsFI7p3zEzMA24/cgg=	f	\N	\N	23
10117	87	1.27	http://192.168.1.65:8080/files/hwmdm-remote-1.27.apk	Breenlk7DnxKbygEiaF5nqKPjs78sPFFt0IOHK4l4z8=	f	\N	\N	27
\.


--
-- Data for Name: settings; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.settings (id, backgroundcolor, textcolor, backgroundimageurl, iconsize, desktopheader, customerid, usedefaultlanguage, language, createnewdevices, newdevicegroupid, newdeviceconfigurationid, phonenumberformat, custompropertyname1, custompropertyname2, custompropertyname3, custommultiline1, custommultiline2, custommultiline3, customsend1, customsend2, customsend3, desktopheadertemplate, senddescription, passwordreset, passwordlength, passwordstrength, twofactor, idlelogout, webprimarycolor, websidebarcolor, webtextcolor, weblogourl) FROM stdin;
1	#1c40e3	#fcfcfc	http://192.168.1.75:8080/files/BG_Tablet.png	LARGE	DEVICE_ID	1	t	\N	t	1	11	+9 (999) 999-99-99	\N	\N	\N	f	f	f	f	f	f	\N	f	f	0	0	f	\N				http://192.168.1.75:8080/files/favicon.png
\.


--
-- Name: applicationversions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.applicationversions_id_seq', 10117, true);


--
-- Name: settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.settings_id_seq', 44, true);


--
-- Name: applicationversions applicationversions_app_version_key; Type: CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.applicationversions
    ADD CONSTRAINT applicationversions_app_version_key UNIQUE (applicationid, version);


--
-- Name: applicationversions applicationversions_pr_key; Type: CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.applicationversions
    ADD CONSTRAINT applicationversions_pr_key PRIMARY KEY (id);


--
-- Name: settings settings_customer_unique; Type: CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_customer_unique UNIQUE (customerid);


--
-- Name: settings settings_pr_key; Type: CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pr_key PRIMARY KEY (id);


--
-- Name: applicationversionss_applicationid_idx; Type: INDEX; Schema: public; Owner: hmdm
--

CREATE INDEX applicationversionss_applicationid_idx ON public.applicationversions USING btree (applicationid);


--
-- Name: applicationversions mdm_application_versions_latest; Type: TRIGGER; Schema: public; Owner: hmdm
--

CREATE TRIGGER mdm_application_versions_latest AFTER INSERT OR DELETE OR UPDATE ON public.applicationversions FOR EACH ROW EXECUTE FUNCTION public.mdm_refresh_latest_version();


--
-- Name: applicationversions applicationversions_applicationid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.applicationversions
    ADD CONSTRAINT applicationversions_applicationid_fkey FOREIGN KEY (applicationid) REFERENCES public.applications(id) ON DELETE CASCADE;


--
-- Name: settings fk_customer_5; Type: FK CONSTRAINT; Schema: public; Owner: hmdm
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT fk_customer_5 FOREIGN KEY (customerid) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

