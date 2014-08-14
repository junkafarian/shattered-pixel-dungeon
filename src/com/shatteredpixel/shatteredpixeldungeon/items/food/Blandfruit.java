package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * Created by debenhame on 12/08/2014.
 */
public class Blandfruit extends Food {

    public String message = "You eat the Blandfruit, bleugh!";
    public String info = "So dry and insubstantial, perhaps cooking could improve it.";

    public Potion potionAttrib = null;
    public ItemSprite.Glowing potionGlow = null;

    {
        name = "Blandfruit";
        stackable = false;
        image = ItemSpriteSheet.BLANDFRUIT;
        energy = (Hunger.STARVING - Hunger.HUNGRY)/2;
    }

    @Override
    public void execute( Hero hero, String action ) {
        if (potionAttrib == null){

            if (action.equals( AC_EAT )) {

                detach(hero.belongings.backpack);

                ((Hunger) hero.buff(Hunger.class)).satisfy(energy);
                GLog.i(message);

                hero.sprite.operate(hero.pos);
                hero.busy();
                SpellSprite.show(hero, SpellSprite.FOOD);
                Sample.INSTANCE.play(Assets.SND_EAT);

                hero.spend(1f);

                Statistics.foodEaten++;
                Badges.validateFoodEaten();
            }

            else super.execute(hero, action);


        } else if (action.equals( AC_EAT )){

            ((Hunger)hero.buff( Hunger.class )).satisfy(Hunger.HUNGRY);

            detach( hero.belongings.backpack );

            hero.spend( 1f );
            hero.busy();

            if (potionAttrib instanceof PotionOfFrost){
                GLog.i( "the Frostfruit takes a bit like Frozen Carpaccio." );
                switch (Random.Int(5)) {
                    case 0:
                        GLog.i( "You see your hands turn invisible!" );
                        Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
                        break;
                    case 1:
                        GLog.i( "You feel your skin harden!" );
                        Buff.affect( hero, Barkskin.class ).level( hero.HT / 4 );
                        break;
                    case 2:
                        GLog.i( "Refreshing!" );
                        Buff.detach( hero, Poison.class );
                        Buff.detach( hero, Cripple.class );
                        Buff.detach( hero, Weakness.class );
                        Buff.detach( hero, Bleeding.class );
                        break;
                    case 3:
                        GLog.i( "You feel better!" );
                        if (hero.HP < hero.HT) {
                            hero.HP = Math.min( hero.HP + hero.HT / 4, hero.HT );
                            hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
                        }
                        break;
                }
            } else
                potionAttrib.apply(hero);

            Sample.INSTANCE.play( Assets.SND_EAT );

            hero.sprite.operate(hero.pos);

            switch (hero.heroClass) {
                case WARRIOR:
                    if (hero.HP < hero.HT) {
                        hero.HP = Math.min( hero.HP + 5, hero.HT );
                        hero.sprite.emitter().burst( Speck.factory(Speck.HEALING), 1 );
                    }
                    break;
                case MAGE:
                    hero.belongings.charge( false );
                    ScrollOfRecharging.charge(hero);
                    break;
                case ROGUE:
                case HUNTRESS:
                    break;
            }

        } else if (action.equals( AC_THROW )){

            if (potionAttrib instanceof PotionOfLiquidFlame ||
                    potionAttrib instanceof PotionOfToxicGas ||
                    potionAttrib instanceof PotionOfParalyticGas ||
                    potionAttrib instanceof PotionOfFrost){
                potionAttrib.execute(hero, action);
                detach( hero.belongings.backpack );
            } else {
                super.execute(hero, action);
            }

        } else {
            super.execute(hero, action);
        }
    }

    @Override
    public String info() {
        return info;
    }

    @Override
    public int price() {
        return 20 * quantity;
    }

    public Item cook(Seed seed){
        Class<? extends Item> plant = seed.alchemyClass;


        try {
            potionAttrib = (Potion)plant.newInstance();
        } catch (Exception e) {
            return null;
        }

        potionAttrib.image = ItemSpriteSheet.BLANDFRUIT;

        if (potionAttrib instanceof PotionOfHealing){

            name = "Healthfruit";
            potionGlow = new ItemSprite.Glowing( 0x2EE62E );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Sungrass seed it was cooked with. It looks delicious and hearty, ready to be eaten!";

        } else if (potionAttrib instanceof PotionOfStrength){

            name = "Powerfruit";
            potionGlow = new ItemSprite.Glowing( 0xCC0022 );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Rotberry seed it was cooked with. It looks delicious and powerful, ready to be eaten!";

        } else if (potionAttrib instanceof PotionOfParalyticGas){

            name = "Paralyzefruit";
            potionGlow = new ItemSprite.Glowing( 0x67583D );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Earthroot seed it was cooked with. It looks firm and volatile, I shouldn't eat this.";

        } else if (potionAttrib instanceof PotionOfInvisibility){

            name = "Invisifruit";
            potionGlow = new ItemSprite.Glowing( 0xE5D273 );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Blindweed seed it was cooked with. It looks delicious and shiny, ready to be eaten!";

        } else if (potionAttrib instanceof PotionOfLiquidFlame){

            name = "Flamefruit";
            potionGlow = new ItemSprite.Glowing( 0xFF7F00 );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Firebloom seed it was cooked with. It looks spicy and volatile, I shouldn't eat this.";

        } else if (potionAttrib instanceof PotionOfFrost){

            name = "Frostfruit";
            potionGlow = new ItemSprite.Glowing( 0x66B3FF );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Icecap seed it was cooked with. It looks delicious and refreshing, ready to be eaten!";

        } else if (potionAttrib instanceof PotionOfMindVision){

            name = "Visionfruit";
            potionGlow = new ItemSprite.Glowing( 0xB8E6CF );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Fadeleaf seed it was cooked with. It looks delicious and shadowy, ready to be eaten!";

        } else if (potionAttrib instanceof PotionOfToxicGas){

            name = "Toxicfruit";
            potionGlow = new ItemSprite.Glowing( 0xA15CE5 );
            info = "The fruit has plumped up from its time soaking in the pot and has even absorbed the properties "+
                    "of the Sorrowmoss seed it was cooked with. It looks crisp and volatile, I shouldn't eat this.";

        }

        return this;
    }

    public static final String NAME = "name";

    @Override
    public void storeInBundle(Bundle bundle){
        super.storeInBundle(bundle);
        bundle.put(NAME name);
    }

    @Override
    public void restoreFromBundle(Bundle bundle){
        super.restoreFromBundle(bundle);
        name = bundle.getString(NAME);

        if (name == "Healthfruit")
            cook(new Sungrass.Seed());
        else if (name == "Powerfruit")
            //TODO: make sure this doesn't break anything
            cook(new Wandmaker.Rotberry.Seed());
        else if (name == "Paralyzefruit")
            cook(new Earthroot.Seed());
        else if (name == "Invisifruit")
            cook(new Blindweed.Seed());
        else if (name == "Flamefruit")
            cook(new Firebloom.Seed());
        else if (name == "Frostfruit")
            cook(new Icecap.Seed());
        else if (name == "Visionfruit")
            cook(new Fadeleaf.Seed());
        else if (name == "Toxicfruit")
            cook(new Sorrowmoss.Seed());

    }


    @Override
    public ItemSprite.Glowing glowing() {
        return potionGlow;
    }

}
