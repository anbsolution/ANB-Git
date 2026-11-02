package com.anb.git

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun applicationId_isExpected() {
        assertEquals("com.anb.git", BuildConfig.APPLICATION_ID)
    }
}
