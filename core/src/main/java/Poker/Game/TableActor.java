package Poker.Game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class TableActor extends Actor {
    private final Image tableImage;
    public TableActor(Texture tableTex, float worldW, float worldH) {
        tableImage = new Image(new TextureRegionDrawable(new TextureRegion(tableTex)));
        tableImage.setSize(worldW * 0.9f, worldH * 0.9f);
        tableImage.setPosition(
            (worldW - tableImage.getWidth())/2f + worldW*0.05f,
            (worldH - tableImage.getHeight())/2f - worldH*0.02f
        );
        tableImage.setTouchable(Touchable.disabled);
    }
    @Override public void draw(Batch batch, float parentAlpha) {
        tableImage.draw(batch, parentAlpha);
    }
}
