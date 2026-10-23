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
-- Data for Name: plugin_webfilter_settings; Type: TABLE DATA; Schema: public; Owner: hmdm
--

COPY public.plugin_webfilter_settings (id, customerid, dnsdomain, blockpagetitle, blockpagemessage, blockpagelogourl, blockpagesupporttext, blockpagecustomhtml, blockpagecustomcss) FROM stdin;
1	1	mdm.pmeto.local	Acesso bloqueado	Esta página não está disponível neste dispositivo por política de segurança.	\N	Em caso de necessidade, solicite liberação ao administrador.	\N	.wf-block-card{border-top:4px solid #1998d5}.wf-block-title{color:#102a43}.wf-block-note{color:#5f6b7a}
\.


--
-- Name: plugin_webfilter_settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hmdm
--

SELECT pg_catalog.setval('public.plugin_webfilter_settings_id_seq', 4, true);


--
-- PostgreSQL database dump complete
--

