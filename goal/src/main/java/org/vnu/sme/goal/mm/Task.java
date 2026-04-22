package org.vnu.sme.goal.mm;

public class Task extends IntentionalElement {
    private String preExpression;
    private String postExpression;
    
    public Task(String name) { super(name); }
    
    public String getPreExpression() { return preExpression; }
    public void setPreExpression(String preExpression) { this.preExpression = preExpression; }
    
    public String getPostExpression() { return postExpression; }
    public void setPostExpression(String postExpression) { this.postExpression = postExpression; }
    
    @Override public String getType() { return "Task"; }
}