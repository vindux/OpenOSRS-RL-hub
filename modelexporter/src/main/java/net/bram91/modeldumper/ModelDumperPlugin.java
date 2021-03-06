/*
 * Copyright (c) 2020, Bram91
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
package net.bram91.modeldumper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import java.awt.Shape;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Model Exporter",
	description = "Allows exporting models by right clicking them.",
	tags = {"Model", "Dumper","Exporter","3d","obj"},
	enabledByDefault = false,
	type = PluginType.MISCELLANEOUS
)
public class ModelDumperPlugin extends Plugin
{
	private static final String EXPORT_MODEL = "Export Model";
	private static final String MENU_TARGET = "Player";
	private static final WidgetMenuOption FIXED_EQUIPMENT_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
		MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_EQUIPMENT_TAB);
	private static final WidgetMenuOption RESIZABLE_EQUIPMENT_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
		MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_EQUIPMENT_TAB);
	private static final WidgetMenuOption RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT = new WidgetMenuOption(EXPORT_MODEL,
		MENU_TARGET,WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);
	private final ImmutableList<String> set = ImmutableList.of(
		"Trade with", "Attack", "Talk-to", "Examine"
	);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MenuManager menuManager;

	@Override
	protected void startUp() throws Exception
	{
		menuManager.addManagedCustomMenu(FIXED_EQUIPMENT_TAB_EXPORT);
		menuManager.addManagedCustomMenu(RESIZABLE_EQUIPMENT_TAB_EXPORT);
		menuManager.addManagedCustomMenu(RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT);
	}

	@Override
	protected void shutDown()
	{
		menuManager.removeManagedCustomMenu(FIXED_EQUIPMENT_TAB_EXPORT);
		menuManager.removeManagedCustomMenu(RESIZABLE_EQUIPMENT_TAB_EXPORT);
		menuManager.removeManagedCustomMenu(RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB_EXPORT);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry[] menuEntries = event.getMenuEntries();
		for (int i = 0; i < set.size(); i++)
		{
			boolean addMenuEntry = false;
			MenuEntry target = null;
			for (MenuEntry menuEntry : menuEntries)
			{
				if (menuEntry.getOption().toLowerCase().equals("drop") || menuEntry.getOption().toLowerCase().equals("destroy") || menuEntry.getOption().toLowerCase().equals("take") || menuEntry.getOption().toLowerCase().equals("pick-up"))
				{
					return;
				}
				else if (menuEntry.getOption().equals(set.get(i)))
				{
					addMenuEntry = true;
					target = menuEntry;
				}
			}

			if (client.isKeyPressed(KeyCode.KC_SHIFT) && addMenuEntry)
			{
				String entityName = target.getTarget();
				final MenuEntry exportMenuEntry = new MenuEntry();
				exportMenuEntry.setOption(EXPORT_MODEL);
				exportMenuEntry.setTarget(entityName);
				exportMenuEntry.setIdentifier(target.getIdentifier());
				exportMenuEntry.setParam1(i);
				addEntry(exportMenuEntry);
				i = set.size();
				break;

			}
		}
	}

	private void addEntry(MenuEntry exportMenuEntry)
	{
		MenuEntry[] oldMenu = client.getMenuEntries();
		MenuEntry[] newMenu = new MenuEntry[oldMenu.length + 1];
		for (int i = 0; i < oldMenu.length + 1; i++) {
			if (i < 1)
				newMenu[i] = oldMenu[i];
			else if (i == 1)
				newMenu[i] = exportMenuEntry;
			else
				newMenu[i] = oldMenu[i - 1];
		}
		client.setMenuEntries(newMenu);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		try
		{
			if (event.getOption().equals(EXPORT_MODEL))
			{
				if(Text.removeFormattingTags(event.getTarget()).equals(MENU_TARGET))
				{
					exportLocalPlayerModel();
				}
				else
				{
					switch (event.getParam1())
					{
						case 0:
							exportPlayerModel(event.getTarget());
							break;
						case 1:
							exportNpcModel(event.getTarget(), event.getIdentifier());
							break;
						case 2:
							exportNpcModel(event.getTarget(), event.getIdentifier());
							break;
						case 3:
							exportObjectModel(event.getTarget(), event.getIdentifier());
							break;
						case 4:
							exportPetModel(event.getTarget(), event.getIdentifier());
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void exportLocalPlayerModel() throws IOException
	{
		DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		export(client.getLocalPlayer().getModel(), "Player " + client.getLocalPlayer().getName() + " " + TIME_FORMAT.format(new Date()) + ".obj");
	}

	private void exportObjectModel(String menuTarget, int id) throws IOException
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();

		int z = client.getPlane();
		for (int x = 0; x < Constants.SCENE_SIZE; ++x)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; ++y)
			{
				Tile tile = tiles[z][x][y];
				if (tile != null)
				{
					GameObject[] gameObjects = tile.getGameObjects();
					for (int i = 0; i < gameObjects.length; i++)
					{
						if (gameObjects[i] != null && gameObjects[i].getId() == id)
						{
							if (gameObjects[i].getEntity() != null)
							{
								DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
								export((Model) gameObjects[i].getEntity(), "Object " + Text.removeFormattingTags(menuTarget) + " " + TIME_FORMAT.format(new Date()) + ".obj");
							}
						}
					}
				}
			}
		}
	}

	private void exportNpcModel(String menuTarget, int identifier) throws IOException
	{
		NPC npc = client.getCachedNPCs()[identifier];
		DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		export(npc.getModel(), "NPC " + Text.removeFormattingTags(menuTarget) + " " + TIME_FORMAT.format(new Date()) + ".obj");
	}

	private void exportPetModel(String menuTarget, int identifier) throws IOException
	{
		NPC npc=null;
		for(NPC npC:client.getNpcs())
		{
			if(npC.getId() == identifier)
			{
				npc = npC;
			}
		}

		DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		if(npc!=null)
		{
			export(npc.getModel(), "Pet " + Text.removeFormattingTags(menuTarget) + " " + TIME_FORMAT.format(new Date()) + ".obj");
		}
	}

	private void exportPlayerModel(String menuTarget) throws IOException
	{
		Pattern REMOVE_TAGS_SECONDARY = Pattern.compile("\\(.+?\\)");
		Matcher m = REMOVE_TAGS_SECONDARY.matcher(menuTarget);
		String trgt = m.replaceAll("");
		trgt = Text.sanitize(Text.removeFormattingTags(trgt.trim()));

		for (int i = 0; i < client.getPlayers().size(); i++)
		{
			if (client.getPlayers().get(i).getName().equals(trgt))
			{
				DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
				export(client.getPlayers().get(i).getModel(), "Player " + trgt + " " + TIME_FORMAT.format(new Date()) + ".obj");
			}
		}
	}

	public void export(Model model, String name) throws IOException
	{
		String modelData = "mtllib none.mtl";
		modelData += "o " + name + "\n";

		for (int i = 0; i < model.getVerticesCount(); ++i)
		{
			modelData += "v " + model.getVerticesX()[i] + " "
				+ model.getVerticesY()[i] * -1 + " "
				+ model.getVerticesZ()[i] * -1 + "\n";
		}

		for (int i = 0; i < model.getTrianglesCount(); ++i)
		{
			int x = model.getTrianglesX()[i] + 1;
			int y = model.getTrianglesY()[i] + 1;
			int z = model.getTrianglesZ()[i] + 1;

			modelData += "usemtl m" + i + "\n";
			modelData += "f " + x + " " + y + " " + z + "\n";
			modelData += "" + "\n";
		}
		String path = RuneLite.RUNELITE_DIR + "//models//";

		File output = new File(path + name);
		File outputDir = new File(path);
		if(!outputDir.exists())
		{
			outputDir.mkdirs();
		}
		FileWriter writer = new FileWriter(output);

		writer.write(modelData);
		writer.flush();
		writer.close();
	}

	//The following code is taken from the Pet Info plugin by Micro Tavor with permission
	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return;
		}
		else if(client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			addMenus();
		}
	}

	private void addMenus()
	{
		Point mouseCanvasPosition = client.getMouseCanvasPosition();

		List<NPC> petsUnderCursor = getPetsUnderCursor(mouseCanvasPosition);
		if (!petsUnderCursor.isEmpty())
		{
			for (NPC pet : petsUnderCursor)
			{
				addPetInfoMenu(pet);
			}
		}
	}

	private void addPetInfoMenu(NPC pet)
	{
		final MenuEntry exportMenuEntry = new MenuEntry();
		exportMenuEntry.setOption(EXPORT_MODEL);
		exportMenuEntry.setTarget(pet.getName());
		exportMenuEntry.setOpcode(MenuOpcode.RUNELITE.getId());
		exportMenuEntry.setIdentifier(pet.getId());
		exportMenuEntry.setParam1(4);
		addEntry(exportMenuEntry);
	}

	private final List<NPC> pets = new ArrayList<>();

	private List<NPC> getPetsUnderCursor(Point mouseCanvasPosition)
	{
		return pets.stream().filter(p -> {
			return isClickable(p, mouseCanvasPosition);
		}).collect(Collectors.toList());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		Pet pet = Pet.findPet(npc.getId());

		if (pet != null)
		{
			pets.add(npc);
		}
	}

	private boolean isClickable(NPC npc, Point mouseCanvasPosition)
	{
		Shape npcHull = npc.getConvexHull();

		if (npcHull != null)
		{
			return npcHull.contains(mouseCanvasPosition.getX(), mouseCanvasPosition.getY());
		}

		return false;
	}
}
