package jd.captcha.pixelgrid;

public class LevenshteinLetter {
    public boolean[][] horizontal;
    public boolean[][] vertical;
    char value;

    public LevenshteinLetter(boolean[][] horizontal, char value) {
        this.horizontal = horizontal;
        int h = horizontal[0].length, w = horizontal.length;

        vertical = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                vertical[y][x] = horizontal[x][y];
            }
        }
        this.value = value;
    }

    public LevenshteinLetter(Letter letter) {
        int w = letter.getWidth(), h = letter.getHeight();
        if (w == 0 || h == 0) return;
        horizontal = new boolean[w][h];
        int avg = (int) (letter.getAverage() * letter.owner.getJas().getDouble("RelativeContrast"));
        for (int x = 0; x < horizontal.length; x++) {
            for (int y = 0; y < horizontal[0].length; y++) {
                horizontal[x][y] = letter.grid[x][y] < avg;
            }
        }
        vertical = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                vertical[y][x] = horizontal[x][y];
            }
        }
        if (letter.getDecodedValue() != null && letter.getDecodedValue().length() > 0) value = letter.getDecodedValue().charAt(0);

    }

    public Letter toLetter() {
        Letter let = new Letter();
        let.grid = new int[getWidth()][getHight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHight(); y++) {
                if (horizontal[x][y])
                    let.grid[x][y] = 0x000000;
                else
                    let.grid[x][y] = 0xffffff;
            }
        }
        let.setDecodedValue("" + value);
        return let;
    }

    public int getWidth() {
        return horizontal.length;
    }

    public int getHight() {
        return vertical.length;
    }
}
