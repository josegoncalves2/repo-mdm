package com.hmdm.rest.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class BaseIPFilter {

    private Logger logger = LoggerFactory.getLogger(BaseIPFilter.class);

    private static final Set<String> INFRASTRUCTURE_IPS = new HashSet<>(Arrays.asList(
        "10.0.17.106",  // proxy management IP
        "10.0.9.1",     // proxy management IP
        "10.1.1.1"      // tunnel IP
    ));

    public class Net {
        private byte[] value;
        private byte[] mask = new byte[4];

        public Net(String net) throws Exception {
            String[] s = net.split("/");
            if (s.length > 2) {
                throw new Exception("Wrong network description, should be at most one / sign");
            }
            value = InetAddress.getByName(s[0]).getAddress();
            int maskLength;
            if (s.length == 1) {
                // Single IP address
                maskLength = 32;
            } else {
                maskLength = Integer.parseInt(s[1]);
            }
            mask[0] = (byte)(maskLength < 8 ? (0xff << (8 - maskLength)) : 0xff);
            mask[1] = (byte)(maskLength < 16 ? (0xff << (16 - maskLength)) : 0xff);
            mask[2] = (byte)(maskLength < 24 ? (0xff << (24 - maskLength)) : 0xff);
            mask[3] = (byte)(maskLength < 32 ? (0xff << (32 - maskLength)) : 0xff);
        }

        public boolean match(byte[] ip) {
            if (ip.length != mask.length) {
                logger.warn("Wrong IP address length: " + ip.length);
                return false;
            }
            for (int i = 0; i < ip.length; i++) {
                if ((ip[i] & mask[i]) != value[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * <p>Whitelist of allowed networks/addresses</p>
     */
    private List<Net> whitelist;

    /**
     * <p>Empty whitelist means all IPs are allowed</p>
     */
    private boolean allAllowed = false;

    /**
     * <p>IP addresses of reverse proxies</p>
     */
    private String[] proxies;

    /**
     * <p>Header containing real IP address when a proxy is used</p>
     */
    private String ipHeader;

    public BaseIPFilter(String allowedNets, String proxyIps, String ipHeader) {
        if (null == allowedNets || "".equals(allowedNets)) {
            allAllowed = true;
        } else {
            whitelist = new LinkedList<Net>();
            String[] nets = allowedNets.split(",");
            for (String net : nets) {
                try {
                    whitelist.add(new Net(net));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
        this.ipHeader = !"".equals(ipHeader) ? ipHeader : "X-Real-IP";
        this.proxies = !"".equals(proxyIps) ? proxyIps.split(",") : new String[0];
    }

    public boolean match(HttpServletRequest request) {
        if (allAllowed) {
            return true;
        }
        return match(getRemoteAddr(request));
    }

    // For tests only, don't use this directly
    // because if a proxy is used, this will return false result
    public boolean match(String ipStr) {
        if (allAllowed) {
            return true;
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipStr);
        } catch (UnknownHostException e) {
            // We shouldn't be here!
            logger.warn(e.getMessage());
            // Attempt to hack? Let's block it!
            return false;
        }
        byte[] ipBytes = addr.getAddress();
        for (Net net : whitelist) {
            if (net.match(ipBytes)) {
                return true;
            }
        }
        logger.info(ipStr + " doesn't match the whitelist, blocked");
        return false;
    }

    public String getRemoteAddr(HttpServletRequest request) {
        boolean isFromProxy = false;
        if (request.getRemoteAddr().equals(request.getLocalAddr())) {
            isFromProxy = true;
        } else {
            for (String p : proxies) {
                if (request.getRemoteAddr().equals(p.trim())) {
                    isFromProxy = true;
                    break;
                }
            }
        }
        if (isFromProxy) {
            String forwardedIp = request.getHeader(ipHeader);
            if (forwardedIp != null) {
                return forwardedIp;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Parse X-Forwarded-For header and extract the first valid (non-infra) IP
     */
    public String parseForwardedIp(String forwardedHeader) {
        if (forwardedHeader == null || forwardedHeader.trim().isEmpty()) {
            return null;
        }
        String[] ips = forwardedHeader.split(",");
        for (String ip : ips) {
            String trimmed = ip.trim();
            if (!trimmed.isEmpty() && !"unknown".equalsIgnoreCase(trimmed)) {
                if (!isInfrastructureIp(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    /**
     * Check if an IP is a known infrastructure/proxy/tunnel IP
     */
    private boolean isInfrastructureIp(String ip) {
        return INFRASTRUCTURE_IPS.contains(ip);
    }

    /**
     * Get the operational (real device) IP, filtering out infrastructure IPs
     */
    public String getOperationalRemoteAddr(HttpServletRequest request) {
        String addr = getRemoteAddr(request);
        if (isInfrastructureIp(addr)) {
            logger.debug("Filtered infrastructure IP: " + addr);
            return null;
        }
        return addr;
    }
}
