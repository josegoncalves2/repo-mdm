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
10116	87	1.22	http://192.168.250.249:8080/files/hwmdm-remote-1.22.apk	yWTmXkwDJQoWvLB/WmmWxmV2JdsFI7p3zEzMA24/cgg=	f	\N	\N	23	\N
10117	87	1.27	http://192.168.250.249:8080/files/hwmdm-remote-1.27.apk	Breenlk7DnxKbygEiaF5nqKPjs78sPFFt0IOHK4l4z8=	f	\N	\N	27	\N
10047	48	1.02	http://192.168.250.249:8080/files/pager-1.02.apk	\N	f	\N	\N	0	\N
10045	46	6.36	http://192.168.250.249:8080/files/hmdm-6.36-os.apk	aNlmR0NqQ2Dl4W7VXXuQWTSnzg40svKKpz2Ii4y433s=	f	\N	\N	0	\N
10133	46	1.7	http://192.168.250.249:8080/files/hmdm-1.7-olimpia.apk	xurdPDTNwSDIUG7lS7h-63cbLZV5wnNY6_xZUWBDqf4=	f	\N	\N	15387	RDcvFA0dZMa1E2Uk9cysA5EXLKM0jQkhhZhIbjx61RA=
10114	87	1.15	http://192.168.250.249:8080/files/hwmdm-remote-1.15.apk	ejZ+YF+LbkppofUjVBSlyKNwSOL42Bb/i+ZSC49OhKk	f	\N	\N	16	\N
10115	87	1.17	http://192.168.250.249:8080/files/hwmdm-remote-1.17.apk	XM/cpp5JJOKzVB0NkaH9AP77X+ceH/4XMcXiTAYOveQ=	f	\N	\N	18	\N
10048	49	1.02	http://192.168.250.249:8080/files/phoneproxy-1.02.apk	\N	f	\N	\N	0	\N
10049	50	1.04	http://192.168.250.249:8080/files/LauncherRestarter-1.04.apk	\N	f	\N	\N	0	\N
10102	87	1.3	http://192.168.250.249:8080/files/hwmdm-remote-1.3.apk	fTOWfYkSzgBndhfyav8mLU6eJyg19T0DOTuOfHrUG4U	f	\N	\N	0	\N
10103	87	1.4	http://192.168.250.249:8080/files/hwmdm-remote-1.4.apk	w8F42l7BvCeWw1t9F++oA8lFisKvnNovwwGjssc34Mw	f	\N	\N	5	\N
10104	87	1.5	http://192.168.250.249:8080/files/hwmdm-remote-1.5.apk	iLED381c+YCo5UIajQPlx+kQRPelU0CET29Oj49AyBM	f	\N	\N	6	\N
10092	86	2.26.29.71	http://192.168.250.249:8080/files/WhatsApp.apk	\N	f	\N	\N	262907130	\N
10105	87	1.6	http://192.168.250.249:8080/files/hwmdm-remote-1.6.apk	VulxLaiyWWhCEM7rm2tdB9d0NpDyUHPKul0FaKdQohU	f	\N	\N	7	\N
10106	87	1.7	http://192.168.250.249:8080/files/hwmdm-remote-1.7.apk	pvp4UTk/0py5P+/JsWIRlv05iw7VgESmIYxXCkc+Kmk	f	\N	\N	8	\N
10107	87	1.8	http://192.168.250.249:8080/files/hwmdm-remote-1.8.apk	s+gHfyLxTAMzqnthya/PLLxQ4AsCOp2DSXHmixiplAI	f	\N	\N	9	\N
10108	87	1.9	http://192.168.250.249:8080/files/hwmdm-remote-1.9.apk	tQ8z/EAavdYpInOeGTuB3Ac/y5geUWBUdUXHds2Fda4	f	\N	\N	10	\N
10109	87	1.10	http://192.168.250.249:8080/files/hwmdm-remote-1.10.apk	SMXyxa12zTB9IMdaXnO71JWMOpLNDJofchcK90mPe1Q	f	\N	\N	11	\N
10110	87	1.11	http://192.168.250.249:8080/files/hwmdm-remote-1.11.apk	jXzRAAwavptrVlxFHDrY1s2+iq6y0cokd4gu3LJWyYg	f	\N	\N	12	\N
10111	87	1.12	http://192.168.250.249:8080/files/hwmdm-remote-1.12.apk	tAJysfJ3DYSdwP2OrEa9Yd6RTiGvpO4xjbqhYC+P+MI	f	\N	\N	13	\N
10112	87	1.13	http://192.168.250.249:8080/files/hwmdm-remote-1.13.apk	FpgS+aeKQgdGDyXTUflEr76YyvJvKsEhcA/cdVp5Mug	f	\N	\N	14	\N
10113	87	1.14	http://192.168.250.249:8080/files/hwmdm-remote-1.14.apk	IgxFToQDmDGSzv8CQJG7tGLFuyu5y9Ds4gsiQ3+kfeI	f	\N	\N	15	\N
10119	87	1.28	http://192.168.250.249:8080/files/hwmdm-remote-1.28.apk	\N	f	\N	\N	28	\N
10120	87	1.29	http://192.168.250.249:8080/files/hwmdm-remote-1.29.apk	\N	f	\N	\N	29	\N
10122	87	1.30	http://192.168.250.249:8080/files/hwmdm-remote-1.30.apk	\N	f	\N	\N	30	\N
10123	87	1.31	http://192.168.250.249:8080/files/hwmdm-remote-1.31.apk	\N	f	\N	\N	31	\N
10124	87	1.32	http://192.168.250.249:8080/files/hwmdm-remote-1.32.apk	\N	f	\N	\N	32	\N
10125	87	1.33	http://192.168.250.249:8080/files/hwmdm-remote-1.33.apk	\N	f	\N	\N	33	\N
10126	87	1.34	http://192.168.250.249:8080/files/hwmdm-remote-1.34.apk	\N	f	\N	\N	34	\N
10127	87	1.35	http://192.168.250.249:8080/files/hwmdm-remote-1.35.apk	\N	f	\N	\N	35	\N
10130	87	1.36	http://192.168.250.249:8080/files/hwmdm-remote-1.36.apk	\N	f	\N	\N	36	\N
10134	46	1.9	http://192.168.250.249:8080/files/hmdm-1.9-olimpia.apk	qCg4-A26ES7J7aNi_9wVuBRThmAa_80rn48os89XJmI	f	\N	\N	15399	\N
10135	46	1.10	http://192.168.250.249:8080/files/hmdm-1.10-olimpia.apk	XHXyxuhbO0_XihmTlk5t57Z_Cv1PhEr2wSBeS_JHM1c	f	\N	\N	15400	\N
10128	46	1.2	http://192.168.250.249:8080/files/hmdm-1.2-olimpia.apk	+spAe9iTTVewZp3TvKJPAAH5UdAwo41WxSUyl9lKTfY=	f	\N	\N	15381	RDcvFA0dZMa1E2Uk9cysA5EXLKM0jQkhhZhIbjx61RA=
10136	88	1.0	http://192.168.250.249:8080/files/hwmdm-webfilter-1.0.apk	\N	f	\N	\N	1	\N
10137	88	1.1	http://192.168.250.249:8080/files/hwmdm-webfilter-1.1.apk	\N	f	\N	\N	2	\N
10138	88	1.2	http://192.168.250.249:8080/files/hwmdm-webfilter-1.2.apk	\N	f	\N	\N	3	\N
10129	46	1.3	http://192.168.250.249:8080/files/hmdm-v1.3.apk	\N	f	\N	\N	0	RDcvFA0dZMa1E2Uk9cysA5EXLKM0jQkhhZhIbjx61RA=
10139	88	1.3	http://192.168.250.249:8080/files/app-release.apk	\N	f	\N	\N	4	\N
10140	88	1.4	http://192.168.250.249:8080/files/app-release.apk	\N	f	\N	\N	5	\N
\.


--
-- Name: applicationversions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.applicationversions_id_seq', 10140, true);


--
-- PostgreSQL database dump complete
--

