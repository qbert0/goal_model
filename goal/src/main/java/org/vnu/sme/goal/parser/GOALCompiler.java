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
import org.tzi.use.parser.ParseErrorHandler;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystemException;

// Import các class Visitor của bạn (Cần mở comment và đảm bảo import đúng)
// import org.vnu.sme.goal.ast.GoalModelCS;
// import org.vnu.sme.goal.mm.GoalModel;

public class GOALCompiler {

    private GOALCompiler() {
    }

    public static void compileSpecification(String inName, PrintWriter err, MModel model)
            throws MSystemException, FileNotFoundException {

        InputStream inStream = new FileInputStream(inName);
        ParseErrorHandler errHandler = new ParseErrorHandler(inName, err);
        
        try {
            // 1. Nạp mã nguồn
            CharStream input = CharStreams.fromStream(inStream);
            GOALLexer lexer = new GOALLexer(input);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            GOALParser parser = new GOALParser(tokenStream);

            // =======================================================
            // 2. BẮT BUỘC: Thêm Error Listener để ép dừng khi sai cú pháp
            // =======================================================
            parser.removeErrorListeners(); // Xóa listener mặc định
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    // Ném Exception ngay lập tức khi phát hiện thiếu }, {, hoặc sai keyword
                    throw new ParseCancellationException("Lỗi cú pháp tại dòng " + line + ":" + charPositionInLine + " - " + msg);
                }
            });

            // =======================================================
            // 3. BẮT BUỘC: Gọi Start Rule để thực thi việc Parse
            // =======================================================
            // Lưu ý: Tên hàm goalModel() phụ thuộc vào rule gốc cùng tên trong file GOAL.g4 của bạn
            ParseTree tree = parser.goalModel(); 

            // =======================================================
            // 4. Duyệt cây cú pháp (Visitor hoặc Listener)
            // =======================================================
            /* * TODO: Mở comment đoạn này sau khi bạn đã viết xong lớp Visitor/Listener
             * Ví dụ dùng Visitor giống đoạn code cũ của bạn:
             * * GoalModelVisitor visitor = new GoalModelVisitor();
             * GoalModelCS goalModelCS = (GoalModelCS) visitor.visit(tree);
             * * if (errHandler.errorCount() == 0) {
             * Context ctx = new Context(inName, err, null, new GoalModelFactory());
             * ctx.setModel(model);
             * return goalModelCS.visitPreOrder(ctx);
             * }
             */

        } catch (ParseCancellationException e) {
            // Bắt lỗi cú pháp (thiếu ngoặc, sai chữ) và in ra màn hình
            err.println("BIÊN DỊCH THẤT BẠI: " + e.getMessage());
        } catch (IOException e) {
            err.println("Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            err.println("Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        } finally {
            err.flush();
        }
    }
}