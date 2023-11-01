package org.apache.synapse.mediators.bsf.access.control;

import org.apache.synapse.mediators.bsf.access.control.config.AccessControlConfig;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import java.util.Comparator;
import java.util.Objects;

public class SandboxNativeJavaObject extends NativeJavaObject {
    private AccessControlConfig nativeObjectAccessControlConfig;

    public SandboxNativeJavaObject(Scriptable scope, Object javaObject, Class staticType,
                                   AccessControlConfig nativeObjectAccessControlConfig) {
        super(scope, javaObject, staticType);
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }

    @Override
    public Object get(String name, Scriptable start) {
        Comparator<String> equalsComparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 != null && o1.equals(o2)) {
                    return 0;
                }
                return -1;
            }
        };
        if (AccessControlUtils.isAccessAllowed(name, nativeObjectAccessControlConfig, equalsComparator)) {
            return super.get(name, start);
        }
        return NOT_FOUND;
    }

}
