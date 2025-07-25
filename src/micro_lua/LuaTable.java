package micro_lua;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class LuaTable {
    public final Map<Object, Object> elements = new HashMap<>();
    public final List<Object> arrayPart = new ArrayList<>();
    private LuaTable metatable = null;
    
    public Object get(Object key) {
        return get(null, key);
    }
    public Object get(LuaInterpreter interpreter, Object key) {
        // Verificar parte array primeiro
        if (key instanceof Double) {
            int index = ((Double) key).intValue();
            if (index >= 1 && index <= arrayPart.size()) {
                return arrayPart.get(index - 1);
            }
        }
        
        // Verificar elementos diretos
        if (elements.containsKey(key)) {
            return elements.get(key);
        }
        
        // Verificar metatable
        if (metatable != null) {
            Object handler = metatable.get(interpreter, "__index");  // Passar interpretador aqui
            if (handler != null) {
                if (handler instanceof LuaCallable) {
                    // Passar interpretador na chamada
                    return ((LuaCallable) handler).call(interpreter, Arrays.asList(this, key));
                } else if (handler instanceof LuaTable) {
                    return ((LuaTable) handler).get(interpreter, key);  // Passar interpretador
                }
            }
        }
        
        return null;
    }

    public void set(Object key, Object value) {
        // Verificar se é uma operação rawset
        boolean isRawSet = false;
        
        // Parte array
        if (key instanceof Double && !isRawSet) {
            int index = ((Double) key).intValue();
            if (index == arrayPart.size() + 1) {
                arrayPart.add(value);
                return;
            } else if (index >= 1 && index <= arrayPart.size()) {
                arrayPart.set(index - 1, value);
                return;
            }
        }
        
        // Verificar __newindex
        if (metatable != null && metatable.get("__newindex") != null && !isRawSet) {
            Object handler = metatable.get("__newindex");
            if (handler instanceof LuaCallable) {
                ((LuaCallable) handler).call(null, Arrays.asList(this, key, value));
                return;
            } else if (handler instanceof LuaTable) {
                ((LuaTable) handler).set(key, value);
                return;
            }
        }
        
        // Operação normal
        elements.put(key, value);
    }

    public void setMetatable(LuaTable mt) {
        this.metatable = mt;
    }

    public LuaTable getMetatable() {
        return metatable;
    }

    public int length() {
        if (metatable != null && metatable.get("__len") instanceof LuaCallable) {
            Object result = ((LuaCallable) metatable.get("__len")).call(null, Arrays.asList(this));
            if (result instanceof Double) {
                return ((Double) result).intValue();
            }
        }
        return arrayPart.size();
    }

    @Override
    public String toString() {
        if (metatable != null && metatable.get("__tostring") instanceof LuaCallable) {
            LuaCallable tostring = (LuaCallable) metatable.get("__tostring");
            return (String) tostring.call(null, Arrays.asList(this));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Parte array
        for (int i = 0; i < arrayPart.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(stringify(arrayPart.get(i)));
        }
        
        // Parte hash
        boolean first = arrayPart.isEmpty();
        for (Map.Entry<Object, Object> entry : elements.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            
            if (entry.getKey() instanceof String && 
                ((String) entry.getKey()).matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                sb.append(entry.getKey()).append(" = ");
            } else {
                sb.append("[").append(stringify(entry.getKey())).append("] = ");
            }
            sb.append(stringify(entry.getValue()));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private String stringify(Object obj) {
        if (obj == null) return "nil";
        if (obj instanceof String) return "\"" + obj + "\"";
        return obj.toString();
    }
}