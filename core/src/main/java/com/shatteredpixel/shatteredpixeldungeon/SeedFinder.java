package com.shatteredpixel.shatteredpixeldungeon;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Scanner;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;

public class SeedFinder {
	enum Condition {
		ANY, ALL
	};

	public static class Options {
		public static int floors;
		public static Condition condition;
		public static String itemListFile;
		public static String ouputFile;
		public static long seed;
	}

	public class HeapItem {
		public Item item;
		public Heap heap;

		public HeapItem(Item item, Heap heap) {
			this.item = item;
			this.heap = heap;
		}
	}

	List<Class<? extends Item>> blacklist;
	ArrayList<String> itemList;

	// TODO: make it parse the item list directly from the arguments
	private void setOptions(String[] args) {
		Options.floors = 4;
		Options.condition = Condition.ANY;
		Options.itemListFile = "search_items.txt";
		Options.ouputFile = "seed_items.log";
		if (args.length == 1) {
			Options.seed = DungeonSeed.convertFromText(args[0]);
		}

	}

	private ArrayList<String> getItemList() {
		ArrayList<String> itemList = new ArrayList<>();

		try {
			Scanner scanner = new Scanner(new File(Options.itemListFile));

			while (scanner.hasNextLine()) {
				itemList.add(scanner.nextLine());
			}

			scanner.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return itemList;
	}

	public SeedFinder(String[] args) {
		setOptions(args);

		try {
			Writer outputFile = new FileWriter(Options.ouputFile);
			outputFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// If a seed has been specified, log the items from that seed and exit
		// regardless of search items
		if (args.length == 1) {
			logSeedItems(Long.toString(Options.seed), Options.floors);
			return;
		}

		itemList = getItemList();
		int matchingSeeds = 0;

		// for (int i = 0; i < DungeonSeed.TOTAL_SEEDS; i++) {
		for (int i = 0; i < 5000; i++) {
			if (i % 1000 == 0) {
				System.out.printf("Checked %d seeds, found %d matching seeds\n", i, matchingSeeds);
				matchingSeeds = 0;
			}
			if (testSeed(Integer.toString(i), Options.floors)) {
				matchingSeeds++;
				logSeedItems(Integer.toString(i), Options.floors);
			}
		}
	}

	// Finding and test logic

	private ArrayList<Heap> getMobDrops(Level l) {
		ArrayList<Heap> heaps = new ArrayList<>();

		for (Mob m : l.mobs) {
			if (m instanceof Statue) {
				Heap h = new Heap();
				h.items = new LinkedList<>();
				h.items.add(((Statue) m).weapon.identify());
				h.type = Type.STATUE;
				heaps.add(h);
			}

			else if (m instanceof ArmoredStatue) {
				Heap h = new Heap();
				h.items = new LinkedList<>();
				h.items.add(((ArmoredStatue) m).armor.identify());
				h.items.add(((ArmoredStatue) m).weapon.identify());
				h.type = Type.STATUE;
				heaps.add(h);
			}

			else if (m instanceof Mimic) {
				Heap h = new Heap();
				h.items = new LinkedList<>();

				for (Item item : ((Mimic) m).items)
					h.items.add(item.identify());

				if (m instanceof GoldenMimic)
					h.type = Type.GOLDEN_MIMIC;
				else if (m instanceof CrystalMimic)
					h.type = Type.CRYSTAL_MIMIC;
				else
					h.type = Type.MIMIC;
				heaps.add(h);
			}
		}

		return heaps;
	}

	private boolean testSeed(String seed, int floors) {
		SPDSettings.customSeed(seed);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();

		boolean[] itemsFound = new boolean[itemList.size()];

		for (int i = 0; i < floors; i++) {
			Level l = Dungeon.newLevel();

			ArrayList<Heap> heaps = new ArrayList<>(l.heaps.valueList());
			heaps.addAll(getMobDrops(l));

			for (Heap h : heaps) {
				for (Item item : h.items) {
					item.identify();

					for (int j = 0; j < itemList.size(); j++) {
						Pattern pattern = Pattern.compile(itemList.get(j), Pattern.CASE_INSENSITIVE);
						if (pattern.matcher(item.title()).find()) {
							if (itemsFound[j] == false) {
								itemsFound[j] = true;
								break;
							}
						}
					}
				}
			}

			Dungeon.depth++;
		}

		if (Options.condition == Condition.ANY) {
			for (int i = 0; i < itemList.size(); i++) {
				if (itemsFound[i] == true)
					return true;
			}

			return false;
		}

		else {
			for (int i = 0; i < itemList.size(); i++) {
				if (itemsFound[i] == false)
					return false;
			}

			return true;
		}
	}

	// Formatting and output

	private void logItem(Item item, String found_in, PrintWriter out) {
		out.printf(
				"{\"seed\": \"%s\", \"name\": \"%s\", \"floor\": %d, \"found_in\": \"%s\", \"is_cursed\": %b, \"item_level\": %d, \"type\": \"%s\"}\n",
				DungeonSeed.convertToCode(Dungeon.seed), item.title().toLowerCase(), Dungeon.depth,
				found_in, item.cursed, item.level(), item.getClass().getSimpleName());
	}

	private void logSeedItems(String seed, int floors) {
		PrintWriter out = null;

		try {
			out = new PrintWriter(new FileOutputStream(Options.ouputFile, true));
		} catch (FileNotFoundException e) { // gotta love Java mandatory exceptions
			e.printStackTrace();
		}

		SPDSettings.customSeed(seed);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();

		blacklist = Arrays.asList(Gold.class, Dewdrop.class, IronKey.class, GoldenKey.class, CrystalKey.class,
				EnergyCrystal.class, CorpseDust.class, Embers.class, CeremonialCandle.class, Pickaxe.class);

		for (int i = 0; i < floors; i++) {
			Level l = Dungeon.newLevel();
			ArrayList<Heap> heaps = new ArrayList<>(l.heaps.valueList());

			// list quest rewards
			if (Ghost.Quest.armor != null) {
				logItem(Ghost.Quest.armor.identify(), "Ghost", out);
				logItem(Ghost.Quest.weapon.identify(), "Ghost", out);
				Ghost.Quest.complete();
			}

			if (Wandmaker.Quest.wand1 != null) {
				logItem(Wandmaker.Quest.wand1.identify(), "Wandmaker", out);
				logItem(Wandmaker.Quest.wand2.identify(), "Wandmaker", out);
				Wandmaker.Quest.complete();
			}

			if (Imp.Quest.reward != null) {
				logItem(Imp.Quest.reward.identify(), "Imp", out);
				Imp.Quest.complete();
			}

			heaps.addAll(getMobDrops(l));

			// list items
			for (Heap heap : heaps) {
				for (Item item : heap.items) {
					item.identify();

					if (heap.type == Type.FOR_SALE)
						continue;
					else if (blacklist.contains(item.getClass()))
						continue;
					else
						logItem(item, heap.title().toLowerCase(), out);
				}
			}

			Dungeon.depth++;
		}

		out.close();
	}

}
