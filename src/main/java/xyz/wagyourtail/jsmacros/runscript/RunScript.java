package xyz.wagyourtail.jsmacros.runscript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.HostAccess;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import net.minecraft.text.LiteralText;
import xyz.wagyourtail.jsmacros.jsMacros;
import xyz.wagyourtail.jsmacros.config.RawMacro;
import xyz.wagyourtail.jsmacros.events.EventTypesEnum;
import xyz.wagyourtail.jsmacros.runscript.functions.chatFunctions;
import xyz.wagyourtail.jsmacros.runscript.functions.jsMacrosFunctions;
import xyz.wagyourtail.jsmacros.runscript.functions.keybindFunctions;
import xyz.wagyourtail.jsmacros.runscript.functions.timeFunctions;


public class RunScript {
    public static HashMap<RawMacro, ArrayList<Thread>> threads = new HashMap<>();
    private static Builder context = Context.newBuilder("js");
    private static HashMap<String, Object> globals = new HashMap<>();
    private static ScriptEngine engine = GraalJSScriptEngine.create(null, context);
    static {
        context.allowHostAccess(HostAccess.ALL);
        context.allowHostClassLookup(s -> true);
        context.allowAllAccess(true);
        context.allowExperimentalOptions(true);
        engine.put("global", globals);
        engine.put("jsmacros", new jsMacrosFunctions());
        engine.put("time", new timeFunctions());
        engine.put("binding", new keybindFunctions());
        engine.put("chat", new chatFunctions());
    }
    
    public static Thread exec(RawMacro macro, EventTypesEnum event, HashMap<String, Object> args) {
        File file = macro.scriptFile;
        
        final Runnable r = new Runnable() {
            public void run() {
                try {
                    engine.eval(new FileReader(file));
                } catch (ScriptException | IOException e) {
                    if (jsMacros.getMinecraft().inGameHud != null) {
                        LiteralText text = new LiteralText(e.toString());
                        jsMacros.getMinecraft().inGameHud.getChatHud().addMessage(text);
                    } else {
                        e.printStackTrace();
                    }
                }
                threads.get(macro).remove(Thread.currentThread());
            }
        };
        
        Thread t = new Thread(r);
        
        engine.put("event", event);
        engine.put("args", args);
        engine.put("file", file);
        
        //function files
        threads.putIfAbsent(macro, new ArrayList<>());
        t.setName(macro.type.toString()+" "+macro.eventkey+" "+macro.scriptFileName()+": "+threads.get(macro).size());
        threads.get(macro).add(t);
        t.start();
        return t;
    }
}