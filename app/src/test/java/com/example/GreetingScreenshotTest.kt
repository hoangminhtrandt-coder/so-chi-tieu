package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleTx = com.example.data.model.TransactionEntity(
      id = 1L,
      type = com.example.data.model.TransactionType.EXPENSE,
      amount = 45000.0,
      categoryId = "food_dining",
      categoryName = "Ăn uống",
      categoryIcon = "Restaurant",
      categoryColor = 0xFFEF4444,
      date = System.currentTimeMillis(),
      note = "Cà phê sáng",
      wallet = "Tiền mặt"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.TransactionItemCard(
          transaction = sampleTx,
          onClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
