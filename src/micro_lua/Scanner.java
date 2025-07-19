package micro_lua;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("and", TokenType.AND);
        keywords.put("break", TokenType.BREAK);
        keywords.put("do", TokenType.DO);
        keywords.put("else", TokenType.ELSE);
        keywords.put("elseif", TokenType.ELSEIF);
        keywords.put("end", TokenType.END);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("function", TokenType.FUNCTION);
        keywords.put("if", TokenType.IF);
        keywords.put("in", TokenType.IN);
        keywords.put("local", TokenType.LOCAL);
        keywords.put("nil", TokenType.NIL);
        keywords.put("not", TokenType.NOT);
        keywords.put("or", TokenType.OR);
        keywords.put("repeat", TokenType.REPEAT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("then", TokenType.THEN);
        keywords.put("true", TokenType.TRUE);
        keywords.put("until", TokenType.UNTIL);
        keywords.put("while", TokenType.WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '[': addToken(TokenType.LEFT_BRACKET); break;
            case ']': addToken(TokenType.RIGHT_BRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': 
                if (match('.')) {
                    addToken(TokenType.DOT_DOT);
                } else {
                    addToken(TokenType.DOT);
                }
                break;
            case '-': 
                if (match('-')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.MINUS);
                }
                break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case ':': addToken(TokenType.COLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '^': addToken(TokenType.CARET); break;
            case '!': addToken(TokenType.NOT); break;
            case '=': 
                if (match('=')) {
                    addToken(TokenType.EQUAL_EQUAL);
                } else {
                    addToken(TokenType.EQUAL);
                }
                break;
            case '<': 
                if (match('=')) {
                    addToken(TokenType.LESS_EQUAL);
                } else {
                    addToken(TokenType.LESS);
                }
                break;
            case '>': 
                if (match('=')) {
                    addToken(TokenType.GREATER_EQUAL);
                } else {
                    addToken(TokenType.GREATER);
                }
                break;
            case '~': 
                if (match('=')) {
                    addToken(TokenType.TILDE_EQUAL);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            case '\'': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lua.error(line, "Unexpected character: '" + c + "'");
                }
                break;
        }
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void string() {
        char quote = source.charAt(current - 1);
        while (peek() != quote && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lua.error(line, "Unterminated string.");
            return;
        }

        advance(); // Consume the closing quote

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the .
            while (isDigit(peek())) advance();
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }
}