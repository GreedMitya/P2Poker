package Poker.Game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.files.FileHandle;

public class CardAtlasSplitter extends ApplicationAdapter {

    @Override
    public void create() {
        Texture atlas = new Texture(Gdx.files.internal("sgx/raw/ACards.png"));
        int cardWidth = 225;
        int cardHeight = 315;

        String[] suits = {"Hearts", "Spades", "Diamonds", "Clubs"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        TextureRegion[][] regions = TextureRegion.split(atlas, cardWidth, cardHeight);

        // Подготавливаем данные текстуры
        TextureData textureData = atlas.getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        Pixmap fullPixmap = textureData.consumePixmap();

        for (int row = 0; row < suits.length; row++) {
            for (int col = 0; col < ranks.length; col++) {
                TextureRegion region = regions[row][col];
                int x = region.getRegionX();
                int y = region.getRegionY();

                // Вырезаем нужную часть
                Pixmap cardPixmap = new Pixmap(cardWidth, cardHeight, fullPixmap.getFormat());
                cardPixmap.drawPixmap(fullPixmap, 0, 0, x, y, cardWidth, cardHeight);

                String fileName = "exported_cards/" + ranks[col] + suits[row] + ".png";
                FileHandle file = Gdx.files.local(fileName);
                file.parent().mkdirs();
                PixmapIO.writePNG(file, cardPixmap);
                cardPixmap.dispose();
            }
        }

        fullPixmap.dispose();
        atlas.dispose();
        Gdx.app.exit();
    }


    // Запуск через main метод
    public static void main(String[] args) {
        CardAtlasSplitter splitter = new CardAtlasSplitter();
        splitter.create();
    }
}
