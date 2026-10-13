package com.hmdm.plugins.webfilter.rest.json;

/**
 * <p>Web filter settings of the customer as shown in the console.</p>
 */
public class SettingsView {

    private String dnsDomain;
    private String blockPageTitle;
    private String blockPageMessage;
    private String blockPageLogoUrl;
    private String blockPageSupportText;
    private String blockPageCustomHtml;
    private String blockPageCustomCss;

    public String getDnsDomain() { return dnsDomain; }
    public void setDnsDomain(String dnsDomain) { this.dnsDomain = dnsDomain; }
    public String getBlockPageTitle() { return blockPageTitle; }
    public void setBlockPageTitle(String blockPageTitle) { this.blockPageTitle = blockPageTitle; }
    public String getBlockPageMessage() { return blockPageMessage; }
    public void setBlockPageMessage(String blockPageMessage) { this.blockPageMessage = blockPageMessage; }
    public String getBlockPageLogoUrl() { return blockPageLogoUrl; }
    public void setBlockPageLogoUrl(String blockPageLogoUrl) { this.blockPageLogoUrl = blockPageLogoUrl; }
    public String getBlockPageSupportText() { return blockPageSupportText; }
    public void setBlockPageSupportText(String blockPageSupportText) { this.blockPageSupportText = blockPageSupportText; }
    public String getBlockPageCustomHtml() { return blockPageCustomHtml; }
    public void setBlockPageCustomHtml(String blockPageCustomHtml) { this.blockPageCustomHtml = blockPageCustomHtml; }
    public String getBlockPageCustomCss() { return blockPageCustomCss; }
    public void setBlockPageCustomCss(String blockPageCustomCss) { this.blockPageCustomCss = blockPageCustomCss; }
}
