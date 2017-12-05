package net.ssehub.kernel_haven.config_mismatches;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests suite to load all tests of this plug-in (entry point).
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    ConfigMismatchDetectorTest.class
    })
public class AllTests {

}
