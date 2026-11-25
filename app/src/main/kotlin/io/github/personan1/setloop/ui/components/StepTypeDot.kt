package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.ui.theme.color

/** Small colored square marking a step type. Decorative: hidden from accessibility. */
@Composable
fun StepTypeDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, RoundedCornerShape(3.dp))
            .clearAndSetSemantics {},
    )
}

@Composable
fun StepTypeDot(type: StepType, modifier: Modifier = Modifier) = StepTypeDot(type.color, modifier)
