package micro_lua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuaInterpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    LuaInterpreter() {
        // Função nativa print
        globals.define("print", new LuaCallable() {
            @Override
            public int arity() { return 1; }
            
            @Override
            public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                System.out.println(stringify(arguments.get(0)));
                return null;
            }
        });
        
        // Função nativa clock
        globals.define("clock", new LuaCallable() {
            @Override
            public int arity() { return 0; }
            
            @Override
            public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
        });
        
        // Função nativa str
        globals.define("str", new LuaCallable() {
            @Override
            public int arity() { return 1; }
            
            @Override
            public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                return stringify(arguments.get(0));
            }
        });
        
        // Função nativa num
        globals.define("num", new LuaCallable() {
            @Override
            public int arity() { return 1; }
            
            @Override
            public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                Object arg = arguments.get(0);
                if (arg instanceof Double) return arg;
                if (arg instanceof String) {
                    try {
                        return Double.parseDouble((String) arg);
                    } catch (NumberFormatException e) {
                        throw new RuntimeError(null, "Cannot convert '" + arg + "' to number.");
                    }
                }
                throw new RuntimeError(null, "Cannot convert to number.");
            }
        });
        
        // Função nativa type
        globals.define("type", new LuaCallable() {
            @Override
            public int arity() { return 1; }
            
            @Override
            public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                Object arg = arguments.get(0);
                if (arg == null) return "nil";
                if (arg instanceof Boolean) return "boolean";
                if (arg instanceof Double) return "number";
                if (arg instanceof String) return "string";
                if (arg instanceof LuaCallable) return "function";
                return "unknown";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lua.runtimeError(error);
        } catch (Break e) {
            Lua.runtimeError(new RuntimeError(null, "Break outside loop"));
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LuaFunction function = new LuaFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitLocalVarStmt(Stmt.LocalVar stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition)) && !Lua.hadRuntimeError) {
            try {
                execute(stmt.body);
            } catch (Break e) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitRepeatStmt(Stmt.Repeat stmt) {
        do {
            try {
                execute(stmt.body);
            } catch (Break e) {
                break;
            }
        } while (!isTruthy(evaluate(stmt.condition)) && !Lua.hadRuntimeError);
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        beginScope();
        execute(new Stmt.LocalVar(stmt.name, stmt.initializer));
        
        while (isTruthy(evaluate(stmt.condition)) && !Lua.hadRuntimeError) {
            try {
                execute(stmt.body);
                if (stmt.increment != null) {
                    evaluate(stmt.increment);
                }
            } catch (Break e) {
                break;
            }
        }
        
        endScope();
        return null;
    }
    
    private void beginScope() {
        environment = new Environment(environment);
    }
    
    private void endScope() {
        environment = environment.enclosing;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;
            case DOT_DOT:
                return stringify(left) + stringify(right);
            default: 
                return null;
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LuaCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions.");
        }

        LuaCallable function = (LuaCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case NOT: return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            default:
                return null;
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }
}