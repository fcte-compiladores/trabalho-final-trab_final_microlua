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
        if (key instanceof Double) {
            int index = ((Double) key).intValue();
            if (index >= 1 && index <= arrayPart.size()) {
                return arrayPart.get(index - 1);
            }
        }
        
        if (elements.containsKey(key)) {
            Object value = elements.get(key);
            if (value == this && key.equals("__index")) {
                return this;
            }
            return value;
        }
        
        if (metatable != null) {
            Object handler = metatable.get("__index");
            if (handler != null) {
                if (handler instanceof LuaCallable) {
                    return ((LuaCallable) handler).call(null, Arrays.asList(this, key));
                } else if (handler instanceof LuaTable) {
                    return ((LuaTable) handler).get(key);
                }
            }
        }
        
        return null;
    }

    public void set(Object key, Object value) {
        if (key instanceof Double) {
            int index = ((Double) key).intValue();
            if (index == arrayPart.size() + 1) {
                arrayPart.add(value);
                return;
            } else if (index >= 1 && index <= arrayPart.size()) {
                arrayPart.set(index - 1, value);
                return;
            }
        }
        
        if (value == this && key instanceof String && ((String)key).equals("__index")) {
            elements.put(key, value);
            return;
        }
        
        elements.put(key, value);
        
        if (metatable != null && elements.containsKey("__newindex")) {
            Object handler = elements.get("__newindex");
            if (handler instanceof LuaCallable) {
                ((LuaCallable) handler).call(null, Arrays.asList(this, key, value));
            } else if (handler instanceof LuaTable) {
                ((LuaTable) handler).set(key, value);
            }
        }
    }

    public void setMetatable(LuaTable mt) {
        this.metatable = mt;
    }

    public LuaTable getMetatable() {
        return metatable;
    }

    @Override
    public String toString() {
        if (metatable != null && metatable.get("__tostring") instanceof LuaCallable) {
            LuaCallable tostring = (LuaCallable) metatable.get("__tostring");
            return (String) tostring.call(null, Arrays.asList(this));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        for (int i = 0; i < arrayPart.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(stringify(arrayPart.get(i)));
        }
        
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