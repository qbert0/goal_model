package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vnu.sme.goal.mm.ocl.OpaqueExpression;

public class Task extends GoalTaskElement {
    private Pre pre;
    private Post post;
    private final List<Resource> neededResources;

    public Task(String name) {
        super(name);
        this.neededResources = new ArrayList<>();
    }

    public Pre getPre() {
        return pre;
    }

    public void setPre(Pre pre) {
        this.pre = pre;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public List<Resource> getNeededResources() {
        return Collections.unmodifiableList(neededResources);
    }

    public void addNeededResource(Resource resource) {
        if (resource != null && !neededResources.contains(resource)) {
            neededResources.add(resource);
            if (!resource.getNeededByTasks().contains(this)) {
                resource.addNeededByTask(this);
            }
        }
    }

    // Compatibility API
    public String getPreExpression() {
        return pre == null ? null : pre.getPrimaryText();
    }

    public void setPreExpression(String preExpression) {
        if (preExpression == null) {
            return;
        }
        if (pre == null) {
            pre = new Pre();
        }
        pre.addExpression(new OpaqueExpression(preExpression));
    }

    public String getPostExpression() {
        return post == null ? null : post.getPrimaryText();
    }

    public void setPostExpression(String postExpression) {
        if (postExpression == null) {
            return;
        }
        if (post == null) {
            post = new Post();
        }
        post.addExpression(new OpaqueExpression(postExpression));
    }

    @Override
    public String getType() {
        return "Task";
    }
}
