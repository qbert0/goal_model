package org.vnu.sme.goal.mm;

public class Contribution {
    private String name;
    private String description;
    private ContributionType con;
    private IntentionalElement source;
    private IntentionalElement target;

    public Contribution(String name) {
        this.name = name;
        this.con = ContributionType.UNKNOWN;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContributionType getCon() {
        return con;
    }

    public void setCon(ContributionType con) {
        this.con = con == null ? ContributionType.UNKNOWN : con;
    }

    public IntentionalElement getSource() {
        return source;
    }

    public void setSource(IntentionalElement source) {
        this.source = source;
    }

    public IntentionalElement getTarget() {
        return target;
    }

    public void setTarget(IntentionalElement target) {
        this.target = target;
    }

    public String getType() {
        return "Contribution";
    }

    public ContributionType getContributionType() {
        return getCon();
    }

    public void setContributionType(ContributionType contributionType) {
        setCon(contributionType);
    }
}
