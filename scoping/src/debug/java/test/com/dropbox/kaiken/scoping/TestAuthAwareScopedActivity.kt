package test.com.dropbox.kaiken.scoping

import android.os.Bundle
import com.dropbox.kaiken.scoping.AuthAwareScopeOwnerActivity

abstract class TestAuthAwareScopedActivity : TestActivity(), AuthAwareScopeOwnerActivity {

    var resolvedDependencies: MyDependencies? = null
    var onCreateExceptionThrown: Throwable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (callFinishIfInvalidAuth && finishIfInvalidAuth()) {
                return
            }

            resolvedDependencies = resolveDependencyProvider<MyDependencies>()
        } catch (t: Throwable) {
            onCreateExceptionThrown = t
        }
    }

    companion object {
        var callFinishIfInvalidAuth = true
    }
}
