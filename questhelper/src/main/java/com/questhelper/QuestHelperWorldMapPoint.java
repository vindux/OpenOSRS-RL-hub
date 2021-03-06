/*
 * Copyright (c) 2018, Morgan Lewis <https://github.com/MESLewis>
 * Copyright (c) 2019, Trevor <https://github.com/Trevor159>
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
package com.questhelper;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.util.ImageUtil;

public class QuestHelperWorldMapPoint extends WorldMapPoint
{
	private final BufferedImage questWorldImage;
	private final Point questWorldImagePoint;
	private final HashMap<Integer, BufferedImage> arrows = new HashMap<>();
	private BufferedImage activeQuestArrow;
	public QuestHelperWorldMapPoint(final WorldPoint worldPoint, BufferedImage image)
	{
		super(worldPoint, null);

		BufferedImage iconBackground = ImageUtil.getResourceStreamFromClass(getClass(), "/util/clue_arrow.png");
		questWorldImage = new BufferedImage(iconBackground.getWidth(), iconBackground.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics graphics = questWorldImage.getGraphics();
		graphics.drawImage(iconBackground, 0, 0, null);
		int buffer = iconBackground.getWidth() / 2 - image.getWidth() / 2;
		buffer = Math.max(buffer, 0);

		graphics.drawImage(image, buffer, buffer, null);

		questWorldImagePoint = new Point(questWorldImage.getWidth() / 2, questWorldImage.getHeight());

		arrows.put(0, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow.png"));
		arrows.put(45, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_45.png"));
		arrows.put(90, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_90.png"));
		arrows.put(135, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_135.png"));
		arrows.put(180, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_180.png"));
		arrows.put(225, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_225.png"));
		arrows.put(270, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_270.png"));
		arrows.put(315, ImageUtil.getResourceStreamFromClass(getClass(), "/quest_step_arrow_315.png"));

		activeQuestArrow = arrows.get(0);

		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setImage(questWorldImage);
		this.setImagePoint(questWorldImagePoint);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(activeQuestArrow);
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(questWorldImage);
		this.setImagePoint(questWorldImagePoint);
	}

	public void rotateArrow(int rotation)
	{
		BufferedImage newArrow = arrows.get(rotation);
		if (activeQuestArrow != newArrow)
		{
			activeQuestArrow = arrows.get(rotation);
			if (isCurrentlyEdgeSnapped())
			{
				setImage(arrows.get(rotation));
			}
		}
	}
}
