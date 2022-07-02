package ellemes.gradle.mod.impl;

import groovy.lang.Closure;
import org.gradle.api.Action;

public class ActionClosure<T> extends Closure<T> {
    private final Action<T> action;

    public ActionClosure(Object owner, Action<T> action) {
        super(owner);
        this.action = action;
    }

    /**
     * Used by Groovy, see {@link #call(Object...)}
     */
    @SuppressWarnings("unused")
    public T doCall(T thing) {
        action.execute(thing);
        return thing;
    }
}
