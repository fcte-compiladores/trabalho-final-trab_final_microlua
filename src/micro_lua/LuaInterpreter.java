package micro_lua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class LuaInterpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    public LuaInterpreter() {

        globals.define("print", new LuaCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                System.out.println(stringify(arguments.get(0)));
                return null;
            }
        });

        globals.define("clock", new LuaCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
        });

        globals.define("type", new LuaCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                Object arg = arguments.get(0);
                if (arg == null) return "nil";
                if (arg instanceof Boolean) return "boolean";
                if (arg instanceof Double) return "number";
                if (arg instanceof String) return "string";
                if (arg instanceof LuaCallable) return "function";
                if (arg instanceof LuaTable) return "table";
                return "unknown";
            }
        });

        globals.define("table", new LuaTable() {{
            set("insert", new LuaCallable() {
                @Override public int arity() { return 2; }
                @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                    if (!(arguments.get(0) instanceof LuaTable)) {
                        throw new RuntimeError(null, "First argument must be a table");
                    }
                    LuaTable table = (LuaTable) arguments.get(0);
                    table.set(table.arrayPart.size() + 1, arguments.get(1));
                    return null;
                }
            });

            set("remove", new LuaCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                    if (!(arguments.get(0) instanceof LuaTable)) {
                        throw new RuntimeError(null, "First argument must be a table");
                    }
                    LuaTable table = (LuaTable) arguments.get(0);
                    if (table.arrayPart.isEmpty()) return null;
                    return table.arrayPart.remove(table.arrayPart.size() - 1);
                }
            });
        }});

        globals.define("getmetatable", new LuaCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                Object arg = arguments.get(0);
                if (arg instanceof LuaTable) {
                    return ((LuaTable) arg).getMetatable();
                }
                return null;
            }
        });

        globals.define("setmetatable", new LuaCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof LuaTable)) {
                    throw new RuntimeError(null, "setmetatable: first argument must be a table");
                }
                LuaTable table = (LuaTable) arguments.get(0);
                LuaTable mt = null;
                if (arguments.get(1) instanceof LuaTable) {
                    mt = (LuaTable) arguments.get(1);
                } else if (arguments.get(1) != null) {
                    throw new RuntimeError(null, "setmetatable: second argument must be a table or nil");
                }
                table.setMetatable(mt);
                return table;
            }
        });

        globals.define("rawget", new LuaCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof LuaTable)) {
                    throw new RuntimeError(null, "bad argument #1 to 'rawget' (table expected)");
                }
                return ((LuaTable) arguments.get(0)).get(arguments.get(1));
            }
        });

        globals.define("rawset", new LuaCallable() {
            @Override public int arity() { return 3; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof LuaTable)) {
                    throw new RuntimeError(null, "bad argument #1 to 'rawset' (table expected)");
                }
                ((LuaTable) arguments.get(0)).set(arguments.get(1), arguments.get(2));
                return arguments.get(0);
            }
        });

        globals.define("pairs", new LuaCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(LuaInterpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof LuaTable)) {
                    throw new RuntimeError(null, "bad argument #1 to 'pairs' (table expected)");
                }
                LuaTable table = (LuaTable) arguments.get(0);
                return new LuaFunction(new Stmt.Function(
                    new Token(TokenType.IDENTIFIER, "pairs_iterator", null, 0),
                    new ArrayList<>(),
                    Arrays.asList(new Stmt.Return(
                        new Token(TokenType.RETURN, "return", null, 0),
                        new Expr.Literal(table)
                    ))
                ), globals);
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

    private Object getMetamethod(Object obj, String metamethod) {
        if (obj instanceof LuaTable) {
            LuaTable table = (LuaTable) obj;
            LuaTable mt = table.getMetatable();
            if (mt != null) {
                return mt.get(this,metamethod);
            }
        }
        return null;
    }

    private String tableToString(LuaTable table) {
        Object tostring = getMetamethod(table, "__tostring");
        if (tostring instanceof LuaCallable) {
            return stringify(((LuaCallable) tostring).call(this, Arrays.asList(table)));
        }
        return table.toString();
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
        if (object instanceof LuaTable) {
            return tableToString((LuaTable) object);
        }
        if (object instanceof String) {
            return (String) object;
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
                Object gt = callMetamethod(left, right, "__gt");
                if (gt != null) return gt;
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                Object ge = callMetamethod(left, right, "__ge");
                if (ge != null) return ge;
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                Object lt = callMetamethod(left, right, "__lt");
                if (lt != null) return lt;
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                Object le = callMetamethod(left, right, "__le");
                if (le != null) return le;
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                Object sub = callMetamethod(left, right, "__sub");
                if (sub != null) return sub;
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                Object add = callMetamethod(left, right, "__add");
                if (add != null) return add;
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                Object div = callMetamethod(left, right, "__div");
                if (div != null) return div;
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                Object mul = callMetamethod(left, right, "__mul");
                if (mul != null) return mul;
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PERCENT:
                Object mod = callMetamethod(left, right, "__mod");
                if (mod != null) return mod;
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;
            case CARET:
                Object pow = callMetamethod(left, right, "__pow");
                if (pow != null) return pow;
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double)left, (double)right);
            case DOT_DOT:
                Object concat = callMetamethod(left, right, "__concat");
                if (concat != null) return concat;
                return stringify(left) + stringify(right);
            default:
                throw new RuntimeError(expr.operator, "Unknown binary operator.");
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
        if (expr.value instanceof String && ((String)expr.value).equals("{}")) {
            return new LuaTable();
        }
        if (expr.value instanceof Stmt.Function) {
            Stmt.Function func = (Stmt.Function)expr.value;
            return new LuaFunction(func, environment);
        }
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
                Object unm = getMetamethod(right, "__unm");
                if (unm instanceof LuaCallable) {
                    return ((LuaCallable) unm).call(this, Arrays.asList(right));
                }
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

    @Override
    public Object visitTableIndexExpr(Expr.TableIndex expr) {
        Object table = evaluate(expr.table);
        Object index = evaluate(expr.index);
        
        if (table instanceof LuaTable) {
            // Passar o interpretador atual para o get
            return ((LuaTable) table).get(this, index);
        }
        
        throw new RuntimeError(expr.bracket, 
            "Attempt to index a non-table value (" + stringify(table) + ")");
    }

    @Override
    public Object visitTableFieldExpr(Expr.TableField expr) {
        Object table = evaluate(expr.table);
        
        if (table instanceof LuaTable) {
            // Passar o interpretador atual para o get
            return ((LuaTable) table).get(this, expr.field.lexeme);
        }
        
        throw new RuntimeError(expr.field, "Attempt to index a non-table value");
    }
    @Override
    public Object visitTableExpr(Expr.Table expr) {
        LuaTable table = new LuaTable();
        
        for (Expr.Field field : expr.fields) {
            Object key = null;
            if (field.key != null) {
                key = evaluate(field.key);
            } else {
                // Chave implícita (array)
                key = (double) (table.arrayPart.size() + 1);
            }
            
            Object value = evaluate(field.value);
            table.set(key, value);
        }
        
        return table;
    }
    private Object callMetamethod(Object a, Object b, String metamethod) {
        Object mm = getMetamethod(a, metamethod);
        if (mm == null) {
            mm = getMetamethod(b, metamethod);
        }
        if (mm instanceof LuaCallable) {
            return ((LuaCallable) mm).call(this, Arrays.asList(a, b));
        }
        return null;
    }
    
}