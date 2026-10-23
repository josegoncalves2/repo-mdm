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

--
-- Data for Name: applications; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.applications (id, pkg, name, showicon, customerid, system, latestversion, runafterinstall, type, icontext, iconid, runatboot, usekiosk, intent) FROM stdin;
1	com.android.systemui	System UI	f	1	t	10000	f	app	\N	\N	f	f	\N
2	com.android.bluetooth	Bluetooth Service	f	1	t	10001	f	app	\N	\N	f	f	\N
3	com.google.android.gms	Google Services	f	1	t	10002	f	app	\N	\N	f	f	\N
34	com.android.email	Email client	t	1	t	10033	f	app	\N	\N	f	f	\N
9	com.android.chrome	Chrome Browser	t	1	t	10008	f	app	\N	\N	f	f	\N
10	com.sec.android.app.browser	Browser (Samsung)	t	1	t	10009	f	app	\N	\N	f	f	\N
11	com.samsung.android.video	Samsung Video	f	1	t	10010	f	app	\N	\N	f	f	\N
12	com.android.providers.media	Media Service	f	1	t	10011	f	app	\N	\N	f	f	\N
13	com.android.gallery3d	Gallery	t	1	t	10012	f	app	\N	\N	f	f	\N
14	com.sec.android.gallery3d	Gallery (Samsung)	t	1	t	10013	f	app	\N	\N	f	f	\N
15	com.android.vending	Google Payment support	f	1	t	10014	f	app	\N	\N	f	f	\N
16	com.samsung.android.app.memo	Notes (Samsung)	t	1	t	10015	f	app	\N	\N	f	f	\N
35	com.android.documentsui	File manager extension	f	1	t	10034	f	app	\N	\N	f	f	\N
5	com.google.android.packageinstaller	Package installer (Google)	f	1	t	10004	f	app	\N	\N	f	f	\N
17	com.android.packageinstaller	Package Installer	f	1	t	10016	f	app	\N	\N	f	f	\N
18	com.samsung.android.calendar	Calendar (Samsung)	t	1	t	10017	f	app	\N	\N	f	f	\N
19	com.android.calculator2	Calculator (generic)	t	1	t	10018	f	app	\N	\N	f	f	\N
20	com.sec.android.app.popupcalculator	Calculator (Samsung)	t	1	t	10019	f	app	\N	\N	f	f	\N
21	com.android.camera	Camera (generic)	t	1	t	10020	f	app	\N	\N	f	f	\N
22	com.huawei.camera	Camera (Huawei)	t	1	t	10021	f	app	\N	\N	f	f	\N
23	org.codeaurora.snapcam	Camera (Lenovo)	t	1	t	10022	f	app	\N	\N	f	f	\N
24	com.mediatek.camera	Camera (Mediatek)	t	1	t	10023	f	app	\N	\N	f	f	\N
25	com.sec.android.app.camera	Camera (Samsung, legacy)	t	1	t	10024	f	app	\N	\N	f	f	\N
26	com.sec.android.camera	Camera (Samsung)	t	1	t	10025	f	app	\N	\N	f	f	\N
27	com.google.android.apps.maps	Google Maps	t	1	t	10026	f	app	\N	\N	f	f	\N
28	com.touchtype.swiftkey	Swiftkey keyboard extension	f	1	t	10027	f	app	\N	\N	f	f	\N
29	com.android.contacts	Contacts	t	1	t	10028	f	app	\N	\N	f	f	\N
31	com.sec.android.app.myfiles	File Manager (Samsung)	t	1	t	10030	f	app	\N	\N	f	f	\N
32	com.android.settings	Settings (usually must be disabled!)	f	1	t	10031	f	app	\N	\N	f	f	\N
33	com.sec.android.inputmethod	Keyboard settings (Samsung)	f	1	t	10032	f	app	\N	\N	f	f	\N
36	com.samsung.android.email.provider	Email service (Samsung)	f	1	t	10035	f	app	\N	\N	f	f	\N
37	android	Android system package	f	1	t	10036	f	app	\N	\N	f	f	\N
38	com.android.mms	Messaging (generic)	t	1	t	10037	f	app	\N	\N	f	f	\N
39	com.google.android.apps.messaging	Messaging (Google)	t	1	t	10038	f	app	\N	\N	f	f	\N
40	com.android.dialer	Phone (generic UI)	t	1	t	10039	f	app	\N	\N	f	f	\N
41	com.sec.phone	Phone (Samsung)	t	1	t	10040	f	app	\N	\N	f	f	\N
42	com.android.phone	Phone (generic service)	t	1	t	10041	f	app	\N	\N	f	f	\N
43	com.huaqin.filemanager	File manager (Lenovo)	t	1	t	10042	f	app	\N	\N	f	f	\N
6	com.google.android.apps.photos	Gallery (Google)	t	1	t	10005	f	app	\N	\N	f	f	\N
4	com.google.android.apps.docs	Google Drive	t	1	t	10003	f	app	\N	\N	f	f	\N
30	com.huawei.android.launcher	Default launcher (Huawei)	f	1	t	10029	f	app	\N	\N	f	f	\N
8	com.android.browser	Browser (generic)	t	1	t	10007	f	app	\N	\N	f	f	\N
47	com.huawei.android.internal.app	Huawei Launcher Selector	f	1	t	10046	f	app	\N	\N	f	f	\N
51	com.miui.cleanmaster	MIUI Clean Master	f	1	t	10050	f	app	\N	\N	f	f	\N
52	com.miui.gallery	MIUI Gallery	t	1	t	10051	f	app	\N	\N	f	f	\N
53	com.miui.notes	MIUI Notes	t	1	t	10052	f	app	\N	\N	f	f	\N
54	com.miui.global.packageinstaller	MIUI Package Installer	f	1	t	10053	f	app	\N	\N	f	f	\N
55	com.miui.msa.global	MIUI Permissions Manager	f	1	t	10054	f	app	\N	\N	f	f	\N
56	com.miui.securitycenter	MIUI Security Center	f	1	t	10055	f	app	\N	\N	f	f	\N
57	com.xiaomi.discover	Xiaomi Updater	f	1	t	10056	f	app	\N	\N	f	f	\N
58	com.google.android.permissioncontroller	Permission Controller	f	1	t	10057	f	app	\N	\N	f	f	\N
59	com.samsung.accessibility	Samsung Accessibility	f	1	t	10058	f	app	\N	\N	f	f	\N
60	com.android.updater	System Update Service	f	1	t	10059	f	app	\N	\N	f	f	\N
61	com.android.printspooler	Print Service	f	1	t	10060	f	app	\N	\N	f	f	\N
62	com.google.android.documentsui	File Manager Extension (Google)	f	1	t	10061	f	app	\N	\N	f	f	\N
63	com.google.android.contacts	Contacts (Google)	t	1	t	10062	f	app	\N	\N	f	f	\N
64	com.google.android.dialer	Dialer (Google)	t	1	t	10063	f	app	\N	\N	f	f	\N
65	com.samsung.android.app.notes	Samsung Notes	t	1	t	10064	f	app	\N	\N	f	f	\N
66	com.hmdglobal.camera2	Nokia Camera (new)	t	1	t	10065	f	app	\N	\N	f	f	\N
67	com.hmdglobal.app.camera	Nokia Camera	t	1	t	10066	f	app	\N	\N	f	f	\N
68	com.samsung.android.dialer	Samsung Dialer	t	1	t	10067	f	app	\N	\N	f	f	\N
69	com.samsung.android.app.contacts	Samsung Contacts	t	1	t	10068	f	app	\N	\N	f	f	\N
70	com.samsung.android.messaging	Samsung Messaging	f	1	t	10069	f	app	\N	\N	f	f	\N
72	com.google.android.apps.photos	Photos (Google)	t	1	t	10071	f	app	\N	\N	f	f	\N
73	com.google.android.apps.nbu.files	File Manager (Google)	t	1	t	10072	f	app	\N	\N	f	f	\N
74	com.android.settings.intelligence	Samsung Search Settings	f	1	t	10073	f	app	\N	\N	f	f	\N
75	com.huawei.bluetooth	Huawei Bluetooth	f	1	t	10074	f	app	\N	\N	f	f	\N
71	com.sec.android.app.launcher	Samsung Launcher (for Recents)	f	1	t	10070	f	app	\N	\N	f	f	\N
76	com.google.android.gms.setup	Google Services Setup	f	1	t	10075	f	app	\N	\N	f	f	\N
77	com.samsung.android.app.telephonyui	Samsung Telephony	f	1	t	10076	f	app	\N	\N	f	f	\N
88	com.hwmdm.webfilter	HWMDM Web Filter	f	1	f	10140	f	app	\N	\N	t	f	\N
87	com.hwmdm.remote	Suporte Remoto	t	1	f	10127	t	app	\N	\N	t	f	\N
46	com.hmdm.launcher	Headwind MDM	f	1	f	10045	f	app	\N	\N	f	f	\N
48	com.hmdm.pager	Headwind MDM Pager Plugin	t	1	f	10047	f	app	\N	\N	f	f	\N
49	com.hmdm.phoneproxy	Dialer Helper	t	1	f	10048	f	app	\N	\N	f	f	\N
50	com.hmdm.emuilauncherrestarter	Headwind MDM update helper	f	1	f	10049	f	app	\N	\N	f	f	\N
86	com.whatsapp	WhatsApp	t	1	f	10092	f	app	\N	\N	f	f	\N
\.


--
-- Data for Name: applicationversions; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.applicationversions (id, applicationid, version, url, apkhash, split, urlarmeabi, urlarm64, versioncode, signaturehash) FROM stdin;
10000	1	0	\N	\N	f	\N	\N	0	\N
10001	2	0	\N	\N	f	\N	\N	0	\N
10002	3	0	\N	\N	f	\N	\N	0	\N
10003	4	0	\N	\N	f	\N	\N	0	\N
10004	5	0	\N	\N	f	\N	\N	0	\N
10005	6	0	\N	\N	f	\N	\N	0	\N
10007	8	0	\N	\N	f	\N	\N	0	\N
10008	9	0	\N	\N	f	\N	\N	0	\N
10009	10	0	\N	\N	f	\N	\N	0	\N
10010	11	0	\N	\N	f	\N	\N	0	\N
10011	12	0	\N	\N	f	\N	\N	0	\N
10012	13	0	\N	\N	f	\N	\N	0	\N
10013	14	0	\N	\N	f	\N	\N	0	\N
10014	15	0	\N	\N	f	\N	\N	0	\N
10015	16	0	\N	\N	f	\N	\N	0	\N
10016	17	0	\N	\N	f	\N	\N	0	\N
10017	18	0	\N	\N	f	\N	\N	0	\N
10018	19	0	\N	\N	f	\N	\N	0	\N
10019	20	0	\N	\N	f	\N	\N	0	\N
10020	21	0	\N	\N	f	\N	\N	0	\N
10021	22	0	\N	\N	f	\N	\N	0	\N
10022	23	0	\N	\N	f	\N	\N	0	\N
10023	24	0	\N	\N	f	\N	\N	0	\N
10024	25	0	\N	\N	f	\N	\N	0	\N
10025	26	0	\N	\N	f	\N	\N	0	\N
10026	27	0	\N	\N	f	\N	\N	0	\N
10027	28	0	\N	\N	f	\N	\N	0	\N
10028	29	0	\N	\N	f	\N	\N	0	\N
10029	30	0	\N	\N	f	\N	\N	0	\N
10030	31	0	\N	\N	f	\N	\N	0	\N
10031	32	0	\N	\N	f	\N	\N	0	\N
10032	33	0	\N	\N	f	\N	\N	0	\N
10033	34	0	\N	\N	f	\N	\N	0	\N
10034	35	0	\N	\N	f	\N	\N	0	\N
10035	36	0	\N	\N	f	\N	\N	0	\N
10036	37	0	\N	\N	f	\N	\N	0	\N
10037	38	0	\N	\N	f	\N	\N	0	\N
10038	39	0	\N	\N	f	\N	\N	0	\N
10039	40	0	\N	\N	f	\N	\N	0	\N
10040	41	0	\N	\N	f	\N	\N	0	\N
10041	42	0	\N	\N	f	\N	\N	0	\N
10042	43	0	\N	\N	f	\N	\N	0	\N
10046	47	0	\N	\N	f	\N	\N	0	\N
10050	51	0	\N	\N	f	\N	\N	0	\N
10051	52	0	\N	\N	f	\N	\N	0	\N
10052	53	0	\N	\N	f	\N	\N	0	\N
10053	54	0	\N	\N	f	\N	\N	0	\N
10054	55	0	\N	\N	f	\N	\N	0	\N
10055	56	0	\N	\N	f	\N	\N	0	\N
10056	57	0	\N	\N	f	\N	\N	0	\N
10057	58	0	\N	\N	f	\N	\N	0	\N
10058	59	0	\N	\N	f	\N	\N	0	\N
10059	60	0	\N	\N	f	\N	\N	0	\N
10060	61	0	\N	\N	f	\N	\N	0	\N
10061	62	0	\N	\N	f	\N	\N	0	\N
10062	63	0	\N	\N	f	\N	\N	0	\N
10063	64	0	\N	\N	f	\N	\N	0	\N
10064	65	0	\N	\N	f	\N	\N	0	\N
10065	66	0	\N	\N	f	\N	\N	0	\N
10066	67	0	\N	\N	f	\N	\N	0	\N
10067	68	0	\N	\N	f	\N	\N	0	\N
10068	69	0	\N	\N	f	\N	\N	0	\N
10069	70	0	\N	\N	f	\N	\N	0	\N
10070	71	0	\N	\N	f	\N	\N	0	\N
10071	72	0	\N	\N	f	\N	\N	0	\N
10072	73	0	\N	\N	f	\N	\N	0	\N
10073	74	0	\N	\N	f	\N	\N	0	\N
10074	75	0	\N	\N	f	\N	\N	0	\N
10075	76	0	\N	\N	f	\N	\N	0	\N
10076	77	0	\N	\N	f	\N	\N	0	\N
10116	87	1.22	http://172.16.24.70:8080/files/hwmdm-remote-1.22.apk	yWTmXkwDJQoWvLB/WmmWxmV2JdsFI7p3zEzMA24/cgg=	f	\N	\N	23	\N
10117	87	1.27	http://172.16.24.70:8080/files/hwmdm-remote-1.27.apk	Breenlk7DnxKbygEiaF5nqKPjs78sPFFt0IOHK4l4z8=	f	\N	\N	27	\N
10047	48	1.02	http://172.16.24.70:8080/files/pager-1.02.apk	\N	f	\N	\N	0	\N
10045	46	6.36	http://172.16.24.70:8080/files/hmdm-6.36-os.apk	aNlmR0NqQ2Dl4W7VXXuQWTSnzg40svKKpz2Ii4y433s=	f	\N	\N	0	\N
10114	87	1.15	http://172.16.24.70:8080/files/hwmdm-remote-1.15.apk	ejZ+YF+LbkppofUjVBSlyKNwSOL42Bb/i+ZSC49OhKk	f	\N	\N	16	\N
10115	87	1.17	http://172.16.24.70:8080/files/hwmdm-remote-1.17.apk	XM/cpp5JJOKzVB0NkaH9AP77X+ceH/4XMcXiTAYOveQ=	f	\N	\N	18	\N
10048	49	1.02	http://172.16.24.70:8080/files/phoneproxy-1.02.apk	\N	f	\N	\N	0	\N
10049	50	1.04	http://172.16.24.70:8080/files/LauncherRestarter-1.04.apk	\N	f	\N	\N	0	\N
10102	87	1.3	http://172.16.24.70:8080/files/hwmdm-remote-1.3.apk	fTOWfYkSzgBndhfyav8mLU6eJyg19T0DOTuOfHrUG4U	f	\N	\N	0	\N
10103	87	1.4	http://172.16.24.70:8080/files/hwmdm-remote-1.4.apk	w8F42l7BvCeWw1t9F++oA8lFisKvnNovwwGjssc34Mw	f	\N	\N	5	\N
10104	87	1.5	http://172.16.24.70:8080/files/hwmdm-remote-1.5.apk	iLED381c+YCo5UIajQPlx+kQRPelU0CET29Oj49AyBM	f	\N	\N	6	\N
10092	86	2.26.29.71	http://172.16.24.70:8080/files/WhatsApp.apk	\N	f	\N	\N	262907130	\N
10105	87	1.6	http://172.16.24.70:8080/files/hwmdm-remote-1.6.apk	VulxLaiyWWhCEM7rm2tdB9d0NpDyUHPKul0FaKdQohU	f	\N	\N	7	\N
10106	87	1.7	http://172.16.24.70:8080/files/hwmdm-remote-1.7.apk	pvp4UTk/0py5P+/JsWIRlv05iw7VgESmIYxXCkc+Kmk	f	\N	\N	8	\N
10107	87	1.8	http://172.16.24.70:8080/files/hwmdm-remote-1.8.apk	s+gHfyLxTAMzqnthya/PLLxQ4AsCOp2DSXHmixiplAI	f	\N	\N	9	\N
10108	87	1.9	http://172.16.24.70:8080/files/hwmdm-remote-1.9.apk	tQ8z/EAavdYpInOeGTuB3Ac/y5geUWBUdUXHds2Fda4	f	\N	\N	10	\N
10109	87	1.10	http://172.16.24.70:8080/files/hwmdm-remote-1.10.apk	SMXyxa12zTB9IMdaXnO71JWMOpLNDJofchcK90mPe1Q	f	\N	\N	11	\N
10110	87	1.11	http://172.16.24.70:8080/files/hwmdm-remote-1.11.apk	jXzRAAwavptrVlxFHDrY1s2+iq6y0cokd4gu3LJWyYg	f	\N	\N	12	\N
10111	87	1.12	http://172.16.24.70:8080/files/hwmdm-remote-1.12.apk	tAJysfJ3DYSdwP2OrEa9Yd6RTiGvpO4xjbqhYC+P+MI	f	\N	\N	13	\N
10112	87	1.13	http://172.16.24.70:8080/files/hwmdm-remote-1.13.apk	FpgS+aeKQgdGDyXTUflEr76YyvJvKsEhcA/cdVp5Mug	f	\N	\N	14	\N
10113	87	1.14	http://172.16.24.70:8080/files/hwmdm-remote-1.14.apk	IgxFToQDmDGSzv8CQJG7tGLFuyu5y9Ds4gsiQ3+kfeI	f	\N	\N	15	\N
10119	87	1.28	http://172.16.24.70:8080/files/hwmdm-remote-1.28.apk	\N	f	\N	\N	28	\N
10120	87	1.29	http://172.16.24.70:8080/files/hwmdm-remote-1.29.apk	\N	f	\N	\N	29	\N
10122	87	1.30	http://172.16.24.70:8080/files/hwmdm-remote-1.30.apk	\N	f	\N	\N	30	\N
10123	87	1.31	http://172.16.24.70:8080/files/hwmdm-remote-1.31.apk	\N	f	\N	\N	31	\N
10124	87	1.32	http://172.16.24.70:8080/files/hwmdm-remote-1.32.apk	\N	f	\N	\N	32	\N
10125	87	1.33	http://172.16.24.70:8080/files/hwmdm-remote-1.33.apk	\N	f	\N	\N	33	\N
10126	87	1.34	http://172.16.24.70:8080/files/hwmdm-remote-1.34.apk	\N	f	\N	\N	34	\N
10127	87	1.35	http://172.16.24.70:8080/files/hwmdm-remote-1.35.apk	\N	f	\N	\N	35	\N
10136	88	1.0	http://172.16.24.70:8080/files/hwmdm-webfilter-1.0.apk	\N	f	\N	\N	1	\N
10137	88	1.1	http://172.16.24.70:8080/files/hwmdm-webfilter-1.1.apk	\N	f	\N	\N	2	\N
10138	88	1.2	http://172.16.24.70:8080/files/hwmdm-webfilter-1.2.apk	\N	f	\N	\N	3	\N
10129	46	1.3	http://172.16.24.70:8080/files/hmdm-v1.3.apk	\N	f	\N	\N	0	RDcvFA0dZMa1E2Uk9cysA5EXLKM0jQkhhZhIbjx61RA=
10139	88	1.3	http://172.16.24.70:8080/files/app-release.apk	\N	f	\N	\N	4	\N
10140	88	1.4	http://172.16.24.70:8080/files/app-release.apk	\N	f	\N	\N	5	\N
\.


--
-- Data for Name: configurationapplications; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.configurationapplications (id, configurationid, applicationid, remove, showicon, applicationversionid, action, screenorder, keycode, bottom, longtap, usekiosk) FROM stdin;
48	2	8	f	t	10007	1	\N	\N	f	f	f
49	2	37	f	f	10036	1	\N	\N	f	f	f
50	2	2	f	f	10001	1	\N	\N	f	f	f
51	2	21	f	t	10020	1	\N	\N	f	f	f
52	2	9	f	t	10008	1	\N	\N	f	f	f
53	2	29	f	t	10028	1	\N	\N	f	f	f
54	2	34	f	t	10033	1	\N	\N	f	f	f
55	2	35	f	f	10034	1	\N	\N	f	f	f
56	2	13	f	f	10012	1	\N	\N	f	f	f
57	2	6	f	f	10005	1	\N	\N	f	f	f
58	2	4	f	f	10003	1	\N	\N	f	f	f
59	2	27	f	f	10026	1	\N	\N	f	f	f
60	2	15	f	f	10014	1	\N	\N	f	f	f
61	2	3	f	f	10002	1	\N	\N	f	f	f
62	2	12	f	f	10011	1	\N	\N	f	f	f
63	2	38	f	t	10037	1	\N	\N	f	f	f
64	2	39	f	t	10038	1	\N	\N	f	f	f
65	2	5	f	f	10004	1	\N	\N	f	f	f
66	2	17	f	f	10016	1	\N	\N	f	f	f
67	2	42	f	t	10041	1	\N	\N	f	f	f
68	2	40	f	t	10039	1	\N	\N	f	f	f
69	2	28	f	f	10027	1	\N	\N	f	f	f
70	2	1	f	f	10000	1	\N	\N	f	f	f
72	2	48	f	t	10047	1	\N	\N	f	f	f
73	2	49	f	t	10048	1	\N	\N	f	f	f
74	2	50	f	f	10049	1	\N	\N	f	f	f
75	2	51	f	f	10050	1	\N	\N	f	f	f
76	2	52	f	f	10051	1	\N	\N	f	f	f
77	2	53	f	f	10052	1	\N	\N	f	f	f
78	2	54	f	f	10053	1	\N	\N	f	f	f
79	2	55	f	f	10054	1	\N	\N	f	f	f
80	2	56	f	f	10055	1	\N	\N	f	f	f
81	2	57	f	f	10056	1	\N	\N	f	f	f
795	1	9	f	t	10008	1	\N	\N	f	f	f
796	1	21	f	t	10020	1	\N	\N	f	f	f
797	1	68	f	t	10067	1	\N	\N	f	f	f
798	1	69	f	t	10068	1	\N	\N	f	f	f
800	1	48	f	f	10047	1	\N	\N	f	f	f
801	1	49	f	f	10048	1	\N	\N	f	f	f
802	1	50	f	f	10049	1	\N	\N	f	f	f
804	1	46	f	f	10045	1	\N	\N	f	f	f
1764	11	88	f	f	10140	1	\N	\N	f	f	f
1652	56	9	f	t	10008	1	\N	\N	f	f	f
1531	57	9	f	t	10008	1	\N	\N	f	f	f
1760	11	9	f	t	10008	1	\N	\N	f	f	f
1762	11	46	f	f	10129	1	\N	\N	f	f	f
1798	60	9	f	t	10008	1	\N	\N	f	f	f
1799	60	46	f	f	10045	1	\N	\N	f	f	f
1801	60	88	f	f	10140	1	\N	\N	f	f	f
850	1	87	f	t	10102	0	\N	\N	f	f	f
852	2	87	f	t	10102	0	\N	\N	f	f	f
1654	56	87	f	t	10102	1	\N	\N	f	f	f
1533	57	87	f	t	10102	1	\N	\N	f	f	f
1761	11	87	f	t	10102	1	\N	\N	f	f	f
1800	60	87	f	f	10102	1	\N	\N	f	f	f
\.


--
-- Name: applications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.applications_id_seq', 88, true);


--
-- Name: applicationversions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.applicationversions_id_seq', 10140, true);


--
-- Name: configurationapplications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.configurationapplications_id_seq', 1801, true);


--
-- PostgreSQL database dump complete
--

