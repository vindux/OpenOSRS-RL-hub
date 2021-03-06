/*
 * Copyright (c) 2020, Lotto <https://github.com/devLotto>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.monkeymetrics;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class NpcStacksOverlay extends Overlay
{
	private static final int OFFSET_Z = 20;

	private final MonkeyMetricsConfig config;

	private final Client client;

	private Map<LocalPoint, Integer> npcStacks;

	@Inject
	NpcStacksOverlay(MonkeyMetricsPlugin monkeyMetricsPlugin, MonkeyMetricsConfig config, Client client)
	{
		super(monkeyMetricsPlugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.config = config;
		this.client = client;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (npcStacks == null || !config.showMetrics())
		{
		   return null;
		}

		npcStacks.forEach(((localPoint, count) ->
		{
			final String text = String.valueOf(count);

			final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
			final Point textPoint = Perspective.getCanvasTextLocation(client,
					graphics,
					localPoint,
					text,
					OFFSET_Z);

			if (polygon == null || textPoint == null)
			{
				return;
			}

			OverlayUtil.renderPolygon(graphics, polygon, new Color(255, 255, 255, 100));
			OverlayUtil.renderTextLocation(graphics, textPoint, text, Color.ORANGE);
		}));

		return null;
	}

	public void setNpcStacks(Map<LocalPoint, Integer> npcStacks)
	{
		this.npcStacks = npcStacks;
	}
}
