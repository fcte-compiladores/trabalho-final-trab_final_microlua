package micro_lua;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lua {
    private static final LuaInterpreter interpreter = new LuaInterpreter();
    
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: mlua [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
    
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        
        if (hadError) System.exit(65);       
        if (hadRuntimeError) System.exit(70);  
    }
    
    private static void runPrompt() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder buffer = new StringBuilder();
        LuaInterpreter interpreterInstance = interpreter;

        System.out.println("MicroLua REPL (digite ':exit' para sair, ':reset' para resetar, ':env' para variáveis, ':help' para ajuda)");
        
        while (true) {
            System.out.print(buffer.length() == 0 ? "> " : ">> ");
            System.out.flush();
            String line = reader.readLine();
            if (line == null) break;

            line = line.trim();

            if (line.equals(":exit")) break;
            if (line.equals(":reset")) {
                interpreterInstance = new LuaInterpreter();
                buffer.setLength(0);
                System.out.println("Ambiente resetado.");
                continue;
            }
            if (line.equals(":env")) {
                System.out.println("Variáveis globais: " + interpreterInstance.globals);
                continue;
            }
            if (line.equals(":help")) {
                printHelp();
                continue;
            }
            if (line.equals(":clear")) {
                buffer.setLength(0);
                System.out.println("Buffer de entrada limpo.");
                continue;
            }

            buffer.append(line).append("\n");

            if (isStatementComplete(buffer.toString())) {
                try {
                    String source = buffer.toString();
                    hadError = false;
                    hadRuntimeError = false;
                    
                    Scanner scanner = new Scanner(source);
                    List<Token> tokens = scanner.scanTokens();
                    Parser parser = new Parser(tokens);
                    List<Stmt> statements = parser.parse();

                    if (hadError) {
                        buffer.setLength(0);
                        continue;
                    }

                    Resolver resolver = new Resolver(interpreterInstance);
                    resolver.resolve(statements);

                    if (hadError) {
                        buffer.setLength(0);
                        continue;
                    }

                    interpreterInstance.interpret(statements);
                    buffer.setLength(0);
                } catch (Exception e) {
                    System.err.println("Erro inesperado: " + e.getMessage());
                    buffer.setLength(0);
                }
            }
        }
    }

    private static boolean isStatementComplete(String source) {

        String[] blockStarters = {"if", "function", "for", "while", "repeat"};
        for (String starter : blockStarters) {
            if (source.contains(starter) && !source.contains("end")) {
                return false;
            }
        }
        
        if (source.contains("then") && !source.contains("end")) {
            return false;
        }
        if (source.contains("do") && !source.contains("end")) {
            return false;
        }
        
        if (countOccurrences(source, '(') > countOccurrences(source, ')')) {
            return false;
        }
        if (countOccurrences(source, '[') > countOccurrences(source, ']')) {
            return false;
        }
        if (countOccurrences(source, '{') > countOccurrences(source, '}')) {
            return false;
        }
        
        return true;
    }

    private static int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private static void printHelp() {
        System.out.println("Comandos disponíveis:");
        System.out.println("  :exit   - Sai do REPL");
        System.out.println("  :reset  - Reseta o ambiente (limpa todas as variáveis)");
        System.out.println("  :env    - Mostra as variáveis globais");
        System.out.println("  :clear  - Limpa o buffer de entrada atual");
        System.out.println("  :help   - Mostra esta ajuda");
    }
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        if (hadError) return;
        
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        
        if (hadError) return;
        
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        
        if (hadError) return;
        
        interpreter.interpret(statements);
    }
    
    public static void error(int line, String message) {
        report(line, "", message);
    }
    
    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
    
    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
    
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}