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
-- Name: configurationapplications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.configurationapplications_id_seq', 1801, true);


--
-- PostgreSQL database dump complete
--

