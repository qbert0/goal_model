package org.vnu.sme.goal.parser.bpmn;

import org.vnu.sme.goal.ast.bpmn.BpmnCollaborationCS;
import org.vnu.sme.goal.ast.bpmn.BpmnEndpointRefCS;
import org.vnu.sme.goal.ast.bpmn.BpmnEndEventCS;
import org.vnu.sme.goal.ast.bpmn.BpmnExclusiveGatewayCS;
import org.vnu.sme.goal.ast.bpmn.BpmnFlowNodeCS;
import org.vnu.sme.goal.ast.bpmn.BpmnMessageFlowCS;
import org.vnu.sme.goal.ast.bpmn.BpmnModelCS;
import org.vnu.sme.goal.ast.bpmn.BpmnParallelGatewayCS;
import org.vnu.sme.goal.ast.bpmn.BpmnParticipantCS;
import org.vnu.sme.goal.ast.bpmn.BpmnProcessCS;
import org.vnu.sme.goal.ast.bpmn.BpmnSequenceFlowCS;
import org.vnu.sme.goal.ast.bpmn.BpmnStartEventCS;
import org.vnu.sme.goal.ast.bpmn.BpmnTaskCS;

final class BpmnAstBuilder extends BPMNBaseVisitor<Object> {
    BpmnModelCS build(BPMNParser.BpmnModelContext ctx) {
        return (BpmnModelCS) visit(ctx);
    }

    @Override
    public Object visitBpmnModel(BPMNParser.BpmnModelContext ctx) {
        BpmnModelCS model = new BpmnModelCS(ctx.modelName.getText());
        model.setCollaboration((BpmnCollaborationCS) visit(ctx.collaborationDecl()));
        for (BPMNParser.ProcessDeclContext processDecl : ctx.processDecl()) {
            model.addProcess((BpmnProcessCS) visit(processDecl));
        }
        return model;
    }

    @Override
    public Object visitCollaborationDecl(BPMNParser.CollaborationDeclContext ctx) {
        BpmnCollaborationCS collaboration = new BpmnCollaborationCS(ctx.name.getText());
        for (BPMNParser.ParticipantDeclContext participantDecl : ctx.participantDecl()) {
            collaboration.addParticipant((BpmnParticipantCS) visit(participantDecl));
        }
        for (BPMNParser.MessageFlowDeclContext messageFlowDecl : ctx.messageFlowDecl()) {
            collaboration.addMessageFlow((BpmnMessageFlowCS) visit(messageFlowDecl));
        }
        return collaboration;
    }

    @Override
    public Object visitParticipantDecl(BPMNParser.ParticipantDeclContext ctx) {
        return new BpmnParticipantCS(ctx.participantName.getText(), ctx.processRef.getText());
    }

    @Override
    public Object visitMessageFlowDecl(BPMNParser.MessageFlowDeclContext ctx) {
        String name = ctx.flowName == null
                ? autoName("messageFlow", ctx.source, ctx.target)
                : ctx.flowName.getText();
        return new BpmnMessageFlowCS(
                name,
                (BpmnEndpointRefCS) visit(ctx.source),
                (BpmnEndpointRefCS) visit(ctx.target));
    }

    @Override
    public Object visitEndpointRef(BPMNParser.EndpointRefContext ctx) {
        return new BpmnEndpointRefCS(ctx.participantName.getText(), ctx.nodeName.getText());
    }

    @Override
    public Object visitProcessDecl(BPMNParser.ProcessDeclContext ctx) {
        BpmnProcessCS process = new BpmnProcessCS(ctx.processName.getText());
        for (BPMNParser.FlowNodeDeclContext flowNodeDecl : ctx.flowNodeDecl()) {
            process.addFlowNode((BpmnFlowNodeCS) visit(flowNodeDecl));
        }
        for (BPMNParser.SequenceFlowDeclContext sequenceFlowDecl : ctx.sequenceFlowDecl()) {
            process.addSequenceFlow((BpmnSequenceFlowCS) visit(sequenceFlowDecl));
        }
        return process;
    }

    @Override
    public Object visitFlowNodeDecl(BPMNParser.FlowNodeDeclContext ctx) {
        if (ctx.startEventDecl() != null) {
            return visit(ctx.startEventDecl());
        }
        if (ctx.endEventDecl() != null) {
            return visit(ctx.endEventDecl());
        }
        if (ctx.taskDecl() != null) {
            return visit(ctx.taskDecl());
        }
        if (ctx.exclusiveGatewayDecl() != null) {
            return visit(ctx.exclusiveGatewayDecl());
        }
        return visit(ctx.parallelGatewayDecl());
    }

    @Override
    public Object visitStartEventDecl(BPMNParser.StartEventDeclContext ctx) {
        return new BpmnStartEventCS(ctx.nodeName.getText());
    }

    @Override
    public Object visitEndEventDecl(BPMNParser.EndEventDeclContext ctx) {
        return new BpmnEndEventCS(ctx.nodeName.getText());
    }

    @Override
    public Object visitTaskDecl(BPMNParser.TaskDeclContext ctx) {
        return new BpmnTaskCS(ctx.nodeName.getText());
    }

    @Override
    public Object visitExclusiveGatewayDecl(BPMNParser.ExclusiveGatewayDeclContext ctx) {
        return new BpmnExclusiveGatewayCS(ctx.nodeName.getText());
    }

    @Override
    public Object visitParallelGatewayDecl(BPMNParser.ParallelGatewayDeclContext ctx) {
        return new BpmnParallelGatewayCS(ctx.nodeName.getText());
    }

    @Override
    public Object visitSequenceFlowDecl(BPMNParser.SequenceFlowDeclContext ctx) {
        String name = ctx.flowName == null
                ? autoName("sequenceFlow", ctx.sourceName.getText(), ctx.targetName.getText())
                : ctx.flowName.getText();
        return new BpmnSequenceFlowCS(name, ctx.sourceName.getText(), ctx.targetName.getText());
    }

    private String autoName(String prefix, BPMNParser.EndpointRefContext source, BPMNParser.EndpointRefContext target) {
        return prefix + "_" + source.getText() + "_to_" + target.getText();
    }

    private String autoName(String prefix, String source, String target) {
        return prefix + "_" + source + "_to_" + target;
    }
}
