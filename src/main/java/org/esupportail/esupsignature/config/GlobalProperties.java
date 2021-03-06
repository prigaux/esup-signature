package org.esupportail.esupsignature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="global")
public class GlobalProperties {

    private String rootUrl;
    private String nexuUrl;
    private String nexuVersion;
    private String nexuDownloadUrl;
    private String hideWizard;
    private String hideAutoSign;
    private String hideSendSignRequest;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getNexuUrl() {
        return nexuUrl;
    }

    public void setNexuUrl(String nexuUrl) {
        this.nexuUrl = nexuUrl;
    }

    public String getNexuVersion() {
        return nexuVersion;
    }

    public void setNexuVersion(String nexuVersion) {
        this.nexuVersion = nexuVersion;
    }

    public String getNexuDownloadUrl() {
        return nexuDownloadUrl;
    }

    public void setNexuDownloadUrl(String nexuDownloadUrl) {
        this.nexuDownloadUrl = nexuDownloadUrl;
    }

    public String getHideWizard() {
        return hideWizard;
    }

    public void setHideWizard(String hideWizard) {
        this.hideWizard = hideWizard;
    }

    public String getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(String hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }

    public String getHideSendSignRequest() {
        return hideSendSignRequest;
    }

    public void setHideSendSignRequest(String hideSendSignRequest) {
        this.hideSendSignRequest = hideSendSignRequest;
    }
}
