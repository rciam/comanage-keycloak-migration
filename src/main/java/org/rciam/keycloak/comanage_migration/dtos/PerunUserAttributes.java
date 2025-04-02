package org.rciam.keycloak.comanage_migration.dtos;

import java.util.List;

public class PerunUserAttributes {

    public PerunUserAttributes(){}

    private String givenName;
    private String sn;
    private String mail;
    private String preferredMail;
    private List<String> eduPersonPrincipalNames;

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPreferredMail() {
        return preferredMail;
    }

    public void setPreferredMail(String preferredMail) {
        this.preferredMail = preferredMail;
    }

    public List<String> getEduPersonPrincipalNames() {
        return eduPersonPrincipalNames;
    }

    public void setEduPersonPrincipalNames(List<String> eduPersonPrincipalNames) {
        this.eduPersonPrincipalNames = eduPersonPrincipalNames;
    }
}
