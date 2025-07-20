package micro_lua;

import java.util.List;

public class LuaFunction implements LuaCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    
    public LuaFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(LuaInterpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        
        // Registra os parâmetros no ambiente
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + (declaration.name != null ? declaration.name.lexeme : "anonymous") + ">";
    }
}