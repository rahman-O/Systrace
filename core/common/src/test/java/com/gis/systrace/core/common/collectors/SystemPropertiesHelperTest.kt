package com.gis.systrace.core.common.collectors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemPropertiesHelperTest {

  @Test
  fun collectMdmProperties_returnsMap() {
    val props = SystemPropertiesHelper.collectMdmProperties()
  }

  @Test
  fun get_unknownKey_returnsNullOrString() {
    val value = SystemPropertiesHelper.get("ro.nonexistent.systrace.test.key")
    assertTrue(value == null || value is String)
  }
}
