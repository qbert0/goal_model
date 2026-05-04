package org.vnu.sme.goal.parser.bpmn;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.vnu.sme.goal.ast.bpmn.BpmnModelCS;

public final class BpmnCompiler {
    private BpmnCompiler() {
    }

    public static BpmnModelCS compileSpecification(String fileName, PrintWriter logWriter) {
        try {
            BPMNLexer lexer = new BPMNLexer(CharStreams.fromPath(Path.of(fileName)));
            BPMNParser parser = new BPMNParser(new CommonTokenStream(lexer));
            ThrowingErrorListener errorListener = new ThrowingErrorListener();
            lexer.removeErrorListeners();
            parser.removeErrorListeners();
            lexer.addErrorListener(errorListener);
            parser.addErrorListener(errorListener);

            BPMNParser.BpmnModelContext tree = parser.bpmnModel();
            return new BpmnAstBuilder().build(tree);
        } catch (IOException | BpmnParseException ex) {
            if (logWriter != null) {
                logWriter.println("[BPMN] " + ex.getMessage());
                logWriter.flush();
            }
            return null;
        }
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new BpmnParseException("Line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}
