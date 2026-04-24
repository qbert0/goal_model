package org.vnu.sme.goal.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystemException;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.mm.GoalModel;

public class GOALCompiler {

    private GOALCompiler() {
    }

    public static GoalModel compileSpecification(String inName, PrintWriter err, MModel model)
            throws MSystemException, FileNotFoundException {

        try (InputStream inStream = new FileInputStream(inName)) {
            CharStream input = CharStreams.fromStream(inStream);
            GOALLexer lexer = new GOALLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener());  // TODO -> tách abstract

            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            GOALParser parser = new GOALParser(tokenStream);
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener());// TODO

            ParseTree tree = parser.goalModel();
            GoalModelCS ast = (GoalModelCS) new GoalAstBuilder().visit(tree);
            GoalModel goalModel = new GoalModelFactory().create(ast);

            err.println("Compiled GoalModel '" + goalModel.getName() + "' with "
                    + goalModel.getActors().size() + " actors and "
                    + goalModel.getDependencies().size() + " dependencies.");
            return goalModel;
        } catch (ParseCancellationException e) {
            err.println("GOAL syntax error: " + e.getMessage());
        } catch (IOException e) {
            err.println("Could not read GOAL file: " + e.getMessage());
        } catch (RuntimeException e) {
            err.println("Could not build GOAL model: " + e.getMessage());
            e.printStackTrace(err);
        } finally {
            err.flush();
        }

        return null;
    }

    private static BaseErrorListener errorListener() {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
            }
        };
    }
}
