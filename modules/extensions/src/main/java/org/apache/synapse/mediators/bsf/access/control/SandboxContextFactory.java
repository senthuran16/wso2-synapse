package org.apache.synapse.mediators.bsf.access.control;

import org.apache.synapse.mediators.bsf.access.control.config.AccessControlConfig;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public class SandboxContextFactory extends ContextFactory {
    private AccessControlConfig nativeObjectAccessControlConfig;

    public SandboxContextFactory(AccessControlConfig nativeObjectAccessControlConfig) {
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }

    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        cx.setWrapFactory(new SandboxWrapFactory(nativeObjectAccessControlConfig));
        return cx;
    }
}
