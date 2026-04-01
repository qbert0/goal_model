package org.vnu.sme.goal.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.tzi.use.parser.ParseErrorHandler;
//import org.tzi.use.uml.mm.MInvalidModelException;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystemException;
import org.vnu.sme.goal.ast.GoalModelCS;
//import org.vnu.sme.goal.mm.GoalModel;

public class GOALCompiler {

    private GOALCompiler() {
    }

    public static void compileSpecification(String inName, PrintWriter err, MModel model)
            throws MSystemException, FileNotFoundException {

        InputStream inStream = new FileInputStream(inName);
        ParseErrorHandler errHandler = new ParseErrorHandler(inName, err);

        try {
            CharStream input = CharStreams.fromStream(inStream);
            GOALLexer lexer = new GOALLexer(input);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            GOALParser parser = new GOALParser(tokenStream);

            // TODO: custom error listener if needed

//            GoalModelCS goalModelCS = parser.goalModelCS().result;
//
//            if (errHandler.errorCount() == 0) {
//                Context ctx = new Context(inName, err, null, new GoalModelFactory());
//                ctx.setModel(model);
//                return goalModelCS.visitPreOrder(ctx);
//            }

        } catch (IOException e) {
            err.println(e.getMessage());
//            return null;
        }
//        catch (MInvalidModelException e) {
//            e.printStackTrace();
//        }

        err.flush();
//        return null;
    }
}