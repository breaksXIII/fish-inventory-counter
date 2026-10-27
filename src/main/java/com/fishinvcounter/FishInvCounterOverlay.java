package com.fishinvcounter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FishInvCounterOverlay extends OverlayPanel
{
	private final FishInvCounterConfig config;

	@Inject
	FishInvCounterOverlay(FishInvCounterConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		int count = config.count();
		int goal = Math.max(1, config.goal());
		boolean done = count >= goal;

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Fish Inventories")
			.color(done ? Color.GREEN : Color.WHITE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Count:")
			.right(count + " / " + goal)
			.build());

		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMaximum(goal);
		bar.setValue(Math.min(count, goal));
		bar.setForegroundColor(done ? new Color(0, 160, 0) : new Color(0, 110, 200));
		panelComponent.getChildren().add(bar);

		return super.render(graphics);
	}
}
