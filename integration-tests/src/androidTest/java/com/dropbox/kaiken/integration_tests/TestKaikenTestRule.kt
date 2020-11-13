package com.dropbox.kaiken.integration_tests

import androidx.test.core.app.ActivityScenario
import com.dropbox.common.kaiken.testing.KaikenTestRule
import com.dropbox.core.test.uidriver.UiTestUtils
import com.dropbox.kaiken.runtime.InjectorFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TestKaikenTestRule {
    private var injectorHolderScenario: ActivityScenario<TestInjectorHolderActivity>? = null
    private var simplerScenario: ActivityScenario<TestSimpleActivity>? = null

    @get:Rule
    var kaikenTestRule: KaikenTestRule = KaikenTestRule()

    @Before
    fun setup() {
        kaikenTestRule.setInjectorFactoryHolderOverrideFor(
            TestInjectorHolderActivity::class,
            OverriddenInjectorFactory()
        )

        kaikenTestRule.setInjectorFactoryHolderOverrideFor(
            TestInjectorHolderFragment::class,
            OverriddenInjectorFactory()
        )
    }

    @After
    fun teardown() {
        injectorHolderScenario?.close()
        simplerScenario?.close()
    }

    @Test
    fun givenInjectorHolderActivityWHENInjectItTHENItUsesTheTestsRuleOverride() {
        injectorHolderScenario = ActivityScenario.launch(TestInjectorHolderActivity::class.java)

        injectorHolderScenario!!.onActivity { activity ->
            assertThat(activity.message).isNull()

            // WHEN
            activity.testInject()

            // THEN
            assertThat(activity.message).isEqualTo(
                "Hello Activity! I've overridden the default injector! Muaha Muaha!"
            )
        }
    }

    @Test
    fun givenSimpleFragmentChildOfInjectorHolderActivityWHENInjectTHENItReachesUpForAndUsesInjectorOverride() {
        injectorHolderScenario = ActivityScenario.launch(TestInjectorHolderActivity::class.java)

        injectorHolderScenario!!.onActivity { activity ->

            // GIVEN
            val fragment = TestSimpleFragment()
            activity.addFragment(fragment)

            assertThat(fragment.message).isNull()

            // WHEN
            fragment.testInject()

            // THEN
            assertThat(fragment.message).isEqualTo(
                "Hello Fragment! I've overridden the default injector! Muaha Muaha!"
            )
        }
    }

    @Test
    fun givenInjectorHolderFragmentChildOfSimpleActivityWHENInjectItTHENItWorks() {
        simplerScenario = ActivityScenario.launch(TestSimpleActivity::class.java)

        simplerScenario!!.onActivity { activity ->
            // GIVEN
            val fragment = TestInjectorHolderFragment()
            activity.addFragment(fragment)

            assertThat(fragment.message).isNull()

            // WHEN
            fragment.testInject()

            // THEN
            assertThat(fragment.message).isEqualTo(
                "Hello Fragment! I've overridden the default injector! Muaha Muaha!"
            )
        }
    }

    @Test
    fun givenInjectorHolderFragmentChildOfInjectorHolderActivityWHENInjectItTHENItDoesNotReachUp() {
        injectorHolderScenario = ActivityScenario.launch(TestInjectorHolderActivity::class.java)

        injectorHolderScenario!!.onActivity { activity ->
            // GIVEN
            val fragment = TestInjectorHolderFragment()
            activity.addFragment(fragment)

            assertThat(fragment.message).isNull()

            // WHEN
            fragment.testInject()

            // THEN
            assertThat(fragment.message).isNotEqualTo(
                "Hello Activity! I've overridden the default injector! Muaha Muaha!"
            )
            assertThat(fragment.message).isEqualTo(
                "Hello Fragment! I've overridden the default injector! Muaha Muaha!"
            )
        }
    }

    /**
     * This is important. Even when overriding the injector for test purposes, we preserve the retention on
     * configuration change functionality.
     */
    @Test
    fun givenInjectorHolderActivityWHENRotatedTHENSameInjectorIsReturned() {
        injectorHolderScenario = ActivityScenario.launch(TestInjectorHolderActivity::class.java)

        var injectorBeforeRotation: TestInjectorHolderActivityInjector? = null
        var injectorAfterRotation: TestInjectorHolderActivityInjector?

        // GIVEN
        injectorHolderScenario!!.onActivity { activity ->
            assertThat(activity.message).isNull()

            injectorBeforeRotation = activity.locateInjector()

            activity.testInject()
            assertThat(activity.message).isNotNull()
        }

        // WHEN
        UiTestUtils.rotateScreenTwice()

        // THEN
        injectorHolderScenario!!.onActivity { activity ->
            assertThat(activity.message).isNull()

            injectorAfterRotation = activity.locateInjector()

            activity.testInject()
            assertThat(activity.message).isNotNull()

            assertThat(injectorAfterRotation).isSameInstanceAs(injectorBeforeRotation)
        }
    }
}

class OverriddenInjectorFactory : InjectorFactory<TestInjectorHolderActivityInjector> {
    override fun createInjector(): TestInjectorHolderActivityInjector {
        return OverriddenTestInjectorHolder()
    }
}

class OverriddenTestInjectorHolder :
    TestInjectorHolderActivityInjector,
    TestInjectorHolderFragmentInjector,
    TestSimpleFragmentInjector {
    override fun inject(activity: TestInjectorHolderActivity) {
        activity.message = "Hello Activity! I've overridden the default injector! Muaha Muaha!"
    }

    override fun inject(fragment: TestInjectorHolderFragment) {
        fragment.message = "Hello Fragment! I've overridden the default injector! Muaha Muaha!"
    }

    override fun inject(fragment: TestSimpleFragment) {
        fragment.message = "Hello Fragment! I've overridden the default injector! Muaha Muaha!"
    }
}