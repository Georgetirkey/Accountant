package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Accounting", appName)
  }

  @Test
  fun `test currency formatter`() {
    val formatted = Formatters.formatCurrency(1250.50)
    assertEquals("₹1,250.50", formatted)
  }
}

