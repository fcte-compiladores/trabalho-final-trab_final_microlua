package micro_lua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static micro_lua.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        if (match(LOCAL)) {
            if (match(FUNCTION)) return localFunction();
            return localDeclaration();
        }
        if (match(FUNCTION)) return function();
        return statement(); 
    }
    private Stmt.Function function() {
        Token name = consume(IDENTIFIER, "Expect function name.");
        consume(LEFT_PAREN, "Expect '(' after function name.");
        
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        
        List<Stmt> body = block("function");
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt.Function localFunction() {
        Token name = consume(IDENTIFIER, "Expect function name.");
        consume(LEFT_PAREN, "Expect '(' after function name.");
        
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        
        List<Stmt> body = block("function");
        return new Stmt.Function(name, parameters, body, true);
    }

    private Stmt localDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        
        match(SEMICOLON);
        return new Stmt.LocalVar(name, initializer);
    }

    private Stmt statement() {
        if (match(DO)) return new Stmt.Block(block("do"));
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(REPEAT)) return repeatStatement();
        if (match(FOR)) return forStatement();
        if (match(RETURN)) return returnStatement();
        if (match(BREAK)) return breakStatement();
        return expressionStatement();
    }

    private Stmt ifStatement() {
        Expr condition = expression();
        consume(THEN, "Expect 'then' after condition.");

        List<Stmt> thenStatements = new ArrayList<>();
        while (!check(END) && !check(ELSE) && !check(ELSEIF) && !check(EOF)) {
            thenStatements.add(declaration());
        }
        Stmt thenBranch = new Stmt.Block(thenStatements);

        List<Stmt.If> elseifBranches = new ArrayList<>();
        while (match(ELSEIF)) {
            Expr elseifCond = expression();
            consume(THEN, "Expect 'then' after elseif condition.");
            
            List<Stmt> elseifStatements = new ArrayList<>();
            while (!check(END) && !check(ELSE) && !check(ELSEIF) && !check(EOF)) {
                elseifStatements.add(declaration());
            }
            elseifBranches.add(new Stmt.If(elseifCond, new Stmt.Block(elseifStatements), null));
        }

        Stmt elseBranch = null;
        if (match(ELSE)) {
            List<Stmt> elseStatements = new ArrayList<>();
            while (!check(END) && !check(EOF)) {
                elseStatements.add(declaration());
            }
            elseBranch = new Stmt.Block(elseStatements);
        }

        consume(END, "Expect 'end' after if statement.");

        for (int i = elseifBranches.size() - 1; i >= 0; i--) {
            Stmt.If current = elseifBranches.get(i);
            if (elseBranch != null) {
                current = new Stmt.If(current.condition, current.thenBranch, elseBranch);
            }
            elseBranch = current;
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        Expr condition = expression();
        consume(DO, "Expect 'do' after condition.");
        List<Stmt> bodyStatements = new ArrayList<>();
        while (!check(END) && !check(EOF)) {
            bodyStatements.add(declaration());
        }
        consume(END, "Expect 'end' after while body.");
        return new Stmt.While(condition, new Stmt.Block(bodyStatements));
    }

    private Stmt repeatStatement() {
        List<Stmt> bodyStatements = new ArrayList<>();
        while (!check(UNTIL) && !check(EOF)) {
            bodyStatements.add(declaration());
        }
        consume(UNTIL, "Expect 'until' after repeat body.");
        Expr condition = expression();
        match(SEMICOLON);
        return new Stmt.Repeat(condition, new Stmt.Block(bodyStatements));
    }

    private Stmt forStatement() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        consume(EQUAL, "Expect '=' after variable name.");
        Expr initializer = expression();
        consume(COMMA, "Expect ',' after initial value.");
        Expr condition = expression();
        
        Expr increment = null;
        if (match(COMMA)) {
            increment = expression();
        }
        
        consume(DO, "Expect 'do' after for clauses.");
        List<Stmt> bodyStatements = new ArrayList<>();
        while (!check(END) && !check(EOF)) {
            bodyStatements.add(declaration());
        }
        consume(END, "Expect 'end' after for body.");
        return new Stmt.For(name, initializer, condition, increment, new Stmt.Block(bodyStatements));
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON) && !check(EOF) && !check(END)) {
            value = expression();
        }
        match(SEMICOLON);
        return new Stmt.Return(keyword, value);
    }

    private Stmt breakStatement() {
        Token keyword = previous();
        match(SEMICOLON);
        return new Stmt.Break(keyword);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        match(SEMICOLON);
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block(String blockType) {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !check(END) && !check(ELSE) && 
               !check(ELSEIF) && !check(UNTIL) && !check(EOF)) {
            statements.add(declaration());
        }
        
        if (blockType.equals("function") || blockType.equals("do") || 
            blockType.equals("while") || blockType.equals("for")) {
            consume(END, "Expect 'end' after " + blockType + " block.");
        }
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(EQUAL_EQUAL, TILDE_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(PLUS, MINUS, DOT_DOT)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR, PERCENT)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(NOT, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(LEFT_BRACKET)) {
                Token bracket = previous();
                Expr index = expression();
                consume(RIGHT_BRACKET, "Expect ']' after index.");
                expr = new Expr.TableIndex(expr, index, bracket);
            } else if (match(DOT)) {
                Token field = consume(IDENTIFIER, "Expect field name after '.'.");
                expr = new Expr.TableField(expr, field);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        // Literais básicos
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        // Função anônima
        if (match(FUNCTION)) {
            consume(LEFT_PAREN, "Expect '(' after 'function'.");
            List<Token> params = new ArrayList<>();
            if (!check(RIGHT_PAREN)) {
                do {
                    if (params.size() >= 255) {
                        error(peek(), "Can't have more than 255 parameters.");
                    }
                    params.add(consume(IDENTIFIER, "Expect parameter name."));
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.");
            List<Stmt> body = block("function");
            return new Expr.Literal(new Stmt.Function(
                new Token(TokenType.IDENTIFIER, "", null, previous().line),
                params,
                body
            ));
        }

        // Construtor de tabela
        if (match(LEFT_BRACE)) return tableConstructor();

        // Variável
        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        // Expressão entre parênteses
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // Chamada de função prefixada com ::
        if (match(COLON)) {
            Token name = consume(IDENTIFIER, "Expect method name after ':'.");
            return new Expr.Variable(name);
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr tableConstructor() {
        Token brace = previous();
        List<Expr.Field> fields = new ArrayList<>();
        
        if (!check(RIGHT_BRACE)) {
            do {
                fields.add(tableField());
            } while (match(COMMA, SEMICOLON));
        }
        
        consume(RIGHT_BRACE, "Expect '}' after table elements.");
        return new Expr.Table(brace, fields);
    }

    private Expr.Field tableField() {
        if (match(LEFT_BRACKET)) {
            Expr key = expression();
            consume(RIGHT_BRACKET, "Expect ']' after table key.");
            consume(EQUAL, "Expect '=' after table key.");
            Expr value = expression();
            return new Expr.Field(key, value);
        }
        
        if (check(IDENTIFIER) && checkNext(EQUAL)) {
            Token name = advance(); // Consome identificador
            advance(); // Consome '='
            Expr value = expression();
            return new Expr.Field(new Expr.Literal(name.lexeme), value);
        }
        
        Expr value = expression();
        return new Expr.Field(null, value); // Chave implícita
    }
    
    private boolean checkNext(TokenType type) {
        if (isAtEnd()) return false;
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

	private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lua.error(token, message);
        return new ParseError();
    }

   
    private static class ParseError extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;}
}