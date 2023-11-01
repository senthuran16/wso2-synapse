package org.apache.synapse.mediators.bsf.access.control;

import org.apache.synapse.mediators.bsf.access.control.config.AccessControlConfig;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class SandboxWrapFactory extends WrapFactory {
    private AccessControlConfig nativeObjectAccessControlConfig;

    public SandboxWrapFactory(AccessControlConfig nativeObjectAccessControlConfig) {
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }

    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class staticType) {
        return new SandboxNativeJavaObject(scope, javaObject, staticType, nativeObjectAccessControlConfig);
    }
}
